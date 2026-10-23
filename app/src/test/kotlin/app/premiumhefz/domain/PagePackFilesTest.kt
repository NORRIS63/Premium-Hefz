package app.premiumhefz.domain

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagePackFilesTest {
    private fun sha(bytes: ByteArray) = PagePackExtractor.toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun tempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "hefz-test-" + System.nanoTime())
        dir.mkdirs()
        return dir
    }

    @Test
    fun sinkCommitsOnlyWhenAskedAndLeavesNoPartFiles() {
        val dir = tempDir()
        val bytes = "page bytes".toByteArray()
        val manifest = mapOf(2 to PageRecord(2, sha(bytes), bytes.size.toLong()))
        val sink = DirectoryPageSink(dir, manifest)

        assertFalse(sink.isInstalled(2))
        sink.open(2).use { it.write(bytes) }
        assertFalse(sink.isInstalled(2))
        sink.commit(2)
        assertTrue(sink.isInstalled(2))
        assertEquals(listOf("page003.gif"), dir.list()!!.toList())

        sink.open(2).use { it.write("other".toByteArray()) }
        sink.discard(2)
        assertEquals(listOf("page003.gif"), dir.list()!!.toList())
        dir.deleteRecursively()
    }

    @Test
    fun verifierSeparatesGoodCorruptAndMissingPages() {
        val dir = tempDir()
        val good = "good".toByteArray()
        val manifest = mapOf(
            2 to PageRecord(2, sha(good), good.size.toLong()),
            3 to PageRecord(3, sha("expected".toByteArray()), 8L),
            4 to PageRecord(4, sha("absent".toByteArray()), 6L)
        )
        File(dir, PageCoordinates.assetName(2)).writeBytes(good)
        File(dir, PageCoordinates.assetName(3)).writeBytes("changed!".toByteArray())

        val result = PackVerifier.verify(dir, manifest)
        assertEquals(setOf(2), result.ok)
        assertEquals(setOf(3), result.corrupt)
        assertEquals(setOf(4), result.missing)
        assertFalse(result.allGood)
        dir.deleteRecursively()
    }
}
