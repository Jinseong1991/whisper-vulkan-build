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
        versionCode = (project.findProperty("vc") as String?)?.toInt() ?: 2
        versionName = "1." + ((project.findProperty("vc") as String?) ?: "2")
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
