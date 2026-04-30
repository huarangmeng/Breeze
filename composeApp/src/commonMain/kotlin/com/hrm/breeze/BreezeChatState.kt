package com.hrm.breeze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message

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
    viewModel: BreezeChatViewModel = koinViewModel(),
): BreezeChatState {
    val uiState by viewModel.state.collectAsState()

    return BreezeChatState(
        conversations = uiState.conversations,
        messages = uiState.messages,
        activeConversationId = uiState.activeConversationId,
        draft = uiState.draft,
        isSending = uiState.isSending,
        errorMessage = uiState.errorMessage,
        settings = uiState.settings,
        onDraftChange = viewModel::onDraftChange,
        onConversationSelected = viewModel::onConversationSelected,
        onNewConversation = viewModel::onNewConversation,
        onSendMessage = viewModel::onSendMessage,
    )
}
