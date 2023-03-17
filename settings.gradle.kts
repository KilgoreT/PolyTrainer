@file:Suppress("UnstableApiUsage")

rootProject.name = "PolyTrainer"

includeBuild("build-settings")

include(":app")

//Core
include(":modules:core:mate")
include(":modules:core:theme")
include(":modules:core:ui")
include(":modules:core:tools")

//Screen
include(":modules:screen:splash")
include(":modules:screen:langpicker")
include(":modules:screen:main")
include(":modules:screen:vocabulary")
include(":modules:screen:wordcard")

//Widget
include(":modules:widget:iconDropDowned")
include(":modules:widget:chipPicker")
include(":modules:widget:coloredText")

//Libraries
include(":modules:library:flags")

include(":modules:datasource:prefs")

//Old
include(":core:core-interactor")
include(":core:core-resources")
//include(":core:core-binding")
//include(":core:core-util")
//include(":core:core-base")
include(":core:core-db-api")
include(":core:core-db-impl")
include(":core:core-db")
//include(":widget:view-progress-quiz")
//include(":feature:feature-training-write-impl")
//include(":feature:feature-training-write-api")
//include(":feature:feature-training-list-impl")
//include(":feature:feature-training-list-api")
//include(":feature:feature-bottom-menu-api")
//include(":feature:feature-bottom-menu-impl")
//include(":feature:feature-vocabulary-impl")
//include(":feature:feature-vocabulary-api")
//include(":feature:feature-statistic-impl")
//include(":feature:feature-statistic-api")

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

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    @Suppress("UnstableApiUsage")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    versionCatalogs {

        /**
         * Link to Kotlin-Compose compatibility
         * @see [https://developer.android.com/jetpack/androidx/releases/compose-kotlin]
         */

        create("kotlinLibs") {
            from(files("deps/kotlin.versions.toml"))
        }
        create("androidLibs") {
            from(files("deps/android.versions.toml"))
        }
        create("composeLibs") {
            from(files("deps/compose.versions.toml"))
        }
        create("datastoreLibs") {
            from(files("deps/datastore.versions.toml"))
        }
        create("firebaseLibs") {
            from(files("deps/firebase.versions.toml"))
        }
        create("diLibs") {
            from(files("deps/di.versions.toml"))
        }
        create("testLibs") {
            from(files("deps/test-libs.versions.toml"))
        }
        create("otherLibs") {
            library("flags", "com.github.blongho:worldCountryData:v1.5.4-alpha-1")
        }
    }
}