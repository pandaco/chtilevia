// Les plugins Kotlin et Android viennent de buildSrc (versions dans gradle/libs.versions.toml)

// Kotlin/JS : utiliser le Node et le npm de la machine (déjà requis par Nx et apps/mcp)
// plutôt que d'en télécharger une copie
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}
