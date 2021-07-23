import java.util.*

plugins {
    id("android-application-convention")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {

    defaultConfig {
        applicationId = "me.apomazkin.polytrainer"
    }

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
        getByName("release") {
            signingConfig = signingConfigs.getByName("signForRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        dataBinding = true
    }

}

dependencies {

    implementation(project("path" to ":core:core-db"))
    implementation(project("path" to ":feature:feature-bottom-menu-api"))
    implementation(project("path" to ":feature:feature-bottom-menu-impl"))

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.10")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    //Dagger2
    implementation("com.google.dagger:dagger:2.37")
    kapt("com.google.dagger:dagger-compiler:2.37")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:26.8.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
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