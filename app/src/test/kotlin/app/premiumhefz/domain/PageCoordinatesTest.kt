package app.premiumhefz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageCoordinatesTest {
    @Test
    fun assetNamesFollowTheOriginalFileNumbering() {
        assertEquals("page003.gif", PageCoordinates.assetName(2))
        assertEquals("page004.gif", PageCoordinates.assetName(3))
        assertEquals("page612.gif", PageCoordinates.assetName(611))
        assertEquals(610, PageCoordinates.PAGE_COUNT)
    }

    @Test
    fun originalNumbersConvertToPrintedPages() {
        assertEquals(2, PageCoordinates.fromOriginalFileIndex(3))
        assertEquals(611, PageCoordinates.fromOriginalFileIndex(612))
        assertEquals(2, PageCoordinates.fromOriginalGoToIndex(1))
        assertEquals(611, PageCoordinates.fromOriginalGoToIndex(610))
        assertEquals(1, PageCoordinates.toOriginalGoToIndex(2))
    }

    @Test
    fun pagerIndexRoundTrips() {
        for (page in PageCoordinates.FIRST_PAGE..PageCoordinates.LAST_PAGE) {
            assertEquals(page, PageCoordinates.pageForPagerIndex(PageCoordinates.pagerIndex(page)))
        }
        assertEquals(0, PageCoordinates.pagerIndex(2))
        assertEquals(609, PageCoordinates.pagerIndex(611))
    }

    @Test
    fun validityBounds() {
        assertFalse(PageCoordinates.isValid(1))
        assertTrue(PageCoordinates.isValid(2))
        assertTrue(PageCoordinates.isValid(611))
        assertFalse(PageCoordinates.isValid(612))
        assertEquals(2, PageCoordinates.coerce(-5))
        assertEquals(611, PageCoordinates.coerce(9999))
    }
}
