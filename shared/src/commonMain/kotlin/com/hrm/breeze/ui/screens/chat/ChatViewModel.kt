package com.hrm.breeze.ui.screens.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Immutable
data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val messages: List<Message> = emptyList(),
    val activeConversationId: String = createConversationId(),
    val draft: String = "帮我确认 M3 是否已经接通。",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
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
    val errorMessage: String?,
    val settings: BreezeSettingsSnapshot,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settings: BreezeSettings,
) : ViewModel() {
    private val draft = MutableStateFlow("帮我确认 M3 是否已经接通。")
    private val activeConversationId = MutableStateFlow(createConversationId())
    private val isSending = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

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

    fun onSendMessage() {
        val text = draft.value.trim()
        if (text.isBlank() || isSending.value) {
            return
        }

        val conversationId = activeConversationId.value
        draft.value = ""
        errorMessage.value = null
        isSending.value = true

        viewModelScope.launch {
            runCatching {
                chatRepository.sendMessage(conversationId, text).collect {}
            }.onFailure { throwable ->
                draft.value = text
                errorMessage.value = throwable.message ?: "消息发送失败，请稍后重试。"
            }
            isSending.value = false
        }
    }
}

internal fun createConversationId(): String = "conversation-${Clock.System.now().toEpochMilliseconds()}"
