package app.premiumhefz.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One bookmark per printed page. */
@Entity(tableName = "bookmarks", indices = [Index(value = ["page"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val page: Int,
    val createdAt: Long,
    val note: String? = null
)

/** A single row (id = 1) holding the last page the reader settled on. */
@Entity(tableName = "reading_position")
data class ReadingPositionEntity(
    @PrimaryKey val id: Int = 1,
    val page: Int,
    val updatedAt: Long
)

/** A page counts as read for a day once it has stayed on screen for a few seconds. */
@Entity(tableName = "read_events", primaryKeys = ["page", "epochDay"])
data class ReadEventEntity(
    val page: Int,
    val epochDay: Long,
    val firstReadAt: Long
)

/** One row per memorized printed page. */
@Entity(tableName = "hifz_pages")
data class HifzEntity(
    @PrimaryKey val page: Int,
    val memorizedAt: Long
)
