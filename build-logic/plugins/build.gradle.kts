plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("MoneroSdkModulePlugin") {
            id = libs.plugins.monero.sdk.module.get().pluginId
            implementationClass = "MoneroSdkModulePlugin"
        }
    }
}
