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
    fun favoritesAreWrittenBackLineByLine() {
        val favorites = FavoritesFile.favorites(file)
        assertEquals(favorites, FavoritesFile.favorites(FavoritesFile.format(favorites)))
        assertEquals("a: bus 10 marais\nb: velo rihour\nb: velo gare\n", FavoritesFile.format(listOf(Favorite("a", listOf("bus 10 marais")), Favorite("b", listOf("velo rihour", "velo gare")))))
    }

    @Test
    fun understoodPhrasesBecomeFavoriteCommands() {
        val words = InterpretUtterance(store).execute("bus dix à Marais en direction de Lille Flandres")
        val command = FavoritesFile.command(words)
        assertEquals("bus 10 marais vers lille flandres", command)
        assertEquals(Command.NextDepartures("10", "marais", "lille flandres", null), CommandLine.parse(FavoritesFile.words(command)))
        // Et dans l'autre sens, pour la modifier avec les propositions
        assertEquals("velo rihour pour prendre", FavoritesFile.phrase("velo  rihour --prendre"))
        assertEquals(listOf("velo", "rihour", "--prendre"), InterpretUtterance(store).execute(FavoritesFile.phrase("velo rihour --prendre")))
        assertEquals("bus 10 marais vers lille flandres", FavoritesFile.phrase("bus 10 marais -d lille flandres"))
    }

    @Test
    fun favoriteNamesMustBeUsableInTheFile() {
        assertEquals(null, FavoritesFile.invalidName("Travail en vélo", listOf("travail")))
        assertEquals("Donne un nom au favori.", FavoritesFile.invalidName(" ", emptyList()))
        assertEquals("Le nom ne peut contenir ni « : » ni « # ».", FavoritesFile.invalidName("a:b", emptyList()))
        assertEquals("Un autre favori porte déjà ce nom.", FavoritesFile.invalidName("TRAVAIL ", listOf("travail")))
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
