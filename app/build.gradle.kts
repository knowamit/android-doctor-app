plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.github.triplet.play")
}

android {
    namespace = "com.androidoctor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.androidoctor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: findProperty("KEYSTORE_PASSWORD") as? String ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: findProperty("KEY_ALIAS") as? String ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: findProperty("KEY_PASSWORD") as? String ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Core
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}

play {
    // Service account JSON key for Play Console API
    serviceAccountCredentials.set(file("../play-service-account.json"))
    // Upload to internal testing track first (safest)
    track.set("internal")
    // Automatically promote to production when ready:
    // track.set("production")
    defaultToAppBundles.set(true)
}
