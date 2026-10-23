package app.premiumhefz.ui.setup

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.premiumhefz.R
import app.premiumhefz.data.PageBitmaps
import app.premiumhefz.data.PackStatus
import app.premiumhefz.data.PagePack
import app.premiumhefz.domain.PagePackExtractor
import app.premiumhefz.ui.LocalContainer
import app.premiumhefz.ui.appViewModel
import app.premiumhefz.ui.components.GoldProgress
import app.premiumhefz.ui.theme.brand
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupViewModel(private val appContext: Context, private val pack: PagePack) : ViewModel() {
    enum class Reason { UNREADABLE, NO_SPACE, NOT_INSTALLED }

    sealed interface UiState {
        data object Idle : UiState
        data class Working(val verified: Int, val failed: Int, val skipped: Int) : UiState
        data class Finished(val verified: Int, val failed: Int, val missing: Int) : UiState
        data class Failed(val reason: Reason, val megabytes: Int = 0) : UiState
    }

    private val uiState = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = uiState.asStateFlow()
    private var job: Job? = null

    val originalInstalled: Boolean get() = installedOriginalApk() != null

    /** The verified page pack bundled in the app's assets, if present. */
    val bundledAvailable: Boolean by lazy {
        try {
            appContext.assets.open(BUNDLED_PACK).use { true }
        } catch (e: Exception) {
            false
        }
    }

    private var autoImportStarted = false

    /** First-run path: extract the bundled pack automatically, with no file picking. Runs at most once. */
    fun autoImportIfNeeded(packComplete: Boolean) {
        if (autoImportStarted || packComplete || !bundledAvailable) return
        autoImportStarted = true
        importFromBundled()
    }

    fun importFromBundled() = startImport { appContext.assets.open(BUNDLED_PACK) }

    fun importFromUri(uri: Uri) = startImport { appContext.contentResolver.openInputStream(uri) }

    fun importFromInstalledOriginal() {
        val apk = installedOriginalApk()
        if (apk == null) uiState.value = UiState.Failed(Reason.NOT_INSTALLED) else startImport { FileInputStream(apk) }
    }

    fun cancel() {
        job?.cancel()
        uiState.value = UiState.Idle
    }

    private fun installedOriginalApk(): File? = try {
        @Suppress("DEPRECATION")
        val info = appContext.packageManager.getApplicationInfo(ORIGINAL_PACKAGE, 0)
        File(info.sourceDir).takeIf { it.canRead() }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun startImport(open: () -> InputStream?) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            // The automatic first-run preparer may already be writing pages;
            // never run two extractions at once. Pack status above keeps
            // showing live progress in that case.
            if (!pack.tryBeginImport()) {
                uiState.value = UiState.Idle
                return@launch
            }
            try {
                uiState.value = UiState.Working(0, 0, 0)
                val outcome: UiState = withContext(Dispatchers.IO) {
                    try {
                        val needed = pack.manifest.values.filter { !pack.sink.isInstalled(it.page) }.sumOf { it.bytes }
                        val free = StatFs(pack.directory.path).availableBytes
                        if (free < needed + SAFETY_BYTES) {
                            return@withContext UiState.Failed(Reason.NO_SPACE, ((needed + SAFETY_BYTES) / 1_048_576L).toInt())
                        }
                        val stream = open() ?: return@withContext UiState.Failed(Reason.UNREADABLE)
                        val result = stream.use {
                            PagePackExtractor.extract(
                                input = it,
                                manifest = pack.manifest,
                                sink = pack.sink,
                                onProgress = { p -> uiState.value = UiState.Working(p.verified, p.failed, p.skipped) },
                                isCancelled = { !isActive }
                            )
                        }
                        PageBitmaps.clear()
                        pack.refresh()
                        // Pages skipped as "already installed" were only size-checked, so claim verification
                        // only when this run hashed every page.
                        pack.markVerified(if (result.verified.size == pack.manifest.size) System.currentTimeMillis() else null)
                        val installed = pack.manifest.keys.count { pack.sink.isInstalled(it) }
                        UiState.Finished(result.verified.size, result.failed.size, pack.manifest.size - installed)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        UiState.Failed(Reason.UNREADABLE)
                    }
                }
                uiState.value = outcome
            } finally {
                pack.endImport()
            }
        }
    }

    private companion object {
        const val ORIGINAL_PACKAGE = "com.golap.hefzquran"
        const val BUNDLED_PACK = "quran_pages.zip"
        const val SAFETY_BYTES = 8L * 1024 * 1024
    }
}

@Composable
fun SetupScreen(onBack: () -> Unit) {
    val container = LocalContainer.current
    val vm: SetupViewModel = appViewModel { SetupViewModel(it.appContext, it.pagePack) }
    val state by vm.state.collectAsStateWithLifecycle()
    val pack by container.pagePack.status.collectAsStateWithLifecycle()
    val originalInstalled = remember { vm.originalInstalled }
    // Bundled pages prepare themselves on first run; no file picking, no other app needed.
    LaunchedEffect(pack.complete) { vm.autoImportIfNeeded(pack.complete) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFromUri(uri)
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PackSummary(pack)

            val busy = state is SetupViewModel.UiState.Working
            if (vm.bundledAvailable && !pack.complete) {
                Button(onClick = vm::importFromBundled, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.setup_bundled))
                }
            }
            if (originalInstalled) {
                Button(onClick = vm::importFromInstalledOriginal, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.setup_installed))
                }
            }
            OutlinedButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.setup_choose)) }

            when (val s = state) {
                SetupViewModel.UiState.Idle -> Unit
                is SetupViewModel.UiState.Working -> {
                    Text(
                        text = stringResource(R.string.setup_working_fmt, s.verified),
                        style = MaterialTheme.typography.titleSmall
                    )
                    GoldProgress(fraction = (s.verified + s.skipped).toFloat() / pack.total.coerceAtLeast(1).toFloat())
                    TextButton(onClick = vm::cancel) { Text(stringResource(R.string.setup_stop)) }
                }
                is SetupViewModel.UiState.Finished -> {
                    Text(
                        text = stringResource(R.string.setup_done_fmt, s.verified),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.brand.gold
                    )
                    if (s.failed > 0) {
                        Text(stringResource(R.string.setup_failed_pages_fmt, s.failed), color = MaterialTheme.colorScheme.error)
                    }
                    if (s.missing > 0) {
                        Text(stringResource(R.string.setup_missing_fmt, s.missing), color = MaterialTheme.colorScheme.error)
                    }
                    if (s.missing == 0) {
                        Button(onClick = onBack) { Text(stringResource(R.string.setup_start_reading)) }
                    }
                }
                is SetupViewModel.UiState.Failed -> Text(
                    text = when (s.reason) {
                        SetupViewModel.Reason.UNREADABLE -> stringResource(R.string.setup_error)
                        SetupViewModel.Reason.NO_SPACE -> stringResource(R.string.setup_no_space_fmt, s.megabytes)
                        SetupViewModel.Reason.NOT_INSTALLED -> stringResource(R.string.setup_not_installed)
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PackSummary(pack: PackStatus) {
    Column {
        Text(
            text = stringResource(R.string.settings_pages_status_fmt, pack.installed, pack.total),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        GoldProgress(fraction = pack.installed.toFloat() / pack.total.coerceAtLeast(1).toFloat())
    }
}
