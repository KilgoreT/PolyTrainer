plugins {
    id("lexeme.android.library")
    id("androidx.room")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24" apply false
    id("kotlin-kapt")
}

android {
    room {
        schemaDirectory("$projectDir/schemas")
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
    namespace = "me.apomazkin.core_db_impl"
}

dependencies {
    implementation(project("path" to ":core:core-db-api"))

    implementation(androidLibs.coreKtx)

    //Room
    implementation(datastoreLibs.roomRuntime)
    implementation("androidx.room:room-rxjava2:2.6.1")
    implementation(datastoreLibs.roomKtx)
    annotationProcessor(datastoreLibs.roomCompiler)
    kapt(datastoreLibs.roomCompiler)

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    //Dagger2
    implementation(diLibs.dagger)
    kapt(diLibs.daggerCompiler)

    androidTestImplementation(datastoreLibs.roomTesting)
}
