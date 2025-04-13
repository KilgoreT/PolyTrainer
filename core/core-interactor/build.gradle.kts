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

    implementation(kotlinLibs.stdLibJdk8)
    implementation(androidLibs.coreKtx)
}
