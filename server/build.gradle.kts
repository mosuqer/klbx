plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    application
}

group = "ch.pc.klbx.demo"
version = "1.0.0"
application {
    mainClass.set("ch.pc.klbx.demo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.bundles.ktor.server)
    implementation(libs.commandline)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}