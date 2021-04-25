plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

val kotlinVersion by extra { "1.4.32" }

android {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    api(project("path" to ":core:core-db"))

    //Dagger2
    implementation("com.google.dagger:dagger:2.33")
    kapt("com.google.dagger:dagger-compiler:2.33")

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.10")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.3.0")
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}