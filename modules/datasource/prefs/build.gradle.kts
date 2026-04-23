plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
//    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

android {
    namespace = "me.apomazkin.prefs"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
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
    implementation(datastoreLibs.preferences)
    testImplementation("junit:junit:4.13.2")
}