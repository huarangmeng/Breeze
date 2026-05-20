package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge = JvmInProcessLlamaRuntimeBridge()

private class JvmInProcessLlamaRuntimeBridge : OnDeviceRuntimeBridge {
    private val runtimeInstaller = JvmLlamaRuntimeInstaller()
    private val nativeBridge = BreezeLlamaNativeBridge(runtimeInstaller)
    private val modelMutex = Mutex()
    private var loadedModel: LoadedModel? = null

    override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
        if (runCatching { loadModel(request) }.isSuccess) {
            InferenceRuntimeState.Ready
        } else {
            InferenceRuntimeState.Failed
        }

    override fun streamCompletion(request: OnDeviceRuntimeRequest): Flow<String> = callbackFlow {
        val normalized = request.normalized()
        val loadedModel = loadModel(normalized.toLaunchRequest())
        val generationHandle =
            nativeBridge.generate(
                modelHandle = loadedModel.handle,
                prompt = normalized.messages.toChatMlPrompt(),
                temperature = normalized.temperature,
                topP = normalized.topP,
                maxTokens = normalized.maxTokens,
                contextWindow = normalized.contextWindow,
                callback =
                    object : BreezeLlamaTokenCallback {
                        override fun onToken(token: String) {
                            trySend(token)
                        }

                        override fun onComplete() {
                            close()
                        }

                        override fun onError(message: String) {
                            close(IllegalStateException(message))
                        }
                    },
            )
        awaitClose { nativeBridge.cancel(generationHandle) }
    }

    private suspend fun loadModel(request: OnDeviceRuntimeLaunchRequest): LoadedModel =
        modelMutex.withLock {
            val normalized = request.normalized()
            val existing = loadedModel
            if (existing != null && existing.matches(normalized)) {
                return@withLock existing
            }
            existing?.let { nativeBridge.unload(it.handle) }
            val handle =
                nativeBridge.loadModel(
                    modelPath = normalized.requireLocalPath(),
                    contextWindow = normalized.contextWindow,
                )
            loadedModel =
                LoadedModel(
                    modelPath = normalized.requireLocalPath(),
                    contextWindow = normalized.contextWindow,
                    handle = handle,
                )
            checkNotNull(loadedModel)
        }
}

private data class LoadedModel(
    val modelPath: String,
    val contextWindow: Int,
    val handle: Long,
) {
    fun matches(request: OnDeviceRuntimeLaunchRequest): Boolean =
        modelPath == request.requireLocalPath() && contextWindow == request.contextWindow
}

private class JvmLlamaRuntimeInstaller {
    fun installAndLoad() {
        val configuredLibraryFile = configuredLibraryFile()
        val libraryFile = configuredLibraryFile ?: installedLibraryFile()
        if (configuredLibraryFile == null) {
            refreshBundledRuntime(libraryFile)
        }
        if (!libraryFile.isFile) {
            error(
                "Breeze llama.cpp runtime is missing: ${libraryFile.absolutePath}. " +
                    "Desktop builds now bundle the current platform runtime automatically, " +
                    "but no native runtime was found for ${runtimePlatformSegment()}. " +
                    "or set $ENV_BREEZE_LLAMA_JNI_LIBRARY_PATH to an existing native library."
            )
        }
        System.load(libraryFile.absolutePath)
    }

    private fun configuredLibraryFile(): File? =
        System.getenv(ENV_BREEZE_LLAMA_JNI_LIBRARY_PATH)
            ?.takeIf(String::isNotBlank)
            ?.let(::File)

    private fun installedLibraryFile(): File {
        val runtimeDirectory = resolveJvmRuntimeDirectory().apply { mkdirs() }
        return File(runtimeDirectory, System.mapLibraryName(BREEZE_LLAMA_LIBRARY_NAME))
    }

    private fun refreshBundledRuntime(target: File) {
        val resourcePath = bundledRuntimeResourcePath(target.name)
        if (target.isFile && bundledRuntimeMatches(target, resourcePath)) {
            return
        }
        val input = JvmLlamaRuntimeInstaller::class.java.getResourceAsStream(resourcePath) ?: return
        target.parentFile?.mkdirs()
        val tempFile = File(target.parentFile ?: target.absoluteFile.parentFile, "${target.name}.tmp")
        input.use { source ->
            tempFile.outputStream().use { sink -> source.copyTo(sink) }
        }
        moveReplacing(tempFile, target)
    }

    private fun bundledRuntimeMatches(
        target: File,
        resourcePath: String,
    ): Boolean {
        if (!target.isFile) {
            return false
        }
        val input = JvmLlamaRuntimeInstaller::class.java.getResourceAsStream(resourcePath) ?: return false
        input.use { bundled ->
            target.inputStream().use { installed ->
                return streamsEqual(installed, bundled)
            }
        }
    }

    private fun bundledRuntimeResourcePath(fileName: String): String =
        "/breeze-runtime/${runtimePlatformSegment()}/$fileName"

    private fun moveReplacing(
        source: File,
        target: File,
    ) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}

private fun streamsEqual(
    first: InputStream,
    second: InputStream,
): Boolean {
    val firstBuffer = ByteArray(DEFAULT_STREAM_COMPARE_BUFFER_SIZE)
    val secondBuffer = ByteArray(DEFAULT_STREAM_COMPARE_BUFFER_SIZE)
    while (true) {
        val firstRead = first.read(firstBuffer)
        val secondRead = second.read(secondBuffer)
        if (firstRead != secondRead) {
            return false
        }
        if (firstRead == -1) {
            return true
        }
        for (index in 0 until firstRead) {
            if (firstBuffer[index] != secondBuffer[index]) {
                return false
            }
        }
    }
}

