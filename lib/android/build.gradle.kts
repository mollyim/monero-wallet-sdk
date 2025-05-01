plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.sdk.android.library.jacoco)
}

kotlin {
    jvmToolchain(17)
}

val vendorDir = File(rootDir, "vendor")
val downloadCacheDir = layout.buildDirectory.dir("downloads").get().asFile

android {
    namespace = "im.molly.monero"
    compileSdk = 35
    ndkVersion = "23.1.7779620"

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testHandleProfiling = true
        testFunctionalTest = true

        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                arguments += "-DVENDOR_DIR=${vendorDir.path}"
                arguments += "-DDOWNLOAD_CACHE=${downloadCacheDir.path}"
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }

        getByName("release") {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }

//    testOptions {
//        managedDevices {
//            localDevices {
//                create("pixel2api30") {
//                    device = "Pixel 2"
//                    apiLevel = 30
//                    systemImageSource = "aosp-atd"
//                }
//            }
//        }
//    }
}

dependencies {
    api(platform(libs.okhttp.bom))
    api(libs.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlin.junit)
    testImplementation(testLibs.junit)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.truth)

    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(testLibs.androidx.test.core)
    androidTestImplementation(testLibs.androidx.test.junit)
    androidTestImplementation(testLibs.androidx.test.truth)
    androidTestImplementation(testLibs.androidx.test.rules)
    androidTestImplementation(testLibs.androidx.test.runner)
    androidTestImplementation(testLibs.mockk)
}
