package com.hrm.breeze.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.breeze.getPlatform
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.generated.resources.*
import com.hrm.breeze.i18n.promptSuggestionTexts
import org.jetbrains.compose.resources.stringResource
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
import com.hrm.breeze.ui.adaptive.WidthClass
import com.hrm.breeze.ui.navigation.ApiConfig
import com.hrm.breeze.ui.navigation.Chat
import com.hrm.breeze.ui.navigation.ModelSettings
import com.hrm.breeze.ui.theme.BreezeTheme
import kotlin.time.Clock

@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit,
    onModelSelected: (String) -> Unit,
    onSendMessage: () -> Unit,
    selectedDesktopRoute: String = Chat.routePattern,
    onOpenSettings: () -> Unit,
    onSelectChatTab: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    embeddedApiConfigContent: @Composable () -> Unit = {},
    embeddedModelSettingsContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
) {
    val windowInfo = LocalWindowInfo.current
    val spacing = BreezeTheme.spacing
    val isMacDesktop = windowInfo.paneMode != PaneMode.Single && getPlatform().isMacDesktop
    val macSidebarLeadingInset = 0.dp
    val macSidebarTopInset = if (isMacDesktop) spacing.sm else 0.dp
    val macHeaderTopInset = if (isMacDesktop) spacing.sm else 0.dp

    if (windowInfo.paneMode == PaneMode.Single) {
        CompactChatLayout(
            state = state,
            onDraftChange = onDraftChange,
            onNewConversation = onNewConversation,
            onModelSelected = onModelSelected,
            onSendMessage = onSendMessage,
            onOpenSettings = onOpenSettings,
            onOpenModelSettings = onOpenModelSettings,
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
            onDraftChange = onDraftChange,
            onSendMessage = onSendMessage,
            onNewConversation = onNewConversation,
            onModelSelected = onModelSelected,
            selectedDesktopRoute = selectedDesktopRoute,
            modifier = Modifier.weight(1f),
            previewMode = previewMode,
            compactMode = false,
            onOpenSettings = onOpenSettings,
            onOpenModelSettings = onOpenModelSettings,
            desktopTopInset = macHeaderTopInset,
            embeddedApiConfigContent = embeddedApiConfigContent,
            embeddedModelSettingsContent = embeddedModelSettingsContent,
        )
    }
}

@Composable
private fun CompactChatLayout(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onNewConversation: () -> Unit,
    onModelSelected: (String) -> Unit,
    onSendMessage: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModelSettings: () -> Unit,
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
            onOpenSettings = onOpenSettings,
            onNewConversation = onNewConversation,
            onModelSelected = onModelSelected,
            onOpenModelSettings = onOpenModelSettings,
        )
        ChatMainPanel(
            state = state,
            onDraftChange = onDraftChange,
            onSendMessage = onSendMessage,
            onNewConversation = onNewConversation,
            onModelSelected = onModelSelected,
            onOpenSettings = onOpenSettings,
            selectedDesktopRoute = Chat.routePattern,
            desktopTopInset = 0.dp,
            embeddedApiConfigContent = {},
            embeddedModelSettingsContent = {},
            compactMode = true,
            modifier = Modifier.weight(1f),
            previewMode = previewMode,
            onOpenModelSettings = onOpenModelSettings,
        )
    }
}

