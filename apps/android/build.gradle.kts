// Appli Android : voix, écran et widget sur le cœur Kotlin (libs/*). Kotlin intégré à AGP 9.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.chtilevia.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.chtilevia"
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

    // Bibliothèque native de Compose livrée déjà allégée : rien à retirer, et pas de NDK requis pour le dire
    packaging {
        jniLibs.keepDebugSymbols += "**/libandroidx.graphics.path.so"
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
    implementation(libs.glance.appwidget)
}
