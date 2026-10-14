// Adaptateurs sortants : GTFS statique, GTFS-RT, V'Lille (GBFS), favoris. Tout est commun aux
// cibles sauf l'accès au système (Platform.kt) : java.io / java.net / java.util.zip pour la JVM et
// Android dans le source set jvmShared, modules node:* pour Node (js).
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("chtipasse.kmp-library")
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":application"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
