import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.apomazkin.mate"
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
    // Э3 (mate → библиотека): модуль стал МОСТОМ — ядро раннера
    // приходит с JitPack, здесь остаётся проектная обвязка
    // (ReducerLogging/LogTags/Constants). api — потребители получают
    // библиотеку транзитивно, их зависимости не меняются.
    api("com.github.KilgoreT.mate:mate-core:v0.1.5")
    api("com.github.KilgoreT.mate:mate-navigation:v0.1.5")
    api(project("path" to ":modules:core:logger"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}