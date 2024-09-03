plugins {
    id("android-library-convention")
}

apply(from = rootProject.file("varch.gradle.kts"))

android {
    namespace = "me.apomazkin.ui"
    buildFeatures(Action {
        compose = true
    })
    composeOptions(Action {
        kotlinCompilerExtensionVersion = project.extra["kCompilerExtensionVersion"] as String
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