plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.flags"
}

dependencies {

    implementation(otherLibs.flags)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}