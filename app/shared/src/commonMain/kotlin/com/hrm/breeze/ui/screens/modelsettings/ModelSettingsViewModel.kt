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
data class ModelSettingsUiState(
    val providerId: LlmProviderId = BreezeSettingsSnapshot().currentProviderId,
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

    fun onModelIdChange(modelId: String) {
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
        val targetModelId = currentState.selectedModelId.trim()
        if (targetModelId.isBlank()) {
            statusMessage.value = Res.string.status_model_id_required
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null

            runCatching {
                settings.updateCurrentModelId(targetModelId)
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
