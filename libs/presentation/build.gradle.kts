// Présentation : mise en forme des résultats (texte terminal, phrases à lire, JSON du contrat).
plugins {
    id("chtipasse.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":application"))
        }
    }
}
