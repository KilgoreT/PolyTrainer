plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.icondropdowned"
}

dependencies {
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
}