@Composable
private fun DesktopSidebar(
    state: ChatUiState,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    selectedDesktopRoute: String,
    onSelectChatTab: () -> Unit,
    leadingInset: Dp,
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(extra.sidebarBackground)
            .padding(
                start = spacing.md,
                top = spacing.lg + topInset,
                end = spacing.md,
                bottom = spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = leadingInset,
                    top = spacing.xs,
                    end = 0.dp,
                    bottom = spacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(shapes.pill)
                    .background(scheme.primaryContainer),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.micro),
            ) {
                Text(
                    text = "Breeze",
                    style = typography.titleMedium,
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Button(
            onClick = onNewConversation,
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
            ),
        ) {
            Text(stringResource(Res.string.new_chat))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            SidebarActionItem(
                label = stringResource(Res.string.current_chat),
                selected = selectedDesktopRoute == Chat.routePattern,
                onClick = onSelectChatTab,
            )
            SidebarActionItem(
                label = stringResource(Res.string.api_config),
                selected = selectedDesktopRoute == ApiConfig.routePattern,
                onClick = onOpenApiConfig,
            )
            SidebarActionItem(
                label = stringResource(Res.string.model_settings),
                selected = selectedDesktopRoute == ModelSettings.routePattern,
                onClick = onOpenModelSettings,
            )
            SidebarActionItem(
                label = stringResource(Res.string.more_features),
                selected = false,
                onClick = {},
            )
        }

        HistorySidebarSection(
            conversations = state.conversations,
            activeConversationId = state.activeConversationId,
            onConversationSelected = onConversationSelected,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun HistorySidebarSection(
    conversations: List<Conversation>,
    activeConversationId: String,
    onConversationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = modifier,
        color = scheme.surface.copy(alpha = 0.42f),
        shape = shapes.large,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant.copy(alpha = 0.54f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.history_conversations),
                modifier = Modifier.padding(horizontal = spacing.xs),
                style = typography.labelLarge,
                color = extra.textSecondary,
            )

            if (conversations.isEmpty()) {
                Text(
                    text = stringResource(Res.string.empty_history_hint),
                    modifier = Modifier.padding(horizontal = spacing.xs),
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    conversationSections(conversations).forEach { section ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            Text(
                                text = section.title,
                                modifier = Modifier.padding(horizontal = spacing.xs),
                                style = typography.labelMedium,
                                color = extra.textTertiary,
                            )
                            section.items.forEach { conversation ->
                                ConversationListItem(
                                    conversation = conversation,
                                    selected = conversation.id == activeConversationId,
                                    onClick = { onConversationSelected(conversation.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarActionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) extra.sidebarSelectedBackground else scheme.surface.copy(alpha = 0f),
        shape = shapes.medium,
        border = if (selected) {
            BorderStroke(spacing.hairline, scheme.outlineVariant.copy(alpha = 0.72f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(shapes.pill)
                    .background(if (selected) scheme.primary else scheme.outlineVariant),
            )
            Text(
                text = label,
                style = typography.bodyMedium,
                color = if (selected) scheme.onSurface else extra.textSecondary,
            )
        }
    }
}

@Composable
private fun CompactChatHeader(
    state: ChatUiState,
    onOpenSettings: () -> Unit,
    onNewConversation: () -> Unit,
    onModelSelected: (String) -> Unit,
    onOpenModelSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing

    Surface(
        color = scheme.surface.copy(alpha = 0f),
        shape = shapes.pill,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xs, vertical = spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderActionButton(label = "⚙", onClick = onOpenSettings)
            ModelSwitcher(
                state = state,
                onModelSelected = onModelSelected,
                onOpenModelSettings = onOpenModelSettings,
                modifier = Modifier.widthIn(max = 180.dp),
                compact = true,
            )
            HeaderActionButton(label = "+", onClick = onNewConversation)
        }
    }
}

@Composable
private fun HeaderActionButton(
    label: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(shapes.pill)
            .clickable(onClick = onClick),
        color = scheme.surface.copy(alpha = 0.92f),
        shape = shapes.pill,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant.copy(alpha = 0.72f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = typography.titleMedium,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ConversationListItem(
    conversation: Conversation,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) scheme.surface.copy(alpha = 0.92f) else scheme.surface.copy(alpha = 0.36f),
        shape = shapes.medium,
        border = BorderStroke(
            spacing.hairline,
            if (selected) scheme.primary.copy(alpha = 0.18f) else scheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.micro),
        ) {
            Text(
                text = conversation.title,
                style = typography.labelLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.model_label, conversation.modelId),
                style = typography.bodySmall,
                color = extra.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatMainPanel(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNewConversation: () -> Unit,
    onModelSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModelSettings: () -> Unit,
    selectedDesktopRoute: String,
    desktopTopInset: Dp,
    embeddedApiConfigContent: @Composable () -> Unit,
    embeddedModelSettingsContent: @Composable () -> Unit,
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
                    start = if (compactMode) spacing.sm else spacing.xl,
                    top = if (compactMode) spacing.sm else spacing.md + desktopTopInset,
                    end = if (compactMode) spacing.sm else spacing.xl,
                    bottom = if (compactMode) spacing.sm else spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compactMode) {
                DesktopWorkspaceHeader(
                    state = state,
                    selectedDesktopRoute = selectedDesktopRoute,
                    onModelSelected = onModelSelected,
                    onOpenModelSettings = onOpenModelSettings,
                )
            }
            when {
                compactMode || selectedDesktopRoute == Chat.routePattern -> {
                    MessageStage(
                        state = state,
                        onPromptSelected = onDraftChange,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = if (compactMode) 720.dp else 980.dp),
                        previewMode = previewMode,
                    )
                    ComposerBar(
                        state = state,
                        onDraftChange = onDraftChange,
                        onSendMessage = onSendMessage,
                        onNewConversation = onNewConversation,
                        onOpenSettings = onOpenSettings,
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
            }
        }
    }
}

@Composable
private fun DesktopWorkspaceHeader(
    state: ChatUiState,
    selectedDesktopRoute: String,
    onModelSelected: (String) -> Unit,
    onOpenModelSettings: () -> Unit,
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
            onModelSelected = onModelSelected,
            onOpenModelSettings = onOpenModelSettings,
            modifier = Modifier.widthIn(max = 180.dp),
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
    else -> state.conversations.firstOrNull { it.id == state.activeConversationId }?.title ?: stringResource(Res.string.new_chat)
}

@Composable
private fun ModelSwitcher(
    state: ChatUiState,
    onModelSelected: (String) -> Unit,
    onOpenModelSettings: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors
    var expanded by remember { mutableStateOf(false) }
    val options = modelOptionsFor(state.settings.currentProviderId)
    val selectedTitle = options.firstOrNull { it.id == state.settings.currentModelId }?.title
        ?: state.settings.currentModelId

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .widthIn(min = 116.dp, max = 176.dp)
                .height(34.dp)
                .clip(shapes.pill)
                .clickable { expanded = true },
            color = scheme.surface.copy(alpha = 0.86f),
            shape = shapes.pill,
            border = BorderStroke(spacing.hairline, extra.focusRing.copy(alpha = 0.42f)),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = spacing.sm,
                    vertical = spacing.xxs,
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!compact) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(shapes.pill)
                            .background(extra.success),
                    )
                }
                if (compact) {
                    Text(
                        text = selectedTitle,
                        modifier = Modifier.widthIn(max = 126.dp),
                        style = typography.labelLarge,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.micro),
                    ) {
                        Text(
                            text = stringResource(Res.string.current_model),
                            style = typography.labelMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${state.settings.currentProviderId.displayName} / $selectedTitle",
                            style = typography.bodySmall,
                            color = extra.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = "▾",
                    style = typography.labelMedium,
                    color = scheme.primary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = scheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            shape = shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .padding(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                options.forEach { option ->
                    ModelMenuItem(
                        option = option,
                        selected = option.id == state.settings.currentModelId,
                        onClick = {
                            expanded = false
                            onModelSelected(option.id)
                        },
                    )
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .clickable {
                            expanded = false
                            onOpenModelSettings()
                        },
                    color = scheme.primaryContainer.copy(alpha = 0.58f),
                    shape = shapes.medium,
                ) {
                    Text(
                        text = stringResource(Res.string.model_settings_menu),
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
                        style = typography.labelLarge,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelMenuItem(
    option: ChatModelOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick),
        color = if (selected) extra.promptChipBackground else scheme.surface,
        shape = shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(shapes.pill)
                    .background(if (selected) scheme.primary else scheme.outlineVariant),
            )
            Text(
                text = option.title,
                modifier = Modifier.weight(1f),
                style = typography.labelLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class ChatModelOption(
    val id: String,
    val title: String,
)

private fun modelOptionsFor(providerId: LlmProviderId): List<ChatModelOption> = when (providerId) {
    LlmProviderId.Local -> listOf(
        ChatModelOption("breeze-echo", "Breeze Echo"),
        ChatModelOption("qwen2.5:7b", "Qwen 2.5 7B"),
        ChatModelOption("llama3.2:3b", "Llama 3.2 3B"),
    )

    LlmProviderId.OpenAI -> listOf(
        ChatModelOption("gpt-4.1-mini", "GPT-4.1 mini"),
        ChatModelOption("gpt-4.1", "GPT-4.1"),
        ChatModelOption("o4-mini", "o4-mini"),
    )

    LlmProviderId.Anthropic -> listOf(
        ChatModelOption("claude-3-5-haiku-latest", "Claude 3.5 Haiku"),
        ChatModelOption("claude-3-7-sonnet-latest", "Claude 3.7 Sonnet"),
        ChatModelOption("claude-opus-4-1", "Claude Opus 4.1"),
    )
}

@Composable
private fun MessageStage(
    state: ChatUiState,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean,
) {
    val spacing = BreezeTheme.spacing

    if (state.messages.isEmpty()) {
        WelcomePanel(
            modifier = modifier,
            onPromptSelected = onPromptSelected,
            previewMode = previewMode,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        state.messages.forEach { message ->
            MessageBubble(message = message)
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
    onDraftChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
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
                Text(
                    text = stringResource(errorMessage),
                    modifier = Modifier.padding(spacing.sm),
                    style = typography.bodySmall,
                    color = scheme.onErrorContainer,
                )
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
                    onValueChange = onDraftChange,
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
                        TextButton(onClick = onNewConversation, shape = shapes.pill) {
                            Text(stringResource(Res.string.quick))
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
                        onClick = onSendMessage,
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
) {
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors
    val isUser = message.role == Message.Role.User

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
            Text(
                text = message.content,
                style = typography.bodyMedium,
                color = if (isUser) extra.chatUserText else extra.chatAiText,
            )
        }
    }
}

private data class ConversationSection(
    val title: String,
    val items: List<Conversation>,
)

@Composable
private fun conversationSections(
    conversations: List<Conversation>,
): List<ConversationSection> {
    if (conversations.isEmpty()) {
        return emptyList()
    }

    val today = stringResource(Res.string.today)
    val yesterday = stringResource(Res.string.yesterday)
    val recent7Days = stringResource(Res.string.recent_7_days)
    val recent30Days = stringResource(Res.string.recent_30_days)
    val earlier = stringResource(Res.string.earlier)
    val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
    val grouped = conversations
        .sortedByDescending { it.updatedAt }
        .groupBy { conversation ->
            val ageDays = ((nowEpochMillis - conversation.updatedAt.toEpochMilliseconds()).coerceAtLeast(0L)) / DAY_MILLIS
            when {
                ageDays == 0L -> today
                ageDays == 1L -> yesterday
                ageDays < 7L -> recent7Days
                ageDays < 30L -> recent30Days
                else -> earlier
            }
        }

    return listOf(today, yesterday, recent7Days, recent30Days, earlier)
        .mapNotNull { title ->
            grouped[title]?.takeIf { it.isNotEmpty() }?.let { items ->
                ConversationSection(title = title, items = items)
            }
        }
}

private const val DAY_MILLIS = 86_400_000L

internal fun previewChatUiState(): ChatUiState {
    val now = Clock.System.now()
    val nowEpochMillis = now.toEpochMilliseconds()
    val conversationId = "preview-conversation"
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
        activeConversationId = conversationId,
        draft = "",
        isSending = false,
        errorMessage = null,
        settings = BreezeSettingsSnapshot(),
    )
}
