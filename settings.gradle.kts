// Racine Gradle du monorepo : cœur Kotlin (libs/*) et, plus tard, appli Android (apps/android)
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "chtilevia"

// Modules nommés à plat (:domain…) mais rangés dans libs/ : pas de projet intermédiaire « :libs »
for (module in listOf("domain", "application", "presentation", "adapters", "core-tests")) {
    include(":$module")
    project(":$module").projectDir = file("libs/$module")
}
