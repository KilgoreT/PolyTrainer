import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "me.apomazkin.groupstab"
    compileSdk = 36

    testOptions {
        targetSdk = 36
    }

    lint {
        targetSdk = 36
    }

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:domain:group"))
    implementation(project("path" to ":modules:widget:wordrow"))
    implementation(project("path" to ":modules:widget:grouptree"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))

    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation("javax.inject:javax.inject:1")
    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)

    testImplementation("junit:junit:4.13.2")
    testImplementation(project("path" to ":modules:core:mate"))
}
