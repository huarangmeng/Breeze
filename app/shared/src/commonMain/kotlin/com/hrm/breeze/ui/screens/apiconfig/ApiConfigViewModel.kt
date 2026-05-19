package com.hrm.breeze.ui.screens.apiconfig

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.llm.LlmMessage
import com.hrm.breeze.data.network.BREEZE_MOCK_ECHO_ENDPOINT
import com.hrm.breeze.data.network.OpenAiCompatibleApiException
import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.generated.resources.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) : ViewModel() {
    private val draftEndpoint = MutableStateFlow<String?>(null)
    private val draftApiToken = MutableStateFlow<String?>(null)
    private val draftModelId = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)
    private val isTesting = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<StringResource?>(null)
    private val statusDetail = MutableStateFlow<String?>(null)
    private val _closePageEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private data class ApiConfigDraftState(
        val endpoint: String?,
        val apiToken: String?,
        val modelId: String?,
        val isSaving: Boolean,
        val isTesting: Boolean,
    )

    private val settingsSnapshot =
        settings.snapshot.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BreezeSettingsSnapshot(),
        )

    private val draftState =
        combine(
            draftEndpoint,
            draftApiToken,
            draftModelId,
            isSaving,
            isTesting,
        ) { endpointDraft, apiTokenDraft, modelIdDraft, isSaving, isTesting ->
            ApiConfigDraftState(
                endpoint = endpointDraft,
                apiToken = apiTokenDraft,
                modelId = modelIdDraft,
                isSaving = isSaving,
                isTesting = isTesting,
            )
        }

    private val formState =
        combine(
            settingsSnapshot,
            draftState,
        ) { snapshot, draft ->
            val endpoint = draft.endpoint ?: snapshot.remoteEndpoint()
            val apiToken = draft.apiToken ?: snapshot.remoteApiToken()
            val modelId = draft.modelId ?: snapshot.remoteModelId()
            val isFormComplete =
                endpoint.trim().isNotBlank() &&
                    apiToken.trim().isNotBlank() &&
                    modelId.trim().isNotBlank()
            val hasUnsavedChanges =
                snapshot.currentProviderId != LlmProviderId.OpenAI ||
                    endpoint != snapshot.remoteEndpoint() ||
                    apiToken != snapshot.remoteApiToken() ||
                    modelId != snapshot.remoteModelId()

            ApiConfigUiState(
                endpoint = endpoint,
                apiToken = apiToken,
                modelId = modelId,
                isSaving = draft.isSaving,
                isTesting = draft.isTesting,
                isFormComplete = isFormComplete,
                hasUnsavedChanges = hasUnsavedChanges,
            )
        }

    val state: StateFlow<ApiConfigUiState> =
        combine(
            formState,
            statusMessage,
            statusDetail,
        ) { formState, statusMessage, statusDetail ->
            formState.copy(statusMessage = statusMessage, statusDetail = statusDetail)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue =
                ApiConfigUiState(
                    endpoint = BreezeSettingsSnapshot().remoteEndpoint(),
                    apiToken = BreezeSettingsSnapshot().remoteApiToken(),
                    modelId = BreezeSettingsSnapshot().remoteModelId(),
                ),
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
        draftEndpoint.value = null
        draftApiToken.value = null
        draftModelId.value = null
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
                settings.updateCurrentProviderId(LlmProviderId.OpenAI)
                settings.updateEchoEndpoint(currentState.endpoint.trim())
                settings.updateApiToken(currentState.apiToken.trim().ifBlank { null })
                settings.updateCurrentModelId(currentState.modelId.trim())
            }.onSuccess {
                draftEndpoint.value = null
                draftApiToken.value = null
                draftModelId.value = null
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
                chatApi.completeChat(
                    endpoint = currentState.endpoint.trim(),
                    apiToken = currentState.apiToken.trim().ifBlank { null },
                    modelId = currentState.modelId.trim(),
                    messages = listOf(LlmMessage(role = LlmMessage.Role.User, content = "ping")),
                )
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

private fun BreezeSettingsSnapshot.remoteEndpoint(): String =
    if (currentProviderId == LlmProviderId.OpenAI && echoEndpoint != BREEZE_MOCK_ECHO_ENDPOINT) {
        echoEndpoint
    } else {
        ""
    }

private fun BreezeSettingsSnapshot.remoteApiToken(): String =
    if (currentProviderId == LlmProviderId.OpenAI) {
        apiToken.orEmpty()
    } else {
        ""
    }

private fun BreezeSettingsSnapshot.remoteModelId(): String =
    if (currentProviderId == LlmProviderId.OpenAI && currentModelId != "breeze-echo") {
        currentModelId
    } else {
        ""
    }
