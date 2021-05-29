rootProject.name = "PolyTrainer"


include(":app")
include(":core:core-interactor")
include(":core:core-resources")
include(":core:core-binding")
include(":core:core-util")
include(":core:core-base")
include(":core:core-db-api")
include(":core:core-db-impl")
include(":core:core-db")
include(":widget:view-progress-quiz")
include(":feature:feature-training-write-impl")
include(":feature:feature-training-write-api")
include(":feature:feature-training-list-impl")
include(":feature:feature-training-list-api")
include(":feature:feature-bottom-menu-api")
include(":feature:feature-bottom-menu-impl")
include(":feature:feature-vocabulary-impl")
include(":feature:feature-vocabulary-api")
include(":feature:feature-statistic-impl")
include(":feature:feature-statistic-api")

includeBuild("build-settings")

//classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.4")
//classpath("com.google.gms:google-services:4.3.5")
//classpath("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
pluginManagement {
    // пока их не получается оставить в блоке dependencyResolutionManagement
    // потому что его нужно выполнить еще до конфигурации проекта.
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            val pluginId = requested.id.id
            println(">>>> pluginId: $pluginId")
            when {
                pluginId.startsWith("org.jetbrains.kotlin") -> {
                    useVersion("1.4.32")
                }
                pluginId.contains("kotlin-") -> {
                    useVersion("1.5.10")
//                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
                }
                pluginId.startsWith("com.android.") -> {
//                    useVersion("4.1.3")
                    useModule("com.android.tools.build:gradle:4.1.3")
                }
                pluginId.startsWith("androidx.navigation.safeargs.kotlin") -> {
//                    useVersion("2.3.4")
                    useModule("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.4")
                    //
                }
                pluginId.startsWith("com.google.gms.google-services") -> {
//                    useVersion("4.3.5")
                    useModule("com.google.gms:google-services:4.3.5")
                    //
                }
                pluginId.startsWith("com.google.firebase.crashlytics") -> {
//                    useVersion("2.5.2")
                    useModule("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
                    //
                }
//                pluginId.startsWith("kotlin-android-extensions") -> {
//                    useModule("kotlin-android-extensions.gradle.plugin:1.4.32")
//                }
//                pluginId.startsWith("kotlin-kapt") -> {
//                    useVersion("1.4.32")
////                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
//                }
            }
        }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
