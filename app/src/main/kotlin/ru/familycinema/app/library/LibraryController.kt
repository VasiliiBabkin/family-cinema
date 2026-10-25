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
        query = ""
        scope.launch { refresh() }
    }

    private fun action(requireParent: Boolean = true, block: suspend () -> Unit) {
        if (requireParent && !parent()) return
        job?.cancel()
        val captured = generation
        job =
            scope.launch {
                busy = true
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
                                e.message?.takeIf { !it.contains("http") && !it.contains("/") }
                                    ?: "Неподдерживаемый или небезопасный адрес/файл"
                            else ->
                                "Не удалось выполнить действие. Источник может быть недоступен или формат не поддерживается."
                        }
                } finally {
                    if (captured == generation) busy = false
                }
            }
    }

    suspend fun refresh() {
        profile = repo.profile()
        works = repo.visible(parent())
        sources = if (parent()) repo.dao.sources() else emptyList()
        decisions = repo.dao.decisions().associateBy { it.versionId }
        favorites = repo.dao.favorites().map { it.workId }.toSet()
        history = repo.dao.history()
        historyWorkIds = history.mapNotNull { repo.dao.version(it.versionId)?.workId }.toSet()
        audit = if (parent()) repo.dao.audit() else emptyList()
        selected?.let { old ->
            selected = works.find { it.id == old.id }
            if (selected != null) {
                episodes = repo.dao.episodes(old.id)
                versions = repo.dao.versions(old.id).filter { parent() || repo.allowed(it) }
            } else {
                versions = emptyList()
                episodes = emptyList()
            }
        }
    }

    fun reload() = action(false) { refresh() }

    fun filtered(): List<Work> {
        return works.filter {
            SearchKeys.matches(query, it.title, it.original, it.aliases) &&
                (!onlyFavorites || it.id in favorites) &&
                (!onlyHistory || it.id in historyWorkIds)
        }
    }

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
        for (source in repo.dao.sources().filter { it.enabled && it.id != "direct" }) {
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
        repo.auto(verified)
        message = "Файл проверен и сохранён для просмотра без интернета"
    }

    fun decide(v: Version, allow: Boolean) = action { repo.decide(v, allow) }

    fun launch(v: Version) =
        action(false) {
            val current = repo.dao.version(v.id) ?: return@action
            val allowed = repo.allowed(current)
            check(parent() || allowed)
            val film = repo.film(current)
            currentCoroutineContext().ensureActive()
            check(parent() || repo.allowed(current))
            play(film, allowed, repo.dao.progress(current.id)?.position ?: 0L)
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
            if (ready)
                repo.dao.progress(
                    Progress(
                        versionId = if (id == "quiet-evening") "demo" else id,
                        position = position,
                    )
                )
        }
    }
}
