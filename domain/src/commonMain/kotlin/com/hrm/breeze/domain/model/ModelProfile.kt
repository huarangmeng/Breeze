package com.hrm.breeze.domain.model

data class ModelProfile(
    val id: String,
    val providerId: LlmProviderId,
    val displayName: String,
    val endpoint: String? = null,
    val apiToken: String? = null,
    val reasoningEnabled: Boolean = false,
    val supportsStreaming: Boolean = false,
    val contextWindow: Int = 2048,
    val supportsReasoningToggle: Boolean = false,
    val runtimeBackend: String? = null,
)
