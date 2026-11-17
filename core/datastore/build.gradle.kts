plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.hilt)
}

android {
    namespace = "com.mfouad.muslimtv.core.datastore"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.datastore.preferences)
}
