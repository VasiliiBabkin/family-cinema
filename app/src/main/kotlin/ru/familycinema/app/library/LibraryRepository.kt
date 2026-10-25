package ru.familycinema.app.library

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.*
import okhttp3.Request
import ru.familycinema.app.DemoCatalog
import ru.familycinema.domain.*
import ru.familycinema.parental.FamilyStore
import ru.familycinema.sources.*

data class Transfer(val bytes: Long = 0, val total: Long = -1)

class LibraryRepository(val context: Context) {
    private val transferState = kotlinx.coroutines.flow.MutableStateFlow(Transfer())
    val transfer: kotlinx.coroutines.flow.StateFlow<Transfer> = transferState

    fun resetTransfer() {
        transferState.value = Transfer()
    }

    val db = CinemaDatabase.get(context)
    val dao = db.library()
    private val vault = SourceVault()
    val media = File(context.filesDir, "media").apply { mkdirs() }
    private val settings = context.getSharedPreferences("library_settings", 0)
    var budget: Long
        get() = settings.getLong("budget", 2L * 1024 * 1024 * 1024)
        set(v) {
            require(v in 256L * 1024 * 1024..4L * 1024 * 1024 * 1024)
            settings.edit().putLong("budget", v).apply()
        }

    fun used() = media.listFiles()?.sumOf { it.length() } ?: 0L

