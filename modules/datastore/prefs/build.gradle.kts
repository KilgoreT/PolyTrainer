plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.prefs"
}

dependencies {

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}