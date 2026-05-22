package com.hrm.breeze.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.generated.resources.Res
import com.hrm.breeze.generated.resources.api_config
import com.hrm.breeze.generated.resources.code
import com.hrm.breeze.generated.resources.composer_placeholder
import com.hrm.breeze.generated.resources.model_settings
import com.hrm.breeze.generated.resources.new_chat
import com.hrm.breeze.generated.resources.preview_responsive_hint
import com.hrm.breeze.generated.resources.preview_welcome_prompt
import com.hrm.breeze.generated.resources.hide
import com.hrm.breeze.generated.resources.quick
import com.hrm.breeze.generated.resources.reasoning_process
import com.hrm.breeze.generated.resources.reasoning_mode
import com.hrm.breeze.generated.resources.send
import com.hrm.breeze.generated.resources.sending
import com.hrm.breeze.generated.resources.on_device_models
import com.hrm.breeze.generated.resources.show
import com.hrm.breeze.generated.resources.welcome_prompt
import com.hrm.breeze.generated.resources.writing
import com.hrm.breeze.generated.resources.you
import com.hrm.breeze.i18n.promptSuggestionTexts
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.WidthClass
import com.hrm.breeze.ui.components.rememberAutoScrollToBottomState
import com.hrm.breeze.ui.navigation.ApiConfig
import com.hrm.breeze.ui.navigation.Chat
import com.hrm.breeze.ui.navigation.ModelSettings
import com.hrm.breeze.ui.navigation.OnDeviceModels
import com.hrm.breeze.ui.theme.BreezeTheme
import com.hrm.markdown.renderer.Markdown
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

internal data class ChatMainPanelActions(
    val onDraftChange: (String) -> Unit,
    val onReasoningEnabledChange: (Boolean) -> Unit,
    val onSendMessage: () -> Unit,
    val onNewConversation: () -> Unit,
    val onModelSelected: (String) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenModelSettings: () -> Unit,
    val onOpenOnDeviceModels: () -> Unit,
)

@Composable
internal fun ChatMainPanel(
    state: ChatUiState,
    actions: ChatMainPanelActions,
    selectedDesktopRoute: String,
    desktopTopInset: Dp,
    embeddedApiConfigContent: @Composable () -> Unit,
    embeddedModelSettingsContent: @Composable () -> Unit,
    embeddedOnDeviceModelsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean,
    compactMode: Boolean,
) {
    val spacing = BreezeTheme.spacing
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(extra.appShellBackground),
        color = extra.appShellBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = spacing.md,
                    top = spacing.md + if (compactMode) 0.dp else desktopTopInset,
                    end = spacing.md,
                    bottom = spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                compactMode || selectedDesktopRoute == Chat.routePattern -> {
                    if (!compactMode) {
                        DesktopWorkspaceHeader(
                            state = state,
                            actions = actions,
                            selectedDesktopRoute = selectedDesktopRoute,
                        )
                    }
                    MessageStage(
                        state = state,
                        onPromptSelected = actions.onDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = if (compactMode) 720.dp else 980.dp),
                        previewMode = previewMode,
                    )
                    ComposerBar(
                        state = state,
                        actions = actions,
                        compactMode = compactMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = if (compactMode) 720.dp else 1080.dp),
                    )
                }

                selectedDesktopRoute == ApiConfig.routePattern -> {
                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        embeddedApiConfigContent()
                    }
                }

                selectedDesktopRoute == ModelSettings.routePattern -> {
                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        embeddedModelSettingsContent()
                    }
                }

                selectedDesktopRoute == OnDeviceModels.routePattern -> {
                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        embeddedOnDeviceModelsContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopWorkspaceHeader(
    state: ChatUiState,
    actions: ChatMainPanelActions,
    selectedDesktopRoute: String,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = desktopHeaderTitle(selectedDesktopRoute, state),
            style = typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ModelSwitcher(
            state = state,
            onModelSelected = actions.onModelSelected,
            onOpenModelSettings = actions.onOpenModelSettings,
            onOpenOnDeviceModels = actions.onOpenOnDeviceModels,
            compact = true,
        )
    }
}

