plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.maqsadah.count_and_play"
    compileSdk = 37

    defaultConfig {
        // Must never change: this is the published Play Store package ID.
        // (The ".twa" suffix survives only here, in the immutable applicationId;
        // the code namespace above no longer carries it.)
        applicationId = "app.maqsadah.count_and_play.twa"
        minSdk = 23
        targetSdk = 37
        versionCode = 23
        versionName = "7.3"

        // Instrumented tests (the emulator screenshot capture) use AndroidX's runner.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Provided by CI (see .github/workflows/build.yml). Local builds
            // without the keystore fall back to an unsigned release build.
            val ksFile = file(System.getenv("KEYSTORE_FILE") ?: "signing.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 code shrinking + resource shrinking. Safe here: no reflection,
            // serialization, or JNI, only framework APIs (TextToSpeech) and
            // Compose, both of which ship their own keep rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ks = signingConfigs.getByName("release")
            if (ks.storeFile?.exists() == true) {
                signingConfig = ks
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Full lint runs in CI next to the unit tests (:app:lintRelease), not
        // just the vital subset that rides along with assembleRelease.
        abortOnError = true
        checkDependencies = false
    }
}

dependencies {
    // The Compose BOM governs every Compose artifact. Keep it reasonably
    // current: ui-test releases after 1.7 work on API 35+ emulators again,
    // which is what lets instrumented runs happen on modern local images.
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // GameViewModel: ViewModel + viewModelScope (viewmodel-compose pulls in
    // lifecycle-viewmodel, which since 2.8.x also carries viewModelScope).
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    // Pure-JVM persistent collections: the domain's list fields stay immutable
    // values, so Compose can see domain states as stable without any Android
    // or Compose annotation touching core/.
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    testImplementation("junit:junit:4.13.2")

    // Instrumented (emulator) screenshot capture: a bare ComponentActivity
    // hosts each state and PixelCopy grabs the window. No compose test rule,
    // no Espresso, no injection machinery: rendering states and copying
    // pixels needs none of it, so captures keep working on whatever image
    // the app targets, including Android 17 where Espresso's injector breaks.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
