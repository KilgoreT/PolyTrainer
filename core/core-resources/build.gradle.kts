plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.core_resources"
}

dependencies {
    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}