    fun digest(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    fun fingerprint(v: Version) =
        digest(
            "${v.workId}|${v.id}|${v.sourceId}|${v.episodeId}|${v.sha256}|${digest(vault.open(v.locator))}"
        )

    fun cancelNetworking() {
        SafeNetwork.client.dispatcher.cancelAll()
        SafeNetwork.client.connectionPool.evictAll()
    }

    suspend fun initialize() =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                if (dao.source("local") == null)
                    dao.source(
                        Source(
                            "local",
                            context.getString(ru.familycinema.app.R.string.local_source),
                            vault.seal("asset:///"),
                            rights = true,
                            capabilities =
                                context.getString(
                                    ru.familycinema.app.R.string.local_source_capabilities
                                ),
                        )
                    )
                if (dao.profile() == null)
                    dao.profile(
                        Profile()
                    ) // New Room policy always OFF, ignores old preliminary toggle.
                if (dao.work("demo") == null) {
                    val film = DemoCatalog.films().first()
                    val store = FamilyStore(context)
                    dao.work(
                        Work(
                            "demo",
                            "local",
                            "demo",
                            "movie",
                            film.title,
                            description = film.description,
                            age = 0,
                            attribution = "Собственное видео · CC0",
                        )
                    )
                    val v =
                        Version(
                            "demo",
                            "demo",
                            sourceId = "local",
                            title = "Оригинал · 960×540",
                            locator = vault.seal(film.version.uri),
                            sha256 = film.version.sha256,
                            verified = true,
                            kind = "asset",
                        )
                    dao.version(v)
                    if (store.approved(film))
                        dao.decision(
                            Decision(
                                versionId = "demo",
                                fingerprint = fingerprint(v),
                                allowed = true,
                                origin = "migration",
                            )
                        )
                    if (store.position(film.id) > 0)
                        dao.progress(
                            Progress(versionId = "demo", position = store.position(film.id))
                        )
                    dao.audit(
                        Audit(
                            event = "Миграция 0.2",
                            versionId = "demo",
                            detail =
                                "PIN сохранён в прежнем хранилище; разрешение и прогресс перенесены; автоодобрение выключено",
                        )
                    )
                }
            }
        }

    suspend fun profile() = dao.profile() ?: Profile()

    fun allowed(v: Version, w: Work, p: Profile, d: Decision?) =
        v.verified &&
            (v.id != "demo" || FamilyStore(context).approved(DemoCatalog.films().first())) &&
            v.sha256.length == 64 &&
            w.age != null &&
            w.age <= p.ageLimit &&
            d?.allowed == true &&
            d.fingerprint == fingerprint(v)

    suspend fun allowed(v: Version): Boolean {
        val w = dao.work(v.workId) ?: return false
        return allowed(v, w, profile(), dao.decision(v.id))
    }

    suspend fun visible(parent: Boolean): List<Work> {
        val all = dao.works()
        if (parent) return all
        return all.filter { w -> dao.versions(w.id).any { allowed(it) } }
    }

    suspend fun syncDemo(allow: Boolean, position: Long? = null) {
        val v = dao.version("demo") ?: return
        if (position != null) dao.progress(Progress(versionId = "demo", position = position))
        else decide(v, allow)
    }

    suspend fun decide(v: Version, allow: Boolean) =
        db.withTransaction {
            val current = dao.version(v.id) ?: error("Версия удалена")
            require(!allow || current.verified) { "Сначала проверьте файл" }
            dao.decision(
                Decision(
                    versionId = v.id,
                    fingerprint = fingerprint(current),
                    allowed = allow,
                    manualDeny = !allow,
                    contentHash = current.sha256,
                )
            )
            dao.audit(
                Audit(
                    event = if (allow) "Одобрено вручную" else "Запрещено вручную",
                    versionId = v.id,
                    detail = "Конкретный fingerprint: ${fingerprint(current).take(16)}",
                )
            )
            if (v.id == "demo") FamilyStore(context).approve(DemoCatalog.films().first(), allow)
        }

    suspend fun auto(v: Version) =
        db.withTransaction {
            val p = profile()
            val w = dao.work(v.workId) ?: return@withTransaction
            val source = dao.source(v.sourceId)
            val old = dao.decision(v.id)
            val contentDenied =
                dao.decisions().any {
                    it.manualDeny && it.contentHash.isNotEmpty() && it.contentHash == v.sha256
                }
            val eligibility =
                ApprovalInput(
                    p.autoApprove,
                    source?.trusted == true,
                    source?.rights == true,
                    v.verified &&
                        (v.sourceId == "local" ||
                            v.expectedSha256.isNotBlank() && v.expectedSha256 == v.sha256),
                    w.age,
                    p.ageLimit,
                    old?.manualDeny == true || contentDenied,
                )
            if (ApprovalPolicy.allows(eligibility) && !allowed(v, w, p, old)) {
                dao.decision(
                    Decision(
                        versionId = v.id,
                        fingerprint = fingerprint(v),
                        allowed = true,
                        origin = "automatic",
                        contentHash = v.sha256,
                    )
                )
                dao.audit(
                    Audit(
                        event = "Одобрено автоматически",
                        versionId = v.id,
                        detail =
                            "Доверенный источник, возраст ${w.age}, проверенный SHA-256 ${v.sha256.take(16)}",
                    )
                )
            } else
                dao.audit(
                    Audit(
                        event = "Автоодобрение: пропуск",
                        versionId = v.id,
                        detail =
                            "Включено=${p.autoApprove}; доверие=${source?.trusted==true}; права=${source?.rights==true}; возраст=${w.age ?: "неизвестен"}; запрет=${old?.manualDeny==true}; проверено=${v.verified}",
                    )
                )
        }

    suspend fun addSource(endpoint: String, rights: Boolean): Source =
        withContext(Dispatchers.IO) {
            require(rights) { "Подтвердите право использовать источник" }
            val client = AddonClient(endpoint)
            val manifest = client.manifest()
            val id = digest(client.base)
            val prior = dao.source(id)
            val source =
                Source(
                    id,
                    manifest.name,
                    vault.seal(client.base),
                    trusted = prior?.trusted ?: false,
                    rights = rights,
                    capabilities =
                        "${manifest.version} · ${manifest.resources.joinToString()} · ${manifest.catalogs.size} каталогов",
                    checked = System.currentTimeMillis(),
                )
            source
        }

    fun client(source: Source) = AddonClient(vault.open(source.endpoint))

    suspend fun cache(source: Source, remote: RemoteWork): Work =
        withContext(Dispatchers.IO) {
            val id =
                if (remote.id.matches(Regex("tt[0-9]{5,12}")))
                    digest("external|${remote.type}|${remote.id}")
                else digest("${source.id}|${remote.type}|${remote.id}")
            val old = dao.work(id)
            val previousVersions = if (old != null) dao.versions(id) else emptyList()
            val protectedVersions = previousVersions.filter { dao.decision(it.id)?.allowed == true }
            val protectedEpisodes = protectedVersions.map { it.episodeId }.toSet()
            val proposed =
                Work(
                    id,
                    source.id,
                    remote.id,
                    remote.type,
                    remote.title,
                    remote.original,
                    remote.aliases,
                    remote.description,
                    remote.year,
                    remote.genres,
                    old?.age ?: remote.age,
                    old?.poster?.takeIf { it.startsWith(context.filesDir.absolutePath) }
                        ?: remote.poster?.let {
                            runCatching {
                                    "remote:" + vault.seal(SafeNetwork.validate(it).toString())
                                }
                                .getOrNull()
                        },
                    remote.background?.let {
                        runCatching { "remote:" + vault.seal(SafeNetwork.validate(it).toString()) }
                            .getOrNull()
                    },
                    source.title,
                )
            val work = if (protectedVersions.isNotEmpty()) old!! else proposed
            db.withTransaction {
                dao.work(work)
                dao.link(WorkSource(id, source.id, remote.id))
                remote.episodes.forEach { e ->
                    val episodeId = digest("$id|${e.id}")
                    if (episodeId !in protectedEpisodes)
                        dao.episode(Episode(episodeId, id, e.id, e.season, e.number, e.title))
                }
            }
            work
        }

    suspend fun cachePoster(work: Work): Work =
        withContext(Dispatchers.IO) {
            val stored = work.poster ?: return@withContext work
            if (stored.startsWith(context.filesDir.absolutePath)) return@withContext work
            val url =
                if (stored.startsWith("remote:")) vault.open(stored.removePrefix("remote:"))
                else stored
            if (dao.versions(work.id).any { dao.decision(it.id)?.allowed == true })
                return@withContext work
            val source = dao.source(work.sourceId) ?: return@withContext work
            require(source.rights)
            val dir = File(context.filesDir, "posters").apply { mkdirs() }
            val bytes =
                SafeNetwork.client
                    .newCall(Request.Builder().url(SafeNetwork.validate(url)).build())
                    .execute()
                    .use { response ->
                        require(response.isSuccessful)
                        val input = response.body?.byteStream() ?: error("Пустая обложка")
                        input.use { src ->
                            val out = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = src.read(buffer)
                                if (n < 0) break
                                require(out.size() + n <= 2 * 1024 * 1024)
                                out.write(buffer, 0, n)
                            }
                            out.toByteArray()
                        }
                    }
            require(bytes.size <= 2 * 1024 * 1024)
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            require(opts.outWidth in 1..8192 && opts.outHeight in 1..8192)
            while (
                (dir.listFiles()?.sumOf { it.length() } ?: 0L) + bytes.size > 32L * 1024 * 1024
            ) {
                dir.listFiles()?.minByOrNull { it.lastModified() }?.delete() ?: break
            }
            val file = File(dir, digest(url) + ".image")
            file.writeBytes(bytes)
            val result = work.copy(poster = file.absolutePath)
            dao.work(result)
            result
        }

    suspend fun candidates(work: Work, episode: Episode?): List<Version> =
        withContext(Dispatchers.IO) {
            val links =
                dao.links(work.id).ifEmpty {
                    listOf(WorkSource(work.id, work.sourceId, work.remoteId))
                }
            val result = mutableListOf<Version>()
            var errors = 0
            for (source in
                dao.sources().filter { it.enabled && it.id !in setOf("local", "direct") }) {
                try {
                    val addon = client(source)
                    val manifest = addon.manifest()
                    if ("stream" !in manifest.resources) continue
                    if (manifest.types.isNotEmpty() && work.type !in manifest.types) continue
                    val link = links.find { it.sourceId == source.id }
                    val shared =
                        work.remoteId.matches(Regex("tt[0-9]+")) ||
                            manifest.idPrefixes.any { work.remoteId.startsWith(it) }
                    if (link == null && !shared) continue
                    val remoteId =
                        if (episode == null) link?.remoteId ?: work.remoteId
                        else if (link != null) episode.remoteId
                        else "${work.remoteId}:${episode.season}:${episode.number}"
                    addon.streams(work.type, remoteId).forEach { stream ->
                        val locator = stream.url ?: "magnet:?xt=urn:btih:${stream.infoHash}"
                        result +=
                            Version(
                                digest(
                                    "candidate|${work.id}|${episode?.id}|${source.id}|$locator|${stream.expectedSha256}"
                                ),
                                work.id,
                                episode?.id ?: "",
                                source.id,
                                stream.title,
                                vault.seal(locator),
                                kind = if (stream.url != null) "https" else "torrent",
                                expectedSha256 = stream.expectedSha256,
                            )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors++
                }
            }
            if (result.isEmpty() && errors > 0) throw java.io.IOException("Источники недоступны")
            result
        }

    // Keep a conservative physical reserve; do not reclaim other apps' caches automatically.
    @android.annotation.SuppressLint("UsableSpace")
    private suspend fun copy(input: InputStream, total: Long = -1): Pair<File, String> =
        withContext(Dispatchers.IO) {
            transferState.value = Transfer(0, total)
            val temp = File.createTempFile("incoming-", ".part", media)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var bytes = 0L
                input.use { src ->
                    temp.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = src.read(buffer)
                            if (n < 0) break
                            bytes += n
                            require(used() + n <= budget && media.usableSpace > 64L * 1024 * 1024) {
                                "Недостаточно места. Увеличьте лимит или удалите неразрешённые копии"
                            }
                            out.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            if (bytes % (256 * 1024) < n)
                                transferState.value = Transfer(bytes, total)
                        }
                    }
                }
                transferState.value = Transfer(bytes, total)
                require(bytes > 0) { "Пустой файл" }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                val file = File(media, "$hash.media")
                if (file.exists()) temp.delete() else check(temp.renameTo(file))
                Pair(file, hash)
            } catch (t: Throwable) {
                temp.delete()
                throw t
            }
        }

    suspend fun verify(version: Version): Version =
        withContext(Dispatchers.IO) {
            if (version.verified) return@withContext version
            require(version.kind == "https") { "Торрент-движок в этой сборке не подключён" }
            val url = SafeNetwork.validate(vault.open(version.locator))
            SafeNetwork.mediaClient.newCall(Request.Builder().url(url).build()).execute().use {
                response ->
                require(response.isSuccessful) { "Файл недоступен: HTTP ${response.code}" }
                val body = response.body ?: error("Нет видео")
                require(body.contentLength() <= budget - used()) {
                    "Файл превышает свободный лимит"
                }
                val (file, sha) = copy(body.byteStream(), body.contentLength())
                require(version.expectedSha256.isBlank() || sha == version.expectedSha256) {
                    "Содержимое не соответствует заявленному SHA-256. Разрешение не выдано"
                }
                try {
                    inspectVideo(file)
                } catch (t: Throwable) {
                    if (
                        dao.works()
                            .flatMap { dao.versions(it.id) }
                            .none { it.file == file.absolutePath }
                    )
                        file.delete()
                    throw t
                }
                version.copy(
                    id =
                        digest(
                            "verified|${version.workId}|${version.episodeId}|${version.sourceId}|${vault.open(version.locator)}|$sha"
                        ),
                    file = file.absolutePath,
                    sha256 = sha,
                    verified = true,
                    bytes = file.length(),
                )
            }
        }

    private fun inspectVideo(file: File) {
        val prefix =
            file.inputStream().use { input ->
                val b = ByteArray(512)
                val n = input.read(b)
                if (n > 0)
                    b.copyOf(n).toString(Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t')
                else ""
            }
        require(!prefix.startsWith("#EXTM3U") && !prefix.startsWith("<")) {
            "Нужен самостоятельный видеофайл; манифесты не импортируются"
        }
        val r = android.media.MediaMetadataRetriever()
        try {
            r.setDataSource(file.absolutePath)
            require(
                r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) ==
                    "yes"
            ) {
                "Нет поддерживаемого видео. HLS/DRM не импортируются как файл"
            }
        } finally {
            r.release()
        }
    }

    private fun localPoster(file: File, id: String): String? {
        if (android.os.Build.VERSION.SDK_INT < 27) return null
        return runCatching {
                val dir = File(context.filesDir, "posters").apply { mkdirs() }
                val output = File(dir, "$id.jpg")
                if (output.exists()) return@runCatching output.absolutePath
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    val duration =
                        retriever
                            .extractMetadata(
                                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                            )
                            ?.toLongOrNull() ?: 0L
                    val bitmap =
                        retriever.getScaledFrameAtTime(
                            duration * 100,
                            android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            512,
                            320,
                        ) ?: return@runCatching null
                    try {
                        output.outputStream().use {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, it)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                } finally {
                    retriever.release()
                }
                var size = dir.listFiles()?.sumOf { it.length() } ?: 0L
                dir.listFiles()
                    ?.filter { it != output }
                    ?.sortedBy { it.lastModified() }
                    ?.forEach { old ->
                        if (size > 32L * 1024 * 1024) {
                            val bytes = old.length()
                            if (old.delete()) size -= bytes
                        }
                    }
                output.absolutePath
            }
            .getOrNull()
    }

    suspend fun importLocal(
        uri: Uri,
        title: String,
        age: Int?,
        series: Boolean,
        season: Int,
        number: Int,
    ): Work =
        withContext(Dispatchers.IO) {
            require(uri.scheme == "content")
            val input = context.contentResolver.openInputStream(uri) ?: error("Файл недоступен")
            val (file, hash) = copy(input)
            inspectVideo(file)
            val id = digest("local|$title|${if(series)"series" else hash}")
            val work =
                dao.work(id)
                    ?: Work(
                        id,
                        "local",
                        id,
                        if (series) "series" else "movie",
                        title,
                        age = age,
                        poster = localPoster(file, id),
                        attribution = "Личный файл · добавлен родителем",
                    )
            val episode =
                if (series)
                    Episode(
                        digest("$id|$season|$number"),
                        id,
                        "$season:$number",
                        season,
                        number,
                        "Серия $number",
                    )
                else null
            val v =
                Version(
                    digest("$id|${episode?.id}|$hash"),
                    id,
                    episode?.id ?: "",
                    "local",
                    context.getString(ru.familycinema.app.R.string.personal_video_version),
                    vault.seal(uri.toString()),
                    file.absolutePath,
                    hash,
                    true,
                    file.length(),
                    "file",
                )
            db.withTransaction {
                dao.work(work)
                if (episode != null) dao.episode(episode)
                dao.version(v)
                auto(v)
                dao.audit(
                    Audit(
                        event = "Локальный импорт",
                        versionId = v.id,
                        detail = "Проверенная приватная копия, SHA-256 ${hash.take(16)}",
                    )
                )
            }
            work
        }

    suspend fun addDirect(url: String, title: String, age: Int?): Work {
        SafeNetwork.validate(url)
        val id = digest("direct|$url")
        val w =
            Work(id, "direct", id, "movie", title, age = age, attribution = "Личная HTTPS-ссылка")
        dao.source(
            Source(
                "direct",
                "Личные HTTPS-ссылки",
                vault.seal("https://example.org/"),
                rights = true,
            )
        )
        dao.work(w)
        dao.version(
            Version(id, id, sourceId = "direct", title = "HTTPS-видео", locator = vault.seal(url))
        )
        return w
    }

    suspend fun streamingFilm(v: Version): Film {
        require(v.kind == "https")
        val url = vault.open(v.locator)
        SafeNetwork.validate(url)
        val w = dao.work(v.workId) ?: error("Произведение не найдено")
        return Film(
            v.id,
            w.title,
            w.description,
            w.age ?: 0,
            w.attribution,
            VideoVersion("preview:${v.id}", url, ""),
        )
    }

    suspend fun film(v: Version): Film =
        withContext(Dispatchers.IO) {
            val w = dao.work(v.workId) ?: error("Произведение не найдено")
            require(v.verified)
            val uri =
                if (v.kind == "asset") vault.open(v.locator)
                else {
                    val file = File(v.file)
                    require(file.canonicalFile.parentFile == media.canonicalFile && file.exists()) {
                        "Локальная копия недоступна"
                    }
                    Uri.fromFile(file).toString()
                }
            Film(
                if (v.id == "demo") "quiet-evening" else v.id,
                w.title,
                w.description,
                w.age ?: 0,
                w.attribution,
                VideoVersion(v.id, uri, v.sha256),
            )
        }

    suspend fun verifyFilm(film: Film): Boolean =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(film.version.uri)
            val input =
                when (uri.scheme) {
                    "asset" -> {
                        require(uri.toString() == "asset:///quiet-evening.mp4")
                        context.assets.open("quiet-evening.mp4")
                    }
                    "file" -> {
                        val f = File(uri.path!!)
                        require(f.canonicalFile.parentFile == media.canonicalFile)
                        f.inputStream()
                    }
                    else -> return@withContext false
                }
            input.use { stream ->
                val d = MessageDigest.getInstance("SHA-256")
                val buf = ByteArray(65536)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = stream.read(buf)
                    if (n < 0) break
                    d.update(buf, 0, n)
                }
                d.digest().joinToString("") { "%02x".format(it) } == film.version.sha256
            }
        }

    suspend fun cleanup(): Int =
        withContext(Dispatchers.IO) {
            val keep =
                dao.works()
                    .flatMap { dao.versions(it.id) }
                    .filter { dao.decision(it.id)?.allowed == true }
                    .map { it.file }
                    .toSet()
            var count = 0
            media
                .listFiles()
                ?.filter { it.absolutePath !in keep }
                ?.forEach { if (it.delete()) count++ }
            // Invalidate removed copies, preserving exact approval tombstones.
            dao.works().forEach { w ->
                dao.versions(w.id)
                    .filter { it.file.isNotEmpty() && !File(it.file).exists() }
                    .forEach {
                        dao.version(it.copy(verified = false, file = "", sha256 = "", bytes = 0))
                    }
            }
            count
        }
}
