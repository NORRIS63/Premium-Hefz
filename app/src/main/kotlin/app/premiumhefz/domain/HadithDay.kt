package app.premiumhefz.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The eight hadith books with a verified Bengali translation, from the
 * isnady-data project (Unlicense; Bengali: Islamic Foundation Bangladesh
 * translation). Catalog ids, titles and compilers are copied from each
 * book's own book.json — not from memory.
 */
data class HadithBookMeta(
    val id: String,
    val title: String,
    val titleAr: String,
    val compiler: String,
    val count: Int
)

val HADITH_BOOKS = listOf(
    HadithBookMeta("sahih_al_bukhari", "Sahih al-Bukhari", "صحيح البخاري", "al-Bukhari (d. 256)", 7590),
    HadithBookMeta("sahih_muslim", "Sahih Muslim", "صحيح مسلم", "Muslim (d. 261)", 7564),
    HadithBookMeta("sunan_abi_dawud", "Sunan Abi Dawud", "سنن أبي داود", "Abu Dawud (d. 275)", 5274),
    HadithBookMeta("jami_al_tirmidhi", "Jami' al-Tirmidhi", "جامع الترمذي", "al-Tirmidhi (d. 279)", 3998),
    HadithBookMeta("sunan_al_nasai", "Sunan al-Nasa'i", "سنن النسائي", "al-Nasa'i (d. 303)", 5765),
    HadithBookMeta("sunan_ibn_majah", "Sunan Ibn Majah", "سنن ابن ماجه", "Ibn Majah (d. 273)", 4343),
    HadithBookMeta("muwatta_malik_ibn_anas", "Muwatta Malik", "موطأ مالك", "Malik (d. 179)", 1927),
    HadithBookMeta("al_arbain_al_nawawiyya", "Forty Hadith of al-Nawawi", "الأربعون النووية", "al-Nawawi (d. 676)", 42)
)

data class HadithEntry(
    val number: String,
    val section: Int,
    val arabic: String?,
    val bengali: String?,
    val inBookRef: String?,
    /** [scholar, grade] pairs; empty for books whose data carries no grades. */
    val grades: List<Pair<String, String>> = emptyList()
)

data class DayHadith(
    val book: HadithBookMeta,
    val day: Long,
    val number: String,
    val arabic: String,
    val bengali: String,
    val sectionTitle: String?,
    val inBookRef: String?,
    val grades: List<Pair<String, String>> = emptyList()
)

/** Which book supplies a given day's hadith: the books rotate in catalog order. */
fun dayBookIndex(epochDay: Long, bookCount: Int): Int =
    Math.floorMod(epochDay, bookCount.toLong()).toInt()

/**
 * Deterministic pick for one day from a book's entries. Only entries with
 * both Arabic and Bengali text are candidates, so the Day hadith never
 * shows a missing translation. Returns null when no candidate exists.
 */
fun selectDayHadith(
    book: HadithBookMeta,
    sections: Map<Int, String>,
    entries: List<HadithEntry>,
    epochDay: Long,
    bookCount: Int
): DayHadith? {
    val candidates = entries.filter { it.arabic != null && it.bengali != null }
        .sortedBy { it.number.toIntOrNull() ?: 0 }
    if (candidates.isEmpty()) return null
    val index = Math.floorMod(epochDay / bookCount, candidates.size.toLong()).toInt()
    val entry = candidates[index]
    return DayHadith(
        book = book,
        day = epochDay,
        number = entry.number,
        arabic = entry.arabic!!,
        bengali = entry.bengali!!,
        sectionTitle = sections[entry.section],
        inBookRef = entry.inBookRef,
        grades = entry.grades
    )
}

/** "Thursday, 29 October 2026" for an epoch day in the device's time zone. */
fun formatDayLabel(epochDay: Long): String {
    val calendar = Calendar.getInstance().apply {
        set(1970, Calendar.JANUARY, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, epochDay.toInt())
    }
    return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(calendar.time)
}
