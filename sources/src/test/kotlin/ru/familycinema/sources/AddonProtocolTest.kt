package ru.familycinema.sources

import java.net.URI
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AddonProtocolTest {
    @Test fun rejectsUnsafeEndpoints() {
        listOf("http://example.com", "https://127.0.0.1", "https://user:pw@example.com",
            "https://localhost", "https://service.local", "https://example.com:8080").forEach {
            try { AddonEndpoint.parse(it); fail("Accepted $it") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun searchParsesResultsAndEncodesRussian() = runBlocking {
        var requested: URI? = null
        val transport = object : AddonTransport {
            override suspend fun getJson(uri: URI): String {
                requested = uri
                return """{"metas":[{"id":"movie:1","type":"movie","name":"Ёжик","poster":"https://images.example.org/a.jpg"}]}"""
            }
        }
        val client = AddonProtocol("trusted", AddonEndpoint.parse("https://addon.example.org"), transport)
        val result = client.search("movie", "public", "ёжик в тумане")
        assertEquals("Ёжик", result.single().name)
        assertTrue(requested.toString().contains("%D1%91"))
        assertTrue(requested.toString().contains("catalog/movie/public/"))
    }

    @Test fun streamsRemainUntrustedCandidates() = runBlocking {
        val transport = object : AddonTransport {
            override suspend fun getJson(uri: URI) =
                """{"streams":[{"name":"HD","url":"https://video.example.org/a.mp4"},{"infoHash":"abc","fileIdx":2}]}"""
        }
        val client = AddonProtocol("test", AddonEndpoint.parse("https://addon.example.org"), transport)
        val result = client.streams("movie", "id")
        assertEquals(2, result.size)
        assertEquals("abc", result[1].infoHash)
        assertNull(result[1].url)
    }
}
