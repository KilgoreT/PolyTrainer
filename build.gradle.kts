// Единственное место объявления плагинов сборки: версии — только в
// deps/*.versions.toml; модули применяют плагины по id без версии.
plugins {
    alias(libs.plugins.android.app.gp) apply false
    alias(libs.plugins.android.lib.gp) apply false
    alias(libs.plugins.kotlin.gp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm.gp) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kover) apply false
    alias(datastoreLibs.plugins.room) apply false
    alias(composeLibs.plugins.navigation.safeargs) apply false
    alias(firebaseLibs.plugins.google.services) apply false
    alias(firebaseLibs.plugins.crashlytics) apply false
}
