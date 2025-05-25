import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

//            plugins.apply(projectVersionCatalog.findPlugin("android-lib-gp").get().get().pluginId)
            plugins.apply("com.android.library")
//            plugins.apply(projectVersionCatalog.findPlugin("kotlin-gp").get().get().pluginId)
            plugins.apply("org.jetbrains.kotlin.android")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                compileSdk = 34
//                    projectVersionCatalog.findVersion("compileSdk").get().toString().toInt()
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }
            dependencies {
                add(
                    "testImplementation",
                    "junit:junit:4.13.2"
//                    testLibCatalog.findLibrary("junit").get()
                )
                add(
                    "androidTestImplementation",
                    "androidx.test.ext:junit:1.2.1",
//                    testLibCatalog.findLibrary("androidxTestExt").get()
                )
                add(
                    "androidTestImplementation",
                    "androidx.test.espresso:espresso-core:3.6.1",
//                    testLibCatalog.findLibrary("espressoCore").get()
                )
            }
        }
    }
}