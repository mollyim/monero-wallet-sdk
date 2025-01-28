pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("testLibs") {
            from(files("gradle/test-libs.versions.toml"))
        }
    }
}

includeProject("lib", "lib/android")
includeProject("demo", "demo/android")

fun includeProject(projectName: String, projectRoot: String) {
    val projectId = ":$projectName"
    include(projectId)
    project(projectId).projectDir = file(projectRoot)
}
