plugins {
    id("android-library-convention")
}

dependencies {

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.10")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.5.0")
    implementation("com.google.android.material:material:1.3.0")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

}