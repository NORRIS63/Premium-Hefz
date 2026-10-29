package app.premiumhefz.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class RuqyahAyah(val ayah: Int, val text: String)

data class RuqyahPassage(val surah: Int, val name: String, val range: String, val ayahs: List<RuqyahAyah>)

/**
 * The six core Ruqyah passages, bundled as an asset so they work fully
 * offline. The Arabic is the Uthmani text (Tanzil, CC BY 3.0), reproduced
 * verbatim from the edition — including its segmentation, where the
 * Basmalah is printed together with the first verse of 112, 113 and 114.
 * Passages are labelled by their verified verse range, never by invented
 * per-line numbers, and no repetition count is shown: counts vary by
 * source, and none is presented as fixed without a citation.
 */
class RuqyahRepository(context: Context) {
    private val appContext = context.applicationContext
    private var cached: List<RuqyahPassage>? = null

    suspend fun passages(): List<RuqyahPassage>? = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val parsed = runCatching {
            val text = appContext.assets.open("ruqyah.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            val passagesJson = root.getJSONArray("passages")
            val passages = mutableListOf<RuqyahPassage>()
            for (i in 0 until passagesJson.length()) {
                val p = passagesJson.getJSONObject(i)
                val ayahsJson = p.getJSONArray("ayahs")
                val ayahs = mutableListOf<RuqyahAyah>()
                for (j in 0 until ayahsJson.length()) {
                    val a = ayahsJson.getJSONObject(j)
                    ayahs += RuqyahAyah(a.getInt("ayah"), a.getString("text"))
                }
                passages += RuqyahPassage(p.getInt("surah"), p.getString("name"), p.getString("range"), ayahs)
            }
            passages.toList()
        }.getOrNull()
        if (parsed != null) cached = parsed
        parsed
    }
}
