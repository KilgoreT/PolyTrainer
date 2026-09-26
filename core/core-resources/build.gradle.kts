import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.core_resources"
    compileSdk = 36

    testOptions {
        targetSdk = 36
    }

    lint {
        targetSdk = 36
    }
    
    defaultConfig {
        minSdk = 23
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(androidLibs.splashscreen)
}
