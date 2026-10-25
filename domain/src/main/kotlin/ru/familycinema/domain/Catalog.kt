package ru.familycinema.domain

data class VideoVersion(
    val id: String,
    val uri: String,
    val sha256: String,
    val episode: Int? = null,
) {
    val fingerprint: String
        get() = "$id|$uri|$sha256|$episode"
}

data class Film(
    val id: String,
    val title: String,
    val description: String,
    val age: Int,
    val license: String,
    val version: VideoVersion,
)

data class Approval(val filmId: String, val fingerprint: String)

fun isApproved(film: Film, approvals: Set<Approval>) =
    Approval(film.id, film.version.fingerprint) in approvals

interface CatalogRepository {
    fun films(): List<Film>
}
