plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.coloredtext"
}

dependencies {
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
}