plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.hilt)
}

android {
    namespace = "com.mfouad.muslimtv.core.downloads"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    api(libs.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.okhttp)
    implementation(libs.androidx.core.ktx)
}
