package app.premiumhefz.data

import android.content.Context
import app.premiumhefz.domain.Juz
import app.premiumhefz.domain.QuranIndex
import app.premiumhefz.domain.Surah
import org.json.JSONObject

/** Loads names, ayah counts and start pages. This file holds navigation facts, not Quran text. */
object QuranIndexLoader {
    fun load(context: Context): QuranIndex {
        val text = context.assets.open("navigation.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val surahArray = root.getJSONArray("surahs")
        val juzArray = root.getJSONArray("juz")
        val surahs = (0 until surahArray.length()).map { i ->
            val o = surahArray.getJSONObject(i)
            Surah(
                number = o.getInt("n"),
                nameEn = o.getString("en"),
                nameAr = o.getString("ar"),
                revelation = o.getString("type"),
                ayahCount = o.getInt("ayahs"),
                startPage = o.getInt("page")
            )
        }
        val juz = (0 until juzArray.length()).map { i ->
            val o = juzArray.getJSONObject(i)
            Juz(
                number = o.getInt("n"),
                nameEn = o.getString("en"),
                openingAr = o.getString("ar"),
                startPage = o.getInt("page")
            )
        }
        return QuranIndex(surahs, juz)
    }
}
