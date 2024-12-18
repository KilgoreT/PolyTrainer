import java.util.Properties

plugins {
    id("lexeme.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {

    namespace = "me.apomazkin.polytrainer"

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

    buildTypes {
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
    }
}

dependencies {

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    implementation(project("path" to ":modules:screen:splash"))
    implementation(project("path" to ":modules:screen:createdictionary"))
    implementation(project("path" to ":modules:screen:main"))
    implementation(project("path" to ":modules:screen:dictionaryTab"))
    implementation(project("path" to ":modules:screen:wordcard"))

    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:datasource:prefs"))

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-db"))

    // Common
    implementation(androidLibs.coreKtx)
    implementation(androidLibs.activityKtx) // for insets support: enableEdgeToEdge()
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