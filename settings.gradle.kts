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
            val qqq = requested.id
            println(">>>> req: $qqq")
            when {
                pluginId.startsWith("org.jetbrains.kotlin") -> {
                    useVersion("1.6.21")
                }
                pluginId.contains("kotlin-") -> {
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
                }
                pluginId.startsWith("com.android.") -> {
                    useModule("com.android.tools.build:gradle:7.2.2")
                }
                pluginId.startsWith("androidx.navigation.safeargs.kotlin") -> {
                    useModule("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
                }
                pluginId.startsWith("com.google.gms.google-services") -> {
                    useModule("com.google.gms:google-services:4.3.5")
                }
                pluginId.startsWith("com.google.firebase.crashlytics") -> {
                    useModule("com.google.firebase:firebase-crashlytics-gradle:2.5.2")
                }
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
