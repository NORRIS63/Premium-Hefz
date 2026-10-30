package app.premiumhefz.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How page images are drawn. The files on disk are never changed. */
enum class PageTone { AUTO, ORIGINAL, PAPER, NIGHT }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val pageTone: PageTone = PageTone.ORIGINAL,
    val keepScreenOn: Boolean = true,
    val reduceMotion: Boolean = false,
    val readerMotion: Boolean = true,
    val dailyReadGoal: Int = 5,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<AppSettings> = state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(state.value)
        prefs.edit()
            .putString(KEY_THEME, next.theme.name)
            .putString(KEY_TONE, next.pageTone.name)
            .putBoolean(KEY_KEEP_AWAKE, next.keepScreenOn)
            .putBoolean(KEY_REDUCE_MOTION, next.reduceMotion)
            .putBoolean(KEY_READER_MOTION, next.readerMotion)
            .putInt(KEY_GOAL, next.dailyReadGoal.coerceIn(1, 50))
            .putBoolean(KEY_REMINDER_ENABLED, next.reminderEnabled)
            .putInt(KEY_REMINDER_HOUR, next.reminderHour.coerceIn(0, 23))
            .putInt(KEY_REMINDER_MINUTE, next.reminderMinute.coerceIn(0, 59))
            .apply()
        state.value = next.copy(
            dailyReadGoal = next.dailyReadGoal.coerceIn(1, 50),
            reminderHour = next.reminderHour.coerceIn(0, 23),
            reminderMinute = next.reminderMinute.coerceIn(0, 59)
        )
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = ThemeMode.values().firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: defaults.theme,
            pageTone = PageTone.values().firstOrNull { it.name == prefs.getString(KEY_TONE, null) } ?: defaults.pageTone,
            keepScreenOn = prefs.getBoolean(KEY_KEEP_AWAKE, defaults.keepScreenOn),
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, defaults.reduceMotion),
            readerMotion = prefs.getBoolean(KEY_READER_MOTION, defaults.readerMotion),
            dailyReadGoal = prefs.getInt(KEY_GOAL, defaults.dailyReadGoal).coerceIn(1, 50),
            reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, defaults.reminderEnabled),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, defaults.reminderHour).coerceIn(0, 23),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, defaults.reminderMinute).coerceIn(0, 59)
        )
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_TONE = "page_tone"
        const val KEY_KEEP_AWAKE = "keep_screen_on"
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_READER_MOTION = "reader_motion"
        const val KEY_GOAL = "daily_read_goal"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
