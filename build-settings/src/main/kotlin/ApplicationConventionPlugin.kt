import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            plugins.apply(projectVersionCatalog.findPlugin("android-app-gp").get().get().pluginId)
            plugins.apply(projectVersionCatalog.findPlugin("kotlin-gp").get().get().pluginId)
            plugins.apply(projectVersionCatalog.findPlugin("kotlin-compose").get().get().pluginId)
            plugins.apply("kotlin-kapt")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.applicationId = "me.apomazkin.polytrainer"
                defaultConfig.targetSdk = this@with.projectVersionCatalog
                    .findVersion("targetSdk").get().toString().toInt()
                val versionName = getVersionName().also {
                    println("=================================")
                    println("| Version Name: $it")
                }
                val versionCode = getVersionCode(versionName).also {
                    println("| Version Code: $it")
                    println("=================================")
                }
                defaultConfig.versionName = versionName
                defaultConfig.versionCode = versionCode
                defaultConfig.multiDexEnabled = true
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = projectVersionCatalog
                        .findVersion("kotlinCompilerExtensionVersion").get().toString()
                }
                lint {
                    checkDependencies = true
                    checkAllWarnings = true
                    absolutePaths = false
                    noLines = false
                    showAll = true
                    explainIssues = false
//                    lintConfig = file("../lint/lint-dependency-updates.xml")
                    quiet = false
                    abortOnError = true
                    warningsAsErrors = true
                    ignoreWarnings = false
                    htmlReport = true
                    textReport = true
                    xmlReport = false
                    disable.add("ObsoleteLintCustomCheck")
                    disable.add("UnusedAttribute") // vector
                    disable.add("VectorPath") // veсtor
                }
            }
        }
    }
}

fun getVersionName(): String {
    val value: String? = System.getenv("RELEASE_VERSION")
    val result = value ?: "undefined"
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