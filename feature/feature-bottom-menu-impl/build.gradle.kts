plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs")
}

val kotlinVersion by extra { "1.4.32" }

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode(1)
        versionName = "1.0"

        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        dataBinding = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    implementation(project("path" to ":core:core-db"))
    implementation(project("path" to ":feature:feature-bottom-menu-api"))
    implementation(project("path" to ":feature:feature-vocabulary-api"))
    implementation(project("path" to ":feature:feature-vocabulary-impl"))
    implementation(project("path" to ":feature:feature-training-list-api"))
    implementation(project("path" to ":feature:feature-training-list-impl"))
    implementation(project("path" to ":feature:feature-statistic-api"))
    implementation(project("path" to ":feature:feature-statistic-impl"))

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Material
    implementation("com.google.android.material:material:1.3.0")

    //Dagger2
    implementation("com.google.dagger:dagger:2.33")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    kapt("com.google.dagger:dagger-compiler:2.33")

    //Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.4")

    // Test
    //noinspection GradleDependency
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

}