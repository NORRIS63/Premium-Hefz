package app.premiumhefz.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.premiumhefz.data.SettingsStore

/** After a reboot, alarms are gone — reschedule the reminder if it is on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore(context).settings.value
        if (settings.reminderEnabled) {
            ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
        }
    }
}
