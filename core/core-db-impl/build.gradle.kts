plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("androidx.room")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
}

android {
    room {
        schemaDirectory("$projectDir/schemas")
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
    namespace = "me.apomazkin.core_db_impl"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 21
        targetSdk = 35
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
    
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation(project("path" to ":core:core-db-api"))

    implementation(androidLibs.coreKtx)

    //Room
    implementation(datastoreLibs.roomRuntime)
    implementation(datastoreLibs.roomKtx)
    ksp(datastoreLibs.roomCompiler)
    implementation(datastoreLibs.roomPaging)

    //Dagger2
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)

    androidTestImplementation(datastoreLibs.roomTesting)
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}