plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.android.compose)
}

android {
    namespace = "com.mfouad.muslimtv.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.activity.compose)
    api(libs.androidx.appcompat)
}
