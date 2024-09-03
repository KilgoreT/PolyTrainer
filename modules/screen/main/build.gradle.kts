plugins {
    id("android-library-convention")
}

apply(from = rootProject.file("varch.gradle.kts"))

android {
    namespace = "me.apomazkin.main"
    buildFeatures(Action {
        compose = true
    })
    composeOptions(Action {
        kotlinCompilerExtensionVersion = project.extra["kCompilerExtensionVersion"] as String
    })
}

dependencies {

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.navigationCompose)
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}