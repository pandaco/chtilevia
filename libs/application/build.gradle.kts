// Application : cas d'usage et ports (interfaces vers le GTFS, le temps réel, V'Lille, l'horloge…).
plugins {
    id("chtilevia.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
        }
    }
}
