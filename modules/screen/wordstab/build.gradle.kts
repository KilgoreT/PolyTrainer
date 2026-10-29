import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "me.apomazkin.wordstab"
    compileSdk = libs.versions.compileSdk.get().toInt()

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
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
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))
    implementation(project("path" to ":modules:widget:chipPicker"))
    implementation(project("path" to ":modules:widget:wordrow"))
    //TODO kilg 29.06.2025 10:39 избавиться от зависимости
    //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей, и избавиться от сущностей ui
    implementation(project("path" to ":modules:widget:dictionarypicker"))

    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation(diLibs.javaxInject)
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)
    implementation(datastoreLibs.paging)
    implementation(composeLibs.pagingCompose)

    testImplementation(testLibs.junit)
    testImplementation(project("path" to ":modules:core:mate"))
    testImplementation(otherLibs.mateTest)
    androidTestImplementation(testLibs.androidxTestExt)
    androidTestImplementation(testLibs.espressoCore)
}