package ru.familycinema.sources

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.*

/** Client-side protocol contracts, independent of any particular media provider. */
data class AddonCatalogItem(
    val sourceId: String,
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
)

data class AddonStream(
    val sourceId: String,
    val name: String?,
    val url: String?,
    val infoHash: String?,
    val fileIndex: Int?,
)

/** Reject insecure URLs, userinfo, fragments and IP-literal endpoints. */
class AddonEndpoint private constructor(val base: URI) {
    companion object {
        fun parse(raw: String): AddonEndpoint {
            val uri = URI(raw.trim()).normalize()
            val host = uri.host ?: throw IllegalArgumentException("Missing host")
            require(uri.scheme.equals("https", ignoreCase = true))
            require(uri.userInfo == null && uri.fragment == null && uri.query == null)
            require(uri.port == -1 || uri.port == 443)
            require(host.matches(Regex("[A-Za-z0-9.-]+")) && host.contains('.'))
            require(!host.matches(Regex("[0-9.]+")))
            require(!host.endsWith(".local", ignoreCase = true))
            require(!host.endsWith(".internal", ignoreCase = true))
            require(!host.equals("localhost", ignoreCase = true))
            require(!host.endsWith(".localhost", ignoreCase = true))
            require(!host.endsWith(".test", ignoreCase = true))
            require(!host.endsWith(".invalid", ignoreCase = true))
            require(!host.endsWith(".example", ignoreCase = true))
            return AddonEndpoint(uri.toString().trimEnd('/').let { URI(it + "/") })
        }
    }

    fun resource(vararg segments: String): URI {
        require(segments.isNotEmpty())
        val path =
            segments.joinToString("/") {
                require(it.isNotBlank() && it != "." && it != "..")
                URLEncoder.encode(it, StandardCharsets.UTF_8).replace("+", "%20")
            }
        return base.resolve(path)
    }
}

interface AddonTransport {
    suspend fun getJson(uri: URI): String
}

/**
 * Read-only Stremio addon protocol client. Does not load arbitrary manifest URLs, download streams,
 * or start BitTorrent. Transport must independently enforce redirect, DNS/IP, response-size and
 * timeout policy.
 */
class AddonProtocol(
    private val sourceId: String,
    private val endpoint: AddonEndpoint,
    private val transport: AddonTransport,
) {
    private fun JsonElement.obj(): JsonObject =
        this as? JsonObject ?: throw IllegalArgumentException("Expected JSON object")

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    suspend fun search(type: String, catalogId: String, query: String): List<AddonCatalogItem> {
        require(type in setOf("movie", "series", "channel", "tv"))
        require(query.isNotBlank() && query.length <= 200)
        val encodedQuery =
            URLEncoder.encode(query.trim(), StandardCharsets.UTF_8).replace("+", "%20")
        val request =
            URI(
                endpoint.resource("catalog", type, catalogId).toString() +
                    "/search=" +
                    encodedQuery +
                    ".json"
            )
        // The Stremio protocol requires a literal search= prefix in this segment.
        val json = Json.parseToJsonElement(transport.getJson(request)).obj()
        val items = json["metas"] as? JsonArray ?: return emptyList()
        return items.take(100).mapNotNull {
            val obj = it as? JsonObject ?: return@mapNotNull null
            val id = obj.string("id")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = obj.string("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            AddonCatalogItem(sourceId, id, obj.string("type") ?: type, name, obj.string("poster"))
        }
    }

    suspend fun streams(type: String, videoId: String): List<AddonStream> {
        require(type in setOf("movie", "series", "channel", "tv"))
        val request = endpoint.resource("stream", type, "$videoId.json")
        val json = Json.parseToJsonElement(transport.getJson(request)).obj()
        val streams = json["streams"] as? JsonArray ?: return emptyList()
        return streams.take(100).mapNotNull {
            val obj = it as? JsonObject ?: return@mapNotNull null
            val url = obj.string("url")
            val hash = obj.string("infoHash")
            if (url == null && hash == null) return@mapNotNull null
            val fileIdx = (obj["fileIdx"] as? JsonPrimitive)?.intOrNull
            AddonStream(sourceId, obj.string("name"), url, hash, fileIdx)
        }
    }
}
