import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---- AdMob -------------------------------------------------------------------------------
// Google's official sample IDs. Debug builds ALWAYS use them, so testing on a real phone can never
// generate invalid traffic. Release builds read the real IDs from gradle.properties
// (measurepulse.admob.*) and fall back to the samples until those are filled in.
object TestAds {
    const val APP = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
}
// local.properties (git-ignored) wins over gradle.properties — keep device IDs there.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun admobProp(name: String, fallback: String): String =
    (localProps.getProperty("measurepulse.admob.$name") ?: providers.gradleProperty("measurepulse.admob.$name").orNull)
        ?.takeIf { it.isNotBlank() } ?: fallback
fun quoted(v: String) = "\"$v\""

// ---- Signing -------------------------------------------------------------------------------
// Play upload key. keystore.properties and keystore/*.jks are git-ignored (see keystore/KEYSTORE_CREDENTIALS.txt).
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.measurepulse.ar"
    compileSdk = 36
    // Pinned: the local build-tools 35.0.0 install is incomplete; 36.0.0 is installed and valid.
    buildToolsVersion = "36.0.0"
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.measurepulse.ar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = TestAds.APP
            buildConfigField("String", "AD_UNIT_BANNER", quoted(TestAds.BANNER))
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", quoted(TestAds.INTERSTITIAL))
            buildConfigField("String", "AD_UNIT_REWARDED", quoted(TestAds.REWARDED))
            buildConfigField("String", "AD_UNIT_NATIVE", quoted(TestAds.NATIVE))
            // Hashed IDs of our own phones (comma-separated) — forces test ads + the EEA consent form.
            buildConfigField("String", "ADMOB_TEST_DEVICE_IDS", quoted(admobProp("testDeviceIds", "")))
        }
        release {
            manifestPlaceholders["admobAppId"] = admobProp("appId", TestAds.APP)
            buildConfigField("String", "AD_UNIT_BANNER", quoted(admobProp("banner", TestAds.BANNER)))
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", quoted(admobProp("interstitial", TestAds.INTERSTITIAL)))
            buildConfigField("String", "AD_UNIT_REWARDED", quoted(admobProp("rewarded", TestAds.REWARDED)))
            buildConfigField("String", "AD_UNIT_NATIVE", quoted(admobProp("native", TestAds.NATIVE)))
            buildConfigField("String", "ADMOB_TEST_DEVICE_IDS", quoted(""))
            // Upload key when keystore.properties exists; otherwise debug-signed (local perf testing only).
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Google Play 16 KB memory page size: keep native libs uncompressed & page-aligned in the APK.
    packaging {
        jniLibs { useLegacyPackaging = false }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Architecture
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Storage
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Camera / AR / ML
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava.android) // CameraControl returns ListenableFuture (R8 strips the rest)
    implementation(libs.arcore)
    implementation(libs.mlkit.objectdetection)

    // Images, widgets, background work
    implementation(libs.coil.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)

    // Monetization
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
