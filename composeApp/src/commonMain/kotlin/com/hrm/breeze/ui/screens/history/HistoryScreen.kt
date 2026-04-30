package com.hrm.breeze.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
import com.hrm.breeze.ui.theme.BreezeTheme
import kotlin.time.Clock

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onConversationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeContentPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        HistoryScreenHeader(
            state = state,
            previewMode = previewMode,
        )

        when (windowInfo.paneMode) {
            PaneMode.Single -> {
                PaneCard(
                    title = "History List",
                    subtitle = "按更新时间展示真实会话，选择后在下方查看详情摘要。",
                ) {
                    HistoryConversationList(
                        state = state,
                        onConversationSelected = onConversationSelected,
                    )
                }
                PaneCard(
                    title = "History Detail",
                    subtitle = "仅展示历史详情骨架，不包含输入与发送操作。",
                ) {
                    HistoryDetailPane(state)
                }
            }

            PaneMode.ListDetail,
            PaneMode.Triple,
            -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    AdaptivePane(
                        weight = 0.4f,
                        title = "History List",
                        subtitle = "左侧展示真实会话历史。",
                    ) {
                        HistoryConversationList(
                            state = state,
                            onConversationSelected = onConversationSelected,
                        )
                    }
                    AdaptivePane(
                        weight = 0.6f,
                        title = "History Detail",
                        subtitle = "右侧展示选中会话的消息摘要。",
                    ) {
                        HistoryDetailPane(state)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surface,
            shape = BreezeTheme.shapes.large,
            tonalElevation = spacing.hairline,
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Text(
                    text = "M4-4 骨架已接通：会话列表与详情摘要来自真实仓库流。",
                    style = BreezeTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            Text(
                text = "M4-5/M4-6 会继续补 API 与模型设置页，M4-7 再统一根布局。",
                style = BreezeTheme.typography.bodySmall,
                color = extra.textSecondary,
            )
            }
        }
    }
}

@Composable
private fun HistoryScreenHeader(
    state: HistoryUiState,
    previewMode: Boolean,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs + spacing.micro)) {
        Text(
            text = "History",
            style = typography.titleLarge,
            color = scheme.onBackground,
        )
        Text(
            text = "M4 ${if (previewMode) "preview" else "runtime"}: ${windowInfo.widthClass} / ${windowInfo.heightClass} / ${windowInfo.paneMode}",
            style = typography.bodyMedium,
            color = extra.textSecondary,
        )
        Text(
            text = "会话 ${state.conversations.size} 个，当前消息 ${state.messages.size} 条",
            style = typography.bodySmall,
            color = extra.textSecondary,
        )
    }
}

@Composable
private fun HistoryConversationList(
    state: HistoryUiState,
    onConversationSelected: (String) -> Unit,
) {
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    if (state.conversations.isEmpty()) {
        Text(
            text = "暂无历史会话。先到 Chat 页面发送消息后，这里会自动出现历史记录。",
            style = typography.bodyMedium,
            color = BreezeTheme.extendedColors.textSecondary,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        state.conversations.forEach { conversation ->
            ConversationItem(
                conversation = conversation,
                selected = conversation.id == state.activeConversationId,
                onClick = { onConversationSelected(conversation.id) },
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.xs)
                .clip(shapes.pill)
                .background(if (selected) scheme.primary else extra.info),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = conversation.title,
                style = typography.labelLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Model: ${conversation.modelId}",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
    }
}

@Composable
private fun HistoryDetailPane(
    state: HistoryUiState,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    if (state.activeConversationId == null) {
        Text(
            text = "请选择会话查看详情。",
            style = typography.bodyMedium,
            color = extra.textSecondary,
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surfaceVariant,
        shape = shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "ConversationId: ${state.activeConversationId}",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            Text(
                text = "最近一条消息：${state.latestMessagePreview ?: "暂无消息"}",
                style = typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )

            if (state.messages.isEmpty()) {
                Text(
                    text = "当前会话暂无消息。",
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
                return@Column
            }

            state.messages.takeLast(6).forEach { message ->
                MessagePreviewItem(message)
            }
        }
    }
}

@Composable
private fun MessagePreviewItem(
    message: Message,
) {
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors
    val isUser = message.role == Message.Role.User

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isUser) extra.chatUserBubble else extra.chatAiBubble,
        shape = if (isUser) shapes.bubbles.outgoing else shapes.bubbles.incoming,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.sm + spacing.micro, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = if (isUser) "User" else "Assistant",
                style = typography.labelMedium,
                color = if (isUser) extra.chatUserText else extra.chatAiText,
            )
            Text(
                text = message.content,
                style = typography.bodyMedium,
                color = if (isUser) extra.chatUserText else extra.chatAiText,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RowScope.AdaptivePane(
    weight: Float,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    PaneCard(
        modifier = Modifier.weight(weight),
        title = title,
        subtitle = subtitle,
        content = content,
    )
}

@Composable
private fun PaneCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = shapes.large,
        tonalElevation = spacing.hairline,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = title,
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = subtitle,
                style = typography.bodySmall,
                color = BreezeTheme.extendedColors.textSecondary,
            )
            content()
        }
    }
}

internal fun previewHistoryUiState(): HistoryUiState {
    val now = Clock.System.now()
    val conversationId = "history-preview"
    return HistoryUiState(
        conversations =
            listOf(
                Conversation(
                    id = conversationId,
                    title = "M4 History Preview",
                    modelId = "breeze-echo",
                    updatedAt = now,
                ),
                Conversation(
                    id = "history-preview-2",
                    title = "另一个真实会话占位",
                    modelId = "breeze-echo",
                    updatedAt = now,
                ),
            ),
        activeConversationId = conversationId,
        messages =
            listOf(
                Message(
                    id = "history-preview-user",
                    conversationId = conversationId,
                    role = Message.Role.User,
                    content = "帮我把历史页骨架接到真实仓库流。",
                    createdAt = now,
                ),
                Message(
                    id = "history-preview-assistant",
                    conversationId = conversationId,
                    role = Message.Role.Assistant,
                    content = "History 页面现在会展示会话列表和选中会话的消息摘要。",
                    createdAt = now,
                ),
            ),
        latestMessagePreview = "History 页面现在会展示会话列表和选中会话的消息摘要。",
    )
}
