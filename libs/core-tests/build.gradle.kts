// Tests transverses du cœur, sur la JVM : règles d'architecture et contrat partagé (libs/contract).
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") // version fournie par buildSrc
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(project(":presentation"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.konsist)
    testRuntimeOnly(libs.junit.platform.launcher)
}


tasks.test {
    useJUnitPlatform()
    // Le compilateur Kotlin embarqué par Konsist utilise sun.misc.Unsafe, signalé à partir du JDK 23
    // (option inconnue des JDK plus anciens, d'où le test)
    if (JavaVersion.current() >= JavaVersion.VERSION_23) jvmArgs("--sun-misc-unsafe-memory-access=allow")
    // Le contrat se lit depuis le dépôt ; le relancer quand ses données changent
    val contract = rootProject.layout.projectDirectory.dir("libs/contract")
    systemProperty("chtilevia.contract", contract.asFile.absolutePath)
    systemProperty("chtilevia.root", rootProject.layout.projectDirectory.asFile.absolutePath)
    inputs.dir(contract).withPropertyName("contract").withPathSensitivity(PathSensitivity.RELATIVE)
}
