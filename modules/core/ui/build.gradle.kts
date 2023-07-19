plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.ui"
    buildFeatures(Action {
        compose = true
    })
    composeOptions(Action {
        kotlinCompilerExtensionVersion = "1.3.2"
    })
}

dependencies {

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleRuntimeCompose)
    debugApi(composeLibs.bundles.composePreview)
    implementation(composeLibs.accompanistSystemUicontroller)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)

}