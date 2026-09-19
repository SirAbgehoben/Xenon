import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "org.abgehoben.xenon.shared"
        compileSdk = 37
        minSdk = 30

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }

        // 1. Opt-in to host unit tests (disabled by default in AGP 9)
        withHostTest { }

        // Optional: Opt-in to on-device instrumented tests if needed
        // withDeviceTest {
        //     instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // }
    }

    sourceSets {
        commonMain.dependencies {
            // Modern direct catalog dependencies (no deprecation warnings):
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            api(libs.compose.material3)

            // Multiplatform Material Icons via Version Catalog
            implementation(libs.material.icons.core)
            implementation(libs.material.icons.extended)

            // Architecture & Lifecycle
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)

            implementation(libs.kotlinx.datetime)

            // Storage & Networking
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Koin Multiplatform
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        // 2. Add test dependencies
        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.koin.test)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.ktor.client.okhttp)
            api(libs.koin.android)
        }
    }
}

//TODO: TEMPORARY
compose.resources {
    packageOfResClass = "xenon.app.generated.resources"
}