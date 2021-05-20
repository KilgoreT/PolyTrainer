plugins {
    id("android-library-convention")
    id("kotlin-kapt")
}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to listOf("*.jar")))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.32")

    api(project("path" to ":core:core-db-api"))
    implementation(project("path" to ":core:core-db-impl"))

    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.5.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.35.1")
    kapt("com.google.dagger:dagger-compiler:2.35.1")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
