// Convention des bibliothèques du cœur : Kotlin Multiplatform, mêmes cibles partout.
//  - jvm     : serveur, outils, tests rapides
//  - android : appli (apps/android)
//  - js/node : serveur MCP v2 (apps/mcp)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    // Bytecode Java 17 : compatible Android et toute JVM récente, quel que soit le JDK de compilation
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    android {
        namespace = "fr.chtilevia." + project.name.replace("-", "")
        compileSdk = 37
        minSdk = 26
        withHostTest {}
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    js {
        nodejs()
    }
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Node et npm de la machine (déjà requis par Nx et apps/mcp) plutôt qu'une copie téléchargée
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}

// Tests Node : le reporter Mocha livré avec Kotlin 2.4.20 (kotlin-web-helpers 3.3.0, jusqu'à 3.5.3)
// affiche à chaque exécution un avertissement sur une option qu'il se passe lui-même. Ce préchargement
// ne retire que ce message. À supprimer quand Kotlin livrera kotlin-web-helpers ≥ 3.5.4.
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().configureEach {
    nodeJsArgs += listOf("--require", rootDir.resolve("gradle/kotlin-js-test-preload.cjs").path)
}

// « test » : même nom de tâche que les autres projets du dépôt (nx run-many -t test), toutes cibles
tasks.register("test") {
    group = "verification"
    description = "Lance les tests de toutes les cibles (JVM, Android hôte, Node)."
    dependsOn("allTests", "testAndroidHostTest")
}
