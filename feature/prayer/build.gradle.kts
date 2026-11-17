plugins {
    alias(libs.plugins.muslimtv.android.feature)
}

android {
    namespace = "com.mfouad.muslimtv.feature.prayer"
}

dependencies {
    implementation(project(":core:media"))
}
