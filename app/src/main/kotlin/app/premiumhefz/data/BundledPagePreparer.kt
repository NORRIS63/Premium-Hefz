package app.premiumhefz.data

import android.content.Context
import android.os.StatFs
import app.premiumhefz.domain.PagePackExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * First-run preparation of the Quran pages bundled in the app's assets.
 *
 * Started from [app.premiumhefz.HefzApp.onCreate], so on a fresh install the
 * pages unpack and hash-verify themselves in the background the moment the
 * app first launches — no import screen, no file picking, no other app.
 * Progress is published through [PagePack.status], which Home already shows.
 * If anything fails, the pack simply stays incomplete and the manual paths
 * in Setup remain available as a fallback.
 */
class BundledPagePreparer(private val appContext: Context, private val pack: PagePack) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var requested = false

    /** The verified page pack bundled in the app's assets, if present. */
    val available: Boolean by lazy {
        try {
            appContext.assets.open(ASSET).use { true }
        } catch (e: Exception) {
            false
        }
    }

    /** Kicks off background preparation when the pack is incomplete. Safe to call on every launch. */
    fun prepareIfNeeded() {
        if (requested) return
        requested = true
        if (!available || pack.status.value.complete) return
        scope.launch { prepare() }
    }

    private suspend fun prepare() {
        if (!pack.tryBeginImport()) return
        try {
            if (pack.status.value.complete) return
            val needed = pack.manifest.values.filter { !pack.sink.isInstalled(it.page) }.sumOf { it.bytes }
            val free = StatFs(pack.directory.path).availableBytes
            if (free < needed + SAFETY_BYTES) return
            val stream = try {
                appContext.assets.open(ASSET)
            } catch (e: Exception) {
                return
            }
            val result = stream.use {
                PagePackExtractor.extract(
                    input = it,
                    manifest = pack.manifest,
                    sink = pack.sink,
                    onProgress = { p ->
                        // Let Home's counter tick up while preparation runs.
                        if ((p.verified + p.skipped) % 30 == 0) pack.refresh()
                    },
                    isCancelled = { false }
                )
            }
            PageBitmaps.clear()
            pack.refresh()
            // Pages skipped as "already installed" were only size-checked, so claim verification
            // only when this run hashed every page.
            pack.markVerified(if (result.verified.size == pack.manifest.size) System.currentTimeMillis() else null)
        } catch (e: Exception) {
            // Leave the pack incomplete; Home and Setup keep the manual fallback.
        } finally {
            pack.endImport()
        }
    }

    private companion object {
        const val ASSET = "quran_pages.zip"
        const val SAFETY_BYTES = 8L * 1024 * 1024
    }
}
