import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hrm.breeze.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { it.binaries.framework { baseName = "BreezeData" } }

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
            api(projects.domain)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.sse)
            implementation(libs.ktor.serialization.kotlinxJson)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)

            api(libs.coil.network.ktor)

            // Room3 已原生支持 android / ios / jvm / js / wasmJs，DAO/Entity/Database
            // 声明统一放在 commonMain，各端通过 KSP 生成具体实现。
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            // Web 端额外需要 WebWorkerSQLiteDriver，具体 Worker 由 composeApp 的 web 入口提供。
            implementation(libs.sqlite.web)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqlite.web)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

// Room3 配置：schema 导出到固定目录，便于 CI 校验迁移。
room3 {
    schemaDirectory("$projectDir/schemas")
}

// 为所有支持 Room3 的 target 挂上 KSP。
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspJs", libs.room.compiler)
    add("kspWasmJs", libs.room.compiler)
}
