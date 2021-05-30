plugins {
    id("android-library-convention")
    id("kotlin-kapt")
}

val roomVersion by extra { "2.2.6" }


android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

dependencies {
    implementation(project("path" to ":core:core-db-api"))

    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.32")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.5.0")

    //Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-rxjava2:$roomVersion")
//    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    kapt("androidx.room:room-compiler:$roomVersion")

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    //Dagger2
    implementation("com.google.dagger:dagger:2.35.1")
    kapt("com.google.dagger:dagger-compiler:2.35.1")

    //noinspection GradleDependency
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
}
