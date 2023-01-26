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

    debugApi("androidx.compose.ui:ui-tooling-preview:1.3.3")
    debugApi("androidx.compose.ui:ui-tooling:1.3.3")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.26.3-beta")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}