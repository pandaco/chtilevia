package fr.chtipasse.presentation

import fr.chtipasse.application.BikeStations
import fr.chtipasse.application.Departures
import fr.chtipasse.domain.BikeMode
import fr.chtipasse.domain.BikeStation
import fr.chtipasse.domain.BikeStationStatus
import fr.chtipasse.domain.Departure
import fr.chtipasse.domain.HeadsignDepartures
import fr.chtipasse.domain.Line
import fr.chtipasse.domain.StopDepartures
import fr.chtipasse.domain.TransportMode
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
