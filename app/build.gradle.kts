plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.neokratos"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.neokratos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    // ========================================
    // UNIT TESTS (test/java/) - Girano su JVM
    // ========================================

    // JUnit 4 (framework base)
    testImplementation(libs.junit)

    // MockK (per creare mock di classi/interfacce)
    testImplementation(libs.mockk)

    // Coroutines Testing (per testare suspend fun e Flow)
    testImplementation(libs.jetbrains.kotlinx.coroutines.test)

    // Turbine (per testare Flow in modo semplice)
    testImplementation(libs.turbine)

    // Robolectric (simula Android senza emulatore)
    testImplementation(libs.robolectric)

    // Room Testing
    testImplementation(libs.room.testing)

    // AndroidX Test - Core
    testImplementation(libs.androidx.core.v170)
    testImplementation(libs.test.core.ktx)

    // AndroidX Test - Runner
    testImplementation(libs.runner)

    // AndroidX Test - Rules
    testImplementation(libs.rules)

    // AndroidX Arch Core Testing (per LiveData/ViewModel)
    testImplementation(libs.androidx.core.testing)

    // ========================================
    // INSTRUMENTED TESTS (androidTest/java/) - Girano su device/emulatore
    // ========================================

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Coroutines Testing per androidTest
    androidTestImplementation(libs.jetbrains.kotlinx.coroutines.test)

    // Room Testing per androidTest
    androidTestImplementation(libs.room.testing)

    // AndroidX Test - Core per androidTest
    androidTestImplementation(libs.core)
    androidTestImplementation(libs.test.core.ktx)

    // AndroidX Test - Runner per androidTest
    androidTestImplementation(libs.runner)

    // AndroidX Test - Rules per androidTest
    androidTestImplementation(libs.rules)

    // ========================================
    // DEBUG (per UI testing)
    // ========================================

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}