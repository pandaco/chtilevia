package fr.chtilevia.coretests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * Harnais du contrat partagé (libs/contract, voir son README) pour le cœur Kotlin.
 * Un test par cas de cases.txt ; tant que le cœur ne sait pas jouer un cas, il est marqué
 * « ignoré » plutôt que réussi, pour que le compteur montre ce qu'il reste à porter.
 */
class ContractTest {
    private val contract = File(System.getProperty("chtilevia.contract") ?: "../contract")

    data class Case(val name: String, val clock: String, val realtime: String, val args: String)

    private fun table(file: String): List<List<String>> =
        File(contract, file).readLines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .map { line -> line.split("|").map { it.trim() } }

    private val clocks = table("clocks.txt").associate { it[0] to it[1].toLong() }
    private val cases = table("cases.txt").map { Case(it[0], it[1], it[2], it[3]) }

    @Test
    fun `le contrat est cohérent`() {
        assertTrue(cases.isNotEmpty(), "aucun cas dans cases.txt")
        for (case in cases) {
            assertTrue(case.clock in clocks, "${case.name} : heure inconnue ${case.clock}")
            assertTrue(File(contract, "expected/${case.name}.txt").isFile, "${case.name} : sortie attendue absente")
            assertTrue(
                case.realtime == "aucun" || File(contract, "fixtures/${case.realtime}").isFile,
                "${case.name} : flux temps réel absent ${case.realtime}",
            )
        }
        assertTrue(cases.map { it.name }.toSet().size == cases.size, "noms de cas en double")
    }

    @TestFactory
    fun `cas du contrat`(): List<DynamicTest> = cases.map { case ->
        DynamicTest.dynamicTest("${case.name} : chti ${case.args}") {
            abort<Unit>("à porter dans le cœur Kotlin (étape 5)")
        }
    }
}
