import java.util.Properties

plugins {
    //TODO kilg 24.05.2025 22:53 эта хуйня почему-то не работает.
    // Вобщем, проблема в том, что какие-то таски из build-logic не запускаются, хотя их ожидают.
    // В этом плане происходит нечто вроде дедлока.
//    id("app.plugin111")

    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {

    namespace = "me.apomazkin.polytrainer"

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "me.apomazkin.polytrainer"
        targetSdk = 35
        minSdk = 21
        multiDexEnabled = true

        val appVersion = getVersionName()
        versionName = appVersion
        versionCode = getVersionCode(appVersion)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
        //                        projectVersionCatalog
        //                        .findVersion("kotlinCompilerExtensionVersion").get().toString()
    }

    signingConfigs {
        register("signForRelease") {
            val keystoreProperties = Properties()
            when (getBuildSource()) {
                BuildSource.LOCAL -> {
                    val keystorePropsFile = file("keystore/keystore_local_config")
                    keystoreProperties.load(keystorePropsFile.inputStream())
                    storeFile = file(keystoreProperties["storeFile"] as String)
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                }
                BuildSource.CI_DEV -> {
                    storeFile = file("keystore/keystore_ci_dev.jks")
                    storePassword = System.getenv("KEYSTORE_DEV_PASSWORD") ?: error("Missing KEYSTORE_DEV_PASSWORD")
                    keyAlias = System.getenv("KEYSTORE_DEV_ALIAS") ?: error("Missing KEYSTORE_DEV_ALIAS")
                    keyPassword = System.getenv("KEYSTORE_DEV_PASSWORD") ?: error("Missing KEYSTORE_DEV_PASSWORD")
                }
                BuildSource.CI_PROD -> {
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
    implementation(project("path" to ":modules:screen:quiztab"))
    implementation(project("path" to ":modules:screen:quiz:chat"))
    implementation(project("path" to ":modules:screen:stattab"))
    implementation(project("path" to ":modules:screen:settingstab"))

    implementation(project("path" to ":modules:widget:dictionarypicker"))

    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:datasource:prefs"))

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-db"))

    implementation(androidLibs.splashscreen)

    // Common
    implementation(androidLibs.coreKtx)
    implementation(androidLibs.activityKtx) // for insets support: enableEdgeToEdge()
    implementation(androidLibs.material)
    implementation(composeLibs.activityCompose)
    implementation(datastoreLibs.documentfile) // works with files through Storage Access Framework

    // Compose navigation
    implementation(composeLibs.navigationCompose)

    //Dagger2
    implementation(diLibs.dagger)
    implementation("com.github.blongho:worldCountryData:v1.5.4-alpha-1")
    ksp(diLibs.daggerCompiler)

    implementation(datastoreLibs.paging)

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

fun getBuildSource(): BuildSource {
    val localFile = file("keystore/keystore_local_config")
    if (localFile.exists()) {
        return BuildSource.LOCAL
    }
    val ciDevFile = file("keystore/keystore_ci_dev.jks")
    if (ciDevFile.exists()) {
        return BuildSource.CI_DEV
    }
    return BuildSource.CI_PROD
}

enum class BuildSource {
    LOCAL,
    CI_DEV,
    CI_PROD
}

fun getVersionName(): String {
    val value: String? = System.getenv("RELEASE_VERSION")
    val result = value ?: "Debug"
    return result
}

fun getVersionCode(versionName: String): Int {
    var result = ""
    val DEFAULT_VERSION_CODE = 1
    val MAX_MAJOR_VERSION = 213
    val LENGTH_MAJOR_VERSION = 3
    val MAX_MINOR_VERSION = 999
    val LENGTH_MINOR_VERSION = 3
    val MAX_PATCH_VERSION = 9999
    val LENGTH_PATCH_VERSION = 4
    if (!verifyVersion(versionName)) return DEFAULT_VERSION_CODE

    val versionList = versionName.split(".")
    val major = versionList[0]
    val minor = versionList[1]
    val patch = versionList[2]

    if (major.isInt() && major.toInt() <= MAX_MAJOR_VERSION) {
        result += alignVersion(major, LENGTH_MAJOR_VERSION)
        println("| Version Code major: $result")
    } else {
        return DEFAULT_VERSION_CODE
    }

    if (minor.isInt() && minor.toInt() <= MAX_MINOR_VERSION) {
        result += alignVersion(minor, LENGTH_MINOR_VERSION)
        println("| Version Code minor: $result")
    } else {
        return DEFAULT_VERSION_CODE
    }

    if (patch.isInt() && patch.toInt() <= MAX_PATCH_VERSION) {
        result += alignVersion(patch, LENGTH_PATCH_VERSION)
        println("| Version Code patch: $result")

    } else {
        return DEFAULT_VERSION_CODE
    }

    return if (result.isInt()) result.toInt() else DEFAULT_VERSION_CODE
}

fun String.isInt(): Boolean = this.toIntOrNull()?.let { true } ?: false

fun verifyVersion(value: String): Boolean {
    return value.filter { it == '.' }.count() + 1 == 3
}

fun alignVersion(value: String, length: Int): String {
    var aligned = ""
    val ALIGN_VALUE = "0"
    var i = value.length
    while (i < length) {
        aligned += ALIGN_VALUE
        i++
    }
    return aligned + value
}