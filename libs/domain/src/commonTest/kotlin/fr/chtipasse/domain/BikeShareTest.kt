package fr.chtipasse.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BikeShareTest {
    private val stations = listOf("Rihour", "Gare Lille Flandres", "Gare Lille Europe", "Sainte Hélène", "Cœur de Ville")
        .mapIndexed { index, name -> BikeStation(index.toString(), name, null, null) }

    private fun names(query: String) = stations.matching(query).map { it.name }

    @Test
    fun stationsContainingTheQueryComeFirst() {
        assertEquals(listOf("Gare Lille Flandres", "Gare Lille Europe"), names("GARE"))
        assertEquals(listOf("Sainte Hélène"), names("sainte helene"))
        assertEquals(listOf("Cœur de Ville"), names("coeur"))
    }

    @Test
    fun otherwiseTheClosestNamesAllTied() {
        assertEquals(listOf("Rihour"), names("rihourr"))
        assertEquals(listOf("Sainte Hélène"), names("sainte élaine"))
        assertEquals(listOf("Gare Lille Flandres", "Gare Lille Europe"), names("garre lile"))
        assertEquals(emptyList(), names("zzz"))
    }
}
