package fr.chtilevia.application

import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.Stop
import fr.chtilevia.domain.StopTime
import fr.chtilevia.domain.TransportMode
import fr.chtilevia.domain.Trip
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestQueryTest {
    /** Ligne 10 (Marais → Gare Lille Flandres, et retour) et un tram, sans horaires utiles ici */
    private val timetable = object : TimetableRepository {
        override fun lines() = listOf(
            Line("10", "10", TransportMode.BUS, ""),
            Line("12", "12", TransportMode.BUS, ""),
            Line("71", "TRAM", TransportMode.TRAM, ""),
        )

        override fun trips(lineId: String) = listOf(
            Trip("a", lineId, "s", "Lille Gare Lille Flandres", "0"),
            Trip("b", lineId, "s", "Lomme Hegel", "1"),
        )

        override fun stops() = listOf("Marais", "Eglise Du Marais", "Gambetta", "Gare Lille Flandres")
            .associate { it to Stop(it, it, "", null) }

        override fun stopTimes(lineId: String) = mapOf(
            "a" to listOf(StopTime(1, "Marais", 0), StopTime(2, "Eglise Du Marais", 60), StopTime(3, "Gambetta", 120), StopTime(4, "Gare Lille Flandres", 180)),
            "b" to listOf(StopTime(1, "Gare Lille Flandres", 0), StopTime(2, "Gambetta", 60)),
        )

        override fun linesServing(stopIds: Set<String>) = emptyMap<String, Set<String>>()

        override fun activeDates(dates: Collection<LocalDate>) = emptyMap<String, Set<LocalDate>>()
    }
    private val favorites = object : FavoritesStore {
        override val location = "favoris"

        override fun read() = "travail: tram croise laroche vers roubaix\n"
    }
    private val bikes = BikeShareFeed { listOf(BikeStation("1", "Rihour", 32, null), BikeStation("2", "Gare Lille Europe", 32, null)) }
    private val suggest = SuggestQuery(timetable, bikes, favorites) { 0L }

    private fun texts(input: String) = suggest.execute(input).items.map { it.text }

    @Test
    fun startsWithModesAndFavorites() {
        assertEquals("Que cherches-tu ?", suggest.execute("").step)
        assertTrue("travail" in texts(""))
        assertEquals(listOf("bus "), texts("bu"))
        assertEquals(listOf("vélo ", "vélo à proximité"), texts("vél"))
    }

    @Test
    fun busLineThenStopThenDirection() {
        assertEquals("Quelle ligne ?", suggest.execute("bus ").step)
        assertEquals(listOf("bus 10 ", "bus 12 "), texts("bus 1"))
        assertEquals(listOf("bus 10 Marais ", "bus 10 Eglise Du Marais "), texts("bus 10 mar"))
        assertEquals(listOf("bus 10 Gambetta ", "bus 10 Gare Lille Flandres "), texts("bus 10 ga"))
        // Arrêt complet : les directions, facultatives
        assertEquals(
            listOf("bus 10 Gambetta vers Lille Gare Lille Flandres", "bus 10 Gambetta vers Lomme Hegel"),
            texts("bus 10 Gambetta "),
        )
        assertEquals(listOf("bus 10 Gambetta vers Lomme Hegel"), texts("bus 10 Gambetta vers hege"))
        assertEquals(listOf("bus à proximité"), texts("bus à").take(1))
    }

    @Test
    fun tramGoesStraightToStops() {
        assertEquals(listOf("tram Gambetta "), texts("tram gamb"))
    }

    @Test
    fun bikeStationThenTakeOrReturn() {
        assertEquals(listOf("vélo Gare Lille Europe "), texts("vélo gare"))
        assertEquals(listOf("vélo Rihour pour prendre", "vélo Rihour pour déposer"), texts("vélo Rihour "))
        assertEquals("C'est complet", suggest.execute("vélo Rihour pour prendre").step)
    }
}
