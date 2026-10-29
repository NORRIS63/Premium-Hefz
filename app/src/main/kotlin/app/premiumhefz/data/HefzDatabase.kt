package app.premiumhefz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema version 2. Later versions must add explicit Migrations; destructive fallbacks are
 * deliberately not used so a user's progress is never wiped by an update.
 *
 * Schema export is OFF on purpose: the project uses no auto-migrations and commits no
 * schema JSONs, so the exported files served no purpose — but on CI the export step
 * intermittently failed the build with Room's "Empty schema file" error (it struck
 * both the debug and the release KSP tasks on different runs). Disabling it removes
 * that failure mode entirely. Do not re-enable it without committing schema JSONs.
 */
@Database(
    entities = [BookmarkEntity::class, ReadingPositionEntity::class, ReadEventEntity::class, HifzEntity::class, RevisionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HefzDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
    abstract fun reading(): ReadingDao
    abstract fun hifz(): HifzDao
    abstract fun revision(): RevisionDao

    companion object {
        // Version 2 never shipped (the Hifz commit stayed local), so the
        // revision table folds into this same migration instead of a v3.
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `hifz_pages` (`page` INTEGER NOT NULL, `memorizedAt` INTEGER NOT NULL, PRIMARY KEY(`page`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `revision_pages` (`page` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`page`))")
            }
        }

        fun build(context: Context): HefzDatabase =
            Room.databaseBuilder(context.applicationContext, HefzDatabase::class.java, "hefz.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
