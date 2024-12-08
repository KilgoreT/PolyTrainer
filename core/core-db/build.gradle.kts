plugins {
    id("lexeme.android.library")
    id("kotlin-kapt")
}
android {
    namespace = "me.apomazkin.core_db"
}

dependencies {

    api(project("path" to ":core:core-db-api"))
    implementation(project("path" to ":core:core-db-impl"))

    //Dagger2
    implementation(diLibs.dagger)
    kapt(diLibs.daggerCompiler)
}
