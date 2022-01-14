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

    implementation(project("path" to ":core:core-db"))
    implementation(project("path" to ":feature:feature-bottom-menu-api"))
    implementation(project("path" to ":feature:feature-vocabulary-api"))
    implementation(project("path" to ":feature:feature-vocabulary-impl"))
    implementation(project("path" to ":feature:feature-training-list-api"))
    implementation(project("path" to ":feature:feature-training-list-impl"))
    implementation(project("path" to ":feature:feature-statistic-api"))
    implementation(project("path" to ":feature:feature-statistic-impl"))

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Material
    implementation("com.google.android.material:material:1.5.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.37")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.37")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

}