plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
}
android {
    namespace = "me.apomazkin.core_db"
    compileSdk = 35
    
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        minSdk = 21
        compileSdk = 35
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    
    api(project("path" to ":core:core-db-api"))
    implementation(project("path" to ":core:core-db-impl"))
    
    //Dagger2
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
