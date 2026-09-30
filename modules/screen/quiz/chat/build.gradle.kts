import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "me.apomazkin.quiz.chat"
    compileSdk = libs.versions.compileSdk.get().toInt()

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // Compose UI-тесты движения ленты (androidTest): без runner'а
        // JUnit4-тесты на девайсе не стартуют.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }

    buildFeatures {
        buildConfig = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

dependencies {
    
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation(diLibs.javaxInject)
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:datasource:prefs"))
    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))


    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)

    testImplementation(testLibs.junit)
    testImplementation(testLibs.mockk)
    testImplementation(kotlinLibs.coroutinesTest)
    testImplementation(otherLibs.mateTest)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
    androidTestImplementation(composeLibs.uiTestJunit4)
    debugImplementation(composeLibs.uiTestManifest)

}