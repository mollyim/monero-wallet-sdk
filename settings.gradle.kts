pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
                includeGroupByRegex("androidx?(\\..*)?")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        // "libs" is predefined by Gradle
        create("testLibs") {
            from(files("gradle/test-libs.versions.toml"))
        }
    }
}

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
    This project requires JDK 17+ but it is currently using JDK ${JavaVersion.current()}.
    Java Home: [${System.getProperty("java.home")}]
    https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}

includeProject("lib", "lib/android")
includeProject("demo", "demo/android")

fun includeProject(projectName: String, projectRoot: String) {
    val projectId = ":$projectName"
    include(projectId)
    project(projectId).projectDir = file(projectRoot)
}
