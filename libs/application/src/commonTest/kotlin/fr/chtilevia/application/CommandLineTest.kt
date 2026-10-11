package fr.chtilevia.application

import fr.chtilevia.domain.BikeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommandLineTest {
    private fun parse(command: String) = CommandLine.parse(command.split(' '))

    @Test
    fun optionsAnywhereAndVersWithoutDash() {
        assertEquals(Command.NextDepartures("10", "marais", "chu", 2), parse("bus 10 --nombre=2 --vers=chu marais"))
        assertEquals(Command.NextDepartures("TRAM", "croise laroche", "roubaix", null), parse("tram croise laroche vers roubaix"))
        assertEquals(Command.NextDepartures("M1", "republique", "lille chu", 4), parse("metro m1 republique -n 4 vers lille chu"))
        assertEquals(Command.NextDepartures("10", "", "hegel", null), parse("bus 10 -d hegel"))
    }

    @Test
    fun countIsValidatedLikeTheShell() {
        assertEquals(0, (parse("tram x -n 00") as Command.NextDepartures).count)
        assertEquals("abc", assertFailsWith<ChtiError.InvalidCount> { parse("tram -n abc") }.value)
        assertFailsWith<ChtiError.InvalidCount> { parse("tram -n 0") }
        val missing = assertFailsWith<ChtiError.MissingOptionValue> { parse("bus 10 marais -d") }
        assertEquals("-d", missing.option)
        assertEquals(ChtiError.MissingOptionValue.Expected.DIRECTION, missing.expected)
    }

    @Test
    fun commandsAndFavorites() {
        assertEquals(Command.BikeStations("sainte helene", BikeMode.RETURN), parse("vélo sainte --déposer helene"))
        assertEquals(Command.BikeStations("", BikeMode.TAKE), parse("velo --prendre"))
        assertEquals(Command.Utterance("métro un république"), parse("vocal métro un république"))
        assertEquals(Command.FavoriteOrUnknown("travail"), parse("travail"))
        assertEquals("inconnu", assertFailsWith<ChtiError.UnknownCommand> { parse("inconnu foo") }.command)
        assertFailsWith<ChtiError.UnknownMetro> { parse("metro 3") }
        assertFailsWith<ChtiError.MissingLine> { parse("bus") }
    }
}
