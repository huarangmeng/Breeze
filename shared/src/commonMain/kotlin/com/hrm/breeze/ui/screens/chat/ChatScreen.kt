package com.hrm.breeze.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
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
            onSendMessage = onSendMessage,
            onOpenSettings = onOpenSettings,
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
            modifier = Modifier.weight(0.24f),
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
            selectedDesktopRoute = selectedDesktopRoute,
            modifier = Modifier.weight(0.76f),
            previewMode = previewMode,
            compactMode = false,
            onOpenSettings = onOpenSettings,
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
    onSendMessage: () -> Unit,
    onOpenSettings: () -> Unit,
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
            previewMode = previewMode,
        )
        ChatMainPanel(
            state = state,
            onDraftChange = onDraftChange,
            onSendMessage = onSendMessage,
            onNewConversation = onNewConversation,
            onOpenSettings = onOpenSettings,
            selectedDesktopRoute = Chat.routePattern,
            desktopTopInset = 0.dp,
            embeddedApiConfigContent = {},
            embeddedModelSettingsContent = {},
            compactMode = true,
            modifier = Modifier.weight(1f),
            previewMode = previewMode,
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
            .background(scheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(
                start = spacing.sm,
                top = spacing.md + topInset,
                end = spacing.sm,
                bottom = spacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.xs + leadingInset,
                    top = spacing.xs,
                    end = spacing.xs,
                    bottom = spacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(shapes.pill)
                    .background(scheme.primaryContainer),
            )
            Text(
                text = "Breeze",
                style = typography.titleMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }

        SidebarActionItem(
            label = "当前对话",
            selected = selectedDesktopRoute == Chat.routePattern,
            onClick = onSelectChatTab,
        )
        SidebarActionItem(
            label = "API 配置",
            selected = selectedDesktopRoute == ApiConfig.routePattern,
            onClick = onOpenApiConfig,
        )
        SidebarActionItem(
            label = "模型设置",
            selected = selectedDesktopRoute == ModelSettings.routePattern,
            onClick = onOpenModelSettings,
        )
        SidebarActionItem(
            label = "更多能力",
            selected = false,
            onClick = {},
        )

        Button(
            onClick = onNewConversation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xs),
            shape = shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primaryContainer,
                contentColor = scheme.primary,
            ),
        ) {
            Text("新对话")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = "历史对话",
                style = typography.labelLarge,
                color = extra.textSecondary,
            )
            if (state.conversations.isEmpty()) {
                Text(
                    text = "发送第一条消息后，会话会出现在这里。",
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            } else {
                conversationSections(state.conversations).forEach { section ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(
                            text = section.title,
                            style = typography.labelMedium,
                            color = extra.textTertiary,
                        )
                        section.items.forEach { conversation ->
                            ConversationListItem(
                                conversation = conversation,
                                selected = conversation.id == state.activeConversationId,
                                onClick = { onConversationSelected(conversation.id) },
                            )
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
        color = if (selected) scheme.surface.copy(alpha = 0.88f) else scheme.surface.copy(alpha = 0f),
        shape = shapes.medium,
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
                    .size(10.dp)
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
    previewMode: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        color = scheme.surfaceVariant.copy(alpha = 0.48f),
        shape = shapes.large,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenSettings, shape = shapes.pill) {
                Text("设置")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.conversations.firstOrNull { it.id == state.activeConversationId }?.title ?: "新对话",
                    style = typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = if (previewMode) "Preview layout" else "内容由豆包 AI 生成，请仔细甄别",
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
            TextButton(onClick = onNewConversation, shape = shapes.pill) {
                Text("新建")
            }
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
                text = conversationPreviewLabel(conversation),
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
    onOpenSettings: () -> Unit,
    selectedDesktopRoute: String,
    desktopTopInset: Dp,
    embeddedApiConfigContent: @Composable () -> Unit,
    embeddedModelSettingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean,
    compactMode: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface.copy(alpha = 0.98f)),
        color = scheme.surface.copy(alpha = 0.98f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compactMode) spacing.md else spacing.xl,
                    top = if (compactMode) spacing.sm else spacing.md + desktopTopInset,
                    end = if (compactMode) spacing.md else spacing.xl,
                    bottom = if (compactMode) spacing.md else spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (!compactMode) {
                DesktopWorkspaceHeader(
                    state = state,
                    previewMode = previewMode,
                    selectedDesktopRoute = selectedDesktopRoute,
                )
            }
            when {
                compactMode || selectedDesktopRoute == Chat.routePattern -> {
                    MessageStage(
                        state = state,
                        modifier = Modifier.weight(1f),
                        previewMode = previewMode,
                    )
                    ComposerBar(
                        state = state,
                        onDraftChange = onDraftChange,
                        onSendMessage = onSendMessage,
                        onNewConversation = onNewConversation,
                        onOpenSettings = onOpenSettings,
                        compactMode = compactMode,
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
    previewMode: Boolean,
    selectedDesktopRoute: String,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(72.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = desktopHeaderTitle(selectedDesktopRoute, state),
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = desktopHeaderSubtitle(selectedDesktopRoute, previewMode),
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
        Box(
            modifier = Modifier.width(112.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(
                color = scheme.surface.copy(alpha = 0.72f),
                shape = BreezeTheme.shapes.pill,
                border = BorderStroke(spacing.hairline, scheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = "${state.settings.currentProviderId.displayName} · ${state.settings.currentModelId}",
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                    style = typography.labelMedium,
                    color = extra.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun desktopHeaderTitle(
    selectedDesktopRoute: String,
    state: ChatUiState,
): String = when (selectedDesktopRoute) {
    ApiConfig.routePattern -> "API 配置"
    ModelSettings.routePattern -> "模型设置"
    else -> state.conversations.firstOrNull { it.id == state.activeConversationId }?.title ?: "新对话"
}

private fun desktopHeaderSubtitle(
    selectedDesktopRoute: String,
    previewMode: Boolean,
): String = when (selectedDesktopRoute) {
    ApiConfig.routePattern -> if (previewMode) "Preview provider settings" else "在同一工作区中调整 Provider 与鉴权配置"
    ModelSettings.routePattern -> if (previewMode) "Preview model settings" else "在同一工作区中调整模型参数与输出偏好"
    else -> if (previewMode) "Preview layout" else "内容由豆包 AI 生成，请仔细甄别"
}

@Composable
private fun MessageStage(
    state: ChatUiState,
    modifier: Modifier = Modifier,
    previewMode: Boolean,
) {
    val spacing = BreezeTheme.spacing

    if (state.messages.isEmpty()) {
        WelcomePanel(
            modifier = modifier,
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
private fun WelcomePanel(
    modifier: Modifier = Modifier,
    previewMode: Boolean,
) {
    val typography = BreezeTheme.typography
    val spacing = BreezeTheme.spacing
    val extra = BreezeTheme.extendedColors
    val scheme = MaterialTheme.colorScheme

    val prompts = listOf(
        "资讯：帮我总结今天值得关注的 AI 动态",
        "深圳中产家庭收入大概是多少？",
        "为什么很多程序员喜欢 Dvorak 键盘布局？",
        "给我一些促进静脉健康的生活建议",
        "天玑和骁龙处理器各自的优缺点是什么",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (previewMode) "预览模式下的首页问题引导" else "有什么我能帮你的吗？",
            style = typography.titleLarge,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.xl))
        prompts.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowItems.forEach { prompt ->
                    Surface(
                        shape = BreezeTheme.shapes.pill,
                        color = scheme.surfaceVariant.copy(alpha = 0.52f),
                    ) {
                        Text(
                            text = prompt,
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                            style = typography.bodyMedium,
                            color = extra.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.sm))
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ComposerBar(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    compactMode: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (state.errorMessage != null) {
            Surface(
                color = scheme.errorContainer,
                shape = shapes.medium,
            ) {
                Text(
                    text = state.errorMessage,
                    modifier = Modifier.padding(spacing.sm),
                    style = typography.bodySmall,
                    color = scheme.onErrorContainer,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surface.copy(alpha = 0.92f),
            shape = shapes.input,
            border = BorderStroke(spacing.hairline, scheme.outlineVariant.copy(alpha = 0.65f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSending,
                    minLines = 3,
                    maxLines = 6,
                    shape = shapes.input,
                    placeholder = {
                        Text("发消息或输入 / 选择技能")
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (compactMode) {
                            TextButton(onClick = onOpenSettings, shape = shapes.pill) {
                                Text("设置")
                            }
                        }
                        TextButton(onClick = onNewConversation, shape = shapes.pill) {
                            Text("快速")
                        }
                        TextButton(onClick = {}, shape = shapes.pill) {
                            Text("帮我写作")
                        }
                        TextButton(onClick = {}, shape = shapes.pill) {
                            Text("编程")
                        }
                    }
                    Button(
                        onClick = onSendMessage,
                        enabled = state.draft.isNotBlank() && !state.isSending,
                        shape = shapes.pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Text(if (state.isSending) "发送中" else "发送")
                    }
                }
            }
        }

        if (!compactMode) {
            Text(
                text = "当前发送链路：${state.settings.currentProviderId.displayName} / ${state.settings.currentModelId}",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
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
                text = if (isUser) "You" else "Breeze",
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

private fun conversationSections(conversations: List<Conversation>): List<ConversationSection> {
    if (conversations.isEmpty()) {
        return emptyList()
    }

    return buildList {
        add(ConversationSection(title = "Today", items = conversations.take(2)))
        if (conversations.size > 2) {
            add(ConversationSection(title = "Yesterday", items = conversations.drop(2).take(2)))
        }
        if (conversations.size > 4) {
            add(ConversationSection(title = "Previous 7 Days", items = conversations.drop(4)))
        }
    }.filter { it.items.isNotEmpty() }
}

private fun conversationPreviewLabel(conversation: Conversation): String = "Model · ${conversation.modelId}"

internal fun previewChatUiState(): ChatUiState {
    val now = Clock.System.now()
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
                updatedAt = now,
            ),
            Conversation(
                id = "preview-conversation-3",
                title = "Design a REST API",
                modelId = "claude-3-5-sonnet",
                updatedAt = now,
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
        draft = "Type your message...",
        isSending = false,
        errorMessage = null,
        settings = BreezeSettingsSnapshot(),
    )
}
