package ru.familycinema.sources

import java.net.InetAddress
import org.junit.Assert.*
import org.junit.Test

class PolicySecurityTest {
    @Test
    fun automaticRequiresEveryCondition() {
        val good = ApprovalInput(true, true, true, true, 6, 12, false)
        assertTrue(ApprovalPolicy.allows(good))
        listOf(
                good.copy(enabled = false),
                good.copy(trusted = false),
                good.copy(rights = false),
                good.copy(verified = false),
                good.copy(age = null),
                good.copy(age = 16),
                good.copy(age = -1),
                good.copy(denied = true),
            )
            .forEach { assertFalse(ApprovalPolicy.allows(it)) }
    }

    @Test
    fun russianAndAliases() {
        assertTrue(SearchKeys.matches("Ежик", "Ёжик в тумане"))
        assertTrue(SearchKeys.matches("tikhiy", "Тихий вечер"))
        assertTrue(SearchKeys.matches("big buck", "Кролик", "Big Buck Bunny"))
        assertFalse(SearchKeys.matches("фильм", "мультфильм"))
    }

    @Test
    fun unsafeUrlsFailClosed() {
        listOf(
                "http://example.org/a",
                "https://127.0.0.1/a",
                "https://10.1.2.3/a",
                "https://[::1]/a",
                "https://2130706433/a",
                "https://u:p@example.org/",
                "https://example.org:8443/",
                "https://localhost/a",
                "https://x.local/a",
                "https://example.org/a#token",
            )
            .forEach { url -> assertTrue(url, runCatching { SafeNetwork.validate(url) }.isFailure) }
        assertEquals(
            "example.org",
            SafeNetwork.validate("https://example.org/movie.mp4?access=secret").host,
        )
    }

    @Test
    fun privateAndMixedResolutionCannotConnect() {
        listOf(
                "0.0.0.0",
                "127.0.0.1",
                "10.2.3.4",
                "172.16.1.2",
                "192.168.1.1",
                "169.254.169.254",
                "100.64.0.1",
                "198.18.0.1",
                "224.1.1.1",
                "::1",
                "fc00::1",
                "fe80::1",
                "::ffff:127.0.0.1",
            )
            .forEach { ip -> assertFalse(ip, SafeNetwork.publicAddress(InetAddress.getByName(ip))) }
        assertTrue(SafeNetwork.publicAddress(InetAddress.getByName("1.1.1.1")))
        assertTrue(SafeNetwork.publicAddress(InetAddress.getByName("2606:4700:4700::1111")))
    }

    @Test
    fun realProtocolShapeAndEncodedQuery() {
        val requests = mutableListOf<String>()
        val client =
            AddonClient("https://example.org/secret/manifest.json") { url ->
                requests += url
                when {
                    url.endsWith("manifest.json") ->
                        """{"id":"own","name":"Own","version":"1","resources":["catalog",{"name":"stream"}],"catalogs":[{"type":"movie","id":"films","extra":[{"name":"search"}]}]}"""
                    url.contains("catalog/") ->
                        """{"metas":[{"id":"one","name":"Ёжик","type":"movie","ageRating":"6"}]}"""
                    url.contains("stream/") ->
                        """{"streams":[{"name":"HD","url":"https://media.example.org/v.mp4"},{"infoHash":"abc"}]}"""
                    else ->
                        """{"subtitles":[{"id":"ru","lang":"rus","url":"https://example.org/ru.srt"}]}"""
                }
            }
        val m = client.manifest()
        assertEquals(setOf("catalog", "stream"), m.resources)
        val works = client.catalog(m.catalogs.first(), "ёжик / &")
        assertEquals(6, works.first().age)
        assertTrue(requests.last().contains("/search=%D1%91%D0%B6%D0%B8%D0%BA%20%2F%20%26.json"))
        assertEquals(2, client.streams("movie", "one").size)
        assertEquals("rus", client.subtitles("movie", "one").first().lang)
    }
}
