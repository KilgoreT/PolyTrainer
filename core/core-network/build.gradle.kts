import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// IS529 (Д3): фасад слоя сети, как :core:core-db для базы. Наружу отдаёт
// api-модуль, реализацию прячет; поднимается один раз из App.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "me.apomazkin.core_network"
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
    api(project("path" to ":core:core-network-api"))
    implementation(project("path" to ":core:core-network-impl"))
    implementation(project("path" to ":modules:core:logger"))

    // Dagger2
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)

    testImplementation(testLibs.junit)
}
