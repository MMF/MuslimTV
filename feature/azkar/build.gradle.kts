plugins {
    alias(libs.plugins.muslimtv.android.feature)
}

android {
    namespace = "com.mfouad.muslimtv.feature.azkar"
}

dependencies {
    implementation(project(":core:media"))
}
