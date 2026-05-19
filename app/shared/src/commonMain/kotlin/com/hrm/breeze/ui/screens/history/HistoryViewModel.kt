package com.hrm.breeze.ui.screens.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@Immutable
data class HistoryUiState(
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val latestMessagePreview: String? = null,
)

class HistoryViewModel(
    chatRepository: ChatRepository,
) : ViewModel() {
    private val selectedConversationId = MutableStateFlow<String?>(null)

    private val conversations =
        chatRepository.observeConversations().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    private val activeConversationId =
        combine(
            conversations,
            selectedConversationId,
        ) { conversations, selectedId ->
            when {
                conversations.isEmpty() -> null
                selectedId != null && conversations.any { it.id == selectedId } -> selectedId
                else -> conversations.first().id
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val messages =
        activeConversationId.flatMapLatest { conversationId ->
            if (conversationId == null) {
                flowOf(emptyList())
            } else {
                chatRepository.observeMessages(conversationId)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    val state: StateFlow<HistoryUiState> =
        combine(
            conversations,
            activeConversationId,
            messages,
        ) { conversations, activeConversationId, messages ->
            HistoryUiState(
                conversations = conversations,
                activeConversationId = activeConversationId,
                messages = messages,
                latestMessagePreview = messages.lastOrNull()?.content,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HistoryUiState(),
        )

    fun onConversationSelected(conversationId: String) {
        selectedConversationId.value = conversationId
    }
}
