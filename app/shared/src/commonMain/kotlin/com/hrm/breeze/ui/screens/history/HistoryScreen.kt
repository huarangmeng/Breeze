package com.hrm.breeze.ui.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNewConversation: () -> Unit,
    onBackToChat: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
) {
    val windowInfo = LocalWindowInfo.current
    val spacing = BreezeTheme.spacing
    var query by remember { mutableStateOf("") }
    val filteredConversations = remember(state.conversations, query) {
        if (query.isBlank()) {
            state.conversations
        } else {
            state.conversations.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    if (windowInfo.paneMode == PaneMode.Single) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            CompactHistoryHeader(
                onBackToChat = onBackToChat,
                onNewConversation = onNewConversation,
                previewMode = previewMode,
            )
            HistoryListPanel(
                conversations = filteredConversations,
                activeConversationId = state.activeConversationId,
                query = query,
                onQueryChange = { query = it },
                onConversationSelected = onConversationSelected,
                modifier = Modifier.weight(1f),
            )
            HistoryDetailPanel(state = state)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        HistorySidebar(
            state = state,
            onNewConversation = onNewConversation,
            previewMode = previewMode,
            modifier = Modifier.weight(0.24f),
        )
        HistoryListPanel(
            conversations = filteredConversations,
            activeConversationId = state.activeConversationId,
            query = query,
            onQueryChange = { query = it },
            onConversationSelected = onConversationSelected,
            modifier = Modifier.weight(0.76f),
        )
    }
}

@Composable
private fun CompactHistoryHeader(
    onBackToChat: () -> Unit,
    onNewConversation: () -> Unit,
    previewMode: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        color = scheme.surfaceVariant.copy(alpha = 0.46f),
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
            Button(
                onClick = onBackToChat,
                shape = shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.surface,
                    contentColor = scheme.onSurface,
                ),
            ) { Text("返回") }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "历史列表",
                    style = typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = if (previewMode) "Preview history route" else "查看本地保存的历史对话",
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
            Button(
                onClick = onNewConversation,
                shape = shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.primary,
                ),
            ) { Text("新建") }
        }
    }
}

@Composable
private fun HistorySidebar(
    state: HistoryUiState,
    onNewConversation: () -> Unit,
    previewMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = scheme.surface,
        shape = shapes.large,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Text(
                    text = "Chat History",
                    style = typography.titleLarge,
                    color = scheme.onSurface,
                )
                Text(
                    text = if (previewMode) "Preview layout" else "历史会话保存在本地设备中",
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
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
                Text("+  New Chat")
            }

            Surface(
                color = scheme.surfaceVariant,
                shape = shapes.medium,
                border = BorderStroke(spacing.hairline, scheme.outlineVariant),
            ) {
                Text(
                    text = "Your conversations are stored locally on this device.",
                    modifier = Modifier.padding(spacing.md),
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }

            HistoryDetailPanel(
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HistoryListPanel(
    conversations: List<Conversation>,
    activeConversationId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onConversationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = scheme.surface,
        shape = shapes.large,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.input,
                placeholder = {
                    Text("Search conversations...")
                },
            )

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "没有匹配的会话记录。",
                        style = typography.bodyMedium,
                        color = extra.textSecondary,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    conversations.forEachIndexed { index, conversation ->
                        HistoryConversationItem(
                            conversation = conversation,
                            selected = conversation.id == activeConversationId,
                            timeLabel = historyTimeLabel(index),
                            onClick = { onConversationSelected(conversation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryConversationItem(
    conversation: Conversation,
    selected: Boolean,
    timeLabel: String,
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
            .clickable(onClick = onClick),
        color = scheme.surface,
        shape = shapes.medium,
        border = BorderStroke(
            spacing.hairline,
            if (selected) scheme.primary.copy(alpha = 0.3f) else scheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
                shape = shapes.medium,
            ) {
                Text(
                    text = "[]",
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm),
                    style = typography.labelMedium,
                    color = if (selected) scheme.primary else extra.textSecondary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.title,
                        style = typography.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = timeLabel,
                        style = typography.bodySmall,
                        color = extra.textSecondary,
                    )
                }
                Text(
                    text = "Model: ${conversation.modelId}",
                    style = typography.bodyMedium,
                    color = extra.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "...",
                style = typography.labelLarge,
                color = extra.textSecondary,
            )
        }
    }
}

@Composable
private fun HistoryDetailPanel(
    state: HistoryUiState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        modifier = modifier,
        color = scheme.surfaceVariant.copy(alpha = 0.7f),
        shape = shapes.medium,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = "Local Summary",
                style = typography.labelLarge,
                color = scheme.onSurface,
            )
            Text(
                text = "会话 ${state.conversations.size} 个，当前消息 ${state.messages.size} 条",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            Text(
                text = state.latestMessagePreview ?: "选择会话后，这里会显示最近一条消息摘要。",
                style = typography.bodySmall,
                color = extra.textSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun historyTimeLabel(index: Int): String = when (index) {
    0 -> "10:30 AM"
    1 -> "Yesterday"
    2 -> "2 days ago"
    3 -> "3 days ago"
    4 -> "5 days ago"
    else -> "1 week ago"
}

internal fun previewHistoryUiState(): HistoryUiState {
    val now = Clock.System.now()
    val conversationId = "history-preview"
    return HistoryUiState(
        conversations = listOf(
            Conversation(
                id = conversationId,
                title = "Explain quantum computing",
                modelId = "claude-3-5-sonnet",
                updatedAt = now,
            ),
            Conversation(
                id = "history-preview-2",
                title = "Python function help",
                modelId = "claude-3-5-sonnet",
                updatedAt = now,
            ),
            Conversation(
                id = "history-preview-3",
                title = "Design a REST API",
                modelId = "gpt-4.1-mini",
                updatedAt = now,
            ),
            Conversation(
                id = "history-preview-4",
                title = "Create a marketing strategy",
                modelId = "claude-opus-4-1",
                updatedAt = now,
            ),
        ),
        activeConversationId = conversationId,
        messages = listOf(
            Message(
                id = "history-preview-user",
                conversationId = conversationId,
                role = Message.Role.User,
                content = "帮我把历史页还原成设计稿的桌面布局。",
                createdAt = now,
            ),
            Message(
                id = "history-preview-assistant",
                conversationId = conversationId,
                role = Message.Role.Assistant,
                content = "历史页现在使用左侧信息栏和右侧大列表布局，并支持本地搜索筛选。",
                createdAt = now,
            ),
        ),
        latestMessagePreview = "历史页现在使用左侧信息栏和右侧大列表布局，并支持本地搜索筛选。",
    )
}
