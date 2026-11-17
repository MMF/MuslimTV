plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.hilt)
}

android {
    namespace = "com.mfouad.muslimtv.core.media"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    api(libs.media3.exoplayer)
    api(libs.media3.session)
    implementation(libs.media3.datasource)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.database)
    implementation(libs.okhttp)
    implementation(libs.androidx.core.ktx)
}
