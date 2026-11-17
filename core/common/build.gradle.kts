plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.hilt)
}

android {
    namespace = "com.mfouad.muslimtv.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}
