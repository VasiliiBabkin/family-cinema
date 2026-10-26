package ru.familycinema.sources

import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Legacy interface backed by the same DNS-pinned bounded network stack. */
class HttpsAddonTransport(
    private val trustedHosts: Set<String>,
    private val maxBytes: Int = 512 * 1024,
) : AddonTransport {
    override suspend fun getJson(uri: URI): String =
        withContext(Dispatchers.IO) {
            require(uri.host.lowercase() in trustedHosts.map(String::lowercase))
            SafeNetwork.get(uri.toString(), maxBytes)
        }
}
