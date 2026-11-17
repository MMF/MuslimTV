plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.hilt)
}

android {
    namespace = "com.mfouad.muslimtv.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:datastore"))
    api(project(":core:database"))
    implementation(libs.okhttp)
    implementation(project(":core:common"))
    implementation(libs.adhan)
    implementation(libs.androidx.core.ktx)
    // Real org.json for JVM unit tests (the Android stub returns nulls).
    testImplementation("org.json:json:20250517")
}
