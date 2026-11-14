plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.jinsung.noticlear"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.jinsung.noticlear"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }
    signingConfigs {
        create("fixed") {
            storeFile = file("release.jks")
            storePassword = "noticlear"
            keyAlias = "noticlear"
            keyPassword = "noticlear"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("fixed") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
