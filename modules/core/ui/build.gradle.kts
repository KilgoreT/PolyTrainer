import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.ui"
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
    api(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleRuntimeCompose)
    // IS496: BackHandler для немодальной панели ввода.
    implementation(composeLibs.activityCompose)
    api(composeLibs.uiToolingPreview)
    debugApi(composeLibs.bundles.composePreview)
    implementation(composeLibs.accompanistSystemUicontroller)
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}