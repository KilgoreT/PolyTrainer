plugins {
    id("lexeme.android.feature")
}

android {
    namespace = "me.apomazkin.theme"
}

dependencies {
    api(composeLibs.material3)
}