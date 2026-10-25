package ru.familycinema.sources

import java.text.Normalizer
import java.util.Locale

object SearchKeys {
    private val cyr = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
    private val lat =
        listOf(
            "a",
            "b",
            "v",
            "g",
            "d",
            "e",
            "e",
            "zh",
            "z",
            "i",
            "y",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "r",
            "s",
            "t",
            "u",
            "f",
            "kh",
            "ts",
            "ch",
            "sh",
            "shch",
            "",
            "y",
            "",
            "e",
            "yu",
            "ya",
        )

    fun normalize(s: String) =
        Normalizer.normalize(s, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    fun latin(s: String) =
        normalize(s)
            .map { ch -> cyr.indexOf(ch).let { if (it < 0) ch.toString() else lat[it] } }
            .joinToString("")

    fun matches(query: String, vararg titles: String): Boolean {
        val q = latin(query)
        return q.isBlank() ||
            titles.any { t ->
                val v = latin(t)
                q.split(' ').all { token -> v.split(' ').any { it.startsWith(token) } }
            }
    }
}

data class ApprovalInput(
    val enabled: Boolean,
    val trusted: Boolean,
    val rights: Boolean,
    val verified: Boolean,
    val age: Int?,
    val limit: Int,
    val denied: Boolean,
)

object ApprovalPolicy {
    fun allows(p: ApprovalInput) =
        p.enabled &&
            p.trusted &&
            p.rights &&
            p.verified &&
            p.age != null &&
            p.age in 0..p.limit &&
            !p.denied
}
