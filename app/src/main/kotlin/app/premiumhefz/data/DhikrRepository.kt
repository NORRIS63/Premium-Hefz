package app.premiumhefz.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Sayyid(val arabic: String, val reference: String, val narrator: String)

data class TasbihPhrase(val arabic: String, val gloss: String, val target: Int)

data class DhikrData(
    val sayyid: Sayyid,
    val tasbihReference: String,
    val tasbihPhrases: List<TasbihPhrase>
)

data class TasbihProgress(val phraseIndex: Int, val count: Int, val done: Boolean)

/**
 * Sayyid al-Istighfar and the Tasbih set are bundled as an asset, so
 * Dhikr works fully offline. The counts (33, 33, 33, then the tahlil
 * once, completing 100) come from Sahih Muslim 597 and are the only
 * counts shown anywhere in this section.
 */
class DhikrRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("dhikr", Context.MODE_PRIVATE)
    private var cached: DhikrData? = null

    suspend fun data(): DhikrData? = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val parsed = runCatching {
            val text = appContext.assets.open("dhikr.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            val sayyidJson = root.getJSONObject("sayyid")
            val tasbihJson = root.getJSONObject("tasbih")
            val phrasesJson = tasbihJson.getJSONArray("phrases")
            val phrases = mutableListOf<TasbihPhrase>()
            for (i in 0 until phrasesJson.length()) {
                val p = phrasesJson.getJSONObject(i)
                phrases += TasbihPhrase(p.getString("arabic"), p.getString("gloss"), p.getInt("target"))
            }
            DhikrData(
                sayyid = Sayyid(
                    arabic = sayyidJson.getString("arabic"),
                    reference = sayyidJson.getString("reference"),
                    narrator = sayyidJson.getString("narrator")
                ),
                tasbihReference = tasbihJson.getString("reference"),
                tasbihPhrases = phrases
            )
        }.getOrNull()
        if (parsed != null) cached = parsed
        parsed
    }

    fun loadProgress(): TasbihProgress {
        val index = prefs.getInt("dhikr_phrase", 0)
        val count = prefs.getInt("dhikr_count", 0)
        val done = prefs.getBoolean("dhikr_done", false)
        return TasbihProgress(index, count, done)
    }

    fun saveProgress(progress: TasbihProgress) {
        prefs.edit()
            .putInt("dhikr_phrase", progress.phraseIndex)
            .putInt("dhikr_count", progress.count)
            .putBoolean("dhikr_done", progress.done)
            .apply()
    }
}
