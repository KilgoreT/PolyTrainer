import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("com.android.library")
                apply("kotlin-android")
            }
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                compileSdk = varch.findVersion("compileSdk").get().toString().toInt()
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = varch
                        .findVersion("kotlinCompilerExtensionVersion").get().toString()
                }
//                lint {
//                    quiet = false
//                    abortOnError = true
//                    warningsAsErrors = true
//                    ignoreWarnings = false
//                    disable.add("TrustAllX509TrustManager")
//                    disable.add("NullSafeMutableLiveData")
//                    disable.add("UnknownIssueId")
//                }
            }


            dependencies {
//                add("implementation", project(":domain"))
//                add("testImplementation", kotlin("test"))
//                add("androidTestImplementation", kotlin("test"))
//                add("implementation", libs.findLibrary("kotlinx.coroutines.android").get())
            }

        }
    }
}