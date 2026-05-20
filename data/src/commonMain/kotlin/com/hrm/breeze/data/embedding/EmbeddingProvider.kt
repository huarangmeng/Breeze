package com.hrm.breeze.data.embedding

interface EmbeddingProvider {
    val modelId: String
    val dimension: Int

    suspend fun isReady(): Boolean

    suspend fun embed(texts: List<String>): List<FloatArray>
}

data class EmbeddingVector(
    val textIndex: Int,
    val values: FloatArray,
)
