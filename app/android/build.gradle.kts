plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.hrm.breeze.demo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "30.0.14904198"

    defaultConfig {
        applicationId = "com.hrm.breeze.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DANDROID_STL=c++_shared"
                arguments +=
                    "-DBREEZE_LLAMA_CPP_SOURCE_DIR=" +
                        rootProject.layout.projectDirectory
                            .dir(rootProject.extra["llamaCppRelativePath"] as String)
                            .asFile.absolutePath
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("../../runtime/llama/src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(projects.app.shared)
    implementation(libs.androidx.activity.compose)

    debugImplementation(libs.compose.uiTooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
