// Racine de composition pour Node : la commande chti du cœur Kotlin, exportée en module ES pour
// le serveur MCP (apps/mcp, CoreGateway). Assemble les cas d'usage avec les adaptateurs Node.
plugins {
    kotlin("multiplatform") // version fournie par buildSrc
}

kotlin {
    js {
        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
    sourceSets {
        jsMain.dependencies {
            implementation(project(":presentation"))
            implementation(project(":adapters"))
        }
    }
}

// Node et npm de la machine (déjà requis par Nx et apps/mcp) plutôt qu'une copie téléchargée
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}
