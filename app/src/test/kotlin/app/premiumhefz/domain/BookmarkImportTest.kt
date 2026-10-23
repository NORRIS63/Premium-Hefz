package app.premiumhefz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkImportTest {
    private val index = NavigationFixture.index

    @Test
    fun printedPagesAcceptListsAndSkipOutOfRange() {
        val result = BookmarkImport.parse("51\n  600, 3 \n999\nfoo", ImportMode.PRINTED_PAGE, index)
        assertEquals(listOf(51, 600, 3), result.pages)
        assertEquals(listOf("999", "foo"), result.skipped)
    }

    @Test
    fun juzAndPageFollowsTheOldBookmarksScreen() {
        val text = "Al Imran Para: 3, Page: 9\n1 20\n1 21\n30 25\n30 26\n29 24\n0 5"
        val result = BookmarkImport.parse(text, ImportMode.JUZ_AND_PAGE, index)
        assertEquals(listOf(51, 22, 611, 586), result.pages)
        assertEquals(listOf("1 21", "30 26", "0 5"), result.skipped)
    }

    @Test
    fun oldGoToNumbersAreOneLessThanPrintedPages() {
        val result = BookmarkImport.parse("1\n610\n0\n611", ImportMode.ORIGINAL_GO_TO, index)
        assertEquals(listOf(2, 611), result.pages)
        assertEquals(listOf("0", "611"), result.skipped)
    }

    @Test
    fun rawStoredEntriesUseTheFileNumber() {
        val json = "[\"52_Oct 12, 2026 1:23:45 PM\",\"605_Oct 13, 2026\",\"2_x\",\"613_y\"]"
        val result = BookmarkImport.parse(json, ImportMode.ORIGINAL_STORED, index)
        assertEquals(listOf(51, 604), result.pages)
        assertEquals(listOf("2", "613"), result.skipped)
    }

    @Test
    fun duplicatesAreMergedKeepingOrder() {
        assertEquals(listOf(51, 3), BookmarkImport.parse("51\n3\n51", ImportMode.PRINTED_PAGE, index).pages)
    }
}
