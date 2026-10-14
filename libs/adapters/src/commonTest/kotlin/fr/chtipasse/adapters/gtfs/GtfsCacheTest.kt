package fr.chtipasse.adapters.gtfs

import fr.chtipasse.adapters.LocalFiles
import fr.chtipasse.application.ChtiError
import fr.chtipasse.application.Notice
import fr.chtipasse.application.Notices
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GtfsCacheTest {
    private val dir = LocalFiles.createTempDirectory(LocalFiles.systemTemporaryDirectory(), "gtfs-cache-test")
    private var now = 1_000_000_000_000L
    private var downloads = 0
    private var archive: ByteArray? = zip(
        "routes.txt" to "route_id,route_short_name,route_desc,route_type\n10,10,Ligne 10,3\n",
        "trips.txt" to "route_id,service_id,trip_id,trip_headsign,direction_id\n10,s,t1,Gare,0\n",
        "stop_times.txt" to "trip_id,arrival_time,departure_time,stop_id,stop_sequence\nt1,08:00:00,,A,1\nt1,,08:05:00,B,2\n",
        "../evasion.txt" to "hors du cache",
    )
    private val notices = mutableListOf<Notice>()
    private val cache = GtfsCache(dir, "https://exemple/gtfs.zip", { downloads++; archive }, { now })

    @AfterTest
    fun cleanUp() {
        LocalFiles.deleteRecursively(dir)
    }

    private fun refresh(force: Boolean = false) = cache.refresh(force, Notices { notices += it })

    @Test
    fun downloadsOnceThenServesTheCacheForADay() {
        refresh()
        assertEquals(listOf(Notice.TIMETABLE_DOWNLOAD), notices)
        assertEquals("t1\tA\t1\t08:00:00\nt1\tB\t2\t08:05:00\n", LocalFiles.readText("${cache.directory}/idx/10.tsv"))
        assertEquals(listOf(8 * 3600, 8 * 3600 + 300), cache.timetable.stopTimes("10").getValue("t1").map { it.time })
        assertFalse(LocalFiles.exists("$dir/evasion.txt"), "une entrée « ../ » ne sort pas du cache")

        now += 23 * 3600_000L
        refresh()
        assertEquals(1, downloads)
        now += 2 * 3600_000L
        refresh()
        assertEquals(2, downloads)
    }

    @Test
    fun keepsTheOldTimetableWhenTheDownloadFails() {
        refresh()
        archive = null
        now += 25 * 3600_000L
        notices.clear()
        refresh()
        assertEquals(listOf(Notice.TIMETABLE_DOWNLOAD, Notice.TIMETABLE_STALE), notices)
        assertTrue(LocalFiles.isFile("${cache.directory}/routes.txt"))
        // Pas de nouvel essai avant une heure
        now += 30 * 60_000L
        refresh()
        assertEquals(2, downloads)
    }

    @Test
    fun canDropStopTimesOnceIndexed() {
        val light = GtfsCache("$dir/leger", "https://exemple/gtfs.zip", { archive }, { now }, keepStopTimes = false)
        light.refresh(force = false, notices = Notices.NONE)
        assertFalse(LocalFiles.exists("${light.directory}/stop_times.txt"))
        assertEquals(listOf(8 * 3600, 8 * 3600 + 300), light.timetable.stopTimes("10").getValue("t1").map { it.time })
    }

    @Test
    fun failsWithoutAnyTimetable() {
        archive = null
        assertFailsWith<ChtiError.TimetableUnavailable> { refresh() }
        archive = "pas un zip".encodeToByteArray()
        assertFailsWith<ChtiError.TimetableUnavailable> { refresh(force = true) }
    }

    /** Archive zip aux entrées stockées sans compression (méthode 0) */
    private fun zip(vararg files: Pair<String, String>): ByteArray {
        val out = LittleEndianBytes()
        val central = LittleEndianBytes()
        for ((name, content) in files) {
            val data = content.encodeToByteArray()
            val path = name.encodeToByteArray()
            val offset = out.size
            val crc = crc32(data)
            // En-tête local : signature, version, drapeaux, méthode, date/heure, crc, tailles, nom
            out.u32(0x04034b50).u16(20).u16(0).u16(0).u32(0).u32(crc)
            out.u32(data.size.toLong()).u32(data.size.toLong()).u16(path.size).u16(0).bytes(path).bytes(data)
            // Entrée du répertoire central, qui renvoie à l'en-tête local
            central.u32(0x02014b50).u16(20).u16(20).u16(0).u16(0).u32(0).u32(crc)
            central.u32(data.size.toLong()).u32(data.size.toLong()).u16(path.size).u16(0).u16(0)
            central.u16(0).u16(0).u32(0).u32(offset).bytes(path)
        }
        val start = out.size
        out.bytes(central.toByteArray())
        out.u32(0x06054b50).u16(0).u16(0).u16(files.size).u16(files.size)
        out.u32(central.size).u32(start).u16(0)
        return out.toByteArray()
    }

    private class LittleEndianBytes {
        private val bytes = mutableListOf<Byte>()
        val size get() = bytes.size.toLong()

        fun u16(value: Int) = apply { bytes += listOf(value.toByte(), (value shr 8).toByte()) }

        fun u32(value: Long) = u16((value and 0xFFFF).toInt()).u16((value shr 16).toInt())

        fun bytes(value: ByteArray) = apply { bytes += value.toList() }

        fun toByteArray() = bytes.toByteArray()
    }

    private fun crc32(data: ByteArray): Long {
        var crc = 0xFFFFFFFFL
        for (b in data) {
            crc = crc xor (b.toLong() and 0xFF)
            repeat(8) { crc = if (crc and 1L != 0L) (crc ushr 1) xor 0xEDB88320L else crc ushr 1 }
        }
        return crc xor 0xFFFFFFFFL
    }
}
