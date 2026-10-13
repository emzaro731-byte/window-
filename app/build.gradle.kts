plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.windowbridge"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        applicationId = "com.windowbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
