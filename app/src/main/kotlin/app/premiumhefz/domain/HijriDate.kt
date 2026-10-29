package app.premiumhefz.domain

import android.os.Build

/**
 * Today's date on the civil (tabular) Islamic calendar, as computed by the
 * platform, e.g. "17 Jumada al-Akhirah 1448 AH".
 *
 * Returns null on Android versions older than 24, where android.icu is absent.
 * The civil calendar can differ by a day from a local moon-sighting
 * announcement; it is shown as-is, never adjusted by hand.
 */
object HijriDate {
    private val MONTHS = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Shaban",
        "Ramadan", "Shawwal", "Dhul-Qadah", "Dhul-Hijjah"
    )

    fun today(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        val cal = android.icu.util.IslamicCalendar.getInstance()
        val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val month = cal.get(android.icu.util.Calendar.MONTH)
        val year = cal.get(android.icu.util.Calendar.YEAR)
        if (month !in MONTHS.indices) return null
        return "$day ${MONTHS[month]} $year AH"
    }
}
