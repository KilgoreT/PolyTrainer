plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.apomazkin.mate"
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
    // Э3 (mate → библиотека): модуль стал МОСТОМ — ядро раннера
    // приходит с JitPack, здесь остаётся проектная обвязка
    // (ReducerLogging/LogTags/Constants). api — потребители получают
    // библиотеку транзитивно, их зависимости не меняются.
    api("com.github.KilgoreT.mate:mate-core:v0.1.0")
    api(project("path" to ":modules:core:logger"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}