plugins {
    id("polytrainer.android.library")
    id("kotlin-kapt")
}

android {
    namespace = "me.apomazkin.feature_training_list_impl"
    buildFeatures(Action {
        dataBinding = true
    })
}

dependencies {

    implementation(project("path" to ":core:core-db"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-base"))
    implementation(project("path" to ":core:core-binding"))
    implementation(project("path" to ":core:core-interactor"))
    implementation(project("path" to ":feature:feature-training-list-api"))
    implementation(project("path" to ":feature:feature-training-write-api"))
    implementation(project("path" to ":feature:feature-training-write-impl"))

    implementation(libKotlin.stdlib)
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.42")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.42")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}