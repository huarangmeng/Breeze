import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.hrm.breeze.runtime.llama"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { it.binaries.framework { baseName = "BreezeRuntimeLlama" } }

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
            api(projects.domain)
            implementation(libs.kotlinx.coroutines.core)
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
val cmakeExecutable =
    providers.provider {
        detectCmakeExecutable() ?: "cmake"
    }
val llamaCppRelativePath = rootProject.extra["llamaCppRelativePath"] as String
val llamaCppSourceDir = rootProject.layout.projectDirectory.dir(llamaCppRelativePath)

fun detectCmakeExecutable(): String? {
    val androidSdkRoot =
        sequenceOf(
            System.getenv("ANDROID_SDK_ROOT"),
            System.getenv("ANDROID_HOME"),
            System.getProperty("android.sdk.path"),
            File(System.getProperty("user.home"), "Library/Android/sdk").absolutePath,
        ).filterNotNull()
            .map(::File)
            .firstOrNull(File::isDirectory)

    val androidSdkCmake =
        androidSdkRoot
            ?.resolve("cmake")
            ?.takeIf(File::isDirectory)
            ?.listFiles()
            ?.filter(File::isDirectory)
            ?.sortedByDescending(File::getName)
            ?.map { it.resolve("bin/cmake") }
            ?.firstOrNull(File::isFile)

    return sequenceOf(
        "/opt/homebrew/bin/cmake",
        "/usr/local/bin/cmake",
        "/Applications/CMake.app/Contents/bin/cmake",
        androidSdkCmake?.absolutePath,
    ).filterNotNull()
        .map(::File)
        .firstOrNull(File::isFile)
        ?.absolutePath
}

val configureDesktopLlamaRuntime by tasks.registering(Exec::class) {
    group = "breeze"
    description = "Configure the in-app Desktop llama.cpp JNI runtime."
    dependsOn(rootProject.tasks.named("syncLlamaCppSubmodule"))
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
    dependsOn(buildDesktopLlamaRuntime)
    from(desktopLlamaOutputDir) {
        include(desktopLlamaLibraryName)
        into(desktopLlamaResourcePath)
    }
}
