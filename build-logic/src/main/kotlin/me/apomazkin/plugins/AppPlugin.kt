package me.apomazkin.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class AppPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        println("Hello from MyPlugin!")
    }
}