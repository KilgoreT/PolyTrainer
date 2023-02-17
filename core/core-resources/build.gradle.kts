plugins {
    id("android-library-convention")
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    implementation("androidx.compose.ui:ui:1.3.3")
//    implementation("androidx.compose.material:material:1.3.1")

    // Material
//    implementation("com.google.android.material:material:1.7.0")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}
android {
    namespace = "me.apomazkin.core_resources"
}
