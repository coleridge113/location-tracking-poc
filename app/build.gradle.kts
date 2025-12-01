import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
}

val localProps = File(rootDir, "local.properties").inputStream().use {
    Properties().apply { load(it) }
}

android {
    namespace = "com.metromart.locationtrackignpoc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.metromart.locationtrackignpoc"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "MAPBOX_ACCESS_TOKEN",
            "\"${localProps["MAPBOX_ACCESS_TOKEN"]}\""
        )
        buildConfigField(
            "String",
            "MAPBOX_DOWNLOADS_TOKEN",
            "\"${localProps["MAPBOX_DOWNLOADS_TOKEN"]}\""
        )
        buildConfigField(
            "String",
            "ABLY_API_KEY",
            "\"${localProps["ABLY_API_KEY"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_APP_ID",
            "\"${localProps["PUSHER_APP_ID"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_KEY",
            "\"${localProps["PUSHER_KEY"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_SECRET",
            "\"${localProps["PUSHER_SECRET"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_API_KEY",
            "\"${localProps["PUSHER_API_KEY"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_CLUSTER",
            "\"${localProps["PUSHER_CLUSTER"]}\""
        )
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
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
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

    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.dynamic.features.fragment)

    // Mapbox
    implementation(libs.android.ndk27)
    implementation(libs.maps.compose.ndk27)
    implementation(libs.navigationcore.android.ndk27)

    // Ably
    implementation(libs.ably.java)

    // Pusher
    implementation(libs.pusher.java.client)
}
