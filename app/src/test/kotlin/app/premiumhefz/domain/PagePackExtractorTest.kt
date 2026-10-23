package app.premiumhefz.domain

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagePackExtractorTest {
    private class MemorySink(private val preinstalled: Set<Int> = emptySet()) : PageSink {
        val committed = HashMap<Int, ByteArray>()
        private val open = HashMap<Int, ByteArrayOutputStream>()
        override fun isInstalled(page: Int) = page in preinstalled
        override fun open(page: Int): OutputStream = ByteArrayOutputStream().also { open[page] = it }
        override fun commit(page: Int) { committed[page] = open.remove(page)!!.toByteArray() }
        override fun discard(page: Int) { open.remove(page) }
    }

    private fun sha(bytes: ByteArray) = PagePackExtractor.toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            for ((name, data) in entries) {
                z.putNextEntry(ZipEntry(name))
                z.write(data)
                z.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    @Test
    fun keepsOnlyPagesWhoseHashMatchesTheManifest() {
        val good = "page two".toByteArray()
        val bad = "tampered".toByteArray()
        val manifest = mapOf(
            2 to PageRecord(2, sha(good), good.size.toLong()),
            3 to PageRecord(3, sha("original three".toByteArray()), 14L),
            4 to PageRecord(4, sha("four".toByteArray()), 4L)
        )
        val archive = zip(
            mapOf(
                "res/drawable-hdpi-v4/page003.gif" to good,      // printed page 2, correct
                "res/drawable-hdpi-v4/page004.gif" to bad,       // printed page 3, wrong bytes
                "res/drawable-hdpi-v4/page005.gif" to "four".toByteArray(), // printed page 4, already installed
                "res/layout/activity_main.xml" to "<x/>".toByteArray(),
                "res/drawable-hdpi-v4/page999.gif" to "unknown".toByteArray()
            )
        )
        val sink = MemorySink(preinstalled = setOf(4))
        val result = PagePackExtractor.extract(archive.inputStream(), manifest, sink)

        assertEquals(setOf(2), result.verified)
        assertEquals(setOf(3), result.failed)
        assertEquals(setOf(4), result.skipped)
        assertEquals(setOf(2), sink.committed.keys)
        assertTrue(sink.committed[2]!!.contentEquals(good))
    }

    @Test
    fun manifestCsvParsesByHeaderName() {
        val csv = "printed_page,file_index,sha256,bytes\n2,3,ABCDEF,10\n3,4,012345,20\n"
        val parsed = PageManifest.parse(csv)
        assertEquals(2, parsed.size)
        assertEquals("abcdef", parsed[2]!!.sha256)
        assertEquals(20L, parsed[3]!!.bytes)
    }
}
