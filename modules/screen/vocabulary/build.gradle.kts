@file:Suppress("UnstableApiUsage")

plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.vocabulary"
    buildFeatures(Action {
        compose = true
    })
    composeOptions(Action {
        kotlinCompilerExtensionVersion = "1.3.2"
    })
}

dependencies {

    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))
    implementation(project("path" to ":modules:widget:chipPicker"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}