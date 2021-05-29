plugins {
    id("android-library-convention")
    id("kotlin-kapt")
}

dependencies {

    api(project("path" to ":core:core-db"))

    //Dagger2
    implementation("com.google.dagger:dagger:2.35.1")
    kapt("com.google.dagger:dagger-compiler:2.35.1")

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.10")
    implementation("androidx.core:core-ktx:1.5.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.3.0")
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}