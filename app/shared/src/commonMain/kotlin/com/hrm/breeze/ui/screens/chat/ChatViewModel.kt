package com.hrm.breeze.ui.screens.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.ModelConfig
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.model.OnDeviceModelState
import com.hrm.breeze.domain.repository.ChatRepository
import com.hrm.breeze.domain.repository.ModelConfigRepository
import com.hrm.breeze.generated.resources.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock

@Immutable
data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val messages: List<Message> = emptyList(),
    val modelConfigs: List<ModelConfig> = emptyList(),
    val activeModelConfig: ModelConfig? = null,
    val activeConversationId: String = createConversationId(),
    val draft: String = "",
    val isSending: Boolean = false,
    val errorMessage: StringResource? = null,
    val settings: BreezeSettingsSnapshot = BreezeSettingsSnapshot(),
    val currentOnDeviceModel: OnDeviceModelState? = null,
)

private data class ChatStateScaffold(
    val conversations: List<Conversation>,
    val activeConversationId: String,
    val draft: String,
    val isSending: Boolean,
)

private data class ChatStateDetail(
    val messages: List<Message>,
    val errorMessage: StringResource?,
    val settings: BreezeSettingsSnapshot,
    val currentOnDeviceModel: OnDeviceModelState?,
    val modelConfigs: List<ModelConfig>,
    val activeModelConfig: ModelConfig?,
)

private data class ModelConfigState(
    val modelConfigs: List<ModelConfig>,
    val activeModelConfig: ModelConfig?,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val settings: BreezeSettings,
    private val onDeviceModelRepository: OnDeviceModelRepository,
) : ViewModel() {
    private val draft = MutableStateFlow("")
    private val activeConversationId = MutableStateFlow(createConversationId())
    private val isSending = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<StringResource?>(null)

    private val conversations =
        chatRepository.observeConversations().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val messages =
        activeConversationId.flatMapLatest(chatRepository::observeMessages).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    private val settingsSnapshot =
        settings.snapshot.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = BreezeSettingsSnapshot(),
        )

    private val currentOnDeviceModel =
        onDeviceModelRepository.observeCurrentModel().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    private val modelConfigs =
        modelConfigRepository.observeModelConfigs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    private val activeModelConfig =
        modelConfigRepository.observeActiveModelConfig().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    private val stateScaffold =
        combine(
            conversations,
            activeConversationId,
            draft,
            isSending,
        ) { conversations, activeConversationId, draft, isSending ->
            ChatStateScaffold(
                conversations = conversations,
                activeConversationId = activeConversationId,
                draft = draft,
                isSending = isSending,
            )
        }

    private val modelConfigState =
        combine(
            modelConfigs,
            activeModelConfig,
        ) { modelConfigs, activeModelConfig ->
            ModelConfigState(
                modelConfigs = modelConfigs,
                activeModelConfig = activeModelConfig,
            )
        }

    private val stateDetail =
        combine(
            messages,
            errorMessage,
            settingsSnapshot,
            currentOnDeviceModel,
            modelConfigState,
        ) { messages, errorMessage, settings, currentOnDeviceModel, modelConfigState ->
            ChatStateDetail(
                messages = messages,
                errorMessage = errorMessage,
                settings = settings,
                currentOnDeviceModel = currentOnDeviceModel,
                modelConfigs = modelConfigState.modelConfigs,
                activeModelConfig = modelConfigState.activeModelConfig,
            )
        }

    val state: StateFlow<ChatUiState> =
        combine(
            stateScaffold,
            stateDetail,
        ) { scaffold, detail ->
            ChatUiState(
                conversations = scaffold.conversations,
                messages = detail.messages,
                modelConfigs = detail.modelConfigs,
                activeModelConfig = detail.activeModelConfig,
                activeConversationId = scaffold.activeConversationId,
                draft = scaffold.draft,
                isSending = scaffold.isSending,
                errorMessage = detail.errorMessage,
                settings = detail.settings,
                currentOnDeviceModel = detail.currentOnDeviceModel,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ChatUiState(activeConversationId = activeConversationId.value),
        )

    init {
        viewModelScope.launch {
            conversations.collect { items ->
                if (items.isNotEmpty() && items.none { it.id == activeConversationId.value }) {
                    activeConversationId.value = items.first().id
                }
            }
        }
    }

    fun onDraftChange(value: String) {
        draft.value = value
        if (errorMessage.value != null) {
            errorMessage.value = null
        }
    }

    fun onConversationSelected(conversationId: String) {
        activeConversationId.value = conversationId
        errorMessage.value = null
    }

    fun onNewConversation() {
        activeConversationId.value = createConversationId()
        draft.value = ""
        errorMessage.value = null
    }

    fun onModelSelected(modelId: String) {
        viewModelScope.launch {
            runCatching {
                modelConfigRepository.setActiveConfig(modelId)
            }.onFailure {
                errorMessage.value = Res.string.status_model_switch_failed
            }
        }
    }

    fun onReasoningEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                settings.updateReasoningEnabled(enabled)
            }.onFailure {
                errorMessage.value = Res.string.status_save_failed
            }
        }
    }

    fun onSendMessage() {
        val text = draft.value.trim()
        if (text.isBlank() || isSending.value) {
            return
        }
        val activeModelConfig = state.value.activeModelConfig
        if (activeModelConfig?.modelId.isNullOrBlank()) {
            errorMessage.value = Res.string.status_model_required_before_send
            return
        }
        val resolvedModelConfig = checkNotNull(activeModelConfig)
        if (resolvedModelConfig.providerId == LlmProviderId.Local && state.value.currentOnDeviceModel?.isReadyForChat != true) {
            errorMessage.value = Res.string.status_local_model_not_ready
            return
        }

        val conversationId = activeConversationId.value
        draft.value = ""
        errorMessage.value = null
        isSending.value = true

        viewModelScope.launch {
            runCatching {
                chatRepository.sendMessage(conversationId, text).collect {}
            }.onFailure {
                draft.value = text
                errorMessage.value = Res.string.status_send_failed
            }
            isSending.value = false
        }
    }
}

internal fun createConversationId(): String = "conversation-${Clock.System.now().toEpochMilliseconds()}"
