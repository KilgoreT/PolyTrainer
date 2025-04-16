plugins {
    id("lexeme.android.library")
    id("androidx.room")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
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
    implementation(datastoreLibs.roomKtx)
    //    annotationProcessor(datastoreLibs.roomCompiler)
    ksp(datastoreLibs.roomCompiler)

    //Dagger2
    implementation(diLibs.dagger)
    kapt(diLibs.daggerCompiler)

    androidTestImplementation(datastoreLibs.roomTesting)
}

//ksp {
////    arg("room.schemaLocation", "$projectDir/schemas")
//    arg("room.incremental", "true")
//    arg("room.expandProjection", "true")
//}

//ksp {
//    arg("room.schemaLocation", "$projectDir/schemas")
//    arg("room.incremental", "true")
//    arg("room.expandProjection", "true")
//}