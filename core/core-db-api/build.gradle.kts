plugins {
    id("lexeme.android.library")
}

android {
    namespace = "me.apomazkin.core_db_api"
}

dependencies {

    // Coroutines
    implementation(kotlinLibs.coroutinesCore)
}
