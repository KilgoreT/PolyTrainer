import java.util.*

plugins {
    id("android-application-convention")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {

    defaultConfig(Action {
        applicationId = "me.apomazkin.polytrainer"
    })

    signingConfigs {
        register("signForRelease") {
            val keystoreProperties = Properties()
            val keystorePropsFile = file("keystore/keystore_local_config")
            when (getBuildSource(keystorePropsFile)) {
                BuildSource.LOCAL -> {
                    keystoreProperties.load(keystorePropsFile.inputStream())
                    storeFile = file(keystoreProperties["storeFile"] as String)
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                }
                BuildSource.CI -> {
                    storeFile = file("keystore/keystore_upload")
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEYSTORE_KEY_ALIAS")
                    keyPassword = System.getenv("KEYSTORE_PASSWORD")
                }
            }
        }
    }
    namespace = "me.apomazkin.polytrainer"

    buildTypes(Action {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("signForRelease")
            isMinifyEnabled = false
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("signForRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    })
    buildFeatures(Action {
        dataBinding = true
    })
    buildFeatures(Action {
        compose = true
    })
    composeOptions(Action {
        kotlinCompilerExtensionVersion = "1.3.2"
    })
}

dependencies {

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    implementation(project("path" to ":modules:screen:splash"))
    implementation(project("path" to ":modules:screen:langpicker"))
    implementation(project("path" to ":modules:screen:main"))
    implementation(project("path" to ":modules:screen:vocabulary"))
    implementation(project("path" to ":modules:screen:wordcard"))

    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:datasource:prefs"))

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-db"))

    // Common
    implementation(androidLibs.coreKtx)
    implementation(androidLibs.material)
    implementation(composeLibs.activityCompose)

    // Compose navigation
    implementation(composeLibs.navigationCompose)

    //Dagger2
    implementation(diLibs.dagger)
    kapt(diLibs.daggerCompiler)

    // Firebase
    implementation(platform(firebaseLibs.firebaseBOM))
    implementation(firebaseLibs.firebaseCrashlytics)
    implementation(firebaseLibs.firebaseAnalytics)

    // Test
    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
    androidTestImplementation(composeLibs.uiTestJunit4)
    debugImplementation(composeLibs.uiTooling)
    debugImplementation(composeLibs.uiTestManifest)
}

fun getBuildSource(file: File): BuildSource {
    return if (file.exists()) {
        BuildSource.LOCAL
    } else {
        BuildSource.CI
    }
}

enum class BuildSource {
    LOCAL,
    CI
}