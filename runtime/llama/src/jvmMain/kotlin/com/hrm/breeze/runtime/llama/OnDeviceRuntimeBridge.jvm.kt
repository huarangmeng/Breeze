package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.platform.PlatformKind
import com.hrm.breeze.platform.platformInfo
import com.hrm.breeze.runtime.api.InferenceMessage
import com.hrm.breeze.runtime.api.OnDeviceRuntimeBackend
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeTargetPlatform
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

    override val capability =
        desktopJvmLlamaCapability(
            defaultBackend = detectJvmDefaultBackend(),
            targetPlatforms = setOf(
                OnDeviceRuntimeTargetPlatform.MacOs,
                OnDeviceRuntimeTargetPlatform.Windows,
                OnDeviceRuntimeTargetPlatform.Linux,
            ),
            supportedBackends = supportedJvmBackends(),
        )

    override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
        if (runCatching { loadModel(request) }.isSuccess) {
            InferenceRuntimeState.Ready
        } else {
            InferenceRuntimeState.Failed
        }

    override fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String> = callbackFlow {
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
        bundledRuntimeResourcePath(
            platformSegment = runtimePlatformSegment(),
            fileName = fileName,
        )

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

private fun List<InferenceMessage>.toChatMlPrompt(): String =
    buildString {
        val hasSystemMessage = this@toChatMlPrompt.any { message -> message.role == InferenceMessage.Role.System }
        if (!hasSystemMessage) {
            append("<|im_start|>system\n")
            append("You are Breeze, a helpful on-device assistant.\n")
            append("<|im_end|>\n")
        }
        for (message in this@toChatMlPrompt) {
            val role =
                when (message.role) {
                    InferenceMessage.Role.System -> "system"
                    InferenceMessage.Role.User -> "user"
                    InferenceMessage.Role.Assistant -> "assistant"
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

internal fun resolveJvmRuntimeDirectory(
    userHome: String = System.getProperty("user.home"),
    platformKind: PlatformKind = platformInfo.kind,
    appData: String? = System.getenv("APPDATA"),
    xdgDataHome: String? = System.getenv("XDG_DATA_HOME"),
): File {
    val baseDirectory =
        when (platformKind) {
            PlatformKind.MacOS -> File(userHome, "Library/Application Support/Breeze")
            PlatformKind.Windows -> {
                if (appData.isNullOrBlank()) {
                    File(userHome, "AppData/Roaming/Breeze")
                } else {
                    File(appData, "Breeze")
                }
            }
            else -> {
                if (xdgDataHome.isNullOrBlank()) {
                    File(userHome, ".local/share/Breeze")
                } else {
                    File(xdgDataHome, "Breeze")
                }
            }
        }
    return File(baseDirectory, "models/runtime/native")
}

internal fun runtimePlatformSegment(
    platformKind: PlatformKind = platformInfo.kind,
    archName: String = System.getProperty("os.arch"),
): String {
    val arch = archName.lowercase()
    val osSegment =
        when (platformKind) {
            PlatformKind.MacOS -> "macos"
            PlatformKind.Windows -> "windows"
            PlatformKind.Linux -> "linux"
            PlatformKind.Android -> "android"
            PlatformKind.IOS -> "ios"
            PlatformKind.WebJs -> "web-js"
            PlatformKind.WebWasm -> "web-wasm"
            PlatformKind.Unknown -> "unknown"
        }
    val archSegment =
        when {
            arch == "aarch64" || arch == "arm64" -> "arm64"
            arch == "x86_64" || arch == "amd64" -> "x64"
            else -> arch.replace(Regex("[^a-z0-9]+"), "-")
        }
    return "$osSegment-$archSegment"
}

internal fun bundledRuntimeResourcePath(
    platformSegment: String,
    fileName: String,
): String = "/breeze-runtime/$platformSegment/$fileName"

private const val BREEZE_LLAMA_LIBRARY_NAME = "breeze_llama_jni"
private const val ENV_BREEZE_LLAMA_JNI_LIBRARY_PATH = "BREEZE_LLAMA_JNI_LIBRARY_PATH"
private const val DEFAULT_STREAM_COMPARE_BUFFER_SIZE = 8 * 1024

private fun detectJvmDefaultBackend(): OnDeviceRuntimeBackend =
    when (platformInfo.kind) {
        PlatformKind.MacOS -> OnDeviceRuntimeBackend.Metal
        PlatformKind.Windows,
        PlatformKind.Linux,
        -> OnDeviceRuntimeBackend.Vulkan
        else -> OnDeviceRuntimeBackend.Cpu
    }

private fun supportedJvmBackends(): Set<OnDeviceRuntimeBackend> =
    when (platformInfo.kind) {
        PlatformKind.MacOS -> setOf(OnDeviceRuntimeBackend.Cpu, OnDeviceRuntimeBackend.Metal)
        PlatformKind.Windows,
        PlatformKind.Linux,
        -> setOf(OnDeviceRuntimeBackend.Cpu, OnDeviceRuntimeBackend.Vulkan)
        else -> setOf(OnDeviceRuntimeBackend.Cpu)
    }
