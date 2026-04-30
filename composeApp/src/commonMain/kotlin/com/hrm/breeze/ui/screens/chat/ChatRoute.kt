package com.hrm.breeze.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoute(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    ChatScreen(
        modifier = modifier,
        state = uiState,
        onDraftChange = viewModel::onDraftChange,
        onConversationSelected = viewModel::onConversationSelected,
        onNewConversation = viewModel::onNewConversation,
        onSendMessage = viewModel::onSendMessage,
    )
}
