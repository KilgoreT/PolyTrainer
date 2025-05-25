plugins {
    id("polytrainer.android.library")
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation(libKotlin.stdlib)

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.9.0")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
}
android {
    namespace = "me.apomazkin.feature_vocabulary_api"
}
