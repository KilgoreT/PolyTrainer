plugins {
    id("android-library-convention")
}

apply(from = rootProject.file("varch.gradle.kts"))

android {
    namespace = "me.apomazkin.theme"
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = project.extra["kCompilerExtensionVersion"] as String
    }
}

dependencies {

    api(composeLibs.material3)

    testImplementation(testLibs.junit)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}