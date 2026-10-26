package ru.familycinema.sources

import java.net.*
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Explicit opt-in integration test over the managed Cloud HTTPS proxy. Tests the Kotlin protocol
 * client against a real hosted owned-content fixture. This injected transport is restricted to this
 * one host and is NOT the app transport.
 */
class LiveProtocolTest {
    @Test
    fun actualHostedManifestSearchMetadataStreamAndBytes() {
        assumeTrue(System.getenv("FAMILY_CINEMA_LIVE_QA") == "1")
        System.getenv("FAMILY_CINEMA_QA_TRUSTSTORE")?.let {
            System.setProperty("javax.net.ssl.trustStore", it)
        }
        val proxyUri =
            URI(
                System.getenv("HTTPS_PROXY")
                    ?: System.getenv("https_proxy")
                    ?: error("Managed proxy required")
            )
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyUri.host, proxyUri.port))
        fun read(raw: String): ByteArray {
            val uri = URI(raw)
            require(uri.scheme == "https" && uri.host == "raw.githubusercontent.com")
            val connection = uri.toURL().openConnection(proxy) as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            try {
                assertEquals(200, connection.responseCode)
                return connection.inputStream.use { src ->
                    val out = java.io.ByteArrayOutputStream()
                    val b = ByteArray(8192)
                    while (true) {
                        val n = src.read(b)
                        if (n < 0) break
                        require(out.size() + n <= 2 * 1024 * 1024)
                        out.write(b, 0, n)
                    }
                    out.toByteArray()
                }
            } finally {
                connection.disconnect()
            }
        }
        val client =
            AddonClient(
                "https://raw.githubusercontent.com/VasiliiBabkin/family-cinema/codex/player-fullscreen-0.2/docs/qa-addon/manifest.json"
            ) {
                read(it).toString(Charsets.UTF_8)
            }
        val manifest = client.manifest()
        assertTrue(manifest.resources.contains("stream"))
        val works = client.catalog(manifest.catalogs.single(), "Тихий")
        assertEquals("quiet-evening", works.single().id)
        assertEquals(0, client.meta("movie", "quiet-evening")!!.age)
        val stream = client.streams("movie", "quiet-evening").single()
        val data = read(stream.url!!)
        assertEquals(
            "8bac7b321a48c45a6ba7bd67a2df2745f68d97430dc0ea0134a55c7c7235051a",
            MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) },
        )
    }
}
