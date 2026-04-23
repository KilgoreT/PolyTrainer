plugins {
    alias(libs.plugins.android.app.gp) apply false
    alias(libs.plugins.kotlin.gp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
}