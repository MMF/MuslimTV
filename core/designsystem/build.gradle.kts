plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.android.compose)
}

android {
    namespace = "com.mfouad.muslimtv.core.designsystem"
}

dependencies {
    api(libs.tv.material)
    api(libs.compose.foundation)
    api(libs.compose.material.icons.extended)
}
