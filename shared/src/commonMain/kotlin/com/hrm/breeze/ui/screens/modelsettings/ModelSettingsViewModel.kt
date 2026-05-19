package com.hrm.breeze.ui.screens.modelsettings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ModelOption(
    val id: String,
    val title: String,
    val description: String,
)

@Immutable
data class ModelSettingsUiState(
    val providerId: LlmProviderId = BreezeSettingsSnapshot().currentProviderId,
    val availableModels: List<ModelOption> = modelOptionsFor(BreezeSettingsSnapshot().currentProviderId),
    val selectedModelId: String = BreezeSettingsSnapshot().currentModelId,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val statusMessage: String? = null,
)

class ModelSettingsViewModel(
    private val settings: BreezeSettings,
) : ViewModel() {
    private val draftModelId = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    private val settingsSnapshot =
        settings.snapshot.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BreezeSettingsSnapshot(),
        )

    private val formState =
        combine(
            settingsSnapshot,
            draftModelId,
            isSaving,
        ) { snapshot, draftModelId, isSaving ->
            val selectedModelId = draftModelId ?: snapshot.currentModelId
            ModelSettingsUiState(
                providerId = snapshot.currentProviderId,
                availableModels = modelOptionsFor(snapshot.currentProviderId),
                selectedModelId = selectedModelId,
                isSaving = isSaving,
                hasUnsavedChanges = selectedModelId != snapshot.currentModelId,
            )
        }

    val state: StateFlow<ModelSettingsUiState> =
        combine(
            formState,
            statusMessage,
        ) { formState, statusMessage ->
            formState.copy(statusMessage = statusMessage)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ModelSettingsUiState(),
        )

    fun onModelSelected(modelId: String) {
        draftModelId.value = modelId
        statusMessage.value = null
    }

    fun onReset() {
        draftModelId.value = null
        statusMessage.value = "已恢复为最近一次保存的模型选择。"
    }

    fun onSave() {
        val currentState = state.value
        if (isSaving.value || !currentState.hasUnsavedChanges) {
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null

            runCatching {
                settings.updateCurrentModelId(currentState.selectedModelId)
            }.onSuccess {
                draftModelId.value = null
                statusMessage.value = "模型设置已保存。"
            }.onFailure { throwable ->
                statusMessage.value = throwable.message ?: "保存失败，请稍后重试。"
            }

            isSaving.value = false
        }
    }
}

private fun modelOptionsFor(providerId: LlmProviderId): List<ModelOption> = when (providerId) {
    LlmProviderId.Local ->
        listOf(
            ModelOption("breeze-echo", "Breeze Echo", "当前本地/Mock 闭环默认模型，用于验证端到端链路。"),
            ModelOption("qwen2.5:7b", "Qwen 2.5 7B", "预留给本地兼容 provider 的常见模型名。"),
            ModelOption("llama3.2:3b", "Llama 3.2 3B", "预留给本地兼容 provider 的轻量模型。"),
        )

    LlmProviderId.OpenAI ->
        listOf(
            ModelOption("gpt-4.1-mini", "GPT-4.1 mini", "适合低成本通用对话，后续会接真实 OpenAI provider。"),
            ModelOption("gpt-4.1", "GPT-4.1", "通用高质量模型，占位用于后续 provider 适配。"),
            ModelOption("o4-mini", "o4-mini", "推理向模型，占位用于后续 provider 适配。"),
        )

    LlmProviderId.Anthropic ->
        listOf(
            ModelOption("claude-3-5-haiku-latest", "Claude 3.5 Haiku", "轻量响应快，占位用于后续 provider 适配。"),
            ModelOption("claude-3-7-sonnet-latest", "Claude 3.7 Sonnet", "通用模型，占位用于后续 provider 适配。"),
            ModelOption("claude-opus-4-1", "Claude Opus 4.1", "高能力模型，占位用于后续 provider 适配。"),
        )
}
