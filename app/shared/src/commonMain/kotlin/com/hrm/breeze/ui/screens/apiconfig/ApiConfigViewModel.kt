package com.hrm.breeze.ui.screens.apiconfig

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.network.OpenAiCompatibleApiException
import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.repository.ModelConfigRepository
import com.hrm.breeze.generated.resources.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

@Immutable
data class ApiConfigUiState(
    val endpoint: String = "",
    val apiToken: String = "",
    val modelId: String = "",
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val isFormComplete: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val statusMessage: StringResource? = null,
    val statusDetail: String? = null,
)

class ApiConfigViewModel(
    private val settings: BreezeSettings,
    private val chatApi: OpenAiCompatibleChatApi,
    private val modelConfigRepository: ModelConfigRepository,
) : ViewModel() {
    private val draftEndpoint = MutableStateFlow("")
    private val draftApiToken = MutableStateFlow("")
    private val draftModelId = MutableStateFlow("")
    private val isSaving = MutableStateFlow(false)
    private val isTesting = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<StringResource?>(null)
    private val statusDetail = MutableStateFlow<String?>(null)
    private val _closePageEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private data class DraftFormState(
        val endpoint: String,
        val apiToken: String,
        val modelId: String,
        val isSaving: Boolean,
        val isTesting: Boolean,
    )

    private val formState =
        combine(
            draftEndpoint,
            draftApiToken,
            draftModelId,
            isSaving,
            isTesting,
        ) { endpoint, apiToken, modelId, isSaving, isTesting ->
            DraftFormState(
                endpoint = endpoint,
                apiToken = apiToken,
                modelId = modelId,
                isSaving = isSaving,
                isTesting = isTesting,
            )
        }

    val state: StateFlow<ApiConfigUiState> =
        combine(
            formState,
            statusMessage,
            statusDetail,
        ) { formState, statusMessage, statusDetail ->
            val isFormComplete =
                formState.endpoint.trim().isNotBlank() &&
                    formState.apiToken.trim().isNotBlank() &&
                    formState.modelId.trim().isNotBlank()

            ApiConfigUiState(
                endpoint = formState.endpoint,
                apiToken = formState.apiToken,
                modelId = formState.modelId,
                isSaving = formState.isSaving,
                isTesting = formState.isTesting,
                isFormComplete = isFormComplete,
                hasUnsavedChanges =
                    formState.endpoint.isNotBlank() ||
                        formState.apiToken.isNotBlank() ||
                        formState.modelId.isNotBlank(),
                statusMessage = statusMessage,
                statusDetail = statusDetail,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ApiConfigUiState(),
        )

    val closePageEvent: SharedFlow<Unit> = _closePageEvent

    fun onEndpointChange(value: String) {
        draftEndpoint.value = value
        statusMessage.value = null
        statusDetail.value = null
    }

    fun onApiTokenChange(value: String) {
        draftApiToken.value = value
        statusMessage.value = null
        statusDetail.value = null
    }

    fun onModelIdChange(value: String) {
        draftModelId.value = value
        statusMessage.value = null
        statusDetail.value = null
    }

    fun onReset() {
        draftEndpoint.value = ""
        draftApiToken.value = ""
        draftModelId.value = ""
        statusMessage.value = Res.string.status_api_reset
        statusDetail.value = null
    }

    fun onSave() {
        val currentState = state.value
        if (isSaving.value || isTesting.value || !currentState.hasUnsavedChanges || !currentState.isFormComplete) {
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            statusDetail.value = null

            runCatching {
                modelConfigRepository.createAndActivateConfig(
                    providerId = LlmProviderId.OpenAI,
                    endpoint = currentState.endpoint.trim(),
                    apiToken = currentState.apiToken.trim().ifBlank { null },
                    modelId = currentState.modelId.trim(),
                )
            }.onSuccess {
                draftEndpoint.value = ""
                draftApiToken.value = ""
                draftModelId.value = ""
                statusMessage.value = Res.string.status_api_saved
                statusDetail.value = null
                _closePageEvent.tryEmit(Unit)
            }.onFailure {
                statusMessage.value = Res.string.status_save_failed
                statusDetail.value = null
            }

            isSaving.value = false
        }
    }

    fun onTestConnection() {
        val currentState = state.value
        if (isSaving.value || isTesting.value || !currentState.isFormComplete) {
            return
        }

        viewModelScope.launch {
            isTesting.value = true
            statusMessage.value = Res.string.status_testing_connection
            statusDetail.value = null

            runCatching {
                chatApi.streamChat(
                    endpoint = currentState.endpoint.trim(),
                    apiToken = currentState.apiToken.trim().ifBlank { null },
                    modelId = currentState.modelId.trim(),
                    messages = listOf(LlmMessage(role = LlmMessage.Role.User, content = "ping")),
                    reasoningEnabled = settings.getReasoningEnabled(),
                ).firstOrNull()?.let { delta ->
                    if (delta.isEmpty) {
                        error("OpenAI-compatible stream did not emit any content")
                    }
                } ?: error("OpenAI-compatible stream did not emit any content")
            }.onSuccess {
                statusMessage.value = Res.string.status_test_connection_success
                statusDetail.value = null
            }.onFailure {
                statusMessage.value = Res.string.status_test_connection_failed
                statusDetail.value = when (it) {
                    is OpenAiCompatibleApiException -> it.message
                    else -> it.message
                }
            }

            isTesting.value = false
        }
    }
}
