plugins {
    alias(libs.plugins.muslimtv.android.feature)
}

android {
    namespace = "com.mfouad.muslimtv.feature.tools"
}

dependencies {
    implementation(project(":core:media"))
    implementation(libs.media3.ui)
    implementation(libs.media3.hls)
}
