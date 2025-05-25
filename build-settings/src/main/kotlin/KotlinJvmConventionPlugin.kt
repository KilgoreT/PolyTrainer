import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            
            //            plugins.apply(projectVersionCatalog.findPlugin("kotlin-jvm-gp").get().get().pluginId)
            plugins.apply("org.jetbrains.kotlin.jvm")
            plugins.apply("com.android.lint")
            
            extensions.configure<KotlinJvmProjectExtension> {
                configureKotlinJvm()
            }
            dependencies {
                add(
                    "implementation",
                    "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10",
                    //                    kotlinLibCatalog.findLibrary("stdLibJdk8").get(),
                )
                add(
                    "implementation",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
//                    kotlinLibCatalog.findLibrary("coroutinesCore").get(),
                )
            }
        }
    }
}