private class BreezeLlamaNativeBridge(
    private val runtimeInstaller: JvmLlamaRuntimeInstaller,
) {
    @Volatile
    private var loaded = false

    fun loadModel(
        modelPath: String,
        contextWindow: Int,
    ): Long {
        ensureRuntimeLoaded()
        return nativeLoadModel(modelPath, contextWindow)
    }

    fun unload(handle: Long) {
        ensureRuntimeLoaded()
        nativeUnload(handle)
    }

    fun generate(
        modelHandle: Long,
        prompt: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
        callback: BreezeLlamaTokenCallback,
    ): Long {
        ensureRuntimeLoaded()
        return nativeGenerate(modelHandle, prompt, temperature, topP, maxTokens, contextWindow, callback)
    }

    fun cancel(handle: Long) {
        ensureRuntimeLoaded()
        nativeCancel(handle)
    }

    private fun ensureRuntimeLoaded() {
        if (loaded) {
            return
        }
        synchronized(this) {
            if (!loaded) {
                runtimeInstaller.installAndLoad()
                loaded = true
            }
        }
    }

    private external fun nativeLoadModel(
        modelPath: String,
        contextWindow: Int,
    ): Long

    private external fun nativeUnload(handle: Long)

    private external fun nativeGenerate(
        modelHandle: Long,
        prompt: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        contextWindow: Int,
        callback: BreezeLlamaTokenCallback,
    ): Long

    private external fun nativeCancel(handle: Long)
}

private interface BreezeLlamaTokenCallback {
    fun onToken(token: String)

    fun onComplete()

    fun onError(message: String)
}

private fun OnDeviceRuntimeLaunchRequest.normalized(): OnDeviceRuntimeLaunchRequest {
    require(contextWindow > 0) { "Context window must be positive" }
    val localPath = requireLocalPath()
    check(File(localPath).isFile) { "Downloaded on-device model file is missing: $localPath" }
    return copy(localPath = localPath)
}

private fun OnDeviceRuntimeRequest.normalized(): OnDeviceRuntimeRequest {
    require(contextWindow > 0) { "Context window must be positive" }
    require(maxTokens > 0) { "Max tokens must be positive" }
    val localPath = requireLocalPath()
    check(File(localPath).isFile) { "Downloaded on-device model file is missing: $localPath" }
    return copy(localPath = localPath)
}

private fun OnDeviceRuntimeLaunchRequest.requireLocalPath(): String =
    checkNotNull(localPath?.takeIf(String::isNotBlank)) { "Missing local model file path" }

private fun OnDeviceRuntimeRequest.requireLocalPath(): String =
    checkNotNull(localPath?.takeIf(String::isNotBlank)) { "Missing local model file path" }

private fun OnDeviceRuntimeRequest.toLaunchRequest(): OnDeviceRuntimeLaunchRequest =
    OnDeviceRuntimeLaunchRequest(
        modelId = modelId,
        localPath = requireLocalPath(),
        contextWindow = contextWindow,
    )

private fun List<LlamaMessage>.toChatMlPrompt(): String =
    buildString {
        val hasSystemMessage = this@toChatMlPrompt.any { message -> message.role == LlamaMessage.Role.System }
        if (!hasSystemMessage) {
            append("<|im_start|>system\n")
            append("You are Breeze, a helpful on-device assistant.\n")
            append("<|im_end|>\n")
        }
        for (message in this@toChatMlPrompt) {
            val role =
                when (message.role) {
                    LlamaMessage.Role.System -> "system"
                    LlamaMessage.Role.User -> "user"
                    LlamaMessage.Role.Assistant -> "assistant"
                }
            append("<|im_start|>")
            append(role)
            append('\n')
            append(message.content)
            append('\n')
            append("<|im_end|>\n")
        }
        append("<|im_start|>assistant\n")
    }

private fun resolveJvmRuntimeDirectory(): File {
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()
    val baseDirectory =
        when {
            osName.contains("mac") -> File(userHome, "Library/Application Support/Breeze")
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (appData.isNullOrBlank()) {
                    File(userHome, "AppData/Roaming/Breeze")
                } else {
                    File(appData, "Breeze")
                }
            }
            else -> {
                val xdgDataHome = System.getenv("XDG_DATA_HOME")
                if (xdgDataHome.isNullOrBlank()) {
                    File(userHome, ".local/share/Breeze")
                } else {
                    File(xdgDataHome, "Breeze")
                }
            }
        }
    return File(baseDirectory, "models/runtime/native")
}

private fun runtimePlatformSegment(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osSegment =
        when {
            os.contains("mac") -> "macos"
            os.contains("win") -> "windows"
            os.contains("linux") -> "linux"
            else -> os.replace(Regex("[^a-z0-9]+"), "-")
        }
    val archSegment =
        when {
            arch == "aarch64" || arch == "arm64" -> "arm64"
            arch == "x86_64" || arch == "amd64" -> "x64"
            else -> arch.replace(Regex("[^a-z0-9]+"), "-")
        }
    return "$osSegment-$archSegment"
}

private const val BREEZE_LLAMA_LIBRARY_NAME = "breeze_llama_jni"
private const val ENV_BREEZE_LLAMA_JNI_LIBRARY_PATH = "BREEZE_LLAMA_JNI_LIBRARY_PATH"
private const val DEFAULT_STREAM_COMPARE_BUFFER_SIZE = 8 * 1024
