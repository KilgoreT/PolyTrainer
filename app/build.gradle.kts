import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    //TODO kilg 24.05.2025 22:53 эта хуйня почему-то не работает.
    // Вобщем, проблема в том, что какие-то таски из build-logic не запускаются, хотя их ожидают.
    // В этом плане происходит нечто вроде дедлока.
//    id("app.plugin111")

    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlinx.kover")
}

android {

    namespace = "me.apomazkin.polytrainer"

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "co.lexeme.app"
        targetSdk = libs.versions.targetSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        multiDexEnabled = true

        val appVersion = getVersionName()
        versionName = appVersion
        versionCode = getVersionCode(appVersion)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    // composeOptions not needed with org.jetbrains.kotlin.plugin.compose

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
            // Отдельный applicationId — позволяет держать release и debug рядом на одном устройстве.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            signingConfig = signingConfigs.getByName("signForRelease")
            isMinifyEnabled = false
            val logLevel = project.findProperty("LOG_LEVEL")?.toString() ?: "DEBUG"
            val remoteLogLevel = project.findProperty("REMOTE_LOG_LEVEL")?.toString() ?: "NONE"
            buildConfigField("String", "LOG_LEVEL", "\"$logLevel\"")
            buildConfigField("String", "REMOTE_LOG_LEVEL", "\"$remoteLogLevel\"")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("signForRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            val logLevel = project.findProperty("LOG_LEVEL")?.toString() ?: "NONE"
            // Отправка в Firebase по умолчанию — только у магазинной сборки;
            // локальный release (проверка R8 и т.п.) молчит, пока не передан
            // -PREMOTE_LOG_LEVEL (spec logger, «Отправка в Firebase»).
            val defaultRemoteLevel = if (getBuildSource() == BuildSource.CI_PROD) "WARNING" else "NONE"
            val remoteLogLevel = project.findProperty("REMOTE_LOG_LEVEL")?.toString() ?: defaultRemoteLevel
            buildConfigField("String", "LOG_LEVEL", "\"$logLevel\"")
            buildConfigField("String", "REMOTE_LOG_LEVEL", "\"$remoteLogLevel\"")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

dependencies {

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    implementation(project("path" to ":modules:screen:splash"))
    implementation(project("path" to ":modules:screen:dictionary"))
    implementation(project("path" to ":modules:screen:main"))
    implementation(project("path" to ":modules:screen:wordstab"))
    implementation(project("path" to ":modules:widget:wordrow"))
    implementation(project("path" to ":modules:screen:vocabulary"))
    implementation(project("path" to ":modules:screen:groupstab"))
    implementation(project("path" to ":modules:screen:wordcard"))
    implementation(project("path" to ":modules:screen:quiztab"))
    implementation(project("path" to ":modules:screen:quiz:chat"))
    implementation(project("path" to ":modules:screen:stattab"))
    implementation(project("path" to ":modules:screen:settingstab"))
    implementation(project("path" to ":modules:screen:components_manager"))
    implementation(project("path" to ":modules:screen:per_dictionary_components"))

    implementation(project("path" to ":modules:widget:dictionarypicker"))
    implementation(project("path" to ":modules:widget:dictionaryappbar"))
    implementation(project("path" to ":modules:widget:component_widgets"))

    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:datasource:prefs"))

    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":modules:domain:group"))
    implementation(project("path" to ":modules:domain:quiz"))

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-db"))
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))

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
    implementation(otherLibs.flagLib) {
        // Кривой pom alpha-библиотеки тянет ТЕСТОВЫЙ androidx.test:monitor
        // в прод-classpath — строгий consistent resolution из-за этого
        // валит androidTest-резолв (monitor 1.5.0 против 1.7.x у espresso).
        exclude(group = "androidx.test", module = "monitor")
    }
    ksp(diLibs.daggerCompiler)

    implementation(datastoreLibs.paging)

    // Firebase
    implementation(platform(firebaseLibs.firebaseBOM))
    implementation(firebaseLibs.firebaseCrashlytics)
    implementation(firebaseLibs.firebaseAnalytics)

    // Test
    testImplementation(testLibs.junit)
    testImplementation(testLibs.mockk)
    testImplementation(kotlinLibs.coroutinesTest)
    // Сценарный харнес mate: registry экранов + DSL сценариев.
    testImplementation(otherLibs.mateAppTest)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
    androidTestImplementation(composeLibs.uiTestJunit4)
    debugImplementation(composeLibs.uiTooling)
    debugImplementation(composeLibs.uiTestManifest)

    constraints {
        // androidx.test:core 1.7.0 (espresso 3.7 / ext junit 1.3 — нужны на
        // Android 15+) тянет concurrent-futures 1.2.0, а боевой runtime
        // резолвит 1.1.0; consistent resolution AGP требует одной версии в
        // androidTest и боевом classpath — поднимаем боевую.
        implementation("androidx.concurrent:concurrent-futures") {
            version { require("1.2.0") }
            because("androidTest-classpath обязан совпадать с боевым (consistent resolution)")
        }
    }
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