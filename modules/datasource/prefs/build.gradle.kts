plugins {
    id("lexeme.android.library")
}

android {
    namespace = "me.apomazkin.prefs"
}

dependencies {
    implementation(datastoreLibs.preferences)
}