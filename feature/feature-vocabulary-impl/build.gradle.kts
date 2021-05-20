plugins {
    id("android-library-convention")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation(project("path" to ":core:core-interactor"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-base"))
    implementation(project("path" to ":core:core-binding"))
    implementation(project("path" to ":feature:feature-vocabulary-api"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.32")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Material
    implementation("com.google.android.material:material:1.3.0")

    //ViewModel
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.35.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.35.1")

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.10")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}