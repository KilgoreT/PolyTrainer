import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "me.apomazkin.mate"
    compileSdk = libs.versions.compileSdk.get().toInt()

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

dependencies {
    // Э3 (mate → библиотека): модуль стал МОСТОМ — ядро раннера
    // приходит с JitPack, здесь остаётся проектная обвязка
    // (ReducerLogging/LogTags/Constants). api — потребители получают
    // библиотеку транзитивно, их зависимости не меняются.
    api(otherLibs.mateCore)
    api(otherLibs.mateNavigation)
    api(project("path" to ":modules:core:logger"))
    implementation(kotlinLibs.coroutinesCore)
}