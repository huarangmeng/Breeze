package com.hrm.breeze.data.llm

import com.hrm.breeze.domain.model.LlmProviderId

class LlmProviderRegistry(
    providers: List<LlmProvider>,
) {
    private val providersById: Map<LlmProviderId, LlmProvider> = providers.associateBy(LlmProvider::id)

    fun require(id: LlmProviderId): LlmProvider =
        checkNotNull(providersById[id]) {
            "LLM provider not registered: $id"
        }
}
