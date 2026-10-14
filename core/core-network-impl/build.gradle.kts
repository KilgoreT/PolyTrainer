import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// IS529 (Д1, Д3): реализация слоя сети — Retrofit поверх OkHttp, DTO с
// kotlinx.serialization, перехватчики, Dagger-компонент. Снаружи виден
// только через :core:core-network.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "me.apomazkin.core_network_impl"
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
    implementation(project("path" to ":core:core-network-api"))
    implementation(project("path" to ":modules:core:logger"))

    implementation(kotlinLibs.coroutinesCore)

    // Сеть
    implementation(networkLibs.retrofit)
    implementation(networkLibs.retrofitConverterSerialization)
    implementation(networkLibs.okhttp)
    implementation(networkLibs.serializationJson)

    // Dagger2
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)

    testImplementation(testLibs.junit)
    testImplementation(kotlinLibs.coroutinesTest)
    testImplementation(networkLibs.okhttpMockWebServer)
}
