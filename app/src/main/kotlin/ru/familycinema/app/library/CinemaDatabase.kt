package ru.familycinema.app.library

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "works")
data class Work(
    @PrimaryKey val id: String,
    val sourceId: String,
    val remoteId: String,
    val type: String,
    val title: String,
    val original: String = "",
    val aliases: String = "",
    val description: String = "",
    val year: Int? = null,
    val genres: String = "",
    val age: Int? = null,
    val poster: String? = null,
    val background: String? = null,
    val attribution: String = "",
    val updated: Long = System.currentTimeMillis(),
)

@Entity(tableName = "episodes", indices = [Index("workId")])
data class Episode(
    @PrimaryKey val id: String,
    val workId: String,
    val remoteId: String,
    val season: Int,
    val number: Int,
    val title: String,
)

@Entity(tableName = "versions", indices = [Index("workId"), Index("episodeId")])
data class Version(
    @PrimaryKey val id: String,
    val workId: String,
    val episodeId: String = "",
    val sourceId: String,
    val title: String,
    val locator: String,
    val file: String = "",
    val sha256: String = "",
    val verified: Boolean = false,
    val bytes: Long = 0,
    val kind: String = "https",
)

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey val id: String,
    val title: String,
    val endpoint: String,
    val trusted: Boolean = false,
    val rights: Boolean = false,
    val enabled: Boolean = true,
    val capabilities: String = "",
    val checked: Long = 0,
)

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String = "child",
    val name: String = "Ребёнок",
    val ageLimit: Int = 12,
    val autoApprove: Boolean = false,
)

@Entity(tableName = "decisions", primaryKeys = ["profileId", "versionId"])
data class Decision(
    val profileId: String = "child",
    val versionId: String,
    val fingerprint: String,
    val allowed: Boolean,
    val manualDeny: Boolean = false,
    val origin: String = "manual",
    val at: Long = System.currentTimeMillis(),
)

@Entity(tableName = "progress", primaryKeys = ["profileId", "versionId"])
data class Progress(
    val profileId: String = "child",
    val versionId: String,
    val position: Long,
    val updated: Long = System.currentTimeMillis(),
)

@Entity(tableName = "favorites", primaryKeys = ["profileId", "workId"])
data class Favorite(val profileId: String = "child", val workId: String)

@Entity(tableName = "audit")
data class Audit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val event: String,
    val versionId: String,
    val detail: String,
    val at: Long = System.currentTimeMillis(),
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM works ORDER BY title COLLATE NOCASE") fun observeWorks(): Flow<List<Work>>

    @Query("SELECT * FROM works ORDER BY title COLLATE NOCASE") suspend fun works(): List<Work>

    @Query("SELECT * FROM works WHERE id=:id") suspend fun work(id: String): Work?

    @Upsert suspend fun work(value: Work)

    @Query("SELECT * FROM versions WHERE workId=:workId")
    suspend fun versions(workId: String): List<Version>

    @Query("SELECT * FROM versions WHERE id=:id") suspend fun version(id: String): Version?

    @Upsert suspend fun version(value: Version)

    @Upsert suspend fun source(value: Source)

    @Query("SELECT * FROM sources ORDER BY title") suspend fun sources(): List<Source>

    @Query("SELECT * FROM sources WHERE id=:id") suspend fun source(id: String): Source?

    @Upsert suspend fun episode(value: Episode)

    @Query("SELECT * FROM episodes WHERE workId=:id ORDER BY season,number")
    suspend fun episodes(id: String): List<Episode>

    @Upsert suspend fun profile(value: Profile)

    @Query("SELECT * FROM profiles WHERE id='child'") suspend fun profile(): Profile?

    @Upsert suspend fun decision(value: Decision)

    @Query("SELECT * FROM decisions WHERE profileId='child' AND versionId=:id")
    suspend fun decision(id: String): Decision?

    @Query("SELECT * FROM decisions WHERE profileId='child'")
    suspend fun decisions(): List<Decision>

    @Upsert suspend fun progress(value: Progress)

    @Query("SELECT * FROM progress WHERE profileId='child' ORDER BY updated DESC")
    suspend fun history(): List<Progress>

    @Query("SELECT * FROM progress WHERE profileId='child' AND versionId=:id")
    suspend fun progress(id: String): Progress?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun favorite(value: Favorite)

    @Query("DELETE FROM favorites WHERE profileId='child' AND workId=:id")
    suspend fun unfavorite(id: String)

    @Query("SELECT * FROM favorites WHERE profileId='child'")
    suspend fun favorites(): List<Favorite>

    @Insert suspend fun audit(value: Audit)

    @Query("SELECT * FROM audit ORDER BY id DESC LIMIT 200") suspend fun audit(): List<Audit>
}

@Database(
    entities =
        [
            Work::class,
            Episode::class,
            Version::class,
            Source::class,
            Profile::class,
            Decision::class,
            Progress::class,
            Favorite::class,
            Audit::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class CinemaDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao

    companion object {
        // v1 is the initial Room catalog schema; v2 introduces persistent decision audit.
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `audit` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `event` TEXT NOT NULL, `versionId` TEXT NOT NULL, `detail` TEXT NOT NULL, `at` INTEGER NOT NULL)"
                    )
                }
            }
        @Volatile private var instance: CinemaDatabase? = null

        fun get(context: Context) =
            instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                                context.applicationContext,
                                CinemaDatabase::class.java,
                                "cinema.db",
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build()
                            .also { instance = it }
                }
    }
}
