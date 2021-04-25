import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    val kotlinVersion by extra { "1.4.32" }

    repositories {
        google()
        jcenter()

    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.4")
        classpath("com.google.gms:google-services:4.3.5")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}

subprojects {
    plugins.matching { it is AppPlugin || it is LibraryPlugin }.whenPluginAdded {

        configure<BaseExtension> {

            setCompileSdkVersion(30)
            buildToolsVersion = "30.0.3"

            defaultConfig {
                setMinSdkVersion(21)
                setTargetSdkVersion(30)
                versionName = getVersionName()
                versionCode = getVersionCode(getVersionName())
                multiDexEnabled = true
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

//            signingConfigs {
//                getByName("release") {
//                    val keystoreProperties = java.util.Properties()
//                    val keystorePropsFile = file("keystore/keystore_config_release")
//                    if (keystorePropsFile.exists()) {
//                        file("keystore/keystore_config_release") //.withInputStream { keystoreProperties.load(it) }
//                        storeFile = file("$keystoreProperties.storeFile")
//                        storePassword = "$keystoreProperties.storePassword"
//                        keyAlias = "$keystoreProperties.keyAlias"
//                        keyPassword = "$keystoreProperties.keyPassword"
//                    } else {
//                        storeFile = file("keystore/keystore_upload")
//                        storePassword = System.getenv("KEYSTORE_PASSWORD")
//                        keyAlias = System.getenv("KEYSTORE_KEY_ALIAS")
//                        keyPassword = System.getenv("KEYSTORE_PASSWORD")
//                    }
//                }
//            }

            buildTypes {
                getByName("release") {
//                    signingConfig = signingConfigs.getByName("release")
//                    setSigningConfig(signingConfigs.getByName("release"))
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }

            compileOptions {
                targetCompatibility = JavaVersion.VERSION_1_8
                sourceCompatibility = JavaVersion.VERSION_1_8
            }

            lintOptions {
                isQuiet = false
                isAbortOnError = true
                isWarningsAsErrors = true
                isIgnoreWarnings = false
            }
        }
    }
}

fun getVersionName(): String {
    val value: String? = System.getenv("RELEASE_VERSION")
    val result = value ?: "undefined"
    println(">>>getVersion>>> version: $result")
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
        println(">>>getVersionCode>>> major: $result")
    } else {
        return DEFAULT_VERSION_CODE
    }

    if (minor.isInt() && minor.toInt() <= MAX_MINOR_VERSION) {
        result += alignVersion(minor, LENGTH_MINOR_VERSION)
        println(">>>getVersionCode>>> +minor: $result")
    } else {
        return DEFAULT_VERSION_CODE
    }

    if (patch.isInt() && patch.toInt() <= MAX_PATCH_VERSION) {
        result += alignVersion(patch, LENGTH_PATCH_VERSION)
        println(">>>getVersionCode>>> +patch: $result")
    } else {
        return DEFAULT_VERSION_CODE
    }

    return if (result.isInt()) result.toInt() else DEFAULT_VERSION_CODE
}

fun String.isInt(): Boolean = this.toIntOrNull()?.let { true } ?: false

fun verifyVersion(value: String): Boolean {
    val result = value.filter { it == '.' }.count() + 1 == 3
    println(">>>verifyVersion>>> String: $value contains 2 dot and it is $result")
    return result
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