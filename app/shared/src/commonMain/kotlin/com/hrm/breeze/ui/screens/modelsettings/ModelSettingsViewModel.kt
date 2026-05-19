package com.hrm.breeze.ui.screens.modelsettings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

@Immutable
data class ModelOption(
    val id: String,
    val title: String,
    val descriptionRes: StringResource,
)

@Immutable
data class ModelSettingsUiState(
    val providerId: LlmProviderId = BreezeSettingsSnapshot().currentProviderId,
    val availableModels: List<ModelOption> = modelOptionsFor(BreezeSettingsSnapshot().currentProviderId),
    val selectedModelId: String = BreezeSettingsSnapshot().currentModelId,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val statusMessage: StringResource? = null,
)

class ModelSettingsViewModel(
    private val settings: BreezeSettings,
) : ViewModel() {
    private val draftModelId = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<StringResource?>(null)

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
        statusMessage.value = Res.string.status_model_reset
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
                statusMessage.value = Res.string.status_model_saved
            }.onFailure {
                statusMessage.value = Res.string.status_save_failed
            }

            isSaving.value = false
        }
    }
}

private fun modelOptionsFor(providerId: LlmProviderId): List<ModelOption> = when (providerId) {
    LlmProviderId.Local ->
        listOf(
            ModelOption("breeze-echo", "Breeze Echo", Res.string.model_desc_breeze_echo),
            ModelOption("qwen2.5:7b", "Qwen 2.5 7B", Res.string.model_desc_qwen25_7b),
            ModelOption("llama3.2:3b", "Llama 3.2 3B", Res.string.model_desc_llama32_3b),
        )

    LlmProviderId.OpenAI ->
        listOf(
            ModelOption("gpt-4.1-mini", "GPT-4.1 mini", Res.string.model_desc_gpt41_mini),
            ModelOption("gpt-4.1", "GPT-4.1", Res.string.model_desc_gpt41),
            ModelOption("o4-mini", "o4-mini", Res.string.model_desc_o4_mini),
        )

    LlmProviderId.Anthropic ->
        listOf(
            ModelOption("claude-3-5-haiku-latest", "Claude 3.5 Haiku", Res.string.model_desc_claude_35_haiku_latest),
            ModelOption("claude-3-7-sonnet-latest", "Claude 3.7 Sonnet", Res.string.model_desc_claude_37_sonnet_latest),
            ModelOption("claude-opus-4-1", "Claude Opus 4.1", Res.string.model_desc_claude_opus_41),
        )
}
