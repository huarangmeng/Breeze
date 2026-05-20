import java.io.ByteArrayOutputStream

val breezeGitExecutable = providers.environmentVariable("BREEZE_GIT").orElse("git")
val llamaCppRelativePath = "third_party/llama.cpp"
val llamaCppSourceDir = layout.projectDirectory.dir(llamaCppRelativePath)
val llamaCppCmakeLists = llamaCppSourceDir.file("CMakeLists.txt")

rootProject.extra["llamaCppRelativePath"] = llamaCppRelativePath

fun ensureLlamaCppSubmoduleInitialized(reason: String) {
    if (llamaCppCmakeLists.asFile.exists()) {
        return
    }

    val stdout = ByteArrayOutputStream()
    val stderr = ByteArrayOutputStream()
    val process =
        ProcessBuilder(
            breezeGitExecutable.get(),
            "submodule",
            "update",
            "--init",
            "--recursive",
            "--",
            llamaCppRelativePath,
        ).directory(rootProject.projectDir)
            .start()
    val stdoutThread = Thread { process.inputStream.use { it.copyTo(stdout) } }
    val stderrThread = Thread { process.errorStream.use { it.copyTo(stderr) } }
    stdoutThread.start()
    stderrThread.start()
    val exitCode = process.waitFor()
    stdoutThread.join()
    stderrThread.join()

    if (exitCode != 0 || !llamaCppCmakeLists.asFile.exists()) {
        val details =
            buildString {
                val stdoutText = stdout.toString().trim()
                val stderrText = stderr.toString().trim()
                if (stdoutText.isNotEmpty()) {
                    appendLine(stdoutText)
                }
                if (stderrText.isNotEmpty()) {
                    appendLine(stderrText)
                }
            }.trim()

        throw GradleException(
            buildString {
                append(
                    "Unable to initialize submodule $llamaCppRelativePath during $reason. " +
                        "Run `git submodule update --init --recursive -- $llamaCppRelativePath` " +
                        "from the repository root."
                )
                if (details.isNotEmpty()) {
                    appendLine()
                    appendLine()
                    append(details)
                }
            }
        )
    }
}

if (System.getProperty("idea.sync.active") == "true") {
    ensureLlamaCppSubmoduleInitialized("Gradle sync")
}

val syncLlamaCppSubmodule by tasks.registering {
    group = "breeze"
    description = "Ensure the pinned llama.cpp submodule is initialized."
    notCompatibleWithConfigurationCache("Runs git submodule initialization through the build script.")
    outputs.file(llamaCppCmakeLists)
    doLast {
        ensureLlamaCppSubmoduleInitialized("task $path")
    }
}
