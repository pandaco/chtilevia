package fr.chtipasse.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApproximateMatchTest {
    @Test
    fun soundMergesFrenchSpellings() {
        assertEquals(sound("maur"), sound("mort"))
        assertEquals(sound("marque"), sound("marc"))
        assertEquals(sound("scientifique"), sound("scientifik"))
        assertEquals(sound("beaux"), sound("bo"))
        assertEquals(sound("terrasse"), sound("terasse"))
        assertEquals("sh", sound("ch"))
    }

    @Test
    fun editDistanceCountsInsertionsDeletionsAndSubstitutions() {
        assertEquals(0, editDistance("marais", "marais"))
        assertEquals(1, editDistance("gambeta", "gambetta"))
        assertEquals(1, editDistance("clemenseau", "clemenceau"))
        assertEquals(3, editDistance("", "abc"))
        assertEquals(3, editDistance("kitten", "sitting"))
    }

    @Test
    fun gapToleratesTyposSpacingMissingWordsAndSounds() {
        assertEquals(0, approximateGap("gambeta", "Gambetta"))
        assertEquals(1, approximateGap("victor", "Victoire"))
        assertEquals(0, approximateGap("croisé la roche", "Croise Laroche"))
        assertEquals(0, approximateGap("eglise marais", "Eglise Du Marais"))
        assertEquals(0, approximateGap("saint mort", "Saint Maur"))
        assertEquals(0, approximateGap("euro teleport", "Roubaix Eurotéléport"))
    }

    @Test
    fun numbersInWordsMatchDigits() {
        assertEquals(0, approximateGap("quatre cantons", "4 Cantons Stade P. Mauroy"))
        assertEquals(0, approximateGap("Quatre-Chemins", "Ascq 4 Chemins"))
        assertEquals(0, approximateGap("dix huit juin", "18 Juin"))
        assertEquals(0, approximateGap("huit mai", "Rue Du 8 Mai 45"))
        // « Pont Neuf » n'est pas « Pont 9 » : le nom tel quel compte aussi
        assertEquals(0, approximateGap("pont neuf", "Pont Neuf"))
    }

    @Test
    fun gapRejectsUnrelatedNames() {
        assertNull(approximateGap("zzz", "Marais"))
        assertNull(approximateGap("bonjour", "Gambetta"))
        assertNull(approximateGap("", "Marais"))
        assertNull(approximateGap("lami", "Lomme Hegel"))
    }

    @Test
    fun closestKeepsEveryTie() {
        val names = listOf("Marais", "Eglise Du Marais", "Loos Mairie", "Lamy")
        assertEquals(listOf("Marais", "Eglise Du Marais", "Loos Mairie"), names.closestTo("marie") { listOf(it) })
        assertEquals(listOf("La Marque"), listOf("Parc Barbieux", "La Marque").closestTo("la marc") { listOf(it) })
        assertEquals(emptyList(), names.closestTo("zzz") { listOf(it) })
    }
}
