plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.vocabulary"
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
    implementation(composeLibs.activityCompose)
}