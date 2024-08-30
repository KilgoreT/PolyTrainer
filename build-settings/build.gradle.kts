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
    //TODO kilg 02.09.2024 10:45 move version to version catalog
    implementation("com.android.tools.build:gradle:8.1.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "polytrainer.android.application"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("androidLibrary") {
            id = "polytrainer.android.library"
            implementationClass = "AndroidLibConventionPlugin"
        }
        register("androidFeature") {
            id = "polytrainer.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}