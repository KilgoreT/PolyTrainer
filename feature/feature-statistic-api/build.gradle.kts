plugins {
    id("android-library-convention")
}

dependencies {

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation(libKotlin.stdlib)

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("com.google.android.material:material:1.8.0")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
android {
    namespace = "me.apomazkin.feature_statistic_api"
}
