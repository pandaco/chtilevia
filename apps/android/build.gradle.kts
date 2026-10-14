// Appli Android : voix, écran et widget sur le cœur Kotlin (libs/*). Kotlin intégré à AGP 9.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    // AppFunctions : le compilateur génère le service et le schéma lus par le système
    id("com.google.devtools.ksp")
}

android {
    namespace = "fr.chtipasse.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.chtipasse"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Timber n'arrive que par MapLibre (ses règles de lint avec) : l'appli journalise avec Log
        disable += "LogNotTimber"
    }

    // Bibliothèques natives (Compose, MapLibre) livrées déjà allégées : rien à retirer, et pas de NDK requis pour le dire
    packaging {
        jniLibs.keepDebugSymbols += listOf("**/libandroidx.graphics.path.so", "**/libmaplibre.so")
    }
}

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":adapters"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.glance.appwidget)
    implementation(libs.work.runtime)
    implementation(libs.maplibre.android)
    implementation(libs.appfunctions)
    ksp(libs.appfunctions.compiler)
}
