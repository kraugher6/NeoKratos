plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    kapt(libs.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    // ========================================
    // UNIT TESTS (test/java/) - Girano su JVM
    // ========================================

    // JUnit 4 (framework base)
    testImplementation("junit:junit:4.13.2")

    // MockK (per creare mock di classi/interfacce)
    testImplementation("io.mockk:mockk:1.13.8")

    // Coroutines Testing (per testare suspend fun e Flow)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Turbine (per testare Flow in modo semplice)
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Robolectric (simula Android senza emulatore)
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Room Testing
    testImplementation("androidx.room:room-testing:2.6.1")

    // AndroidX Test - Core
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:core-ktx:1.5.0")

    // AndroidX Test - Runner
    testImplementation("androidx.test:runner:1.5.2")

    // AndroidX Test - Rules
    testImplementation("androidx.test:rules:1.5.0")

    // AndroidX Arch Core Testing (per LiveData/ViewModel)
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // ========================================
    // INSTRUMENTED TESTS (androidTest/java/) - Girano su device/emulatore
    // ========================================

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Coroutines Testing per androidTest
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Room Testing per androidTest
    androidTestImplementation("androidx.room:room-testing:2.6.1")

    // AndroidX Test - Core per androidTest
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")

    // AndroidX Test - Runner per androidTest
    androidTestImplementation("androidx.test:runner:1.5.2")

    // AndroidX Test - Rules per androidTest
    androidTestImplementation("androidx.test:rules:1.5.0")

    // ========================================
    // DEBUG (per UI testing)
    // ========================================

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}