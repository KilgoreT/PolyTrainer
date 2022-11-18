plugins {
    id("android-library-convention")
    id("kotlin-kapt")
}

android {
    buildFeatures(Action {
        dataBinding = true
    })
}

dependencies {

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-util"))

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10")

    implementation("androidx.appcompat:appcompat:1.5.1")


    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")

}