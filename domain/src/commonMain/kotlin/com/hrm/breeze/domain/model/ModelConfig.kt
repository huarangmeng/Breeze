package com.hrm.breeze.domain.model

import kotlin.time.Instant

data class ModelConfig(
    val id: String,
    val providerId: LlmProviderId,
    val endpoint: String,
    val apiToken: String?,
    val modelId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
