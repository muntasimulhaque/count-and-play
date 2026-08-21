plugins {
    // These two move together: org.jetbrains.kotlin.plugin.compose must always
    // be the same version as the Kotlin compiler built into the AGP line.
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
