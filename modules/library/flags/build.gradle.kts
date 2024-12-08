plugins {
    id("lexeme.android.library")
}

android {
    namespace = "me.apomazkin.flags"
}

dependencies {
    implementation(otherLibs.flagLib)
}