package fr.chtilevia.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleTest {
    private val paris = TimeZone.of("Europe/Paris")

    @Test
    fun serviceDayOriginIsNoonMinusTwelveHours() {
        // Attendus calculés par le Python de chti (zoneinfo) : « midi moins 12 h », pas minuit,
        // y compris les jours de changement d'heure
        assertEquals(1791583200, serviceDayOrigin(LocalDate(2026, 10, 10), paris))
        assertEquals(1792882800, serviceDayOrigin(LocalDate(2026, 10, 25), paris))
        assertEquals(1774735200, serviceDayOrigin(LocalDate(2026, 3, 29), paris))
    }

    @Test
    fun serviceDatesAroundMidnight() {
        // 2026-10-11 00:00:18 à Paris : la veille compte encore (courses après minuit)
        assertEquals(
            listOf(LocalDate(2026, 10, 10), LocalDate(2026, 10, 11), LocalDate(2026, 10, 12)),
            serviceDates(1791669618, paris),
        )
    }

    @Test
    fun realtimeDelayIsPropagatedToLaterStops() {
        val origin = 1_000_000L
        val stopTimes = listOf(StopTime(1, "A", 100), StopTime(2, "B", 200), StopTime(3, "C", 300), StopTime(4, "D", 400))
        // Le flux ne connaît que B (heure absolue, 60 s de retard) et D (retard seul) : A est déjà passé,
        // C hérite du retard de B. L'heure de B désigne aussi le bon jour de service parmi les trois.
        val update = TripUpdate(
            cancelled = false,
            stops = mapOf("B" to StopTimeUpdate(time = origin + 260, delay = null), "D" to StopTimeUpdate(time = null, delay = 30)),
        )
        assertEquals(
            mapOf("B" to origin + 260, "C" to origin + 360, "D" to origin + 430),
            realtimeTimes(stopTimes, update, listOf(origin - 86_400, origin, origin + 86_400)),
        )
    }

    @Test
    fun realtimeWithoutKnownStopGivesNothing() {
        val update = TripUpdate(cancelled = false, stops = mapOf("Z" to StopTimeUpdate(time = 5, delay = null)))
        assertTrue(realtimeTimes(listOf(StopTime(1, "A", 100)), update, listOf(0L)).isEmpty())
    }

    @Test
    fun departuresMergeScheduleAndRealtime() {
        val line = Line("10", "10", TransportMode.BUS, "")
        val stops = mapOf(
            "S" to Stop("S", "Gare", "", null),
            "S1" to Stop("S1", "Gare quai 1", "", "S"),
            "T" to Stop("T", "Terminus", "", null),
        )
        val trips = listOf(Trip("t1", "10", "lun", "Centre", "0"), Trip("t2", "10", "lun", "", "0"))
        val stopTimes = mapOf(
            "t1" to listOf(StopTime(1, "S1", 12 * 3600), StopTime(2, "T", 12 * 3600 + 600)),
            "t2" to listOf(StopTime(1, "S1", 13 * 3600), StopTime(2, "T", 13 * 3600 + 600)),
        )
        val schedule = LineSchedule(line, trips, stops, stopTimes)
        val day = LocalDate(2026, 10, 10)
        val origin = serviceDayOrigin(day, paris)
        val matched = schedule.matchStops("gare", trips)
        assertEquals(mapOf("S" to setOf("S1")), matched)

        val result = schedule.departures(
            of = trips,
            matched = matched,
            now = origin + 11 * 3600,
            zone = paris,
            activeDates = mapOf("lun" to setOf(day)),
            updates = mapOf("t1" to TripUpdate(false, mapOf("S1" to StopTimeUpdate(time = null, delay = 120)))),
        )
        // t1 suivie en temps réel (+2 min) ; t2 théorique, sans destination : celle de son dernier arrêt
        assertEquals(
            listOf(
                StopDepartures(
                    stopId = "S",
                    name = "Gare",
                    description = "",
                    directions = listOf(
                        HeadsignDepartures("Centre", listOf(Departure(origin + 12 * 3600 + 120, "Centre", true))),
                        HeadsignDepartures("Terminus", listOf(Departure(origin + 13 * 3600, "Terminus", false))),
                    ),
                ),
            ),
            result,
        )
    }
}
