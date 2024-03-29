plugins {
    id("android-library-convention")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    buildFeatures(Action {
        dataBinding = true
    })
}

dependencies {

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-base"))
    implementation(project("path" to ":core:core-binding"))
    implementation(project("path" to ":core:core-interactor"))
    implementation(project("path" to ":feature:feature-training-write-api"))
    implementation(project("path" to ":widget:view-progress-quiz"))

    // AndroidX
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //Dagger2
    implementation("com.google.dagger:dagger:2.42")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.42")

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
}