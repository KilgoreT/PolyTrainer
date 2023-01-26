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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10")

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    implementation(project("path" to ":modules:screen:splash"))
    implementation(project("path" to ":modules:screen:langpicker"))

    implementation(project("path" to ":modules:library:flags"))

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-db"))
//    implementation(project("path" to ":feature:feature-bottom-menu-api"))
//    implementation(project("path" to ":feature:feature-bottom-menu-impl"))


    // AndroidX
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    // Compose navigation
    implementation("androidx.navigation:navigation-compose:2.5.3")

    //no need??
    implementation("androidx.appcompat:appcompat:1.6.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.42")
    kapt("com.google.dagger:dagger-compiler:2.42")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:26.8.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.3.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.3.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.3.3")
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