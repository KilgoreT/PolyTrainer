import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "me.apomazkin.polytrainer.buildsettings"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
//    implementation(projectVersions.kotlin.gradlePlugin)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
//    implementation(projectVersions.android.gradlePlugin)
    implementation("com.android.tools.build:gradle:8.7.3")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "lexeme.android.application"
            implementationClass = "ApplicationConventionPlugin"
        }
        register("androidFeature") {
            id = "lexeme.android.feature"
            implementationClass = "FeatureConventionPlugin"
        }
        register("androidLibrary") {
            id = "lexeme.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kotlinJvmLibrary") {
            id = "lexeme.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
    }
}