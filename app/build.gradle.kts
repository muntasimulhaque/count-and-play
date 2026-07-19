plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.maqsadah.count_and_play"
    compileSdk = 35

    defaultConfig {
        // Must never change: this is the published Play Store package ID.
        // (The ".twa" suffix survives only here, in the immutable applicationId;
        // the code namespace above no longer carries it.)
        applicationId = "app.maqsadah.count_and_play.twa"
        minSdk = 23
        targetSdk = 35
        versionCode = 6
        versionName = "2.4"

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
            // serialization, or JNI — only framework APIs (TextToSpeech) and Compose,
            // both of which ship their own keep rules.
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // GameViewModel: ViewModel + viewModelScope (viewmodel-compose pulls in
    // lifecycle-viewmodel, which since 2.8.x also carries viewModelScope).
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")

    // Instrumented (emulator) screenshot capture — real Compose rendering.
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    // Provides the empty ComponentActivity that createComposeRule() hosts content in.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
