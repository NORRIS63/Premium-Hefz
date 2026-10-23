package app.premiumhefz.domain

enum class ImportMode {
    /** The number printed on the page itself. */
    PRINTED_PAGE,

    /** Juz and page inside the juz, as listed on the old app's Bookmarks screen ("Para: 3, Page: 9"). */
    JUZ_AND_PAGE,

    /** The number typed into the old app's Go To Page screen (1..610). */
    ORIGINAL_GO_TO,

    /** Raw entries from the old app's saved data, shaped like "52_Oct 12, 2026 1:23:45 PM". */
    ORIGINAL_STORED
}

data class ImportParse(val pages: List<Int>, val skipped: List<String>)

object BookmarkImport {
    private val digits = Regex("\\d+")
    private val onlyNumbers = Regex("^[\\d\\s,]+$")
    private val storedEntry = Regex("(\\d+)_")

    fun parse(text: String, mode: ImportMode, index: QuranIndex): ImportParse {
        val pages = LinkedHashSet<Int>()
        val skipped = ArrayList<String>()

        if (mode == ImportMode.ORIGINAL_STORED) {
            val entries = storedEntry.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
            if (entries.isNotEmpty()) {
                for (fileIndex in entries) {
                    val page = PageCoordinates.fromOriginalFileIndex(fileIndex)
                    if (PageCoordinates.isValid(page)) pages.add(page) else skipped.add(fileIndex.toString())
                }
                return ImportParse(pages.toList(), skipped)
            }
        }

        for (raw in text.split('\n', '\r', ';')) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val numbers = digits.findAll(line).mapNotNull { it.value.toIntOrNull() }.toList()
            val candidates: List<Int?> = when (mode) {
                ImportMode.JUZ_AND_PAGE ->
                    if (numbers.size >= 2) listOf(fromJuzAndPage(numbers[0], numbers[1], index)) else listOf(null)
                ImportMode.ORIGINAL_STORED ->
                    listOf(numbers.firstOrNull()?.let { PageCoordinates.fromOriginalFileIndex(it) })
                ImportMode.PRINTED_PAGE, ImportMode.ORIGINAL_GO_TO -> {
                    val picked = if (onlyNumbers.matches(line)) numbers else numbers.take(1)
                    if (picked.isEmpty()) listOf(null) else picked.map {
                        if (mode == ImportMode.ORIGINAL_GO_TO) PageCoordinates.fromOriginalGoToIndex(it) else it
                    }
                }
            }
            var anyValid = false
            for (page in candidates) {
                if (page != null && PageCoordinates.isValid(page)) {
                    pages.add(page)
                    anyValid = true
                }
            }
            if (!anyValid) skipped.add(line)
        }
        return ImportParse(pages.toList(), skipped)
    }

    private fun fromJuzAndPage(juz: Int, pageInJuz: Int, index: QuranIndex): Int? {
        val j = index.juzByNumber(juz) ?: return null
        if (pageInJuz < 1 || pageInJuz > index.juzPageCount(juz)) return null
        return j.startPage + pageInJuz - 1
    }
}
