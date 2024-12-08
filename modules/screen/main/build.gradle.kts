plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.main"
}

dependencies {
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.navigationCompose)
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
}