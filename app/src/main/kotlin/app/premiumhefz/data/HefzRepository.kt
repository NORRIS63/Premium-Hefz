package app.premiumhefz.data

import app.premiumhefz.domain.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class HefzRepository(db: HefzDatabase, private val settingsStore: SettingsStore) {
    private val bookmarkDao = db.bookmarks()
    private val readingDao = db.reading()
    private val hifzDao = db.hifz()

    val settings: StateFlow<AppSettings> = settingsStore.settings

    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    val bookmarkedPages: Flow<Set<Int>> = bookmarkDao.observePages().map { it.toSet() }
    val readingPosition: Flow<Int?> = readingDao.observePosition().map { it?.page }
    val activeDays: Flow<List<Long>> = readingDao.observeActiveDays()
    val distinctPagesRead: Flow<Int> = readingDao.observeDistinctPagesRead()
    val distinctPages: Flow<List<Int>> = readingDao.observeDistinctPages()
    val memorizedPages: Flow<List<Int>> = hifzDao.observeAll().map { list -> list.map { it.page } }

    suspend fun markMemorized(page: Int) {
        hifzDao.insert(HifzEntity(page, System.currentTimeMillis()))
    }

    suspend fun unmarkMemorized(page: Int) {
        hifzDao.deleteByPage(page)
    }

    /** Pages read per day for the last 7 days (today included), re-emitted at midnight. */
    fun weekCounts(): Flow<List<Int>> = today().flatMapLatest { today ->
        readingDao.observeCountsSince(today - 6).map { rows ->
            val byDay = rows.associate { it.epochDay to it.count }
            (today - 6..today).map { byDay[it] ?: 0 }
        }
    }

    /** Emits again at midnight (checked once a minute) so "today" never goes stale. */
    fun pagesReadToday(): Flow<Int> = today().flatMapLatest { readingDao.observePagesReadOn(it) }

    fun today(): Flow<Long> = flow {
        while (true) {
            emit(Dates.epochDay(System.currentTimeMillis()))
            delay(60_000L)
        }
    }.distinctUntilChanged()

    /** Returns true if the page is bookmarked after the call. */
    suspend fun toggleBookmark(page: Int): Boolean {
        return if (bookmarkDao.countForPage(page) > 0) {
            bookmarkDao.deleteByPage(page)
            false
        } else {
            bookmarkDao.insert(BookmarkEntity(page = page, createdAt = System.currentTimeMillis()))
            true
        }
    }

    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteById(id)

    /** Adds the pages in list order (first one shown first); pages that are already bookmarked are kept as they are. */
    suspend fun importBookmarks(pages: List<Int>): Int {
        val now = System.currentTimeMillis()
        val results = bookmarkDao.insertAll(pages.mapIndexed { i, page -> BookmarkEntity(page = page, createdAt = now - i) })
        return results.count { it != -1L }
    }

    suspend fun saveReadingPosition(page: Int) {
        readingDao.upsertPosition(ReadingPositionEntity(page = page, updatedAt = System.currentTimeMillis()))
    }

    suspend fun recordRead(page: Int) {
        val now = System.currentTimeMillis()
        readingDao.insertReadEvent(ReadEventEntity(page = page, epochDay = Dates.epochDay(now), firstReadAt = now))
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)
}
