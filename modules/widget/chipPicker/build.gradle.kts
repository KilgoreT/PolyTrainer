@file:Suppress("UnstableApiUsage")

plugins {
    id("android-library-convention")
}
apply(from = rootProject.file("varch.gradle.kts"))

android {
    namespace = "me.apomazkin.chippicker"

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = project.extra["kCompilerExtensionVersion"] as String
    }
}

dependencies {

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}