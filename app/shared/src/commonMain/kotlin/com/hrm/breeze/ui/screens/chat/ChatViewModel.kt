package com.hrm.breeze.ui.screens.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.llm.ondevice.OnDeviceModelRepository
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.model.ModelConfig
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
    val reasoningEnabled: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: StringResource? = null,
    val currentOnDeviceModel: OnDeviceModelState? = null,
)

private data class ChatStateScaffold(
    val conversations: List<Conversation>,
    val activeConversationId: String,
    val sessionState: ConversationSessionState,
)

private data class ChatStateDetail(
    val messages: List<Message>,
    val currentOnDeviceModel: OnDeviceModelState?,
    val modelConfigs: List<ModelConfig>,
    val activeModelConfig: ModelConfig?,
)

private data class ConversationSessionState(
    val draft: String = "",
    val reasoningEnabled: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: StringResource? = null,
)

private data class ModelConfigState(
    val modelConfigs: List<ModelConfig>,
    val activeModelConfig: ModelConfig?,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val onDeviceModelRepository: OnDeviceModelRepository,
) : ViewModel() {
    private val activeConversationId = MutableStateFlow(createConversationId())
    private val sessionStates = MutableStateFlow<Map<String, ConversationSessionState>>(emptyMap())

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
            sessionStates,
        ) { conversations, activeConversationId, sessionStates ->
            ChatStateScaffold(
                conversations = conversations,
                activeConversationId = activeConversationId,
                sessionState = sessionStates[activeConversationId] ?: ConversationSessionState(),
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
            currentOnDeviceModel,
            modelConfigState,
        ) { messages, currentOnDeviceModel, modelConfigState ->
            ChatStateDetail(
                messages = messages,
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
                draft = scaffold.sessionState.draft,
                reasoningEnabled = scaffold.sessionState.reasoningEnabled,
                isSending = scaffold.sessionState.isSending,
                errorMessage = scaffold.sessionState.errorMessage,
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
                if (items.isNotEmpty()) {
                    sessionStates.value = sessionStates.value.toMutableMap().apply {
                        items.forEach { conversation ->
                            if (conversation.id !in this) {
                                this[conversation.id] = ConversationSessionState()
                            }
                        }
                    }
                }
                if (items.isNotEmpty() && items.none { it.id == activeConversationId.value }) {
                    activeConversationId.value = items.first().id
                }
            }
        }
    }

    fun onDraftChange(value: String) {
        updateSession(activeConversationId.value) {
            copy(
                draft = value,
                errorMessage = null,
            )
        }
    }

    fun onConversationSelected(conversationId: String) {
        activeConversationId.value = conversationId
    }

    fun onNewConversation() {
        val conversationId = createConversationId()
        ensureSession(conversationId)
        activeConversationId.value = conversationId
    }

    fun onModelSelected(modelId: String) {
        viewModelScope.launch {
            runCatching {
                modelConfigRepository.setActiveConfig(modelId)
            }.onFailure {
                updateSession(activeConversationId.value) {
                    copy(errorMessage = Res.string.status_model_switch_failed)
                }
            }
        }
    }

    fun onReasoningEnabledChange(enabled: Boolean) {
        updateSession(activeConversationId.value) {
            copy(
                reasoningEnabled = enabled,
                errorMessage = null,
            )
        }
    }

    fun onSendMessage() {
        val conversationId = activeConversationId.value
        val sessionState = sessionStates.value[conversationId] ?: ConversationSessionState()
        val text = sessionState.draft.trim()
        if (text.isBlank() || sessionState.isSending) {
            return
        }
        val activeModelConfig = state.value.activeModelConfig
        if (activeModelConfig?.modelId.isNullOrBlank()) {
            updateSession(conversationId) {
                copy(errorMessage = Res.string.status_model_required_before_send)
            }
            return
        }
        val resolvedModelConfig = checkNotNull(activeModelConfig)
        if (resolvedModelConfig.providerId == LlmProviderId.Local && state.value.currentOnDeviceModel?.isReadyForChat != true) {
            updateSession(conversationId) {
                copy(errorMessage = Res.string.status_local_model_not_ready)
            }
            return
        }

        val reasoningEnabled = sessionState.reasoningEnabled
        updateSession(conversationId) {
            copy(
                draft = "",
                errorMessage = null,
                isSending = true,
            )
        }

        viewModelScope.launch {
            runCatching {
                chatRepository.sendMessage(conversationId, text, reasoningEnabled).collect {}
            }.onFailure {
                updateSession(conversationId) {
                    copy(
                        draft = text,
                        errorMessage = Res.string.status_send_failed,
                    )
                }
            }
            updateSession(conversationId) {
                copy(isSending = false)
            }
        }
    }

    private fun ensureSession(conversationId: String) {
        sessionStates.value = sessionStates.value.toMutableMap().apply {
            if (conversationId !in this) {
                this[conversationId] = ConversationSessionState()
            }
        }
    }

    private fun updateSession(
        conversationId: String,
        transform: ConversationSessionState.() -> ConversationSessionState,
    ) {
        sessionStates.value = sessionStates.value.toMutableMap().apply {
            val current = this[conversationId] ?: ConversationSessionState()
            this[conversationId] = current.transform()
        }
    }
}

internal fun createConversationId(): String = "conversation-${Clock.System.now().toEpochMilliseconds()}"
