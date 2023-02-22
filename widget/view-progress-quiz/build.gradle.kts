plugins {
    id("android-library-convention")
    id("kotlin-kapt")
}

android {
    namespace = "me.apomazkin.view_progress_quiz"
    buildFeatures(Action {
        dataBinding = true
    })
}

dependencies {

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-util"))

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation(libKotlin.stdlib)

    implementation("androidx.appcompat:appcompat:1.6.1")


    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}