package app.premiumhefz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HadithDayTest {
    private val book = HADITH_BOOKS[0]

    private fun entry(n: String, ara: String? = "ar$n", ben: String? = "bn$n") =
        HadithEntry(number = n, section = 1, arabic = ara, bengali = ben, inBookRef = "1:$n")

    @Test
    fun dayBookRotatesInOrder() {
        assertEquals(0, dayBookIndex(0, 8))
        assertEquals(1, dayBookIndex(1, 8))
        assertEquals(7, dayBookIndex(7, 8))
        assertEquals(0, dayBookIndex(8, 8))
    }

    @Test
    fun pickIsDeterministicForADay() {
        val entries = (1..10).map { entry(it.toString()) }
        val sections = mapOf(1 to "Chapter")
        val a = selectDayHadith(book, sections, entries, 100L, 8)
        val b = selectDayHadith(book, sections, entries, 100L, 8)
        assertEquals(a, b)
    }

    @Test
    fun pickSkipsEntriesWithoutBengali() {
        val entries = listOf(entry("1", ben = null), entry("2"), entry("3", ben = null))
        // Only "2" is a candidate, so every day picks it.
        assertEquals("2", selectDayHadith(book, emptyMap(), entries, 0L, 8)?.number)
        assertEquals("2", selectDayHadith(book, emptyMap(), entries, 57L, 8)?.number)
    }

    @Test
    fun pickIsNullWhenNoCandidateHasBengali() {
        val entries = listOf(entry("1", ben = null), entry("2", ben = null))
        assertNull(selectDayHadith(book, emptyMap(), entries, 0L, 8))
    }

    @Test
    fun pickAdvancesWithinABookAcrossDays() {
        val entries = (1..10).map { entry(it.toString()) }
        // Day 0 uses index 0, day 8 uses index 1 (same book, 8 days later).
        assertEquals("1", selectDayHadith(book, emptyMap(), entries, 0L, 8)?.number)
        assertEquals("2", selectDayHadith(book, emptyMap(), entries, 8L, 8)?.number)
    }
}
