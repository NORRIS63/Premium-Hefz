package app.premiumhefz.data

import android.content.Context
import app.premiumhefz.domain.DayHadith
import app.premiumhefz.domain.HADITH_BOOKS
import app.premiumhefz.domain.HadithBookMeta
import app.premiumhefz.domain.HadithEntry
import app.premiumhefz.domain.selectDayHadith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class HadithBookData(
    val meta: HadithBookMeta,
    /** Chapter titles from the Bengali file (they are in English there). */
    val sections: Map<Int, String>,
    val entries: List<HadithEntry>
)

sealed interface DayState {
    data object Loading : DayState
    data class Ready(val hadith: DayHadith, val offline: Boolean) : DayState
    data object Unavailable : DayState
}

/**
 * Hadith texts from the isnady-data project, fetched on demand and cached
 * in the app's files dir. Quran pages stay fully offline; this is the
 * app's first online feature, and it degrades honestly: when a book
 * cannot be fetched and nothing is cached, the Day card falls back to
 * the last hadith that was actually shown, and says so.
 */
class HadithRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "hadith").apply { mkdirs() }
    private val prefs = appContext.getSharedPreferences("hadith_day", Context.MODE_PRIVATE)

    companion object {
        private const val BASE = "https://raw.githubusercontent.com/bayramkotan/isnady-data/main/sunni"
    }

    /** Cached book data if present, otherwise fetched; null when neither works. */
    suspend fun bookData(bookId: String): HadithBookData? = withContext(Dispatchers.IO) {
        val meta = HADITH_BOOKS.firstOrNull { it.id == bookId } ?: return@withContext null
        val bookJson = readCached(bookId, "book") ?: fetchAndCache(bookId, "book")
        val araJson = readCached(bookId, "ara") ?: fetchAndCache(bookId, "ara")
        val benJson = readCached(bookId, "ben") ?: fetchAndCache(bookId, "ben")
        if (bookJson == null || araJson == null || benJson == null) return@withContext null
        runCatching { parseBookData(meta, bookJson, araJson, benJson) }.getOrNull()
    }

    suspend fun dayHadith(epochDay: Long): DayState = withContext(Dispatchers.IO) {
        val books = HADITH_BOOKS
        val book = books[Math.floorMod(epochDay, books.size.toLong()).toInt()]
        val data = bookData(book.id)
        if (data != null) {
            val pick = selectDayHadith(book, data.sections, data.entries, epochDay, books.size)
            if (pick != null) {
                saveLastShown(pick)
                return@withContext DayState.Ready(pick, offline = false)
            }
        }
        // No fresh pick: show the last hadith that was actually shown, labelled offline.
        lastShown()?.let { return@withContext DayState.Ready(it, offline = true) }
        DayState.Unavailable
    }

    fun lastShown(): DayHadith? {
        val bookId = prefs.getString("book", null) ?: return null
        val book = HADITH_BOOKS.firstOrNull { it.id == bookId } ?: return null
        val arabic = prefs.getString("arabic", null) ?: return null
        val bengali = prefs.getString("bengali", null) ?: return null
        return DayHadith(
            book = book,
            day = prefs.getLong("day", 0L),
            number = prefs.getString("number", "") ?: "",
            arabic = arabic,
            bengali = bengali,
            sectionTitle = prefs.getString("section", null),
            inBookRef = prefs.getString("ref", null),
            grades = emptyList()
        )
    }

    private fun saveLastShown(pick: DayHadith) {
        prefs.edit()
            .putString("book", pick.book.id)
            .putString("number", pick.number)
            .putString("arabic", pick.arabic)
            .putString("bengali", pick.bengali)
            .putString("section", pick.sectionTitle)
            .putString("ref", pick.inBookRef)
            .putLong("day", pick.day)
            .apply()
    }

    private fun cacheFile(bookId: String, lang: String): File =
        File(File(dir, bookId).apply { mkdirs() }, "$lang.json")

    private fun readCached(bookId: String, lang: String): String? =
        runCatching { cacheFile(bookId, lang).takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()

    private fun fetchAndCache(bookId: String, lang: String): String? {
        val text = fetchText("$BASE/$bookId/$lang.json") ?: return null
        runCatching {
            val target = cacheFile(bookId, lang)
            val tmp = File(target.parent, "${target.name}.tmp")
            tmp.writeText(text, Charsets.UTF_8)
            tmp.renameTo(target)
        }
        return text
    }

    private fun fetchText(url: String): String? {
        val conn = (URL(url).openConnection() as? HttpURLConnection) ?: return null
        conn.connectTimeout = 15_000
        conn.readTimeout = 90_000
        conn.setRequestProperty("User-Agent", "PremiumHefz")
        conn.instanceFollowRedirects = true
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) null
            else conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun parseBookData(meta: HadithBookMeta, bookJson: String, araJson: String, benJson: String): HadithBookData {
        val book = JSONObject(bookJson)
        val araTexts = JSONObject(araJson).getJSONObject("texts")
        val ben = JSONObject(benJson)
        val benTexts = ben.getJSONObject("texts")
        val benSections = ben.getJSONObject("sections")
        val sections = mutableMapOf<Int, String>()
        benSections.keys().forEach { key ->
            key.toIntOrNull()?.let { sections[it] = benSections.getString(key) }
        }
        val entries = mutableListOf<HadithEntry>()
        val hadiths = book.getJSONArray("hadiths")
        for (i in 0 until hadiths.length()) {
            val h = hadiths.getJSONObject(i)
            val n = h.getString("n")
            val grades = mutableListOf<Pair<String, String>>()
            if (h.has("grades")) {
                val g = h.getJSONArray("grades")
                for (j in 0 until g.length()) {
                    val pair = g.optJSONArray(j) ?: continue
                    if (pair.length() >= 2) grades += pair.getString(0) to pair.getString(1)
                }
            }
            val refs = if (h.has("refs")) h.optJSONObject("refs") else null
            entries += HadithEntry(
                number = n,
                section = h.optInt("s", 0),
                arabic = araTexts.optString(n, null),
                bengali = benTexts.optString(n, null),
                inBookRef = refs?.optString("in-book", null),
                grades = grades
            )
        }
        return HadithBookData(meta, sections, entries)
    }
}
