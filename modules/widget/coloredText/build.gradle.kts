@file:Suppress("UnstableApiUsage")

plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.coloredtext"
    buildFeatures(Action {
        compose = true
    })
    composeOptions(Action {
        kotlinCompilerExtensionVersion = "1.3.2"
    })
}

dependencies {
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}