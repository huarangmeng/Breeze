package com.hrm.breeze.ui.screens.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.repository.ChatRepository
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
    val activeConversationId: String = createConversationId(),
    val draft: String = "",
    val isSending: Boolean = false,
    val errorMessage: StringResource? = null,
    val settings: BreezeSettingsSnapshot = BreezeSettingsSnapshot(),
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
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settings: BreezeSettings,
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

    private val stateDetail =
        combine(
            messages,
            errorMessage,
            settingsSnapshot,
        ) { messages, errorMessage, settings ->
            ChatStateDetail(
                messages = messages,
                errorMessage = errorMessage,
                settings = settings,
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
                activeConversationId = scaffold.activeConversationId,
                draft = scaffold.draft,
                isSending = scaffold.isSending,
                errorMessage = detail.errorMessage,
                settings = detail.settings,
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
                settings.updateCurrentModelId(modelId)
            }.onFailure {
                errorMessage.value = Res.string.status_model_switch_failed
            }
        }
    }

    fun onSendMessage() {
        val text = draft.value.trim()
        if (text.isBlank() || isSending.value) {
            return
        }
        if (state.value.settings.currentModelId.isBlank()) {
            errorMessage.value = Res.string.status_model_required_before_send
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
