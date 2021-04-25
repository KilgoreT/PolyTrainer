plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

val kotlinVersion by extra { "1.4.32" }

dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.3.2")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

}