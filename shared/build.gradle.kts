import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.sqldelight")
}

kotlin {
    android {
        namespace = "com.coffeedial.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTestBuilder {}
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "CoffeeDialShared"
            isStatic = true
            linkerOpts("-lsqlite3")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("app.cash.sqldelight:runtime:2.4.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.4.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation("app.cash.sqldelight:android-driver:2.4.0")
            implementation("androidx.activity:activity-compose:1.13.0")
        }
        getByName("androidHostTest").dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.4.0")
        }
        iosMain.dependencies { implementation("app.cash.sqldelight:native-driver:2.4.0") }
    }
}

sqldelight {
    databases {
        create("CoffeeDatabase") {
            packageName.set("com.coffeedial.database")
            verifyMigrations.set(true)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
