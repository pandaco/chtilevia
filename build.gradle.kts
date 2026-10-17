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
