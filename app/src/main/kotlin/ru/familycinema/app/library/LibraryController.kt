package ru.familycinema.app.library

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import ru.familycinema.domain.Film
import ru.familycinema.sources.SearchKeys

/** UI state belongs to the Activity's retained ViewModel; Room owns durable data. */
class LibraryController(
    app: Application,
    private val scope: CoroutineScope,
    private val parent: () -> Boolean,
    private val play: (Film, Boolean, Long) -> Unit,
) {
    val repo = LibraryRepository(app)
    var ready by mutableStateOf(false)
        private set

    var busy by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    var works by mutableStateOf<List<Work>>(emptyList())
        private set

    var sources by mutableStateOf<List<Source>>(emptyList())
        private set

    var profile by mutableStateOf(Profile())
        private set

    var selected by mutableStateOf<Work?>(null)
        private set

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var episode by mutableStateOf<Episode?>(null)
        private set

    var versions by mutableStateOf<List<Version>>(emptyList())
        private set

    var exactGrants by mutableStateOf<Set<String>>(emptySet())
        private set

    var decisions by mutableStateOf<Map<String, Decision>>(emptyMap())
        private set

    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set

    var history by mutableStateOf<List<Progress>>(emptyList())
        private set

    var audit by mutableStateOf<List<Audit>>(emptyList())
        private set

    var query by mutableStateOf("")
    var onlyFavorites by mutableStateOf(false)
    var onlyHistory by mutableStateOf(false)
    private var job: Job? = null
    private var generation = 0
    private var operation = 0

    init {
        scope.launch {
            try {
                repo.initialize()
                ready = true
                refresh()
            } catch (e: Exception) {
                message = "Не удалось открыть локальную библиотеку"
            }
        }
    }

    fun lock(clearSelection: Boolean = true) {
        generation++
        job?.cancel()
        repo.cancelNetworking()
        works = emptyList()
        sources = emptyList()
        audit = emptyList()
        busy = false
        message = ""
        if (clearSelection) selected = null
        episodes = emptyList()
        versions = emptyList()
        exactGrants = emptySet()
        query = ""
        scope.launch { refresh() }
    }

    private fun action(requireParent: Boolean = true, block: suspend () -> Unit) {
        if (requireParent && !parent()) return
        job?.cancel()
        repo.cancelNetworking()
        val captured = generation
        val currentOperation = ++operation
        job =
            scope.launch {
                busy = true
                repo.resetTransfer()
                message = ""
                try {
                    block()
                    currentCoroutineContext().ensureActive()
                    if (captured == generation) refresh()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message =
                        when {
                            e is java.net.UnknownHostException ||
                                e is java.net.SocketTimeoutException ->
                                "Источник недоступен. Проверьте подключение. Локальная библиотека работает без интернета."
                            e is javax.net.ssl.SSLException ->
                                "Не удалось проверить защищённое соединение"
                            e is IllegalArgumentException ->
                                "Не удалось проверить адрес или файл. Нужен публичный HTTPS-источник, поддерживаемое видео и свободное место в дисковом лимите."
                            else ->
                                "Не удалось выполнить действие. Источник может быть недоступен или формат не поддерживается."
                        }
                } finally {
                    if (captured == generation && currentOperation == operation) busy = false
                }
            }
    }

    fun pickerUnavailable() {
        message = repo.context.getString(ru.familycinema.app.R.string.picker_unavailable)
    }

    fun pickerExpired() {
        message = repo.context.getString(ru.familycinema.app.R.string.picker_expired)
    }

    fun cancel() {
        ++operation
        job?.cancel()
        repo.cancelNetworking()
        busy = false
        message = repo.context.getString(ru.familycinema.app.R.string.operation_cancelled)
    }

    suspend fun refresh() {
        val captured = generation
        val role = parent()
        val p = repo.profile()
        val visible = repo.visible(role)
        val sourceList = if (role) repo.dao.sources() else emptyList()
        val decisionMap = repo.dao.decisions().associateBy { it.versionId }
        val favoriteIds = repo.dao.favorites().map { it.workId }.toSet()
        val progressList = repo.dao.history()
        val historyIds = progressList.mapNotNull { repo.dao.version(it.versionId)?.workId }.toSet()
        val auditList = if (role) repo.dao.audit() else emptyList()
        val selection = selected?.let { old -> visible.find { it.id == old.id } }
        val allEpisodes = selection?.let { repo.dao.episodes(it.id) } ?: emptyList()
        val versionList =
            selection?.let { repo.dao.versions(it.id).filter { role || repo.allowed(it) } }
                ?: emptyList()
        val exact =
            withContext(Dispatchers.IO) {
                versionList
                    .filter { v ->
                        val d = decisionMap[v.id]
                        d?.allowed == true && d.fingerprint == repo.fingerprint(v)
                    }
                    .map { it.id }
                    .toSet()
            }
        val permittedEpisodes = versionList.map { it.episodeId }.toSet()
        val episodeList =
            if (role) allEpisodes else allEpisodes.filter { it.id in permittedEpisodes }
        val progressIds =
            progressList
                .filter { it.position > 1000 }
                .mapNotNull { repo.dao.version(it.versionId)?.workId }
                .toSet()
        currentCoroutineContext().ensureActive()
        if (captured != generation || role != parent()) return
        inProgressWorkIds = progressIds
        profile = p
        works = visible
        sources = sourceList
        exactGrants = exact
        decisions = decisionMap
        favorites = favoriteIds
        history = progressList
        historyWorkIds = historyIds
        audit = auditList
        selected = selection
        episodes = episodeList
        versions = versionList
    }

    fun reload() = action(false) { refresh() }

    fun filtered(): List<Work> {
        return works.filter {
            SearchKeys.matches(query, it.title, it.original, it.aliases) &&
                (!onlyFavorites || it.id in favorites) &&
                (!onlyHistory || it.id in historyWorkIds)
        }
    }

    var inProgressWorkIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var historyWorkIds by mutableStateOf<Set<String>>(emptySet())
        private set

    fun updateHistoryIndex() =
        scope.launch {
            historyWorkIds =
                repo.dao.history().mapNotNull { repo.dao.version(it.versionId)?.workId }.toSet()
        }

    fun select(w: Work) =
        action(false) {
            if (!parent() && repo.visible(false).none { it.id == w.id }) return@action
            selected = w
            episode = null
            refresh()
            updateHistoryIndex()
        }

    fun selectEpisode(e: Episode) {
        episode = e
    }

    fun addSource(url: String, rights: Boolean) = action {
        val result = repo.addSource(url, rights)
        currentCoroutineContext().ensureActive()
        check(parent())
        repo.dao.source(result)
        message = "Источник подключён. Доверие для автоодобрения включается отдельно."
    }

    fun sourcePolicy(s: Source, trust: Boolean = s.trusted, enabled: Boolean = s.enabled) = action {
        repo.dao.source(s.copy(trusted = trust, enabled = enabled))
        repo.dao.audit(
            Audit(
                event = "Политика источника",
                versionId = s.id,
                detail = "Доверие=$trust; включён=$enabled",
            )
        )
    }

    fun discover(search: Boolean) = action {
        var successes = 0
        var failures = 0
        for (source in
            repo.dao.sources().filter { it.enabled && it.id !in setOf("direct", "local") }) {
            try {
                val items =
                    withContext(Dispatchers.IO) {
                        val client = repo.client(source)
                        val manifest = client.manifest()
                        manifest.catalogs
                            .filter { !search || it.search }
                            .flatMap { client.catalog(it, if (search) query else "") }
                    }
                currentCoroutineContext().ensureActive()
                check(parent())
                items.forEach { repo.cache(source, it) }
                successes++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures++
            }
        }
        message =
            if (successes == 0 && failures > 0) "Источники недоступны. Показан локальный кеш."
            else "Обновлено источников: $successes. Недоступно: $failures."
        updateHistoryIndex()
    }

    fun details() = action {
        val w = selected ?: return@action
        val source = repo.dao.source(w.sourceId) ?: return@action
        val meta = withContext(Dispatchers.IO) { repo.client(source).meta(w.type, w.remoteId) }
        currentCoroutineContext().ensureActive()
        check(parent())
        if (meta != null) {
            selected = repo.cache(source, meta)
            selected = runCatching { repo.cachePoster(selected!!) }.getOrDefault(selected)
        }
    }

    fun findVersions() = action {
        val w = selected ?: return@action
        if (w.type == "series" && episode == null) {
            message = "Выберите сезон и серию"
            return@action
        }
        val candidates = repo.candidates(w, episode)
        currentCoroutineContext().ensureActive()
        check(parent())
        candidates.forEach { if (repo.dao.version(it.id) == null) repo.dao.version(it) }
        message =
            if (candidates.isEmpty()) "Источник не вернул доступных версий"
            else "Найдено версий: ${candidates.size}"
    }

    fun verify(v: Version) = action {
        val verified = repo.verify(v)
        currentCoroutineContext().ensureActive()
        check(parent())
        repo.dao.version(verified)
        if (
            v.id != verified.id &&
                repo.dao.decision(v.id) == null &&
                repo.dao.progress(v.id) == null
        )
            repo.dao.removeCandidate(v.id)
        repo.auto(verified)
        message = "Файл проверен и сохранён для просмотра без интернета"
    }

    fun decide(v: Version, allow: Boolean) = action { repo.decide(v, allow) }

    fun preview(v: Version) = action {
        val film = repo.streamingFilm(v)
        currentCoroutineContext().ensureActive()
        check(parent())
        play(film, false, 0L)
    }

    fun launch(v: Version) =
        action(false) {
            val current = repo.dao.version(v.id) ?: return@action
            val allowed = repo.allowed(current)
            check(parent() || allowed)
            val film = repo.film(current)
            currentCoroutineContext().ensureActive()
            check(parent() || repo.allowed(current))
            play(film, allowed, if (parent()) 0L else repo.dao.progress(current.id)?.position ?: 0L)
        }

    fun favorite(w: Work) =
        action(false) {
            check(parent() || repo.visible(false).any { it.id == w.id })
            if (w.id in favorites) repo.dao.unfavorite(w.id)
            else repo.dao.favorite(Favorite(workId = w.id))
        }

    fun setPolicy(age: Int, auto: Boolean) = action {
        require(age in 0..21)
        repo.dao.profile(profile.copy(ageLimit = age, autoApprove = auto))
        repo.dao.audit(
            Audit(
                event = "Политика ребёнка",
                versionId = "child",
                detail = "Возрастной предел=$age; автоодобрение=$auto",
            )
        )
    }

    fun sweepAuto() = action {
        for (w in repo.dao.works()) for (v in repo.dao.versions(w.id)) {
            currentCoroutineContext().ensureActive()
            check(parent())
            repo.auto(v)
        }
    }

    fun editMetadata(work: Work) = action {
        require(work.title.isNotBlank() && work.title.length <= 300)
        require(work.age == null || work.age in 0..21)
        repo.dao.work(work)
        selected = work
    }

    fun editAge(w: Work, age: Int?) = action {
        require(age == null || age in 0..21)
        repo.dao.work(w.copy(age = age))
        selected = w.copy(age = age)
    }

    fun import(uri: Uri, title: String, age: Int?, series: Boolean, season: Int, number: Int) =
        action {
            require(title.isNotBlank())
            selected = repo.importLocal(uri, title, age, series, season, number)
            currentCoroutineContext().ensureActive()
            check(parent())
            message = "Файл добавлен. Выберите версию и разрешите её ребёнку."
        }

    fun direct(url: String, title: String, age: Int?) = action {
        require(title.isNotBlank())
        selected = repo.addDirect(url, title, age)
        message = "Ссылка добавлена. Сначала скачайте и проверьте файл."
    }

    fun cleanup() = action {
        val n = repo.cleanup()
        message = "Удалено неразрешённых копий: $n"
    }

    fun setBudget(gb: Int) = action { repo.budget = gb.toLong() * 1024 * 1024 * 1024 }

    fun checkpoint(id: String, position: Long) {
        scope.launch {
            if (!ready) return@launch
            val key = if (id == "quiet-evening") "demo" else id
            if (position > 0 || repo.dao.progress(key) != null)
                repo.dao.progress(Progress(versionId = key, position = position))
        }
    }
}
