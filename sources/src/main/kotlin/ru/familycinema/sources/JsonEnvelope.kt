package ru.familycinema.sources

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Resource bounds before the standard JSON parser, not an alternative JSON implementation. */
internal object JsonEnvelope {
    fun parse(raw: String): JsonElement {
        require(raw.length <= 2 * 1024 * 1024) { "Ответ источника слишком большой" }
        var depth = 0
        var quoted = false
        var escaped = false
        for (ch in raw) {
            if (quoted) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true else if (ch == '"') quoted = false
            } else
                when (ch) {
                    '"' -> quoted = true
                    '{',
                    '[' -> {
                        depth++
                        require(depth <= 64) { "Ответ источника слишком сложный" }
                    }
                    '}',
                    ']' -> depth--
                }
        }
        return Json.parseToJsonElement(raw)
    }
}
