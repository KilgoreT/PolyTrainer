plugins {
    id("android-library-convention")
}

android {
    namespace = "me.apomazkin.flags"
}

dependencies {

    implementation("com.github.blongho:worldCountryData:v1.5.4-alpha-1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}