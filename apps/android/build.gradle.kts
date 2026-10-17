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
        // Écrits par Nx Release dans gradle.properties (voir ce fichier)
        versionCode = property("versionCode").toString().toInt()
        versionName = property("versionName").toString()
    }

    // Signature de release fournie par la CI (secrets GitHub) ; sans elle, l'APK de release n'est pas signé
    val keystore = providers.environmentVariable("ANDROID_KEYSTORE_FILE")
    if (keystore.isPresent) {
        signingConfigs.create("release") {
            storeFile = file(keystore.get())
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
            keyPassword = storePassword
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Zéro avertissement : le moindre avertissement du lint Android fait échouer le build
        warningsAsErrors = true
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
