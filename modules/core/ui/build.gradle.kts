plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.ui"
}

dependencies {
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleRuntimeCompose)
    api(composeLibs.uiToolingPreview)
    debugApi(composeLibs.bundles.composePreview)
    implementation(composeLibs.accompanistSystemUicontroller)
}