package com.hrm.breeze.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.domain.model.ModelConfig
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.getPlatform
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
import com.hrm.breeze.ui.navigation.Chat
import com.hrm.breeze.ui.theme.BreezeTheme
import kotlin.time.Clock

@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onReasoningEnabledChange: (Boolean) -> Unit,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit,
    onModelSelected: (String) -> Unit,
    onSendMessage: () -> Unit,
    selectedDesktopRoute: String = Chat.routePattern,
    onOpenSettings: () -> Unit,
    onSelectChatTab: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onOpenOnDeviceModels: () -> Unit,
    embeddedApiConfigContent: @Composable () -> Unit = {},
    embeddedModelSettingsContent: @Composable () -> Unit = {},
    embeddedOnDeviceModelsContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
) {
    val windowInfo = LocalWindowInfo.current
    val spacing = BreezeTheme.spacing
    val isMacDesktop = windowInfo.paneMode != PaneMode.Single && getPlatform().isMacDesktop
    val macSidebarLeadingInset = 0.dp
    val macSidebarTopInset = if (isMacDesktop) spacing.sm else 0.dp
    val macHeaderTopInset = if (isMacDesktop) spacing.sm else 0.dp
    val mainPanelActions = remember(
        onDraftChange,
        onSendMessage,
        onNewConversation,
        onModelSelected,
        onOpenSettings,
        onOpenModelSettings,
        onOpenOnDeviceModels,
    ) {
        ChatMainPanelActions(
            onDraftChange = onDraftChange,
            onReasoningEnabledChange = onReasoningEnabledChange,
            onSendMessage = onSendMessage,
            onNewConversation = onNewConversation,
            onModelSelected = onModelSelected,
            onOpenSettings = onOpenSettings,
            onOpenModelSettings = onOpenModelSettings,
            onOpenOnDeviceModels = onOpenOnDeviceModels,
        )
    }

    if (windowInfo.paneMode == PaneMode.Single) {
        CompactChatLayout(
            state = state,
            actions = mainPanelActions,
            modifier = modifier.padding(spacing.md),
            previewMode = previewMode,
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize(),
    ) {
        DesktopSidebar(
            state = state,
            onConversationSelected = onConversationSelected,
            onNewConversation = onNewConversation,
            onOpenApiConfig = onOpenApiConfig,
            onOpenModelSettings = onOpenModelSettings,
            onOpenOnDeviceModels = onOpenOnDeviceModels,
            selectedDesktopRoute = selectedDesktopRoute,
            onSelectChatTab = onSelectChatTab,
            leadingInset = macSidebarLeadingInset,
            topInset = macSidebarTopInset,
            modifier = Modifier.width(320.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(spacing.hairline)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        )
        ChatMainPanel(
            state = state,
            actions = mainPanelActions,
            selectedDesktopRoute = selectedDesktopRoute,
            modifier = Modifier.weight(1f),
            previewMode = previewMode,
            compactMode = false,
            desktopTopInset = macHeaderTopInset,
            embeddedApiConfigContent = embeddedApiConfigContent,
            embeddedModelSettingsContent = embeddedModelSettingsContent,
            embeddedOnDeviceModelsContent = embeddedOnDeviceModelsContent,
        )
    }
}

@Composable
internal fun CompactChatLayout(
    state: ChatUiState,
    actions: ChatMainPanelActions,
    modifier: Modifier = Modifier,
    previewMode: Boolean,
) {
    val spacing = BreezeTheme.spacing

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        CompactChatHeader(
            state = state,
            onOpenSettings = actions.onOpenSettings,
            onNewConversation = actions.onNewConversation,
            onModelSelected = actions.onModelSelected,
            onOpenModelSettings = actions.onOpenModelSettings,
            onOpenOnDeviceModels = actions.onOpenOnDeviceModels,
        )
        ChatMainPanel(
            state = state,
            actions = actions,
            selectedDesktopRoute = Chat.routePattern,
            desktopTopInset = 0.dp,
            embeddedApiConfigContent = {},
            embeddedModelSettingsContent = {},
            embeddedOnDeviceModelsContent = {},
            compactMode = true,
            modifier = Modifier.weight(1f),
            previewMode = previewMode,
        )
    }
}

internal fun previewChatUiState(): ChatUiState {
    val now = Clock.System.now()
    val nowEpochMillis = now.toEpochMilliseconds()
    val conversationId = "preview-conversation"
    val previewConfigs =
        listOf(
            ModelConfig(
                id = "preview-config-1",
                providerId = LlmProviderId.OpenAI,
                endpoint = "https://openrouter.ai/api/v1",
                apiToken = null,
                modelId = "nvidia/nemotron-3-super-120b-a12b:free",
                createdAt = now,
                updatedAt = now,
            ),
            ModelConfig(
                id = "preview-config-2",
                providerId = LlmProviderId.OpenAI,
                endpoint = "https://openrouter.ai/api/v1",
                apiToken = null,
                modelId = "google/gemma-3-27b-it:free",
                createdAt = now,
                updatedAt = now,
            ),
        )
    return ChatUiState(
        conversations = listOf(
            Conversation(
                id = conversationId,
                title = "Explain quantum computing",
                modelId = "claude-3-5-sonnet",
                updatedAt = now,
            ),
            Conversation(
                id = "preview-conversation-2",
                title = "Python function help",
                modelId = "claude-3-5-sonnet",
                updatedAt = kotlin.time.Instant.fromEpochMilliseconds(nowEpochMillis - DAY_MILLIS),
            ),
            Conversation(
                id = "preview-conversation-3",
                title = "Design a REST API",
                modelId = "claude-3-5-sonnet",
                updatedAt = kotlin.time.Instant.fromEpochMilliseconds(nowEpochMillis - 4 * DAY_MILLIS),
            ),
            Conversation(
                id = "preview-conversation-4",
                title = "Plan a product launch",
                modelId = "gpt-4.1-mini",
                updatedAt = kotlin.time.Instant.fromEpochMilliseconds(nowEpochMillis - 12 * DAY_MILLIS),
            ),
        ),
        messages = listOf(
            Message(
                id = "preview-user",
                conversationId = conversationId,
                role = Message.Role.User,
                content = "Can you explain what a binary search algorithm is?",
                createdAt = now,
            ),
            Message(
                id = "preview-assistant",
                conversationId = conversationId,
                role = Message.Role.Assistant,
                content = "Certainly. A binary search algorithm finds a target value in a sorted array by repeatedly comparing against the middle element and shrinking the search range.",
                createdAt = now,
            ),
        ),
        modelConfigs = previewConfigs,
        activeModelConfig = previewConfigs.first(),
        activeConversationId = conversationId,
        draft = "",
        isSending = false,
        errorMessage = null,
        settings = BreezeSettingsSnapshot(),
    )
}
