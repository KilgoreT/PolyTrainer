plugins {
    id("android-library-convention")
    id("kotlin-kapt")
}

android {
    namespace = "me.apomazkin.core_interactor"
}

dependencies {

    api(project("path" to ":core:core-db"))

    //Dagger2
    implementation(diLibs.dagger)
    kapt(diLibs.daggerCompiler)

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation(kotlinLibs.stdlib)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
