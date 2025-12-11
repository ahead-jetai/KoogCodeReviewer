import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Serialization for ACP JSON-RPC protocol
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Logging (stderr only, not stdout)
    implementation("ch.qos.logback:logback-classic:1.5.12")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    // NOTE: Kotlin top-level main in Main.kt compiles to MainKt
    mainClass.set("com.example.codereview.MainKt")
    applicationName = "koog-reviewer"
}

tasks.installDist {
    // Ensure predictable output for acp.json mapping
}
