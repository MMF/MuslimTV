import java.util.Properties

plugins {
    alias(libs.plugins.muslimtv.android.application)
    alias(libs.plugins.muslimtv.android.compose)
    alias(libs.plugins.muslimtv.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mfouad.muslimtv"

    defaultConfig {
        applicationId = "com.mfouad.muslimtv"
        versionCode = 1
        versionName = "0.1.0"
    }

    androidResources {
        localeFilters += listOf("en", "ar")
    }

    // Upload key for Google Play, read from the git-ignored keystore.properties.
    // Without that file (e.g. on CI or another machine) release falls back to the debug key.
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.isFile }?.let { f ->
        Properties().apply { f.inputStream().use { load(it) } }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:prayer"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:quran"))
    implementation(project(":feature:azkar"))
    implementation(project(":feature:tools"))
    implementation(project(":core:media"))
    implementation(project(":core:downloads"))
    implementation(project(":feature:downloads"))
    implementation(libs.work.runtime)
    implementation(libs.androidx.hilt.work)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
