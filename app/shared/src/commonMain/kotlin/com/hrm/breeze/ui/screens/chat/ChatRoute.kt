package com.hrm.breeze.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoute(
    modifier: Modifier = Modifier,
    selectedDesktopRoute: String = "chat",
    onOpenSettings: () -> Unit = {},
    onSelectChatTab: () -> Unit = {},
    onOpenApiConfig: () -> Unit = {},
    onOpenModelSettings: () -> Unit = {},
    onOpenOnDeviceModels: () -> Unit = {},
    embeddedApiConfigContent: @Composable () -> Unit = {},
    embeddedModelSettingsContent: @Composable () -> Unit = {},
    embeddedOnDeviceModelsContent: @Composable () -> Unit = {},
    viewModel: ChatViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    ChatScreen(
        modifier = modifier,
        state = uiState,
        onDraftChange = viewModel::onDraftChange,
        onReasoningEnabledChange = viewModel::onReasoningEnabledChange,
        onConversationSelected = viewModel::onConversationSelected,
        onNewConversation = viewModel::onNewConversation,
        onModelSelected = viewModel::onModelSelected,
        onSendMessage = viewModel::onSendMessage,
        selectedDesktopRoute = selectedDesktopRoute,
        onOpenSettings = onOpenSettings,
        onSelectChatTab = onSelectChatTab,
        onOpenApiConfig = onOpenApiConfig,
        onOpenModelSettings = onOpenModelSettings,
        onOpenOnDeviceModels = onOpenOnDeviceModels,
        embeddedApiConfigContent = embeddedApiConfigContent,
        embeddedModelSettingsContent = embeddedModelSettingsContent,
        embeddedOnDeviceModelsContent = embeddedOnDeviceModelsContent,
    )
}
