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
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.datastore.preferences.core)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.serialization.kotlinxJson)

            implementation(libs.coil.core)
            api(libs.coil.network.ktor)

            // Room3 已原生支持 android / ios / jvm / js / wasmJs，DAO/Entity/Database
            // 声明统一放在 commonMain，各端通过 KSP 生成具体实现。
            implementation(libs.room.runtime)
            implementation(libs.sqlite)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.bundled)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.sqlite.bundled)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            // Web 端额外需要 WebWorkerSQLiteDriver，具体 Worker 由 app/web 入口提供。
            implementation(libs.sqlite.web)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.testJunit)
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

val desktopLlamaOsSegment: String =
    run {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("mac") -> "macos"
            os.contains("win") -> "windows"
            os.contains("linux") -> "linux"
            else -> os.replace(Regex("[^a-z0-9]+"), "-")
        }
    }

val desktopLlamaArchSegment: String =
    run {
        val arch = System.getProperty("os.arch").lowercase()
        when {
            arch == "aarch64" || arch == "arm64" -> "arm64"
            arch == "x86_64" || arch == "amd64" -> "x64"
            else -> arch.replace(Regex("[^a-z0-9]+"), "-")
        }
    }

val desktopLlamaPlatform = "$desktopLlamaOsSegment-$desktopLlamaArchSegment"
val desktopLlamaGpuBackend =
    providers.gradleProperty("breezeDesktopLlamaGpuBackend").orElse("auto").map { requested ->
        when (val normalized = requested.lowercase()) {
            "auto" ->
                when (desktopLlamaOsSegment) {
                    "macos" -> "metal"
                    "windows", "linux" -> "vulkan"
                    else -> "cpu"
                }
            "none" -> "cpu"
            "cpu", "metal", "vulkan", "cuda", "hip", "sycl", "opencl" -> normalized
            else -> error(
                "Unsupported breezeDesktopLlamaGpuBackend=$requested. " +
                    "Use auto, cpu, metal, vulkan, cuda, hip, sycl, or opencl."
            )
        }
    }

val desktopLlamaBuildDir =
    desktopLlamaGpuBackend.map { backend -> layout.buildDirectory.dir("native/llama/$desktopLlamaPlatform/$backend").get() }
val desktopLlamaOutputDir = desktopLlamaBuildDir.map { it.dir("out") }
val desktopLlamaResourcePath = "breeze-runtime/$desktopLlamaPlatform"
val desktopLlamaLibraryName = System.mapLibraryName("breeze_llama_jni")
val cmakeExecutable = providers.environmentVariable("BREEZE_CMAKE").orElse("cmake")
val llamaCppSourceDir = rootProject.layout.projectDirectory.dir("third_party/llama.cpp")
val shouldBundleDesktopLlamaRuntime =
    providers.gradleProperty("breezeBuildDesktopLlamaRuntime").map(String::toBoolean).orElse(false)

val configureDesktopLlamaRuntime by tasks.registering(Exec::class) {
    group = "breeze"
    description = "Configure the in-app Desktop llama.cpp JNI runtime."
    val buildDir = desktopLlamaBuildDir.get().asFile
    inputs.files(fileTree("src/jvmMain/cpp"))
    inputs.dir(llamaCppSourceDir)
    inputs.property("desktopLlamaGpuBackend", desktopLlamaGpuBackend)
    outputs.dir(desktopLlamaBuildDir)
    commandLine(
        cmakeExecutable.get(),
        "-S",
        "$projectDir/src/jvmMain/cpp",
        "-B",
        buildDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${desktopLlamaOutputDir.get().asFile.absolutePath}",
        "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${desktopLlamaOutputDir.get().asFile.absolutePath}",
        "-DBREEZE_LLAMA_CPP_SOURCE_DIR=${llamaCppSourceDir.asFile.absolutePath}",
        "-DBREEZE_LLAMA_GPU_BACKEND=${desktopLlamaGpuBackend.get()}",
    )
}

val buildDesktopLlamaRuntime by tasks.registering(Exec::class) {
    group = "breeze"
    description = "Build the in-app Desktop llama.cpp JNI runtime."
    dependsOn(configureDesktopLlamaRuntime)
    inputs.dir(desktopLlamaBuildDir)
    outputs.file(desktopLlamaOutputDir.map { it.file(desktopLlamaLibraryName) })
    commandLine(
        cmakeExecutable.get(),
        "--build",
        desktopLlamaBuildDir.get().asFile.absolutePath,
        "--config",
        "Release",
        "--target",
        "breeze_llama_jni",
        "--parallel",
    )
}

tasks.named<Copy>("jvmProcessResources") {
    if (shouldBundleDesktopLlamaRuntime.get()) {
        dependsOn(buildDesktopLlamaRuntime)
        from(desktopLlamaOutputDir) {
            include(desktopLlamaLibraryName)
            into(desktopLlamaResourcePath)
        }
    }
}
