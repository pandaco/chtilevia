package fr.chtilevia.adapters.gtfs

import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.Notice
import fr.chtilevia.application.Notices
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GtfsCacheTest {
    private val dir = Files.createTempDirectory("gtfs-cache-test").toFile()
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
        dir.deleteRecursively()
    }

    private fun refresh(force: Boolean = false) = cache.refresh(force, Notices { notices += it })

    @Test
    fun downloadsOnceThenServesTheCacheForADay() {
        refresh()
        assertEquals(listOf(Notice.TIMETABLE_DOWNLOAD), notices)
        assertEquals("t1\tA\t1\t08:00:00\nt1\tB\t2\t08:05:00\n", File(cache.directory, "idx/10.tsv").readText())
        assertEquals(listOf(8 * 3600, 8 * 3600 + 300), cache.timetable.stopTimes("10").getValue("t1").map { it.time })
        assertFalse(File(dir, "evasion.txt").exists(), "une entrée « ../ » ne sort pas du cache")

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
        assertTrue(File(cache.directory, "routes.txt").isFile)
        // Pas de nouvel essai avant une heure
        now += 30 * 60_000L
        refresh()
        assertEquals(2, downloads)
    }

    @Test
    fun failsWithoutAnyTimetable() {
        archive = null
        assertFailsWith<ChtiError.TimetableUnavailable> { refresh() }
        archive = "pas un zip".toByteArray()
        assertFailsWith<ChtiError.TimetableUnavailable> { refresh(force = true) }
    }

    private fun zip(vararg files: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { out ->
            for ((name, content) in files) {
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
}