@Composable
private fun desktopHeaderTitle(
    selectedDesktopRoute: String,
    state: ChatUiState,
): String = when (selectedDesktopRoute) {
    ApiConfig.routePattern -> stringResource(Res.string.api_config)
    ModelSettings.routePattern -> stringResource(Res.string.model_settings)
    OnDeviceModels.routePattern -> stringResource(Res.string.on_device_models)
    else -> state.conversations.firstOrNull { it.id == state.activeConversationId }?.title ?: stringResource(Res.string.new_chat)
}

@Composable
private fun MessageStage(
    state: ChatUiState,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean,
) {
    val spacing = BreezeTheme.spacing
    val scrollState = rememberScrollState()
    val autoScrollState = rememberAutoScrollToBottomState(scrollState)

    if (state.messages.isEmpty()) {
        WelcomePanel(
            modifier = modifier,
            onPromptSelected = onPromptSelected,
            previewMode = previewMode,
        )
        return
    }

    val latestMessage = state.messages.lastOrNull()
    val latestAssistantStreamSignature =
        remember(state.messages, state.isSending) {
            state.messages.lastOrNull { it.role == Message.Role.Assistant }?.let { message ->
                Triple(
                    message.id,
                    message.content.length,
                    message.reasoningContent?.length ?: 0,
                )
            }
        }
    var wasSending by remember(state.activeConversationId) { mutableStateOf(state.isSending) }

    LaunchedEffect(state.activeConversationId) {
        autoScrollState.scrollToBottom(force = true)
    }

    LaunchedEffect(state.messages.size) {
        if (latestMessage?.role == Message.Role.User) {
            autoScrollState.scrollToBottom(force = true)
        } else {
            autoScrollState.scrollToBottom()
        }
    }

    LaunchedEffect(latestAssistantStreamSignature, state.isSending) {
        if (state.isSending && latestAssistantStreamSignature != null) {
            autoScrollState.scrollToBottom()
        }
    }

    LaunchedEffect(state.isSending, latestAssistantStreamSignature, state.activeConversationId) {
        val justFinishedStreaming = wasSending && !state.isSending
        wasSending = state.isSending
        if (
            justFinishedStreaming &&
            latestAssistantStreamSignature != null &&
            autoScrollState.shouldAutoScroll
        ) {
            autoScrollState.scrollToBottom()
        }
    }

    val latestAssistantMessageId = state.messages.lastOrNull { it.role == Message.Role.Assistant }?.id

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(state.isSending) {
                if (!state.isSending) {
                    return@pointerInput
                }
                detectVerticalDragGestures { _, dragAmount ->
                    if (abs(dragAmount) > 0f) {
                        autoScrollState.disableAutoScroll()
                    }
                }
            }
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        state.messages.forEach { message ->
            MessageBubble(
                message = message,
                isStreaming = state.isSending && message.role == Message.Role.Assistant && message.id == latestAssistantMessageId,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WelcomePanel(
    modifier: Modifier = Modifier,
    onPromptSelected: (String) -> Unit,
    previewMode: Boolean,
) {
    val typography = BreezeTheme.typography
    val spacing = BreezeTheme.spacing
    val extra = BreezeTheme.extendedColors
    val scheme = MaterialTheme.colorScheme
    val maxPromptItemsInRow = if (LocalWindowInfo.current.widthClass == WidthClass.Compact) 1 else 2
    val prompts = promptSuggestionTexts()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.72f))
        Text(
            text = if (previewMode) stringResource(Res.string.preview_welcome_prompt) else stringResource(Res.string.welcome_prompt),
            style = typography.titleLarge,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.lg))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            maxItemsInEachRow = maxPromptItemsInRow,
        ) {
            prompts.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .clip(BreezeTheme.shapes.pill)
                        .clickable { onPromptSelected(prompt) },
                    shape = BreezeTheme.shapes.pill,
                    color = extra.promptChipBackground,
                    border = BorderStroke(spacing.hairline, scheme.outlineVariant.copy(alpha = 0.36f)),
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                        style = typography.bodyMedium,
                        color = extra.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (previewMode) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = stringResource(Res.string.preview_responsive_hint),
                style = typography.bodySmall,
                color = extra.textTertiary,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ComposerBar(
    state: ChatUiState,
    actions: ChatMainPanelActions,
    compactMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        state.errorMessage?.let { errorMessage ->
            Surface(
                color = scheme.errorContainer,
                shape = shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = stringResource(errorMessage),
                        style = typography.bodySmall,
                        color = scheme.onErrorContainer,
                    )
                    state.errorDetail?.let { errorDetail ->
                        Text(
                            text = errorDetail,
                            style = typography.bodySmall,
                            color = scheme.onErrorContainer.copy(alpha = 0.9f),
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = extra.composerBackground,
            shape = shapes.input,
            border = BorderStroke(spacing.hairline, extra.chatInputBorder),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = actions.onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                    minLines = 2,
                    maxLines = 4,
                    shape = shapes.input,
                    placeholder = {
                        Text(stringResource(Res.string.composer_placeholder))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = scheme.surface.copy(alpha = 0f),
                        focusedContainerColor = scheme.surface.copy(alpha = 0f),
                        disabledContainerColor = scheme.surface.copy(alpha = 0f),
                        unfocusedBorderColor = scheme.surface.copy(alpha = 0f),
                        focusedBorderColor = scheme.surface.copy(alpha = 0f),
                        disabledBorderColor = scheme.surface.copy(alpha = 0f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        maxItemsInEachRow = 4,
                    ) {
                        TextButton(onClick = actions.onNewConversation, shape = shapes.pill) {
                            Text(stringResource(Res.string.quick))
                        }
                        TextButton(
                            onClick = { actions.onReasoningEnabledChange(!state.reasoningEnabled) },
                            enabled = !state.isSending,
                            shape = shapes.pill,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (state.reasoningEnabled) scheme.primary.copy(alpha = 0.14f) else scheme.surface,
                                contentColor = if (state.reasoningEnabled) scheme.primary else extra.textSecondary,
                            ),
                        ) {
                            Text(stringResource(Res.string.reasoning_mode))
                        }
                        TextButton(onClick = {}, shape = shapes.pill) {
                            Text(stringResource(Res.string.writing))
                        }
                        TextButton(onClick = {}, shape = shapes.pill) {
                            Text(stringResource(Res.string.code))
                        }
                    }
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Button(
                        onClick = actions.onSendMessage,
                        enabled = state.draft.isNotBlank() && !state.isSending,
                        shape = shapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Text(if (state.isSending) stringResource(Res.string.sending) else stringResource(Res.string.send))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isStreaming: Boolean = false,
) {
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors
    val isUser = message.role == Message.Role.User
    val reasoningContent = message.reasoningContent?.takeIf { it.isNotBlank() }
    var reasoningExpanded by rememberSaveable(message.id) { mutableStateOf(false) }

    LaunchedEffect(message.id, isStreaming) {
        reasoningExpanded = isStreaming
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(if (isUser) shapes.bubbles.outgoing else shapes.bubbles.incoming)
                .background(if (isUser) extra.chatUserBubble else extra.chatAiBubble)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = if (isUser) stringResource(Res.string.you) else "Breeze",
                style = typography.labelMedium,
                color = if (isUser) extra.chatUserText else extra.chatAiText,
            )
            if (isUser) {
                Text(
                    text = message.content,
                    style = typography.bodyMedium,
                    color = extra.chatUserText,
                )
            } else {
                if (reasoningContent != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = extra.promptChipBackground,
                        shape = shapes.medium,
                        border = BorderStroke(
                            spacing.hairline,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.sm, vertical = spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(Res.string.reasoning_process),
                                    style = typography.labelMedium,
                                    color = extra.textSecondary,
                                )
                                TextButton(
                                    onClick = { reasoningExpanded = !reasoningExpanded },
                                    shape = shapes.pill,
                                ) {
                                    Text(
                                        text = if (reasoningExpanded) {
                                            stringResource(Res.string.`hide`)
                                        } else {
                                            stringResource(Res.string.`show`)
                                        },
                                    )
                                }
                            }
                            if (reasoningExpanded) {
                                Markdown(
                                    markdown = reasoningContent,
                                    modifier = Modifier.fillMaxWidth(),
                                    isStreaming = isStreaming,
                                    enableScroll = false,
                                )
                            }
                        }
                    }
                }
                Markdown(
                    markdown = message.content,
                    modifier = Modifier.fillMaxWidth(),
                    isStreaming = isStreaming,
                    enableScroll = false,
                )
            }
        }
    }
}
