package fr.chtipasse.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoTest {
    // Attendus calculés par le shell (haversine en Python) : même arrondi sur toutes les plateformes
    @Test
    fun distanceMatchesShellImplementation() {
        val rihour = Position(50.636062, 3.062306)
        assertEquals(633, distanceMeters(rihour, Position(50.637779, 3.070865)))
        assertEquals(0, distanceMeters(Position(50.6363, 3.064), Position(50.6363, 3.064)))
        assertEquals(203644, distanceMeters(Position(50.63, 3.06), Position(48.8566, 2.3522)))
    }
}
