package ru.familycinema.sources

import java.net.InetAddress
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Resolution returned here is the resolution OkHttp connects to. No second DNS lookup. */
object SafeNetwork {
    fun publicAddress(a: InetAddress): Boolean {
        if (
            a.isAnyLocalAddress ||
                a.isLoopbackAddress ||
                a.isLinkLocalAddress ||
                a.isSiteLocalAddress ||
                a.isMulticastAddress
        )
            return false
        val b = a.address.map { it.toInt() and 255 }
        if (b.size == 4)
            return !(b[0] in listOf(0, 10, 127) ||
                b[0] >= 224 ||
                b[0] == 169 && b[1] == 254 ||
                b[0] == 172 && b[1] in 16..31 ||
                b[0] == 192 && b[1] in listOf(0, 168) ||
                b[0] == 100 && b[1] in 64..127 ||
                b[0] == 198 && b[1] in 18..19)
        // Only global-unicast IPv6; excludes ULA, link-local, mapped and NAT64 addresses.
        return b.size == 16 &&
            b[0] and 0xe0 == 0x20 &&
            !(b[0] == 0x20 && b[1] == 0x01 && b[2] == 0x0d && b[3] == 0xb8)
    }

    fun validate(raw: String): HttpUrl {
        require(raw.length <= 8192)
        val u = raw.toHttpUrlOrNull() ?: throw IllegalArgumentException("Некорректный адрес")
        require(
            u.isHttps &&
                u.port == 443 &&
                u.username.isEmpty() &&
                u.password.isEmpty() &&
                u.fragment == null
        ) {
            "Нужен HTTPS без логина, нестандартного порта и фрагмента"
        }
        require(!u.host.matches(Regex("[0-9.]+")) && !u.host.contains(':')) {
            "IP-адреса запрещены"
        }
        require(
            u.host.contains('.') &&
                !u.host.endsWith(".local") &&
                !u.host.endsWith(".internal") &&
                !u.host.endsWith(".localhost")
        )
        return u
    }

    val dns =
        object : Dns {
            override fun lookup(host: String): List<InetAddress> =
                Dns.SYSTEM.lookup(host).also { addresses ->
                    require(addresses.isNotEmpty() && addresses.all(::publicAddress)) {
                        "Локальные и служебные адреса запрещены"
                    }
                }
        }
    val client: OkHttpClient =
        OkHttpClient.Builder()
            .dns(dns)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                validate(chain.request().url.toString())
                val r = chain.proceed(chain.request())
                if (r.code in 300..399) {
                    r.close()
                    throw java.io.IOException("Перенаправления запрещены")
                }
                r
            }
            .build()

    fun get(raw: String, max: Int = 2 * 1024 * 1024): String {
        val request =
            Request.Builder().url(validate(raw)).header("Accept", "application/json").build()
        client.newCall(request).execute().use { r ->
            check(r.isSuccessful) { "Источник ответил HTTP ${r.code}" }
            val body = r.body ?: error("Пустой ответ")
            require(body.contentLength() <= max) { "Ответ слишком большой" }
            body.byteStream().use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    require(out.size() + n <= max)
                    out.write(buf, 0, n)
                }
                return out.toString("UTF-8")
            }
        }
    }
}
