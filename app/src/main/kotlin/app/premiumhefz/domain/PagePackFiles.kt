package app.premiumhefz.domain

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest

object Hashing {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return PagePackExtractor.toHex(digest.digest())
    }
}

/** Stores pages as pageNNN.gif in [directory]; bytes go to a .part file first and are renamed on commit. */
class DirectoryPageSink(private val directory: File, private val manifest: Map<Int, PageRecord>) : PageSink {
    init {
        directory.mkdirs()
    }

    fun target(page: Int): File = File(directory, PageCoordinates.assetName(page))

    private fun part(page: Int): File = File(directory, PageCoordinates.assetName(page) + ".part")

    override fun isInstalled(page: Int): Boolean {
        val file = target(page)
        return file.isFile && file.length() == manifest[page]?.bytes
    }

    override fun open(page: Int): OutputStream = FileOutputStream(part(page))

    override fun commit(page: Int) {
        val destination = target(page)
        if (!part(page).renameTo(destination)) {
            destination.delete()
            check(part(page).renameTo(destination)) { "Could not move page $page into place" }
        }
    }

    override fun discard(page: Int) {
        part(page).delete()
    }
}

data class VerifyProgress(val checked: Int, val total: Int)

data class VerifyResult(val ok: Set<Int>, val corrupt: Set<Int>, val missing: Set<Int>) {
    val allGood: Boolean get() = corrupt.isEmpty() && missing.isEmpty()
}

object PackVerifier {
    /** Re-hashes every installed page and compares it with the manifest. */
    fun verify(
        directory: File,
        manifest: Map<Int, PageRecord>,
        onProgress: (VerifyProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): VerifyResult {
        val ok = LinkedHashSet<Int>()
        val corrupt = LinkedHashSet<Int>()
        val missing = LinkedHashSet<Int>()
        var checked = 0
        for ((page, record) in manifest) {
            if (isCancelled()) break
            val file = File(directory, PageCoordinates.assetName(page))
            when {
                !file.isFile -> missing.add(page)
                file.length() != record.bytes -> corrupt.add(page)
                Hashing.sha256(file) == record.sha256 -> ok.add(page)
                else -> corrupt.add(page)
            }
            checked++
            onProgress(VerifyProgress(checked, manifest.size))
        }
        return VerifyResult(ok, corrupt, missing)
    }
}
