package app.premiumhefz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT page FROM bookmarks")
    fun observePages(): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE page = :page")
    suspend fun countForPage(page: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<BookmarkEntity>): List<Long>

    @Query("DELETE FROM bookmarks WHERE page = :page")
    suspend fun deleteByPage(page: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReadingDao {
    @Query("SELECT * FROM reading_position WHERE id = 1")
    fun observePosition(): Flow<ReadingPositionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosition(entity: ReadingPositionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadEvent(event: ReadEventEntity)

    @Query("SELECT COUNT(*) FROM read_events WHERE epochDay = :epochDay")
    fun observePagesReadOn(epochDay: Long): Flow<Int>

    @Query("SELECT DISTINCT epochDay FROM read_events ORDER BY epochDay DESC")
    fun observeActiveDays(): Flow<List<Long>>
}
