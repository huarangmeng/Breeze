package com.hrm.breeze.ui.screens.apiconfig

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
data class ApiConfigUiState(
    val availableProviders: List<LlmProviderId> = LlmProviderId.entries,
    val selectedProviderId: LlmProviderId = LlmProviderId.Local,
    val endpoint: String = "",
    val apiToken: String = "",
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val statusMessage: String? = null,
    val providerNotice: String = "",
)

class ApiConfigViewModel(
    private val settings: BreezeSettings,
) : ViewModel() {
    private val draftProviderId = MutableStateFlow<LlmProviderId?>(null)
    private val draftEndpoint = MutableStateFlow<String?>(null)
    private val draftApiToken = MutableStateFlow<String?>(null)
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
            draftProviderId,
            draftEndpoint,
            draftApiToken,
            isSaving,
        ) { snapshot, providerDraft, endpointDraft, apiTokenDraft, isSaving ->
            val selectedProviderId = providerDraft ?: snapshot.currentProviderId
            val endpoint = endpointDraft ?: snapshot.echoEndpoint
            val apiToken = apiTokenDraft ?: snapshot.apiToken.orEmpty()
            val hasUnsavedChanges =
                selectedProviderId != snapshot.currentProviderId ||
                    endpoint != snapshot.echoEndpoint ||
                    apiToken != snapshot.apiToken.orEmpty()

            ApiConfigUiState(
                selectedProviderId = selectedProviderId,
                endpoint = endpoint,
                apiToken = apiToken,
                isSaving = isSaving,
                hasUnsavedChanges = hasUnsavedChanges,
                providerNotice = providerNoticeFor(selectedProviderId),
            )
        }

    val state: StateFlow<ApiConfigUiState> =
        combine(
            formState,
            statusMessage,
        ) { formState, statusMessage ->
            formState.copy(statusMessage = statusMessage)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue =
                ApiConfigUiState(
                    selectedProviderId = BreezeSettingsSnapshot().currentProviderId,
                    endpoint = BreezeSettingsSnapshot().echoEndpoint,
                    apiToken = BreezeSettingsSnapshot().apiToken.orEmpty(),
                    providerNotice = providerNoticeFor(BreezeSettingsSnapshot().currentProviderId),
                ),
        )

    fun onProviderSelected(providerId: LlmProviderId) {
        draftProviderId.value = providerId
        statusMessage.value = null
    }

    fun onEndpointChange(value: String) {
        draftEndpoint.value = value
        statusMessage.value = null
    }

    fun onApiTokenChange(value: String) {
        draftApiToken.value = value
        statusMessage.value = null
    }

    fun onReset() {
        draftProviderId.value = null
        draftEndpoint.value = null
        draftApiToken.value = null
        statusMessage.value = "已恢复为最近一次保存的配置。"
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
                settings.updateCurrentProviderId(currentState.selectedProviderId)
                settings.updateEchoEndpoint(currentState.endpoint.trim())
                settings.updateApiToken(currentState.apiToken.trim().ifBlank { null })
            }.onSuccess {
                draftProviderId.value = null
                draftEndpoint.value = null
                draftApiToken.value = null
                statusMessage.value = "API 配置已保存。"
            }.onFailure { throwable ->
                statusMessage.value = throwable.message ?: "保存失败，请稍后重试。"
            }

            isSaving.value = false
        }
    }
}

private fun providerNoticeFor(providerId: LlmProviderId): String = when (providerId) {
    LlmProviderId.Local -> "Local 当前走本地/Mock Echo 兼容链路，适合继续验证共享 UI 和仓库闭环。"
    LlmProviderId.OpenAI -> "OpenAI 已可在 UI 中选择并持久化，当前阶段先走兼容桥接，真实 OpenAI adapter 在 M5-3 接入。"
    LlmProviderId.Anthropic -> "Anthropic 已可在 UI 中选择并持久化，当前阶段先走兼容桥接，真实 Anthropic adapter 在 M5-4 接入。"
}
