package app.premiumhefz.domain

/**
 * One canonical coordinate for the whole app: the PRINTED page number, which is the number printed
 * at the top of each page image in the original mushaf (2..611).
 *
 * The original app used three different numbers for the same page:
 *  - file index  = printed + 1  (pageNNN.gif; what it stored for bookmarks and the last-read page)
 *  - Go To index = printed - 1  (what its Go To Page screen accepted, 1..610)
 *  - printed     = what the surah and para lists showed and what is on the page itself
 */
object PageCoordinates {
    const val FIRST_PAGE = 2
    const val LAST_PAGE = 611
    const val PAGE_COUNT = LAST_PAGE - FIRST_PAGE + 1

    fun isValid(page: Int): Boolean = page in FIRST_PAGE..LAST_PAGE

    fun coerce(page: Int): Int = page.coerceIn(FIRST_PAGE, LAST_PAGE)

    fun fileIndex(page: Int): Int = page + 1

    fun assetName(page: Int): String = "page" + fileIndex(page).toString().padStart(3, '0') + ".gif"

    fun fromOriginalFileIndex(fileIndex: Int): Int = fileIndex - 1

    fun fromOriginalGoToIndex(goTo: Int): Int = goTo + 1

    fun toOriginalGoToIndex(page: Int): Int = page - 1

    fun pagerIndex(page: Int): Int = page - FIRST_PAGE

    fun pageForPagerIndex(index: Int): Int = index + FIRST_PAGE
}
