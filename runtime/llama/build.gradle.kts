import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

abstract class ValidateDesktopLlamaBuildHostTask : DefaultTask() {
    @get:Input
    abstract val targetPlatform: Property<String>

    @get:Input
    abstract val hostPlatform: Property<String>

    @TaskAction
    fun validate() {
        check(targetPlatform.get() == hostPlatform.get()) {
            "Cross-compiling the Desktop llama runtime is not supported yet. " +
                "Requested target=${targetPlatform.get()}, current host=${hostPlatform.get()}. " +
                "Build Windows runtime and MSI artifacts on a Windows host."
        }
    }
}

abstract class VerifyBundledDesktopLlamaRuntimeTask : DefaultTask() {
    @get:InputDirectory
    abstract val resourcesDirectory: DirectoryProperty

    @get:Input
    abstract val resourcePath: Property<String>

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val targetPlatform: Property<String>

    @TaskAction
    fun verify() {
        val bundledLibrary = resourcesDirectory.get().file("${resourcePath.get()}/${libraryName.get()}").asFile
        check(bundledLibrary.isFile) {
            "Expected bundled Desktop llama runtime at ${bundledLibrary.absolutePath}, " +
                "but it was not produced for target ${targetPlatform.get()}."
        }
    }
}

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
            api(projects.core)
            api(projects.runtime.api)
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

fun normalizeDesktopLlamaOsSegment(value: String): String =
    when {
        value.contains("mac") -> "macos"
        value.contains("win") -> "windows"
        value.contains("linux") -> "linux"
        else -> value.replace(Regex("[^a-z0-9]+"), "-")
    }

fun normalizeDesktopLlamaArchSegment(value: String): String =
    when {
        value == "aarch64" || value == "arm64" -> "arm64"
        value == "x86_64" || value == "amd64" -> "x64"
        else -> value.replace(Regex("[^a-z0-9]+"), "-")
    }

val desktopLlamaHostOsSegment: String =
    run {
        val os = System.getProperty("os.name").lowercase()
        normalizeDesktopLlamaOsSegment(os)
    }

val desktopLlamaHostArchSegment: String =
    run {
        val arch = System.getProperty("os.arch").lowercase()
        normalizeDesktopLlamaArchSegment(arch)
    }

val desktopLlamaTargetOsSegment =
    providers.gradleProperty("breezeDesktopLlamaTargetOs")
        .orElse(desktopLlamaHostOsSegment)
        .map { normalizeDesktopLlamaOsSegment(it.lowercase()) }
val desktopLlamaTargetArchSegment =
    providers.gradleProperty("breezeDesktopLlamaTargetArch")
        .orElse(desktopLlamaHostArchSegment)
        .map { normalizeDesktopLlamaArchSegment(it.lowercase()) }
val desktopLlamaHostPlatform = "$desktopLlamaHostOsSegment-$desktopLlamaHostArchSegment"
val desktopLlamaPlatform =
    providers.provider {
        "${desktopLlamaTargetOsSegment.get()}-${desktopLlamaTargetArchSegment.get()}"
    }
val desktopLlamaGpuBackend =
    providers.gradleProperty("breezeDesktopLlamaGpuBackend").orElse("auto").map { requested ->
        when (val normalized = requested.lowercase()) {
            "auto" ->
                when (desktopLlamaTargetOsSegment.get()) {
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
    desktopLlamaGpuBackend.map { backend -> layout.buildDirectory.dir("native/llama/${desktopLlamaPlatform.get()}/$backend").get() }
val desktopLlamaOutputDir = desktopLlamaBuildDir.map { it.dir("out") }
val desktopLlamaResourcePath = providers.provider { "breeze-runtime/${desktopLlamaPlatform.get()}" }
val desktopLlamaLibraryName = System.mapLibraryName("breeze_llama_jni")
val cmakeExecutable =
    providers.provider {
        detectCmakeExecutable() ?: "cmake"
    }
val llamaCppRelativePath = rootProject.extra["llamaCppRelativePath"] as String
val llamaCppSourceDir = rootProject.layout.projectDirectory.dir(llamaCppRelativePath)

val validateDesktopLlamaBuildHost by tasks.registering(ValidateDesktopLlamaBuildHostTask::class) {
    group = "breeze"
    description = "Validate that the Desktop llama runtime target matches the current build host."
    targetPlatform.set(desktopLlamaPlatform)
    hostPlatform.set(desktopLlamaHostPlatform)
}

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
    if (!llamaCppSourceDir.file("CMakeLists.txt").asFile.exists()) {
        dependsOn(rootProject.tasks.named("syncLlamaCppSubmodule"))
    }
    dependsOn(validateDesktopLlamaBuildHost)
    val buildDir = desktopLlamaBuildDir.get().asFile
    inputs.files(fileTree("src/jvmMain/cpp"))
    inputs.dir(llamaCppSourceDir)
    inputs.property("desktopLlamaGpuBackend", desktopLlamaGpuBackend)
    inputs.property("desktopLlamaPlatform", desktopLlamaPlatform)
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
        into(desktopLlamaResourcePath.get())
    }
}

val verifyDesktopLlamaBundledRuntime by tasks.registering(VerifyBundledDesktopLlamaRuntimeTask::class) {
    group = "verification"
    description = "Verify that the current Desktop llama runtime has been bundled into JVM resources."
    dependsOn(tasks.named("jvmProcessResources"))
    resourcesDirectory.set(layout.buildDirectory.dir("processedResources/jvm/main"))
    resourcePath.set(desktopLlamaResourcePath)
    libraryName.set(desktopLlamaLibraryName)
    targetPlatform.set(desktopLlamaPlatform)
}
