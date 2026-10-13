plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.windowbridge"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.windowbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
