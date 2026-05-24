plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cloud.kasaflow.ksefinvoicer"
    compileSdk = 34

    defaultConfig {
        applicationId = "cloud.kasaflow.ksefinvoicer"
        // V2s = Android 7.1 (API 25). minSdk 24 obejmuje V2s + V2 + V3.
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-alpha"
    }

    buildTypes {
        debug {
            isDebuggable = true
            // applicationIdSuffix = ".debug"   // opcjonalnie do koegzystencji
        }
        release {
            isMinifyEnabled = true
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
        viewBinding = true
        aidl = true   // wymaga AIDL Sunmi Printer Service
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.webkit:webkit:1.11.0")

    // Sunmi Printer Library v3.0+ (jezeli niedostepne w Maven Central,
    // bedziemy uzywac aar lokalnego z libs/ — zobacz README.md)
    // implementation("com.sunmi:printerlibrary:1.0.20")
}
