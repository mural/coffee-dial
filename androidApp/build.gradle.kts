plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.coffeedial"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.coffeedial.app"
        minSdk = 26
        targetSdk = 37
        val clientId = providers.gradleProperty("coffeeDial.googleWebClientId").orElse("").get()
        require(clientId.matches(Regex("[A-Za-z0-9.\\-]*"))) { "Invalid Google client ID" }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$clientId\"")
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
