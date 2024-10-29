plugins {
    id("polytrainer.android.feature")
}

android {
    namespace = "me.apomazkin.wordcard"
}

dependencies {

    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:core:tools"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:coloredText"))
    implementation(project("path" to ":modules:widget:chipPicker"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}