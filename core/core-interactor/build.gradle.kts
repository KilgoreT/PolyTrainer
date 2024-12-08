plugins {
    id("lexeme.android.library")
    id("kotlin-kapt")
}

android {
    namespace = "me.apomazkin.core_interactor"
}

dependencies {

    api(project("path" to ":core:core-db"))

    //Dagger2
    implementation(diLibs.dagger)
    kapt(diLibs.daggerCompiler)

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation(kotlinLibs.stdLibJdk8)
    implementation(androidLibs.coreKtx)
}
