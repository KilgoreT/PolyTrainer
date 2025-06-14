plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.settingstab"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 21
        targetSdk = 35
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {

    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)
}