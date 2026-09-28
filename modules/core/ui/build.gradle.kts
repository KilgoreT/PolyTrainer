import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.ui"
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
    api(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleRuntimeCompose)
    // IS496: BackHandler для немодальной панели ввода.
    implementation(composeLibs.activityCompose)
    api(composeLibs.uiToolingPreview)
    debugApi(composeLibs.bundles.composePreview)
    implementation(composeLibs.accompanistSystemUicontroller)
    
    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}