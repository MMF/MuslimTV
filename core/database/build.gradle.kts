plugins {
    alias(libs.plugins.muslimtv.android.library)
    alias(libs.plugins.muslimtv.hilt)
}

android {
    namespace = "com.mfouad.muslimtv.core.database"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
