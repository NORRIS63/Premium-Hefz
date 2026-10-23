package app.premiumhefz.domain

data class PageRecord(val page: Int, val sha256: String, val bytes: Long)

/** Fingerprints of the 610 original page images, keyed by printed page number. */
object PageManifest {
    fun parse(csv: String): Map<Int, PageRecord> {
        val lines = csv.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return emptyMap()
        val header = lines.first().split(',')
        val pageCol = header.indexOf("printed_page")
        val shaCol = header.indexOf("sha256")
        val bytesCol = header.indexOf("bytes")
        require(pageCol >= 0 && shaCol >= 0 && bytesCol >= 0) { "Unexpected manifest header: ${lines.first()}" }
        val out = LinkedHashMap<Int, PageRecord>()
        for (line in lines.drop(1)) {
            val cells = line.split(',')
            val page = cells[pageCol].toInt()
            out[page] = PageRecord(page, cells[shaCol].lowercase(), cells[bytesCol].toLong())
        }
        return out
    }
}
