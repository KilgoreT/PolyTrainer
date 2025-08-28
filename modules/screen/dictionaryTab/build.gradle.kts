plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.dictionarytab"
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
    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))
    implementation(project("path" to ":modules:widget:chipPicker"))
    //TODO kilg 29.06.2025 10:39 избавиться от зависимости
    //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей, и избавиться от сущностей ui
    implementation(project("path" to ":modules:widget:dictionarypicker"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)
    implementation(datastoreLibs.paging)
    implementation(composeLibs.pagingCompose)

    testImplementation("junit:junit:4.13.2")
    testImplementation(project("path" to ":modules:core:mate"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}