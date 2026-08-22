plugins {
    // These two move together: org.jetbrains.kotlin.plugin.compose must always
    // be the same version as the Kotlin compiler built into the AGP line.
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    // The offline asset generators (:tools) are plain JVM Kotlin, pinned to
    // the same compiler version the app builds with.
    id("org.jetbrains.kotlin.jvm") version "2.2.10" apply false
}
