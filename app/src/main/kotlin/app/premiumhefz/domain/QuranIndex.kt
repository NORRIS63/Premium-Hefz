package app.premiumhefz.domain

data class Surah(
    val number: Int,
    val nameEn: String,
    val nameAr: String,
    val revelation: String,
    val ayahCount: Int,
    val startPage: Int
)

data class Juz(
    val number: Int,
    val nameEn: String,
    val openingAr: String,
    val startPage: Int
)

/**
 * Navigation facts only (names, ayah counts, start pages). It holds no Quran text.
 * All pages are printed page numbers, see [PageCoordinates].
 */
class QuranIndex(val surahs: List<Surah>, val juz: List<Juz>) {

    init {
        require(surahs.size == 114) { "Expected 114 surahs, got ${surahs.size}" }
        require(juz.size == 30) { "Expected 30 juz, got ${juz.size}" }
    }

    fun surahByNumber(number: Int): Surah? = surahs.getOrNull(number - 1)

    fun juzByNumber(number: Int): Juz? = juz.getOrNull(number - 1)

    fun surahsStartingOn(page: Int): List<Surah> = surahs.filter { it.startPage == page }

    /** The surah that starts on this page or, failing that, the latest one that started before it. */
    fun surahOpenAt(page: Int): Surah? = surahs.lastOrNull { it.startPage <= page }

    /** Null for the opening page (page 2), which comes before the first counted juz page. */
    fun juzForPage(page: Int): Juz? = juz.lastOrNull { it.startPage <= page }

    fun pageInJuz(page: Int): Int? = juzForPage(page)?.let { page - it.startPage + 1 }

    fun juzPageCount(number: Int): Int {
        val current = juzByNumber(number) ?: return 0
        val end = juzByNumber(number + 1)?.startPage ?: (PageCoordinates.LAST_PAGE + 1)
        return end - current.startPage
    }

    /** Names shown for a page: the surahs that start on it, otherwise the surah in progress. */
    fun surahLabel(page: Int): String {
        val starting = surahsStartingOn(page)
        if (starting.isNotEmpty()) return starting.joinToString(" \u00B7 ") { it.nameEn }
        return surahOpenAt(page)?.nameEn ?: ""
    }

    fun search(query: String): List<Surah> {
        val q = normalize(query)
        if (q.isEmpty()) return surahs
        val number = q.toIntOrNull()
        return surahs.filter { s -> (number != null && s.number == number) || normalize(s.nameEn).contains(q) }
    }

    private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }
}
