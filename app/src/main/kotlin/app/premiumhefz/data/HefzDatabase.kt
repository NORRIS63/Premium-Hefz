package app.premiumhefz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema version 1. Later versions must add explicit Migrations; destructive fallbacks are
 * deliberately not used so a user's progress is never wiped by an update.
 */
@Database(
    entities = [BookmarkEntity::class, ReadingPositionEntity::class, ReadEventEntity::class],
    version = 1,
    exportSchema = true
)
abstract class HefzDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
    abstract fun reading(): ReadingDao

    companion object {
        fun build(context: Context): HefzDatabase =
            Room.databaseBuilder(context.applicationContext, HefzDatabase::class.java, "hefz.db").build()
    }
}
