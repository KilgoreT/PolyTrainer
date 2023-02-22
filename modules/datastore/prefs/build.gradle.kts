plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.prefs"
}

dependencies {

    implementation(datastoreLibs.preferences)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}