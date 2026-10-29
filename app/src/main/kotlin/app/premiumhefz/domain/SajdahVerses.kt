package app.premiumhefz.domain

/**
 * The 15 verses that carry a prostration (sajdah) marker in printed mushafs.
 *
 * Sources agree on the 15-verse union but NOT on one universal set of 14:
 * - 22:77 (Al-Hajj) is counted by Shafi'i and Hanbali, but not by Hanafis.
 * - 38:24 (Sad) is counted by Hanafis; Shafi'i and Hanbali treat it as a
 *   prostration of gratitude, not a recitation sajdah.
 * Maliki and Hanbali details vary between sources, so no Maliki- or
 * Hanbali-specific list is encoded here.
 *
 * Display default: the Hanafi set (14 verses = union minus 22:77), which
 * fits this app's Bangladeshi, Hanafi-majority user base and its Indo-Pak
 * style mushaf. The full union of 15 is shown, with the two disputed
 * verses carrying their notes, so no school's verse is hidden.
 *
 * Ayah labels use ranges where sources cite the marker verse two ways
 * (e.g. 16:49-50), to avoid false precision.
 *
 * Navigation is surah-level on purpose: this app's reader shows page
 * images of a 610-page Indo-Pak mushaf, and no verified verse-to-page
 * table for that mushaf exists. Tapping a verse opens its surah's first
 * page; the screen says so plainly. Never invent page precision.
 */
/**
 * Which of the two disputed verses this is, if any. The UI layer maps
 * each value to its explanatory string; no school-specific list beyond
 * the Hanafi default is encoded.
 */
enum class SajdahDispute { HAJJ_77, SAD_24 }

data class SajdahVerse(
    val surah: Int,
    val ayahLabel: String,
    val inHanafiSet: Boolean,
    val dispute: SajdahDispute? = null
)

object SajdahVerses {
    // R.string references are resolved by the UI layer, keeping this
    // file free of Android UI imports.
    val all: List<SajdahVerse> = listOf(
        SajdahVerse(7, "206", true),
        SajdahVerse(13, "15", true),
        SajdahVerse(16, "49–50", true),
        SajdahVerse(17, "107–109", true),
        SajdahVerse(19, "58", true),
        SajdahVerse(22, "18", true),
        SajdahVerse(22, "77", false, SajdahDispute.HAJJ_77),
        SajdahVerse(25, "60", true),
        SajdahVerse(27, "25–26", true),
        SajdahVerse(32, "15", true),
        SajdahVerse(38, "24", true, SajdahDispute.SAD_24),
        SajdahVerse(41, "37–38", true),
        SajdahVerse(53, "62", true),
        SajdahVerse(84, "21", true),
        SajdahVerse(96, "19", true)
    )

    /** Hanafi set, in surah order. */
    val hanafi: List<SajdahVerse> = all.filter { it.inHanafiSet }

    /** Marked verses outside the Hanafi set (currently only 22:77). */
    val otherSchools: List<SajdahVerse> = all.filter { !it.inHanafiSet }
}
