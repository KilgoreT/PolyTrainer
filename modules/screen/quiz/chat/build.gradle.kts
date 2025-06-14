plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.quiz.chat"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 21
        targetSdk = 35
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
    
    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:dictionarypicker"))
    
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    
}