// Adaptateurs sortants : GTFS statique, GTFS-RT, V'Lille (GBFS), favoris. Le décodage est commun
// à toutes les cibles ; l'accès aux fichiers et au réseau (java.io, java.net, java.util.zip) est
// partagé par la JVM et Android dans le source set jvmShared. Node viendra avec le MCP v2.
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("chtilevia.kmp-library")
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
