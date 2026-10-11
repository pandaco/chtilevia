package fr.chtilevia.application

import fr.chtilevia.domain.Favorite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FavoritesTest {
    private val file = """
        |# Favoris de test
        |travail:      tram croise laroche vers roubaix -n 2
        |Rentrer-Velo: velo gare lille europe --prendre   # commentaire
        |rentrer-velo: velo la madeleine gare --deposer
        |vide:
        |   indenté : bus 10 marais
        |
    """.trimMargin()

    private val store = object : FavoritesStore {
        override val location = "/tmp/favoris"

        override fun read() = file
    }

    @Test
    fun favoritesAreGroupedByNameIgnoringCase() {
        assertEquals(
            listOf(
                Favorite("travail", listOf("tram croise laroche vers roubaix -n 2")),
                Favorite("Rentrer-Velo", listOf("velo gare lille europe --prendre", "velo la madeleine gare --deposer")),
                Favorite("indenté", listOf("bus 10 marais")),
            ),
            FavoritesFile.favorites(file),
        )
    }

    @Test
    fun commandsOfAFavorite() {
        assertEquals(
            listOf("velo gare lille europe --prendre", "velo la madeleine gare --deposer"),
            FindFavorite(store).execute("RENTRER-VELO"),
        )
        assertEquals(emptyList(), FindFavorite(store).execute("vide"))
        assertEquals(listOf("velo", "gare", "lille"), FavoritesFile.words(" velo\tgare  lille "))
    }

    @Test
    fun listingKeepsLinesAsWritten() {
        val listing = ListFavorites(store).execute()
        assertEquals(false, listing.isEmpty)
        assertEquals("Rentrer-Velo: velo gare lille europe --prendre", listing.lines[1])
        assertEquals("   indenté : bus 10 marais", listing.lines.last())
    }

    @Test
    fun utterancesBecomeCommands() {
        val interpret = InterpretUtterance(store)
        assertEquals(listOf("rentrer-velo"), interpret.execute("Rentrer à vélo"))
        assertEquals(listOf("bus", "10", "marais", "-d", "lille flandres"), interpret.execute("le prochain bus dix à Marais en direction de Lille Flandres"))
        assertEquals(listOf("bus", "17", "gambetta"), interpret.execute("bus dix sept gambetta"))
        assertEquals(listOf("bus", "L5", "gare"), interpret.execute("liane cinq gare"))
        assertEquals(listOf("velo", "rihour", "--deposer"), interpret.execute("V'Lille rihour pour rendre mon vélo"))
        assertEquals("tram", assertFailsWith<ChtiError.UtteranceMissingStop> { interpret.execute("tram") }.example)
        assertFailsWith<ChtiError.EmptyUtterance> { interpret.execute(" … ") }
    }
}
