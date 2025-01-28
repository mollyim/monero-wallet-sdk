plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = "im.molly"
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}
