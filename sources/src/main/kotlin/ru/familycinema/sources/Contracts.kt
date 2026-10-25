package ru.familycinema.sources
import ru.familycinema.domain.Film
import java.net.URI

data class SourceConfig(val id: String, val endpoint: URI, val credentialAlias: String?)
data class SearchQuery(val russianTitle: String, val year: Int? = null, val season: Int? = null, val episode: Int? = null)
interface SourceAdapter { val id: String; suspend fun search(query: SearchQuery): List<Film> }
interface TorznabAdapter : SourceAdapter { val configuration: SourceConfig }
data class TorrentFile(val index: Int, val path: String, val sizeBytes: Long)
data class TorrentStatus(val bufferedBytes: Long, val peers: Int, val error: String?)
interface TorrentEngine : AutoCloseable {
 suspend fun inspect(input: URI): List<TorrentFile>
 suspend fun start(fileIndex: Int, diskBudgetBytes: Long): URI
 suspend fun seek(positionBytes: Long)
 suspend fun status(): TorrentStatus
 fun cancel()
}
// Контракты будущих интеграций. Реализаций Torznab и P2P в этапе 1 нет.
