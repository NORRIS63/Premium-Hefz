package app.premiumhefz.domain

import java.util.TimeZone

object Dates {
    private const val DAY_MS = 86_400_000L

    /** Whole days since 1970-01-01 in the given time zone (floor division, safe for negative values). */
    fun epochDay(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(nowMillis + zone.getOffset(nowMillis), DAY_MS)
}

object Streaks {
    /** Consecutive active days ending today, or ending yesterday if today has no activity yet. */
    fun current(activeDays: Set<Long>, today: Long): Int {
        var day = if (today in activeDays) today else today - 1
        var count = 0
        while (day in activeDays) {
            count++
            day--
        }
        return count
    }

    fun longest(activeDays: Collection<Long>): Int {
        val sorted = activeDays.toSortedSet()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in sorted) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }
}
