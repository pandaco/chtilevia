// Domaine : modèles et règles métier purs. Ni Android, ni I/O ; seule bibliothèque : les dates
// (fuseau du réseau, jours de service), indispensables aux horaires.
plugins {
    id("chtipasse.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
        jsMain.dependencies {
            // Sous Node, kotlinx-datetime ne connaît que UTC sans cette base de fuseaux
            implementation(npm("@js-joda/timezone", libs.versions.js.joda.timezone.get()))
        }
    }
}
