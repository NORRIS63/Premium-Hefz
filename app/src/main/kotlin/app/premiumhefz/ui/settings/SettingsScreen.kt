package app.premiumhefz.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import app.premiumhefz.R
import app.premiumhefz.data.AppSettings
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.data.PageTone
import app.premiumhefz.data.ThemeMode
import app.premiumhefz.reminder.ReminderScheduler
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.ScreenTitle
import app.premiumhefz.ui.components.SectionLabel
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val repo: HefzRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings

    fun update(transform: (AppSettings) -> AppSettings) = repo.updateSettings(transform)
}

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.repository) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    // Notification permission is only requested when the reminder is
    // turned on. If it is denied, the toggle honestly stays off.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.update { it.copy(reminderEnabled = true) }
            ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
        }
    }
    fun enableReminder() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            vm.update { it.copy(reminderEnabled = true) }
            ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        ScreenTitle(stringResource(R.string.settings_title))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.settings_appearance))
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
            ChipRow {
                ThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = settings.theme == mode,
                        onClick = { vm.update { it.copy(theme = mode) } },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    }
                                )
                            )
                        }
                    )
                }
            }
            Text(stringResource(R.string.settings_page_tone), style = MaterialTheme.typography.titleSmall)
            ChipRow {
                PageTone.values().forEach { tone ->
                    FilterChip(
                        selected = settings.pageTone == tone,
                        onClick = { vm.update { it.copy(pageTone = tone) } },
                        label = {
                            Text(
                                stringResource(
                                    when (tone) {
                                        PageTone.AUTO -> R.string.tone_auto
                                        PageTone.ORIGINAL -> R.string.tone_original
                                        PageTone.PAPER -> R.string.tone_paper
                                        PageTone.NIGHT -> R.string.tone_night
                                    }
                                )
                            )
                        }
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_page_tone_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))
            SectionLabel(stringResource(R.string.settings_reading))
            SwitchRow(
                title = stringResource(R.string.settings_keep_awake),
                subtitle = null,
                checked = settings.keepScreenOn,
                onCheckedChange = { checked -> vm.update { it.copy(keepScreenOn = checked) } }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_goal), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.settings_goal_fmt, settings.dailyReadGoal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { vm.update { it.copy(dailyReadGoal = it.dailyReadGoal - 1) } }) { Text("\u2212") }
                Spacer(Modifier.padding(horizontal = 4.dp))
                OutlinedButton(onClick = { vm.update { it.copy(dailyReadGoal = it.dailyReadGoal + 1) } }) { Text("+") }
            }

            Spacer(Modifier.height(6.dp))
            SectionLabel(stringResource(R.string.settings_motion))
            SwitchRow(
                title = stringResource(R.string.settings_reduce_motion),
                subtitle = stringResource(R.string.settings_reduce_motion_hint),
                checked = settings.reduceMotion,
                onCheckedChange = { checked -> vm.update { it.copy(reduceMotion = checked) } }
            )

            Spacer(Modifier.height(6.dp))
            SectionLabel(stringResource(R.string.settings_reminders))
            SwitchRow(
                title = stringResource(R.string.reminder_title),
                subtitle = stringResource(R.string.reminder_sub),
                checked = settings.reminderEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        enableReminder()
                    } else {
                        vm.update { it.copy(reminderEnabled = false) }
                        ReminderScheduler.cancel(context)
                    }
                }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(
                            R.string.reminder_time_fmt,
                            settings.reminderHour,
                            settings.reminderMinute
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    enabled = settings.reminderEnabled,
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                vm.update { it.copy(reminderHour = hour, reminderMinute = minute) }
                                ReminderScheduler.schedule(context, hour, minute)
                            },
                            settings.reminderHour,
                            settings.reminderMinute,
                            true
                        ).show()
                    }
                ) { Text(stringResource(R.string.reminder_change)) }
            }

            Spacer(Modifier.height(6.dp))
            SectionLabel(stringResource(R.string.settings_about))
            Text(
                text = stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_version_fmt, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
