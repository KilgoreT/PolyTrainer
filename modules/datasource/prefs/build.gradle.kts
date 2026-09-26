import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
//    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

android {
    namespace = "me.apomazkin.prefs"
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
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(datastoreLibs.preferences)
    testImplementation("junit:junit:4.13.2")
}