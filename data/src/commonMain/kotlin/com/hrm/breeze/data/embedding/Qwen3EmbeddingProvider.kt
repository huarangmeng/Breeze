package com.hrm.breeze.data.embedding

import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.data.llm.ondevice.QWEN3_EMBEDDING_PRESET_ID
import com.hrm.breeze.runtime.api.EmbeddingRuntimeRequest
import com.hrm.breeze.runtime.api.OnDeviceEmbeddingRuntime

class Qwen3EmbeddingProvider(
    private val onDeviceModelRepository: OnDeviceModelRepository,
    private val embeddingRuntime: OnDeviceEmbeddingRuntime,
) : EmbeddingProvider {
    override val modelId: String = QWEN3_EMBEDDING_PRESET_ID
    override val dimension: Int = QWEN3_EMBEDDING_DIMENSION

    override suspend fun isReady(): Boolean =
        runCatching {
            val model = onDeviceModelRepository.ensureEmbeddingModelReady()
            embeddingRuntime.embed(
                EmbeddingRuntimeRequest(
                    modelId = model.preset.id,
                    localPath = checkNotNull(model.localPath),
                    inputs = listOf("breeze"),
                    normalize = true,
                )
            ).single().values.size == dimension
        }.getOrDefault(false)

    override suspend fun embed(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) return emptyList()
        val model = onDeviceModelRepository.ensureEmbeddingModelReady()
        return embeddingRuntime.embed(
            EmbeddingRuntimeRequest(
                modelId = model.preset.id,
                localPath = checkNotNull(model.localPath),
                inputs = texts,
                normalize = true,
            )
        ).sortedBy { vector -> vector.textIndex }
            .map { vector -> vector.values }
    }
}

const val QWEN3_EMBEDDING_DIMENSION: Int = 1024
