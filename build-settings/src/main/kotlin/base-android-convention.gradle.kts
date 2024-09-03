import com.android.build.gradle.BaseExtension

configure<BaseExtension> {

    setCompileSdkVersion(34)

    defaultConfig {
        minSdk = 21
        setTargetSdkVersion(34)
        versionName = getVersionName()
        versionCode = getVersionCode(getVersionName())
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

//    lintOptions {
//        isQuiet = false
//        isAbortOnError = true
//        isWarningsAsErrors = true
//        isIgnoreWarnings = false
//        disable("TrustAllX509TrustManager")
//        disable("NullSafeMutableLiveData")
//        disable("UnknownIssueId")
//
//    }
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