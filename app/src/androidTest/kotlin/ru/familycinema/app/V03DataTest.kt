package ru.familycinema.app

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import ru.familycinema.app.library.*

class V03DataTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun roomMigrationPreservesWorkAndCreatesAudit() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema =
            JSONObject(
                    InstrumentationRegistry.getInstrumentation()
                        .context
                        .assets
                        .open("ru.familycinema.app.library.CinemaDatabase/2.json")
                        .bufferedReader()
                        .readText()
                )
                .getJSONObject("database")
                .getJSONArray("entities")
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file, null)
        for (i in 0 until schema.length()) {
            val entity = schema.getJSONObject(i)
            val table = entity.getString("tableName")
            if (table == "audit") continue
            sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
            for (j in 0 until indices.length()) sqlite.execSQL(
                indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)
            )
        }
        val values =
            ContentValues().apply {
                put("id", "retained")
                put("sourceId", "local")
                put("remoteId", "retained")
                put("type", "movie")
                put("title", "Сохранённый фильм")
                put("original", "")
                put("aliases", "")
                put("description", "")
                put("genres", "")
                put("attribution", "")
                put("updated", 0L)
            }
        sqlite.insertOrThrow("works", null, values)
        sqlite.version = 1
        sqlite.close()
        val db =
            Room.databaseBuilder(context, CinemaDatabase::class.java, name)
                .addMigrations(
                    CinemaDatabase.MIGRATION_1_2,
                    CinemaDatabase.MIGRATION_2_3,
                    CinemaDatabase.MIGRATION_3_4,
                )
                .build()
        assertEquals("Сохранённый фильм", db.library().works().single().title)
        db.library().audit(Audit(event = "migration", versionId = "retained", detail = "ok"))
        assertEquals(1, db.library().audit().size)
        db.close()
        context.deleteDatabase(name)
        Unit
    }

    @Test
    fun persistentExactApprovalsTombstonesAndOfflineCopy() = runBlocking {
        val repo = LibraryRepository(context)
        withContext(Dispatchers.IO) { repo.db.clearAllTables() }
        repo.initialize()
        assertFalse(repo.profile().autoApprove)
        val work = Work("test-film", "trusted", "one", "movie", "Ёжик", age = 6)
        val vault = SourceVault()
        repo.dao.work(work)
        repo.dao.source(
            Source(
                "trusted",
                "Owned source",
                vault.seal("https://example.org/"),
                trusted = true,
                rights = true,
            )
        )
        repo.dao.profile(Profile(ageLimit = 12, autoApprove = true))
        val media = File(repo.media, "test-copy.media")
        context.assets.open("quiet-evening.mp4").use { src ->
            media.outputStream().use { src.copyTo(it) }
        }
        var v =
            Version(
                "test-v",
                "test-film",
                sourceId = "trusted",
                title = "Версия",
                locator = vault.seal("https://example.org/video.mp4"),
                file = media.absolutePath,
                sha256 = GeneratedDemo.sha256,
                verified = true,
            )
        repo.dao.version(v)
        repo.auto(v)
        assertTrue(repo.allowed(v))
        assertEquals("automatic", repo.dao.decision(v.id)!!.origin)
        repo.decide(v, false)
        repo.auto(v)
        assertFalse(repo.allowed(v))
        assertTrue(repo.dao.decision(v.id)!!.manualDeny)
        val relinked =
            v.copy(
                id = "same-bytes-new-link",
                locator = vault.seal("https://example.org/another.mp4"),
            )
        repo.dao.version(relinked)
        repo.auto(relinked)
        assertFalse(repo.allowed(relinked))
        repo.decide(v, true)
        assertTrue(repo.allowed(v))
        repo.dao.profile(repo.profile().copy(autoApprove = false))
        repo.auto(v)
        assertTrue(repo.allowed(v))
        repo.dao.work(work.copy(age = 16))
        assertFalse(repo.allowed(v))
        repo.dao.work(work)
        val changed = v.copy(locator = vault.seal("https://example.org/replaced.mp4"))
        repo.dao.version(changed)
        assertFalse(repo.allowed(changed))
        repo.dao.version(v)
        repo.dao.progress(Progress(versionId = v.id, position = 12345))
        assertEquals(12345L, repo.dao.progress(v.id)!!.position)
        assertTrue(repo.verifyFilm(repo.film(v)))
        media.appendText("changed")
        assertFalse(repo.verifyFilm(repo.film(v)))
        assertTrue(repo.dao.audit().any { it.event == "Запрещено вручную" })
        media.delete()
        withContext(Dispatchers.IO) { repo.db.clearAllTables() }
    }
}
