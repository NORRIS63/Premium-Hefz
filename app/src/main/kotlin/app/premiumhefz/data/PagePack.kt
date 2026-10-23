package app.premiumhefz.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.premiumhefz.domain.DirectoryPageSink
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.domain.PageRecord
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class PackStatus(val installed: Int, val total: Int, val verifiedAt: Long?) {
    val complete: Boolean get() = total > 0 && installed >= total
}

/**
 * The 610 original page images, stored in the app's private files directory (never on the network).
 * The images are imported from the user's own copy of the original app and checked against
 * [manifest], so what is on disk is byte-identical to the original.
 */
class PagePack(context: Context, val manifest: Map<Int, PageRecord>) {
    val directory: File = File(context.filesDir, "pages").apply { mkdirs() }
    val sink = DirectoryPageSink(directory, manifest)

    private val prefs = context.getSharedPreferences("page_pack", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readStatus())

    val status: StateFlow<PackStatus> = state.asStateFlow()

    fun pageFile(page: Int): File = sink.target(page)

    fun refresh() {
        state.value = readStatus()
    }

    /** Pass null to record that the pages have changed since they were last verified. */
    fun markVerified(at: Long?) {
        prefs.edit().putLong(KEY_VERIFIED_AT, at ?: 0L).apply()
        refresh()
    }

    private fun readStatus(): PackStatus {
        val installed = manifest.keys.count { sink.target(it).isFile }
        val verified = prefs.getLong(KEY_VERIFIED_AT, 0L).takeIf { it > 0L }
        return PackStatus(installed = installed, total = manifest.size, verifiedAt = verified)
    }

    private companion object {
        const val KEY_VERIFIED_AT = "verified_at"
    }
}

object PageBitmaps {
    private val cache = LruCache<Int, ImageBitmap>(12)

    fun peek(page: Int): ImageBitmap? = cache.get(page)

    suspend fun load(page: Int, pack: PagePack): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(page) ?: run {
            val file = pack.pageFile(page)
            if (!file.isFile) return@run null
            BitmapFactory.decodeFile(file.path)?.asImageBitmap()?.also { cache.put(page, it) }
        }
    }

    fun clear() = cache.evictAll()
}

/** Page numbers are always printed numbers; see PageCoordinates. */
fun PagePack.hasPage(page: Int): Boolean = PageCoordinates.isValid(page) && pageFile(page).isFile
