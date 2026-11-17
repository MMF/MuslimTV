plugins {
    alias(libs.plugins.muslimtv.android.feature)
}

android {
    namespace = "com.mfouad.muslimtv.feature.quran"
}

dependencies {
    implementation(project(":core:media"))
}
