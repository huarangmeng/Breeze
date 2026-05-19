package com.hrm.breeze.ui.screens.modelsettings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.domain.repository.ModelConfigRepository
import com.hrm.breeze.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

@Immutable
data class ModelSettingsUiState(
    val providerDisplayName: String = "",
    val selectedModelId: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val contextWindow: Int = 2048,
    val streamOutput: Boolean = true,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val statusMessage: StringResource? = null,
)

class ModelSettingsViewModel(
    private val modelConfigRepository: ModelConfigRepository,
    private val settings: BreezeSettings,
) : ViewModel() {
    private val draftModelId = MutableStateFlow<String?>(null)
    private val draftTemperature = MutableStateFlow<Float?>(null)
    private val draftTopP = MutableStateFlow<Float?>(null)
    private val draftMaxTokens = MutableStateFlow<Int?>(null)
    private val draftContextWindow = MutableStateFlow<Int?>(null)
    private val draftStreamOutput = MutableStateFlow<Boolean?>(null)
    private val isSaving = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<StringResource?>(null)

    private val activeModelConfig =
        modelConfigRepository.observeActiveModelConfig().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    private val settingsSnapshot =
        settings.snapshot.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = com.hrm.breeze.data.settings.BreezeSettingsSnapshot(),
        )

    private data class ModelSettingsDraftState(
        val temperature: Float,
        val topP: Float,
        val maxTokens: Int,
        val contextWindow: Int,
        val streamOutput: Boolean,
    )

    private data class NumericDraftOverrides(
        val temperature: Float?,
        val topP: Float?,
        val maxTokens: Int?,
        val contextWindow: Int?,
    )

    private val numericDraftOverrides =
        combine(
            draftTemperature,
            draftTopP,
            draftMaxTokens,
            draftContextWindow,
        ) { draftTemperature, draftTopP, draftMaxTokens, draftContextWindow ->
            NumericDraftOverrides(
                temperature = draftTemperature,
                topP = draftTopP,
                maxTokens = draftMaxTokens,
                contextWindow = draftContextWindow,
            )
        }

    private val parameterDraftState =
        combine(
            numericDraftOverrides,
            draftStreamOutput,
            settingsSnapshot,
        ) { numericDraftOverrides, draftStreamOutput, settingsSnapshot ->
            ModelSettingsDraftState(
                temperature = numericDraftOverrides.temperature ?: settingsSnapshot.temperature,
                topP = numericDraftOverrides.topP ?: settingsSnapshot.topP,
                maxTokens = numericDraftOverrides.maxTokens ?: settingsSnapshot.maxTokens,
                contextWindow = numericDraftOverrides.contextWindow ?: settingsSnapshot.contextWindow,
                streamOutput = draftStreamOutput ?: settingsSnapshot.streamOutput,
            )
        }

    private val formState =
        combine(
            activeModelConfig,
            parameterDraftState,
            settingsSnapshot,
            draftModelId,
            isSaving,
        ) { activeModelConfig, draftState, settingsSnapshot, draftModelId, isSaving ->
            val selectedModelId = draftModelId ?: activeModelConfig?.modelId.orEmpty()
            ModelSettingsUiState(
                providerDisplayName = activeModelConfig?.providerId?.displayName.orEmpty(),
                selectedModelId = selectedModelId,
                temperature = draftState.temperature,
                topP = draftState.topP,
                maxTokens = draftState.maxTokens,
                contextWindow = draftState.contextWindow,
                streamOutput = draftState.streamOutput,
                isSaving = isSaving,
                hasUnsavedChanges =
                    selectedModelId != activeModelConfig?.modelId.orEmpty() ||
                        draftState.temperature != settingsSnapshot.temperature ||
                        draftState.topP != settingsSnapshot.topP ||
                        draftState.maxTokens != settingsSnapshot.maxTokens ||
                        draftState.contextWindow != settingsSnapshot.contextWindow ||
                        draftState.streamOutput != settingsSnapshot.streamOutput,
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

    fun onModelIdChange(modelId: String) {
        draftModelId.value = modelId
        statusMessage.value = null
    }

    fun onTemperatureChange(value: Float) {
        draftTemperature.value = value
        statusMessage.value = null
    }

    fun onTopPChange(value: Float) {
        draftTopP.value = value
        statusMessage.value = null
    }

    fun onMaxTokensChange(value: Int) {
        draftMaxTokens.value = value
        statusMessage.value = null
    }

    fun onContextWindowChange(value: Int) {
        draftContextWindow.value = value
        statusMessage.value = null
    }

    fun onStreamOutputChange(value: Boolean) {
        draftStreamOutput.value = value
        statusMessage.value = null
    }

    fun onReset() {
        draftModelId.value = null
        draftTemperature.value = null
        draftTopP.value = null
        draftMaxTokens.value = null
        draftContextWindow.value = null
        draftStreamOutput.value = null
        statusMessage.value = Res.string.status_model_reset
    }

    fun onSave() {
        val currentState = state.value
        if (isSaving.value || !currentState.hasUnsavedChanges) {
            return
        }
        val targetModelId = currentState.selectedModelId.trim()
        if (targetModelId.isBlank()) {
            statusMessage.value = Res.string.status_model_id_required
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null

            runCatching {
                modelConfigRepository.updateActiveConfigModelId(targetModelId)
                settings.updateTemperature(currentState.temperature)
                settings.updateTopP(currentState.topP)
                settings.updateMaxTokens(currentState.maxTokens)
                settings.updateContextWindow(currentState.contextWindow)
                settings.updateStreamOutput(currentState.streamOutput)
            }.onSuccess {
                draftModelId.value = null
                draftTemperature.value = null
                draftTopP.value = null
                draftMaxTokens.value = null
                draftContextWindow.value = null
                draftStreamOutput.value = null
                statusMessage.value = Res.string.status_model_saved
            }.onFailure {
                statusMessage.value = Res.string.status_save_failed
            }

            isSaving.value = false
        }
    }
}
