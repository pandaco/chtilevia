package fr.chtilevia.coretests

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Règle de dépendance de l'architecture hexagonale, vérifiée sur les imports du code de production :
 * domain ← application ← presentation, et le domaine n'importe que la bibliothèque standard.
 */
class ArchitectureTest {
    private val core = Konsist.scopeFromProduction().files.filter { it.path.contains("/libs/") }

    private fun layer(name: String) = core.filter { it.packagee?.name?.startsWith("fr.chtilevia.$name") == true }

    @Test
    fun `le domaine ne dépend que de la bibliothèque standard`() {
        // java.text : décomposition Unicode côté JVM/Android, équivalent de String.normalize en JS
        val allowed = listOf("kotlin.", "java.text.", "fr.chtilevia.domain")
        layer("domain").assertTrue(testName = "domain") { file ->
            file.imports.all { import -> allowed.any { import.name.startsWith(it) } }
        }
    }

    @Test
    fun `application ne dépend que du domaine`() {
        val allowed = listOf("kotlin.", "fr.chtilevia.domain", "fr.chtilevia.application")
        layer("application").assertTrue(testName = "application") { file ->
            file.imports.all { import -> allowed.any { import.name.startsWith(it) } }
        }
    }

    @Test
    fun `presentation ne dépend que du domaine et de l'application`() {
        val allowed = listOf("kotlin.", "fr.chtilevia.domain", "fr.chtilevia.application", "fr.chtilevia.presentation")
        layer("presentation").assertTrue(testName = "presentation") { file ->
            file.imports.all { import -> allowed.any { import.name.startsWith(it) } }
        }
    }

    @Test
    fun `chaque fichier du cœur est dans le paquet de sa couche`() {
        core.assertTrue(testName = "paquets") { file ->
            val module = Regex("/libs/([^/]+)/").find(file.path)?.groupValues?.get(1)
            module == null || file.packagee?.name?.startsWith("fr.chtilevia.$module") == true
        }
    }
}
