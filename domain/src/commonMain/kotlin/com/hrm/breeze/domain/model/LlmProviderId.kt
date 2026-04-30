package com.hrm.breeze.domain.model

enum class LlmProviderId(
    val storageValue: String,
    val displayName: String,
) {
    OpenAI(
        storageValue = "openai",
        displayName = "OpenAI",
    ),
    Anthropic(
        storageValue = "anthropic",
        displayName = "Anthropic",
    ),
    Local(
        storageValue = "local",
        displayName = "Local",
    ),
    ;

    companion object {
        fun fromStorageValue(value: String?): LlmProviderId =
            entries.firstOrNull { it.storageValue == value } ?: Local
    }
}
