plugins {
    alias(libs.plugins.muslimtv.android.feature)
}

android {
    namespace = "com.mfouad.muslimtv.feature.downloads"
}

dependencies {
    implementation(project(":core:downloads"))
}
