package fr.chtipasse.coretests

import fr.chtipasse.adapters.bikeshare.GbfsFeed
import fr.chtipasse.adapters.favorites.FileFavoritesStore
import fr.chtipasse.adapters.fetch
import fr.chtipasse.adapters.gtfs.GtfsCache
import fr.chtipasse.adapters.realtime.GtfsRealtimeFeed
import fr.chtipasse.application.Clock
import fr.chtipasse.application.FindFavorite
import fr.chtipasse.application.FindNearby
import fr.chtipasse.application.GetBikeStations
import fr.chtipasse.application.GetDepartures
import fr.chtipasse.application.InterpretUtterance
import fr.chtipasse.application.ListFavorites
import fr.chtipasse.application.NetworkProfile
import fr.chtipasse.application.Notices
import fr.chtipasse.application.UpdateTimetable
import fr.chtipasse.presentation.Chti
import fr.chtipasse.presentation.ChtiServices
import fr.chtipasse.presentation.Console
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files

/**
 * Contrat partagé (libs/contract, voir son README) joué par le cœur Kotlin : chaque cas de
 * cases.txt lance la commande [Chti] sur les fixtures, à l'heure figée du cas, et sa sortie
 * (sorties standard et d'erreur dans l'ordre, puis `[exit N]`) doit être identique à celle du shell.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContractTest {
    private val root = File(System.getProperty("chtipasse.root") ?: "../..")
    private val contract = File(System.getProperty("chtipasse.contract") ?: "../contract")
    private val fixtures = File(contract, "fixtures")

    data class Case(val name: String, val clock: String, val realtime: String, val args: String)

    private fun table(file: String): List<List<String>> =
        File(contract, file).readLines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .map { line -> line.split("|").map { it.trim() } }

    private val clocks = table("clocks.txt").associate { it[0] to it[1].toLong() }
    private val cases = table("cases.txt").map { Case(it[0], it[1], it[2], it[3]) }

    // Référentiel construit une fois pour tous les cas, depuis le zip des fixtures (≡ chti update)
    private val cache = Files.createTempDirectory("chti-kotlin-tests").toFile()
    private val gtfs by lazy {
        GtfsCache(File(cache, "chtipasse").path, fixtures.resolve("gtfs.zip").toURI().toString())
            .also { it.refresh(force = true, notices = Notices.NONE) }
    }

    @AfterAll
    fun cleanUp() {
        cache.deleteRecursively()
    }

    @Test
    fun `le contrat est cohérent`() {
        assertTrue(cases.isNotEmpty(), "aucun cas dans cases.txt")
        for (case in cases) {
            assertTrue(case.clock in clocks, "${case.name} : heure inconnue ${case.clock}")
            assertTrue(File(contract, "expected/${case.name}.txt").isFile, "${case.name} : sortie attendue absente")
            assertTrue(
                case.realtime == "aucun" || File(fixtures, case.realtime).isFile,
                "${case.name} : flux temps réel absent ${case.realtime}",
            )
        }
        assertTrue(cases.map { it.name }.toSet().size == cases.size, "noms de cas en double")
    }

    @TestFactory
    fun `cas du contrat`(): List<DynamicTest> = cases.map { case ->
        DynamicTest.dynamicTest("${case.name} : chti ${case.args}") {
            val expected = File(contract, "expected/${case.name}.txt").readText()
            assertEquals(expected, play(case))
        }
    }

    /** Sortie du cas, normalisée comme celle du shell (racine du dépôt remplacée par <racine>) */
    private fun play(case: Case): String {
        val profile = NetworkProfile.ILEVIA
        val now = clocks.getValue(case.clock)
        val clock = Clock { now }
        // « aucun » : flux absent, comme une URL injoignable
        val realtime = fixtures.resolve(if (case.realtime == "aucun") "absent.pb" else case.realtime).toURI().toString()
        val favorites = FileFavoritesStore(File(fixtures, "config/chtipasse/favoris").path)
        val bikeShare = GbfsFeed { fetch(fixtures.resolve(it).toURI().toString()) }
        val services = ChtiServices(
            departures = GetDepartures(gtfs.timetable, gtfs, GtfsRealtimeFeed { fetch(realtime) }, clock, profile),
            bikeStations = GetBikeStations(bikeShare, clock),
            nearby = FindNearby(gtfs.timetable, gtfs, bikeShare, clock),
            listFavorites = ListFavorites(favorites),
            findFavorite = FindFavorite(favorites),
            interpretUtterance = InterpretUtterance(favorites),
            updateTimetable = UpdateTimetable(gtfs),
        )
        val output = StringBuilder()
        val console = object : Console {
            override fun out(text: String) {
                output.append(text)
            }

            override fun err(text: String) {
                output.append(text)
            }
        }
        // Arguments découpés sur les espaces, sans guillemets ni jokers (comme `set -- $args`)
        val args = case.args.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val exit = Chti(services, profile.timeZone).run(args, console)
        return output.toString().replace(root.path, "<racine>") + "[exit $exit]\n"
    }
}
