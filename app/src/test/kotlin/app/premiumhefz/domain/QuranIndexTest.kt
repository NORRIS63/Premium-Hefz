package app.premiumhefz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranIndexTest {
    private val index = NavigationFixture.index

    @Test
    fun hasAllSurahsAndJuzWithTheStandardAyahTotal() {
        assertEquals(114, index.surahs.size)
        assertEquals(30, index.juz.size)
        assertEquals(6236, index.surahs.sumOf { it.ayahCount })
        assertEquals(7, index.surahByNumber(1)!!.ayahCount)
        assertEquals(286, index.surahByNumber(2)!!.ayahCount)
        assertEquals(6, index.surahByNumber(114)!!.ayahCount)
    }

    @Test
    fun startPagesNeverGoBackwards() {
        for (i in 1 until index.surahs.size) assertTrue(index.surahs[i - 1].startPage <= index.surahs[i].startPage)
        for (i in 1 until index.juz.size) assertTrue(index.juz[i - 1].startPage < index.juz[i].startPage)
    }

    @Test
    fun juzPageCountsAddUpToTheCountedPages() {
        assertEquals(20, index.juzPageCount(1))
        assertEquals(20, index.juzPageCount(28))
        assertEquals(24, index.juzPageCount(29))
        assertEquals(25, index.juzPageCount(30))
        assertEquals(609, (1..30).sumOf { index.juzPageCount(it) })
    }

    // These expectations were read off the page images themselves (printed page, juz and page-in-juz markers).
    @Test
    fun pageInJuzMatchesTheMarkersPrintedOnThePages() {
        assertNull(index.juzForPage(2))
        assertEquals(1, index.juzForPage(3)!!.number)
        assertEquals(1, index.pageInJuz(3))
        assertEquals(3, index.juzForPage(51)!!.number)
        assertEquals(9, index.pageInJuz(51))
        assertEquals(30, index.juzForPage(600)!!.number)
        assertEquals(14, index.pageInJuz(600))
        assertEquals(18, index.pageInJuz(604))
    }

    @Test
    fun surahLookupMatchesWhatTheImagesShow() {
        assertEquals(1, index.surahOpenAt(2)!!.number)
        assertEquals(listOf(2), index.surahsStartingOn(3).map { it.number })
        assertEquals(listOf(3), index.surahsStartingOn(51).map { it.number })
        assertEquals(89, index.surahOpenAt(600)!!.number)
        assertEquals(listOf(95, 96), index.surahsStartingOn(604).map { it.number })
        assertEquals(index.surahByNumber(95)!!.nameEn + " \u00B7 " + index.surahByNumber(96)!!.nameEn, index.surahLabel(604))
        assertEquals(index.surahByNumber(89)!!.nameEn, index.surahLabel(600))
    }

    @Test
    fun searchByNameAndNumber() {
        assertEquals(listOf(2), index.search("baqarah").map { it.number })
        assertEquals(listOf(2), index.search("Al-Baqarah").map { it.number })
        assertEquals(listOf(3), index.search("al imran").map { it.number })
        assertEquals(listOf(113), index.search("113").map { it.number })
        assertEquals(114, index.search("").size)
        assertEquals(0, index.search("zzzz").size)
    }
}
