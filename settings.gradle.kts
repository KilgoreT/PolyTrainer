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
include(":core:core-db-api")
include(":core:core-db-impl")
include(":core:core-db")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.0"
        id("com.android.library") version "8.1.1" apply false
        id("com.android.application") version "8.1.4"
        id("androidx.navigation.safeargs.kotlin") version "2.5.3"
        id("com.google.gms.google-services") version "4.4.1"
        id("com.google.firebase.crashlytics") version "2.9.9"
        id("androidx.room") version "2.6.1" apply false
    }
}


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

        create("general") {
            from(files("deps/general.versions.toml"))
        }
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