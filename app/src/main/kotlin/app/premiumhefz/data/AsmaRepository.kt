package app.premiumhefz.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AsmaName(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val meaningEn: String,
    /** One short when-to-use line, drawn from the name's meaning. Never a promised outcome. */
    val usageEn: String = ""
)

sealed interface AsmaState {
    data object Loading : AsmaState
    data class Ready(val names: List<AsmaName>, val offline: Boolean) : AsmaState
    data object Unavailable : AsmaState
}

/**
 * The 99 names from the AlAdhan API, fetched on demand and cached in the
 * app's files dir. The API's redistribution terms are unverified, so the
 * list is never bundled — it is fetched at runtime with attribution.
 * Offline, the last fetched list is shown and labelled as saved; when
 * nothing was ever fetched, the screen says so honestly instead of
 * showing an invented list.
 *
 * Numbering follows the AlAdhan list (Ar-Rahman = 1 … As-Saboor = 99).
 * It matches the Tirmidhi 3507 enumeration except for one name: Al-Ahad
 * appears here but not there (there, the count of 99 includes Allah
 * itself). The screen states this plainly.
 */
class AsmaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "asma").apply { mkdirs() }
    private val cacheFile = File(dir, "asma.json")

    companion object {
        private const val URL = "https://api.aladhan.com/v1/asmaAlHusna"
    }

    suspend fun names(): AsmaState = withContext(Dispatchers.IO) {
        val fetched = fetchText(URL)
        if (fetched != null) {
            val parsed = runCatching { parse(fetched) }.getOrNull()
            if (parsed != null) {
                runCatching {
                    val tmp = File(dir, "asma.json.tmp")
                    tmp.writeText(fetched, Charsets.UTF_8)
                    tmp.renameTo(cacheFile)
                }
                return@withContext AsmaState.Ready(attachUsage(parsed), offline = false)
            }
        }
        val cached = readCached()?.let { runCatching { parse(it) }.getOrNull() }
        if (cached != null) return@withContext AsmaState.Ready(attachUsage(cached), offline = true)
        AsmaState.Unavailable
    }

    /** When-to-use lines bundled with the app (our own words, keyed by number). */
    private var usageCache: Map<Int, String>? = null

    private fun usageMap(): Map<Int, String> {
        usageCache?.let { return it }
        val map = runCatching {
            val text = appContext.assets.open("asma_usage.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val obj = JSONObject(text)
            val out = mutableMapOf<Int, String>()
            obj.keys().forEach { key ->
                key.toIntOrNull()?.let { out[it] = obj.optString(key) }
            }
            out
        }.getOrDefault(emptyMap())
        usageCache = map
        return map
    }

    private fun attachUsage(names: List<AsmaName>): List<AsmaName> {
        val usage = usageMap()
        if (usage.isEmpty()) return names
        return names.map { it.copy(usageEn = usage[it.number] ?: "") }
    }

    private fun readCached(): String? =
        runCatching { cacheFile.takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()

    private fun fetchText(url: String): String? {
        val conn = (URL(url).openConnection() as? HttpURLConnection) ?: return null
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
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

    private fun parse(text: String): List<AsmaName> {
        val data = JSONObject(text).getJSONArray("data")
        val names = mutableListOf<AsmaName>()
        for (i in 0 until data.length()) {
            val e = data.getJSONObject(i)
            val en = e.optJSONObject("en")
            names += AsmaName(
                number = e.getInt("number"),
                arabic = e.getString("name"),
                transliteration = e.getString("transliteration"),
                meaningEn = en?.optString("meaning") ?: ""
            )
        }
        return names.sortedBy { it.number }
    }
}
