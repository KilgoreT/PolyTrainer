import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            plugins.apply(projectVersionCatalog.findPlugin("kotlin-jvm-gp").get().get().pluginId)
            plugins.apply("com.android.lint")

            extensions.configure<KotlinJvmProjectExtension> {
                configureKotlinJvm()
            }
            dependencies {
                add("implementation", kotlinLibCatalog.findLibrary("stdLibJdk8").get())
                add("implementation", kotlinLibCatalog.findLibrary("coroutinesCore").get())
            }
        }
    }
}