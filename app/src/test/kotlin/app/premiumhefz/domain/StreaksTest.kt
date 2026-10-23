package app.premiumhefz.domain

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {
    @Test
    fun currentStreakEndsTodayOrYesterday() {
        assertEquals(3, Streaks.current(setOf(8L, 9L, 10L), today = 10L))
        assertEquals(2, Streaks.current(setOf(9L, 10L), today = 11L))
        assertEquals(0, Streaks.current(setOf(10L), today = 12L))
        assertEquals(0, Streaks.current(emptySet(), today = 5L))
    }

    @Test
    fun longestStreak() {
        assertEquals(3, Streaks.longest(listOf(1L, 2L, 3L, 7L, 8L)))
        assertEquals(0, Streaks.longest(emptyList()))
        assertEquals(1, Streaks.longest(listOf(5L)))
    }

    @Test
    fun epochDayUsesTheLocalDate() {
        val utc = TimeZone.getTimeZone("UTC")
        val dhaka = TimeZone.getTimeZone("Asia/Dhaka")
        assertEquals(0L, Dates.epochDay(0L, utc))
        assertEquals(-1L, Dates.epochDay(-1L, utc))
        assertEquals(3L, Dates.epochDay(3 * 86_400_000L + 5, utc))
        val eightPmUtc = 20 * 3_600_000L
        assertEquals(0L, Dates.epochDay(eightPmUtc, utc))
        assertEquals(1L, Dates.epochDay(eightPmUtc, dhaka))
    }
}
