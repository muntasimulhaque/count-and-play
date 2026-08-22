// Offline asset generators: sound synthesis and store-art rendering. Plain
// Kotlin on the JVM, no Android, no third-party libraries: sounds write WAVs
// directly, art draws with Java2D. Outputs are committed; these tasks only run
// when a design deliberately changes.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    // Match the app: build with whatever JDK runs Gradle (the Studio JBR),
    // emit Java 17 bytecode.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.register<JavaExec>("makeSounds") {
    group = "tools"
    description = "Regenerate the six sound assets in app/src/main/res/raw."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "toybox.tools.MakeSoundsMainKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkSounds") {
    group = "tools"
    description = "Verify the committed sound assets match a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "toybox.tools.MakeSoundsMainKt"
    args = listOf(rootDir.absolutePath, "--check")
}

tasks.register<JavaExec>("makeArt") {
    group = "tools"
    description = "Regenerate store art and launcher icons from the design system."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "toybox.tools.MakeArtMainKt"
    args = listOf(rootDir.absolutePath)
}
