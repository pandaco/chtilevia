package fr.chtilevia.presentation

import fr.chtilevia.application.BikeStations
import fr.chtilevia.application.Departures
import fr.chtilevia.domain.BikeMode
import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.BikeStationStatus
import fr.chtilevia.domain.Departure
import fr.chtilevia.domain.HeadsignDepartures
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.StopDepartures
import fr.chtilevia.domain.TransportMode
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class BriefFormatterTest {
    private val brief = BriefFormatter(TimeZone.of("Europe/Paris"))
    private val now = 1791629160L // 2026-10-10 12:46 à Paris

    @Test
    fun departuresOnOneLinePerDirection() {
        val tram = Line("71", "TRAM", TransportMode.TRAM, "")
        val stop = StopDepartures(
            "1137", "Croise Laroche", "",
            listOf(
                HeadsignDepartures("Roubaix Eurotéléport", listOf(Departure(now + 20, "R", true), Departure(now + 780, "R", true))),
                HeadsignDepartures("Lille", listOf(Departure(now + 4200, "L", false))),
            ),
        )
        assertEquals(
            "🚊 Tram · Croise Laroche → Roubaix Eurotéléport : à l'approche, 13 min\n" +
                "🚊 Tram · Croise Laroche → Lille : 13:56 (théorique)\n",
            brief.departures(Departures(tram, now, listOf(stop), emptyList())),
        )
    }

    @Test
    fun bikeStationsWithPictograms() {
        fun station(name: String, renting: Boolean, returning: Boolean) = BikeStation(
            name, name, 32, BikeStationStatus(installed = true, renting = renting, returning = returning, bikes = 14, docks = 18, lastReported = now),
        )
        val stations = listOf(station("Gare Lille Europe", true, true), station("Rihour", true, false), station("Port", false, false))
        assertEquals(
            "Gare Lille Europe : 🚲 14 · 🅿️ 18\nRihour : 🚲 14 · 🅿️ 18 (dépôt impossible)\nPort : ⛔ hors service\n",
            brief.bikeStations(BikeStations("gare", BikeMode.ALL, now, stations)),
        )
        assertEquals("Gare Lille Europe : 🅿️ 18\n", brief.bikeStations(BikeStations("gare", BikeMode.RETURN, now, stations.take(1))))
    }
}
