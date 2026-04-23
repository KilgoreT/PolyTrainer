plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.core_resources"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 23
        targetSdk = 35
    }
}

dependencies {
    implementation(androidLibs.splashscreen)
}
