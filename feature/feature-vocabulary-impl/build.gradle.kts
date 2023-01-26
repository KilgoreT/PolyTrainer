plugins {
    id("android-library-convention")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
//    id("com.google.gms.google-services")
}

android {
    buildFeatures(Action {
        dataBinding = true
    })
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation(project("path" to ":core:core-interactor"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-base"))
    implementation(project("path" to ":core:core-binding"))
    implementation(project("path" to ":feature:feature-vocabulary-api"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Material
    implementation("com.google.android.material:material:1.8.0")

    //ViewModel
//    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.42")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.42")

//    implementation("com.google.firebase:firebase-auth-ktx:21.1.0")
//    implementation("com.google.android.gms:play-services-auth:20.4.0")
//    implementation("com.google.android.gms:play-services-base:18.1.0")

    implementation("com.google.android.gms:play-services-auth:20.4.0")
    implementation("com.google.api-client:google-api-client-android:2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev69-1.22.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava")
    }
    implementation("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}