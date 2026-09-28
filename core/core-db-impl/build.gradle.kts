import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("androidx.room")
    id("com.google.devtools.ksp")
}

android {
    room {
        schemaDirectory("$projectDir/schemas")
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
    namespace = "me.apomazkin.core_db_impl"
    compileSdk = libs.versions.compileSdk.get().toInt()

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }

    buildFeatures {
        buildConfig = true
    }
    
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

dependencies {
    implementation(project("path" to ":core:core-db-api"))
    implementation(project("path" to ":modules:core:logger"))

    implementation(androidLibs.coreKtx)

    //Room
    implementation(datastoreLibs.roomRuntime)
    implementation(datastoreLibs.roomKtx)
    ksp(datastoreLibs.roomCompiler)
    implementation(datastoreLibs.roomPaging)
    implementation(datastoreLibs.sqliteBundled)

    //Dagger2
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)

    androidTestImplementation(project("path" to ":modules:core:ui"))
    androidTestImplementation(datastoreLibs.roomTesting)

    testImplementation(testLibs.junit)
    // Реальный org.json для JVM unit-тестов (Android SDK поставляет только stub, бросающий "not mocked").
    testImplementation(testLibs.orgJson)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}