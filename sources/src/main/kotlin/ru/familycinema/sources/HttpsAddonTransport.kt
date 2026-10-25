package ru.familycinema.sources

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bounded read-only transport for parent-configured HTTPS addon hosts.
 *
 * Network DNS validation and connection resolution are not atomic here.
 * Consequently this is NOT sufficient for hostile or arbitrary user-supplied
 * endpoints. Only use with explicitly trusted, operator-controlled hosts;
 * production hardening needs DNS-pinned transport and redirect checks.
 */
class HttpsAddonTransport(
    private val trustedHosts: Set<String>,
    private val maxBytes: Int = 512 * 1024,
) : AddonTransport {
    init {
        require(maxBytes in 1024..(2 * 1024 * 1024))
        require(trustedHosts.isNotEmpty())
    }

    override suspend fun getJson(uri: URI): String = withContext(Dispatchers.IO) {
        require(uri.scheme.equals("https", ignoreCase = true))
        require(uri.userInfo == null && uri.fragment == null)
        val host = uri.host ?: throw IllegalArgumentException("Host required")
        require(host.lowercase() in trustedHosts.map(String::lowercase))
        require(uri.port == -1 || uri.port == 443)
        val resolved = InetAddress.getAllByName(host)
        require(resolved.isNotEmpty() && resolved.all(::isPublicAddress)) {
            "Host resolves to non-public address"
        }
        val connection = URL(uri.toString()).openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Unexpected HTTP status"
            }
            require(connection.contentLengthLong <= maxBytes || connection.contentLengthLong < 0)
            connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    require(out.size() + n <= maxBytes) { "Response too large" }
                    out.write(buf, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isPublicAddress(address: InetAddress): Boolean =
        !(address.isAnyLocalAddress || address.isLoopbackAddress ||
          address.isLinkLocalAddress || address.isSiteLocalAddress ||
          address.isMulticastAddress)
}
