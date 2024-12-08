dependencyResolutionManagement {
    versionCatalogs {
        create("projectVersions") {
            from(files("../deps/project.versions.toml"))
        }
    }
}