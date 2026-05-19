package com.hrm.breeze.ui.screens.ondevicemodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.domain.model.OnDeviceModelState
import com.hrm.breeze.generated.resources.Res
import com.hrm.breeze.generated.resources.status_download_failed
import com.hrm.breeze.generated.resources.status_local_model_deleted
import com.hrm.breeze.generated.resources.status_local_model_download_started
import com.hrm.breeze.generated.resources.status_local_model_selected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

@Immutable
data class OnDeviceModelsUiState(
    val models: List<OnDeviceModelState> = emptyList(),
    val activePresetId: String? = null,
    val statusMessage: StringResource? = null,
)

class OnDeviceModelsViewModel(
    private val repository: OnDeviceModelRepository,
) : ViewModel() {
    private val statusMessage = MutableStateFlow<StringResource?>(null)
    private val activePresetId = MutableStateFlow<String?>(null)

    val state: StateFlow<OnDeviceModelsUiState> =
        combine(
            repository.observeModels(),
            activePresetId,
            statusMessage,
        ) { models, activePresetId, statusMessage ->
            OnDeviceModelsUiState(
                models = models,
                activePresetId = activePresetId,
                statusMessage = statusMessage,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = OnDeviceModelsUiState(),
        )

    fun onDownload(presetId: String) {
        viewModelScope.launch {
            activePresetId.value = presetId
            statusMessage.value = null
            runCatching {
                repository.downloadModel(presetId)
            }.onSuccess {
                statusMessage.value = Res.string.status_local_model_download_started
            }.onFailure {
                statusMessage.value = Res.string.status_download_failed
            }
            activePresetId.value = null
        }
    }

    fun onSelect(presetId: String) {
        viewModelScope.launch {
            activePresetId.value = presetId
            runCatching {
                repository.selectModel(presetId)
            }.onSuccess {
                statusMessage.value = Res.string.status_local_model_selected
            }.onFailure {
                statusMessage.value = Res.string.status_download_failed
            }
            activePresetId.value = null
        }
    }

    fun onDelete(presetId: String) {
        viewModelScope.launch {
            activePresetId.value = presetId
            runCatching {
                repository.deleteModel(presetId)
            }.onSuccess {
                statusMessage.value = Res.string.status_local_model_deleted
            }.onFailure {
                statusMessage.value = Res.string.status_download_failed
            }
            activePresetId.value = null
        }
    }
}
