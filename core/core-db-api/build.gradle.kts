plugins {
    id("lexeme.android.library")
}

android {
    namespace = "me.apomazkin.core_db_api"
}

dependencies {

    // Coroutines
    implementation(kotlinLibs.coroutinesCore)

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
}
