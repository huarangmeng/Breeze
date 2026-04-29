import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hrm.breeze.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { it.binaries.framework { baseName = "BreezeCoreUi" } }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core)

            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.material3.adaptive)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            api(libs.navigation.compose)
            api(libs.coil.compose)

            api(libs.markdown.parser)
            api(libs.markdown.runtime)
            api(libs.markdown.renderer)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
