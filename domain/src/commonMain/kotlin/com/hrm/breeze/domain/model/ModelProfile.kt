package com.hrm.breeze.domain.model

data class ModelProfile(
    val id: String,
    val providerId: LlmProviderId,
    val displayName: String,
    val supportsStreaming: Boolean = false,
)
