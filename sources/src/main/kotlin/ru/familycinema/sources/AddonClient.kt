package ru.familycinema.sources

import java.net.URLEncoder
import kotlinx.serialization.json.*

private fun JsonObject.str(k: String) = (get(k) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.arr(k: String) = get(k) as? JsonArray ?: JsonArray(emptyList())

data class CatalogSpec(val type: String, val id: String, val search: Boolean)

data class Manifest(
    val id: String,
    val name: String,
    val version: String,
    val catalogs: List<CatalogSpec>,
    val resources: Set<String>,
)

data class RemoteEpisode(val id: String, val title: String, val season: Int, val number: Int)

data class RemoteWork(
    val id: String,
    val type: String,
    val title: String,
    val original: String,
    val aliases: String,
    val description: String,
    val year: Int?,
    val age: Int?,
    val genres: String,
    val poster: String?,
    val background: String?,
    val episodes: List<RemoteEpisode>,
)

data class RemoteStream(val title: String, val url: String?, val infoHash: String?)

data class RemoteSubtitle(val id: String, val lang: String, val url: String)

/** Stremio protocol implementation; no Node runtime or provider code is executed. */
class AddonClient(
    rawEndpoint: String,
    private val fetch: (String) -> String = { SafeNetwork.get(it) },
) {
    val base = rawEndpoint.trim().removeSuffix("manifest.json").trimEnd('/') + "/"

    init {
        SafeNetwork.validate(base)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun get(path: String) =
        Json.parseToJsonElement(fetch(base + path)) as? JsonObject
            ?: error("Источник вернул неверный JSON")

    fun manifest(): Manifest {
        val j = get("manifest.json")
        val resources =
            j.arr("resources")
                .mapNotNull {
                    if (it is JsonObject) it.str("name") else (it as? JsonPrimitive)?.contentOrNull
                }
                .toSet()
        val catalogs =
            j.arr("catalogs")
                .take(30)
                .mapNotNull { it as? JsonObject }
                .mapNotNull { c ->
                    val type = c.str("type") ?: return@mapNotNull null
                    val id = c.str("id") ?: return@mapNotNull null
                    if (type !in setOf("movie", "series")) return@mapNotNull null
                    CatalogSpec(
                        type,
                        id,
                        c.arr("extra").any { it is JsonObject && it.str("name") == "search" } ||
                            c.arr("extraSupported").any {
                                (it as? JsonPrimitive)?.contentOrNull == "search"
                            },
                    )
                }
        require(catalogs.isNotEmpty()) { "Нет поддерживаемых каталогов movie/series" }
        return Manifest(
            j.str("id") ?: error("Нет ID источника"),
            j.str("name") ?: error("Нет имени источника"),
            j.str("version") ?: "?",
            catalogs,
            resources,
        )
    }

    private fun work(j: JsonObject, fallbackType: String): RemoteWork? {
        val id = j.str("id") ?: return null
        val name = j.str("name") ?: return null
        val episodes =
            j.arr("videos")
                .take(1000)
                .mapNotNull { it as? JsonObject }
                .mapNotNull { v ->
                    val vid = v.str("id") ?: return@mapNotNull null
                    val season = v.str("season")?.toIntOrNull() ?: return@mapNotNull null
                    val num = v.str("episode")?.toIntOrNull() ?: return@mapNotNull null
                    RemoteEpisode(vid, v.str("title") ?: "$season:$num", season, num)
                }
        return RemoteWork(
            id,
            j.str("type") ?: fallbackType,
            name,
            j.str("originalName") ?: "",
            j.arr("alternativeNames")
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .joinToString(" | "),
            j.str("description") ?: "",
            j.str("releaseInfo")?.take(4)?.toIntOrNull(),
            j.str("ageRating")?.toIntOrNull()?.takeIf { it in 0..21 },
            j.arr("genres").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.joinToString(", "),
            j.str("poster"),
            j.str("background"),
            episodes,
        )
    }

    fun catalog(c: CatalogSpec, query: String = ""): List<RemoteWork> {
        val extra =
            if (query.isNotBlank()) {
                require(c.search)
                "/search=${enc(query.take(200))}"
            } else ""
        return get("catalog/${enc(c.type)}/${enc(c.id)}$extra.json")
            .arr("metas")
            .take(100)
            .mapNotNull { (it as? JsonObject)?.let { o -> work(o, c.type) } }
    }

    fun meta(type: String, id: String): RemoteWork? =
        (get("meta/${enc(type)}/${enc(id)}.json")["meta"] as? JsonObject)?.let { work(it, type) }

    fun streams(type: String, id: String): List<RemoteStream> =
        get("stream/${enc(type)}/${enc(id)}.json")
            .arr("streams")
            .take(100)
            .mapNotNull { it as? JsonObject }
            .map {
                RemoteStream(
                    it.str("title") ?: it.str("name") ?: "Видео",
                    it.str("url"),
                    it.str("infoHash"),
                )
            }

    fun subtitles(type: String, id: String): List<RemoteSubtitle> =
        get("subtitles/${enc(type)}/${enc(id)}.json")
            .arr("subtitles")
            .take(100)
            .mapNotNull { it as? JsonObject }
            .mapNotNull { j ->
                val url = j.str("url") ?: return@mapNotNull null
                RemoteSubtitle(j.str("id") ?: url, j.str("lang") ?: "?", url)
            }
}
