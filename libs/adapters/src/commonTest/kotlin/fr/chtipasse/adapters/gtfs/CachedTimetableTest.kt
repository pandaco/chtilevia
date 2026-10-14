package fr.chtipasse.adapters.gtfs

import fr.chtipasse.application.TimetableRepository
import fr.chtipasse.domain.Line
import fr.chtipasse.domain.Stop
import fr.chtipasse.domain.StopTime
import fr.chtipasse.domain.TransportMode
import fr.chtipasse.domain.Trip
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CachedTimetableTest {
    /** Référentiel qui compte ses lectures */
    private class CountingTimetable : TimetableRepository {
        val reads = mutableListOf<String>()

        override fun lines(): List<Line> = listOf(Line("10", "10", TransportMode.BUS, "")).also { reads += "lines" }

        override fun trips(lineId: String): List<Trip> = emptyList<Trip>().also { reads += "trips $lineId" }

        override fun stops(): Map<String, Stop> =
            listOf("A", "B").associateWith { Stop(it, it, "", null) }.also { reads += "stops" }

        override fun stopTimes(lineId: String): Map<String, List<StopTime>> =
            emptyMap<String, List<StopTime>>().also { reads += "stopTimes $lineId" }

        override fun linesServing(stopIds: Set<String>): Map<String, Set<String>> =
            stopIds.associateWith { setOf("10") }.also { reads += "linesServing ${stopIds.sorted()}" }

        override fun activeDates(dates: Collection<LocalDate>): Map<String, Set<LocalDate>> =
            emptyMap<String, Set<LocalDate>>().also { reads += "activeDates" }
    }

    private val source = CountingTimetable()
    private var version = 1
    private val cached = CachedTimetable(source, keptLines = 2) { version }
    private val today = listOf(LocalDate(2026, 10, 12))

    @Test
    fun readsOnceUntilTheTimetableChanges() {
        repeat(2) {
            cached.lines()
            cached.stops()
            cached.trips("10")
            cached.stopTimes("10")
            cached.activeDates(today)
        }
        assertEquals(listOf("lines", "stops", "trips 10", "stopTimes 10", "activeDates"), source.reads)

        version = 2
        cached.stops()
        cached.activeDates(today)
        assertEquals(listOf("stops", "activeDates"), source.reads.drop(5))
    }

    @Test
    fun keepsOnlyTheLastStopTimes() {
        for (line in listOf("A", "B", "A", "C", "A", "B")) cached.stopTimes(line)
        // A reste (utilisée récemment), B sort quand C arrive, puis revient
        assertEquals(listOf("stopTimes A", "stopTimes B", "stopTimes C", "stopTimes B"), source.reads)
    }

    @Test
    fun linesOfAllStopsAreReadOnce() {
        assertEquals(mapOf("A" to setOf("10")), cached.linesServing(setOf("A", "Z")))
        assertEquals(mapOf("B" to setOf("10")), cached.linesServing(setOf("B")))
        assertEquals(listOf("stops", "linesServing [A, B]"), source.reads)
    }
}
