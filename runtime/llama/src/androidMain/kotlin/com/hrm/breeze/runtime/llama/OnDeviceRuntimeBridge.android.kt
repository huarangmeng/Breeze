package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.runtime.api.OnDeviceRuntimeBackend
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCapability
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeFamily
import com.hrm.breeze.runtime.api.OnDeviceRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeTargetPlatform
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal actual fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge = AndroidInProcessLlamaRuntimeBridge()

private class AndroidInProcessLlamaRuntimeBridge : OnDeviceRuntimeBridge {
    private val nativeBridge = BreezeAndroidLlamaNativeBridge()
    private val modelMutex = Mutex()
    private var loadedModel: LoadedModel? = null

    override val capability =
        OnDeviceRuntimeCapability(
            isAvailable = true,
            family = OnDeviceRuntimeFamily.AndroidJni,
            targetPlatforms = setOf(OnDeviceRuntimeTargetPlatform.Android),
            defaultBackend = OnDeviceRuntimeBackend.Cpu,
            supportedBackends = setOf(OnDeviceRuntimeBackend.Cpu),
            supportsModelPersistence = true,
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
                maxTokens = normalized.maxTokens,
                temperature = normalized.temperature,
                topP = normalized.topP,
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
        awaitClose {
            generationHandle.cancel()
        }
    }

    private suspend fun loadModel(request: OnDeviceRuntimeLaunchRequest): LoadedModel {
        val normalized = request.normalized()
        val localPath = normalized.requireLocalPath()
        check(File(localPath).isFile) { "Downloaded on-device model file is missing: $localPath" }
        return modelMutex.withLock {
            loadedModel?.takeIf { it.path == localPath && it.contextWindow == normalized.contextWindow }?.let { return it }
            loadedModel?.let { existing ->
                nativeBridge.unload(existing.handle)
                loadedModel = null
            }
            val handle =
                nativeBridge.loadModel(
                    modelPath = localPath,
                    contextWindow = normalized.contextWindow,
                )
            LoadedModel(
                path = localPath,
                contextWindow = normalized.contextWindow,
                handle = handle,
            ).also { loadedModel = it }
        }
    }
}

private data class LoadedModel(
    val path: String,
    val contextWindow: Int,
    val handle: Long,
)

private class BreezeAndroidLlamaNativeBridge {
    private val runtimeLoaded = AtomicBoolean(false)

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
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        contextWindow: Int,
        callback: BreezeLlamaTokenCallback,
    ): GenerationHandle {
        ensureRuntimeLoaded()
        val handle =
            nativeGenerate(
                modelHandle = modelHandle,
                prompt = prompt,
                maxTokens = maxTokens,
                temperature = temperature,
                topP = topP,
                contextWindow = contextWindow,
                callback = callback,
            )
        return GenerationHandle(handle = handle)
    }

    private fun ensureRuntimeLoaded() {
        if (runtimeLoaded.compareAndSet(false, true)) {
            runCatching {
                System.loadLibrary(BREEZE_LLAMA_ANDROID_LIBRARY_NAME)
            }.onFailure {
                runtimeLoaded.set(false)
                throw it
            }
        }
    }

    inner class GenerationHandle(
        private val handle: Long,
    ) {
        fun cancel() {
            ensureRuntimeLoaded()
            nativeCancel(handle)
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
        maxTokens: Int,
        temperature: Float,
        topP: Float,
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

private const val BREEZE_LLAMA_ANDROID_LIBRARY_NAME = "breeze_llama_jni"
