plugins {
    id("polytrainer.android.library")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "me.apomazkin.feature_training_write_impl"
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
    implementation(libKotlin.stdlib)
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //Dagger2
    implementation("com.google.dagger:dagger:2.42")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.42")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
}