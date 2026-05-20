@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hrm.breeze.runtime.llama

import com.hrm.breeze.domain.model.InferenceRuntimeState
import com.hrm.breeze.runtime.api.OnDeviceRuntimeBackend
import com.hrm.breeze.runtime.api.OnDeviceRuntimeCompletionRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeFamily
import com.hrm.breeze.runtime.api.OnDeviceRuntimeLaunchRequest
import com.hrm.breeze.runtime.api.OnDeviceRuntimeTargetPlatform
import com.hrm.breeze.runtime.llama.apple.breeze_llama_cancel
import com.hrm.breeze.runtime.llama.apple.breeze_llama_generation_free
import com.hrm.breeze.runtime.llama.apple.breeze_llama_last_error
import com.hrm.breeze.runtime.llama.apple.breeze_llama_load_model
import com.hrm.breeze.runtime.llama.apple.breeze_llama_next_token
import com.hrm.breeze.runtime.llama.apple.breeze_llama_start_generation
import com.hrm.breeze.runtime.llama.apple.breeze_llama_string_free
import com.hrm.breeze.runtime.llama.apple.breeze_llama_unload
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal actual fun createOnDeviceRuntimeBridge(): OnDeviceRuntimeBridge = IosInProcessLlamaRuntimeBridge()

private class IosInProcessLlamaRuntimeBridge : OnDeviceRuntimeBridge {
    private val nativeBridge = BreezeIosLlamaNativeBridge()
    private val modelMutex = Mutex()
    private var loadedModel: LoadedModel? = null

    override val capability =
        com.hrm.breeze.runtime.api.OnDeviceRuntimeCapability(
            isAvailable = true,
            family = OnDeviceRuntimeFamily.AppleNative,
            targetPlatforms = setOf(OnDeviceRuntimeTargetPlatform.Ios),
            defaultBackend = OnDeviceRuntimeBackend.Metal,
            supportedBackends = setOf(OnDeviceRuntimeBackend.Cpu, OnDeviceRuntimeBackend.Metal),
            supportsModelPersistence = true,
        )

    override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
        if (runCatching { loadModel(request) }.isSuccess) {
            InferenceRuntimeState.Ready
        } else {
            InferenceRuntimeState.Failed
        }

    override fun streamCompletion(request: OnDeviceRuntimeCompletionRequest): Flow<String> =
        flow {
            val normalized = request.normalized()
            val loadedModel = loadModel(normalized.toLaunchRequest())
            val generation =
                nativeBridge.startGeneration(
                    modelHandle = loadedModel.handle,
                    prompt = normalized.messages.toChatMlPrompt(),
                    maxTokens = normalized.maxTokens,
                    temperature = normalized.temperature,
                    topP = normalized.topP,
                    contextWindow = normalized.contextWindow,
                )
            try {
                while (currentCoroutineContext().isActive) {
                    when (val step = nativeBridge.nextToken(generation)) {
                        is GenerationStep.Token -> if (step.value.isNotEmpty()) emit(step.value)
                        GenerationStep.Done -> break
                    }
                }
                if (!currentCoroutineContext().isActive) {
                    nativeBridge.cancel(generation)
                }
            } finally {
                nativeBridge.cancel(generation)
                nativeBridge.freeGeneration(generation)
            }
        }.flowOn(Dispatchers.Default)

    private suspend fun loadModel(request: OnDeviceRuntimeLaunchRequest): LoadedModel {
        val normalized = request.normalized()
        val localPath = normalized.requireLocalPath()
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

private sealed interface GenerationStep {
    data class Token(
        val value: String,
    ) : GenerationStep

    data object Done : GenerationStep
}

private class BreezeIosLlamaNativeBridge {
    fun loadModel(
        modelPath: String,
        contextWindow: Int,
    ): Long {
        val handle = breeze_llama_load_model(modelPath, contextWindow)
        check(handle != 0L) { lastNativeError("Failed to load GGUF model") }
        return handle
    }

    fun unload(handle: Long) {
        breeze_llama_unload(handle)
    }

    fun startGeneration(
        modelHandle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        contextWindow: Int,
    ): Long {
        val handle =
            breeze_llama_start_generation(
                modelHandle,
                prompt,
                temperature,
                topP,
                maxTokens,
                contextWindow,
            )
        check(handle != 0L) { lastNativeError("Failed to start llama generation") }
        return handle
    }

    fun nextToken(generationHandle: Long): GenerationStep =
        memScoped {
            val tokenPointer = alloc<CPointerVar<ByteVar>>()
            tokenPointer.value = null
            when (breeze_llama_next_token(generationHandle, tokenPointer.ptr)) {
                1 -> {
                    val value = tokenPointer.value?.toKString().orEmpty()
                    tokenPointer.value?.let { breeze_llama_string_free(it) }
                    GenerationStep.Token(value)
                }
                0 -> GenerationStep.Done
                else -> error(lastNativeError("Failed to generate next llama token"))
            }
        }

    fun cancel(handle: Long) {
        breeze_llama_cancel(handle)
    }

    fun freeGeneration(handle: Long) {
        breeze_llama_generation_free(handle)
    }

    private fun lastNativeError(defaultMessage: String): String =
        breeze_llama_last_error()?.toKString()?.takeIf(String::isNotBlank) ?: defaultMessage
}
