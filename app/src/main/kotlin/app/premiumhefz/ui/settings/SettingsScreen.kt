package app.premiumhefz.ui.settings

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
import androidx.compose.material3.Button
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.AppSettings
import app.premiumhefz.data.HefzRepository
import app.premiumhefz.data.PackStatus
import app.premiumhefz.data.PagePack
import app.premiumhefz.data.PageTone
import app.premiumhefz.data.ThemeMode
import app.premiumhefz.domain.PackVerifier
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.GoldProgress
import app.premiumhefz.ui.components.ScreenTitle
import app.premiumhefz.ui.components.SectionLabel
import app.premiumhefz.ui.theme.brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val repo: HefzRepository, private val pack: PagePack) : ViewModel() {
    sealed interface VerifyState {
        data object Idle : VerifyState
        data class Running(val checked: Int, val total: Int) : VerifyState
        data class Done(val ok: Int, val corrupt: Int, val missing: Int) : VerifyState
    }

    val settings: StateFlow<AppSettings> = repo.settings
    val packStatus: StateFlow<PackStatus> = pack.status

    private val verifyState = MutableStateFlow<VerifyState>(VerifyState.Idle)
    val verify: StateFlow<VerifyState> = verifyState.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) = repo.updateSettings(transform)

    /** Re-hashes every installed page against the manifest of the original images. */
    fun verifyPages() {
        if (verifyState.value is VerifyState.Running) return
        viewModelScope.launch {
            verifyState.value = VerifyState.Running(0, pack.manifest.size)
            val result = withContext(Dispatchers.IO) {
                PackVerifier.verify(
                    directory = pack.directory,
                    manifest = pack.manifest,
                    onProgress = { p -> verifyState.value = VerifyState.Running(p.checked, p.total) }
                )
            }
            pack.markVerified(if (result.allGood) System.currentTimeMillis() else null)
            verifyState.value = VerifyState.Done(result.ok.size, result.corrupt.size, result.missing.size)
        }
    }
}

@Composable
fun SettingsScreen(onOpenSetup: () -> Unit) {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.repository, it.pagePack) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pack by vm.packStatus.collectAsStateWithLifecycle()
    val verify by vm.verify.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
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
            SectionLabel(stringResource(R.string.settings_pages))
            Text(
                text = stringResource(R.string.settings_pages_status_fmt, pack.installed, pack.total),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(if (pack.verifiedAt != null) R.string.settings_pages_verified else R.string.settings_pages_unverified),
                style = MaterialTheme.typography.bodySmall,
                color = if (pack.verifiedAt != null) MaterialTheme.brand.gold else MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (val state = verify) {
                is SettingsViewModel.VerifyState.Running -> {
                    Text(
                        text = stringResource(R.string.settings_verifying_fmt, state.checked, state.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GoldProgress(fraction = state.checked.toFloat() / state.total.coerceAtLeast(1).toFloat())
                }
                is SettingsViewModel.VerifyState.Done -> Text(
                    text = if (state.corrupt == 0 && state.missing == 0) {
                        stringResource(R.string.settings_verify_ok_fmt, state.ok)
                    } else {
                        stringResource(R.string.settings_verify_bad_fmt, state.corrupt, state.missing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.corrupt == 0 && state.missing == 0) {
                        MaterialTheme.brand.gold
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                SettingsViewModel.VerifyState.Idle -> Unit
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenSetup) { Text(stringResource(R.string.settings_import)) }
                OutlinedButton(
                    onClick = vm::verifyPages,
                    enabled = pack.installed > 0 && verify !is SettingsViewModel.VerifyState.Running
                ) { Text(stringResource(R.string.settings_verify)) }
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
