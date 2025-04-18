import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            plugins.apply(projectVersionCatalog.findPlugin("android-lib-gp").get().get().pluginId)
            plugins.apply(projectVersionCatalog.findPlugin("kotlin-gp").get().get().pluginId)
            plugins.apply(projectVersionCatalog.findPlugin("kotlin-compose").get().get().pluginId)

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                compileSdk =
                    projectVersionCatalog.findVersion("compileSdk").get().toString().toInt()
                defaultConfig {
                    minSdk = projectVersionCatalog.findVersion("minSdk").get().toString().toInt()
                }
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = projectVersionCatalog
                        .findVersion("kotlinCompilerExtensionVersion").get().toString()
                }
            }

            dependencies {
                add("testImplementation", testLibCatalog.findLibrary("junit").get())
                add(
                    "androidTestImplementation",
                    testLibCatalog.findLibrary("androidxTestExt").get()
                )
                add("androidTestImplementation", testLibCatalog.findLibrary("espressoCore").get())
            }
        }
    }
}