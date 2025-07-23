import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("kotlin-kapt")
}

android {
    namespace = "com.focx"
    compileSdk = 34
    val day = SimpleDateFormat("yyMMdd").format(Date())
    val time = SimpleDateFormat("HHmm").format(Date())
    defaultConfig {
        applicationId = "com.focx"
        minSdk = 24
        targetSdk = 34

        val majorV = 1
        val minorV = 0
        val patchV = 0

        versionCode = day.toInt() * 1000 + majorV * 100 + minorV * 10 + patchV
        versionName = "${majorV}.${minorV}.${patchV}"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../app.jks")
            storePassword = "Focx@123"
            keyAlias = "focx"
            keyPassword = "Focx@123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            versionNameSuffix = ".$day.$time"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.configureEach {
                (this as? ApkVariantOutputImpl)?.outputFileName = "Focx.V${versionName}.apk"
            }
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.runtime.ktx)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Image Loading
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Lottie
    implementation(libs.lottie.compose)

    // Solana Mobile Wallet Adapter
    implementation(libs.solanamobile.mobile.wallet.adapter)
    implementation(libs.solanamobile.web3.solana)
    implementation(libs.solanamobile.rpc.core)
    implementation(libs.funkatronics.multimult)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.solanamobile.rpc.solana)
    implementation(libs.solanamobile.rpc.ktordriver)
    implementation(libs.solanamobile.rpc.okiodriver)
    implementation(libs.bcprov.jdk15on)
    implementation("io.github.funkatronics:kborsh:0.1.1") {
        exclude(group = "com.ditchoom", module = "buffer")
    }
    implementation("com.ditchoom:buffer-android:1.4.2@aar") {
        isTransitive = true
    }
    implementation("com.syntifi.near:borshj:0.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.navigation.testing)
}