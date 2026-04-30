package com.hrm.breeze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.hrm.breeze.data.BreezeDataContainer
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Immutable
data class BreezeChatState(
    val conversations: List<Conversation>,
    val messages: List<Message>,
    val activeConversationId: String,
    val draft: String,
    val isSending: Boolean,
    val errorMessage: String?,
    val settings: BreezeSettingsSnapshot,
    val onDraftChange: (String) -> Unit,
    val onConversationSelected: (String) -> Unit,
    val onNewConversation: () -> Unit,
    val onSendMessage: () -> Unit,
)

@Composable
fun rememberBreezeChatState(
    dataContainer: BreezeDataContainer,
): BreezeChatState {
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("帮我确认 M3 是否已经接通。") }
    var activeConversationId by remember { mutableStateOf(createConversationId()) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val conversations by dataContainer.chatRepository.observeConversations().collectAsState(initial = emptyList())
    val settings by dataContainer.settings.snapshot.collectAsState(initial = BreezeSettingsSnapshot())

    LaunchedEffect(conversations, activeConversationId) {
        if (conversations.isNotEmpty() && conversations.none { it.id == activeConversationId }) {
            activeConversationId = conversations.first().id
        }
    }

    val messages by dataContainer.chatRepository.observeMessages(activeConversationId).collectAsState(initial = emptyList())

    return BreezeChatState(
        conversations = conversations,
        messages = messages,
        activeConversationId = activeConversationId,
        draft = draft,
        isSending = isSending,
        errorMessage = errorMessage,
        settings = settings,
        onDraftChange = { value ->
            draft = value
            if (errorMessage != null) {
                errorMessage = null
            }
        },
        onConversationSelected = { conversationId ->
            activeConversationId = conversationId
            errorMessage = null
        },
        onNewConversation = {
            activeConversationId = createConversationId()
            draft = ""
            errorMessage = null
        },
        onSendMessage = {
            val text = draft.trim()
            if (text.isBlank() || isSending) {
                return@BreezeChatState
            }

            val conversationId = activeConversationId
            draft = ""
            errorMessage = null
            isSending = true

            scope.launch {
                runCatching {
                    dataContainer.chatRepository.sendMessage(conversationId, text).collect()
                }.onFailure { throwable ->
                    draft = text
                    errorMessage = throwable.message ?: "消息发送失败，请稍后重试。"
                }
                isSending = false
            }
        },
    )
}

private fun createConversationId(): String = "conversation-${Clock.System.now().toEpochMilliseconds()}"
