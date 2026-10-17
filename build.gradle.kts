// Les plugins Kotlin et Android viennent de buildSrc (versions dans gradle/libs.versions.toml)
plugins {
    alias(libs.plugins.nx.project.graph)
}

// Kotlin/JS : utiliser le Node et le npm de la machine (déjà requis par Nx et apps/mcp)
// plutôt que d'en télécharger une copie
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}

// Chaque projet décrit ses tâches à Nx (cibles nx build/test inférées)
allprojects {
    apply(plugin = "dev.nx.gradle.project-graph")
}

// Zéro avertissement : un avertissement Kotlin fait échouer la compilation, dans tous les modules
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}

// Dépendances verrouillées (gradle.lockfile par projet) : builds reproductibles, et Trivy peut lire
// les versions exactes. Après un changement de dépendance : ./gradlew resolveAndLockAll --write-locks
allprojects {
    dependencyLocking { lockAllConfigurations() }

    // Outil de lint d'AGP 9.4.1 : il embarque Bouncy Castle 1.80.2 (CVE-2026-8763, CVE-2026-13506),
    // corrigé en 1.85. Il ne tourne qu'au build, jamais dans l'APK ; on le monte en attendant un AGP
    // qui le fasse lui-même
    configurations.matching { it.name == "androidLintTool" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.bouncycastle") useVersion("1.85")
        }
    }

    tasks.register("resolveAndLockAll") {
        description = "Résout toutes les configurations pour écrire les gradle.lockfile (avec --write-locks)."
        notCompatibleWithConfigurationCache("résout les configurations au moment de l'exécution")
        doFirst { require(gradle.startParameter.isWriteDependencyLocks) { "lancer avec --write-locks" } }
        doLast { configurations.filter { it.isCanBeResolved }.forEach { it.incoming.resolutionResult.rootComponent.get() } }
    }
}
