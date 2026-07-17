plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.maqsadah.count_and_play.twa"
    compileSdk = 35

    defaultConfig {
        // Must never change: this is the published Play Store package ID.
        applicationId = "app.maqsadah.count_and_play.twa"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
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
            isMinifyEnabled = false
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
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
