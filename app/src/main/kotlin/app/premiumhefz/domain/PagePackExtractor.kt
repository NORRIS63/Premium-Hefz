package app.premiumhefz.domain

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Where verified pages are written. Implementations write to a temporary file until [commit]. */
interface PageSink {
    fun isInstalled(page: Int): Boolean
    fun open(page: Int): OutputStream
    fun commit(page: Int)
    fun discard(page: Int)
}

data class ExtractProgress(val entriesSeen: Int, val verified: Int, val failed: Int, val skipped: Int)

data class ExtractResult(
    val verified: Set<Int>,
    val failed: Set<Int>,
    val skipped: Set<Int>,
    val cancelled: Boolean
)

/**
 * Reads page images out of an APK or ZIP stream and keeps a page only if its SHA-256 matches the
 * manifest, so what lands on the device is byte-identical to the original.
 */
object PagePackExtractor {
    private val entryPattern = Regex(".*?page(\\d{3})\\.gif$")

    fun extract(
        input: InputStream,
        manifest: Map<Int, PageRecord>,
        sink: PageSink,
        onProgress: (ExtractProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExtractResult {
        val verified = LinkedHashSet<Int>()
        val failed = LinkedHashSet<Int>()
        val skipped = LinkedHashSet<Int>()
        var seen = 0
        val buffer = ByteArray(64 * 1024)

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                if (isCancelled()) return ExtractResult(verified, failed, skipped, true)
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val match = entryPattern.find(entry.name) ?: continue
                val page = PageCoordinates.fromOriginalFileIndex(match.groupValues[1].toInt())
                val record = manifest[page] ?: continue
                seen++

                if (sink.isInstalled(page)) {
                    skipped.add(page)
                } else {
                    val digest = MessageDigest.getInstance("SHA-256")
                    var committed = false
                    try {
                        sink.open(page).use { out ->
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                digest.update(buffer, 0, n)
                                out.write(buffer, 0, n)
                            }
                        }
                        if (toHex(digest.digest()) == record.sha256) {
                            sink.commit(page)
                            committed = true
                            verified.add(page)
                        } else {
                            failed.add(page)
                        }
                    } finally {
                        if (!committed) sink.discard(page)
                    }
                }
                onProgress(ExtractProgress(seen, verified.size, failed.size, skipped.size))
            }
        }
        return ExtractResult(verified, failed, skipped, false)
    }

    fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append(String.format("%02x", b.toInt() and 0xff))
        return sb.toString()
    }
}
