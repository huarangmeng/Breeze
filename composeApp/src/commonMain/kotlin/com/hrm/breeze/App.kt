package com.hrm.breeze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.hrm.breeze.data.BreezeDataContainer
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.domain.model.Message
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
import com.hrm.breeze.ui.adaptive.ProvideWindowInfo
import com.hrm.breeze.ui.theme.BreezeAppTheme
import com.hrm.breeze.ui.theme.BreezeTheme
import kotlin.time.Clock

@Composable
fun App(
    previewMode: Boolean = false,
) {
    BreezeAppTheme {
        ProvideWindowInfo {
            if (previewMode) {
                BreezeChatShell(
                    state = previewChatState(),
                    previewMode = true,
                )
            } else {
                BreezeRuntimeApp()
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App(previewMode = true)
}

@Composable
private fun BreezeRuntimeApp() {
    val dataContainer = remember { BreezeDataContainer.create(useMockEchoService = true) }

    DisposableEffect(dataContainer) {
        onDispose {
            dataContainer.httpClient.close()
        }
    }

    BreezeChatShell(
        state = rememberBreezeChatState(dataContainer),
        previewMode = false,
    )
}

@Composable
private fun BreezeChatShell(
    state: BreezeChatState,
    previewMode: Boolean,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeContentPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        BreezeScreenHeader(
            state = state,
            previewMode = previewMode,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            StatusChip(windowInfo.widthClass.name, extra.info, extra.onInfo)
            StatusChip(windowInfo.paneMode.name, extra.success, extra.onSuccess)
            StatusChip(if (previewMode) "Preview" else "Mock Echo", extra.warning, extra.onWarning)
        }

        when (windowInfo.paneMode) {
            PaneMode.Single -> {
                PaneCard(
                    title = "Conversations",
                    subtitle = "当前会话直接来自 Room3，发送后会实时更新排序。",
                ) {
                    ConversationPaneContent(state)
                }
                PaneCard(
                    title = "Chat Detail",
                    subtitle = "消息发送链路使用真实 Repository，默认走仓库内 mock echo。",
                ) {
                    ChatPaneContent(state)
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
                        weight = 0.38f,
                        title = "Conversations",
                        subtitle = "左侧展示真实会话列表，支持新建对话。",
                    ) {
                        ConversationPaneContent(state)
                    }
                    AdaptivePane(
                        weight = 0.62f,
                        title = "Chat Detail",
                        subtitle = "右侧展示真实消息与输入区，验证 M3 闭环已接通。",
                    ) {
                        ChatPaneContent(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun BreezeScreenHeader(
    state: BreezeChatState,
    previewMode: Boolean,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs + spacing.micro)) {
        Text(
            text = "Breeze",
            style = typography.titleLarge,
            color = scheme.onBackground,
        )
        Text(
            text = "M3 ${if (previewMode) "preview" else "runtime"}: ${windowInfo.widthClass} / ${windowInfo.heightClass} / ${windowInfo.paneMode}",
            style = typography.bodyMedium,
            color = extra.textSecondary,
        )
        Text(
            text = "Endpoint: ${state.settings.echoEndpoint}  |  Model: ${state.settings.currentModelId}",
            style = typography.bodySmall,
            color = extra.textSecondary,
        )
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
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
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

@Composable
private fun ConversationPaneContent(
    state: BreezeChatState,
) {
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = state.onNewConversation) {
            Text("新对话")
        }
        Text(
            text = "共 ${state.conversations.size} 个会话",
            style = typography.bodySmall,
            color = BreezeTheme.extendedColors.textSecondary,
        )
    }

    if (state.conversations.isEmpty()) {
        Text(
            text = "还没有历史会话。发送第一条消息后，Room3 会自动创建会话与消息记录。",
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
            ConversationListItem(
                conversation = conversation,
                selected = conversation.id == state.activeConversationId,
                onClick = { state.onConversationSelected(conversation.id) },
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
private fun ChatPaneContent(
    state: BreezeChatState,
) {
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val extra = BreezeTheme.extendedColors
    val typography = BreezeTheme.typography

    if (state.errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(spacing.sm),
        ) {
            Text(
                text = state.errorMessage,
                style = typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }

    ChatMetaCard(state)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (state.messages.isEmpty()) {
            Text(
                text = "发送消息后，这里会显示写入 Room3 的用户消息和 mock echo 返回的助手消息。",
                style = typography.bodyMedium,
                color = extra.textSecondary,
            )
        } else {
            state.messages.forEach { message ->
                MessageBubble(message)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = shapes.large,
        tonalElevation = spacing.hairline,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = state.onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSending,
                minLines = 3,
                maxLines = 6,
                shape = shapes.input,
                label = { Text("输入消息") },
                placeholder = { Text("例如：帮我验证 M3 的持久化和网络链路") },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = state.onSendMessage,
                    enabled = state.draft.isNotBlank() && !state.isSending,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(if (state.isSending) "发送中..." else "发送")
                }
                Text(
                    text = "当前发送链路：Room3 -> Ktor MockEngine -> Room3",
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ChatMetaCard(
    state: BreezeChatState,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors
    val activeConversation = state.conversations.firstOrNull { it.id == state.activeConversationId }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Text(
            text = activeConversation?.title ?: "未发送的新对话",
            style = typography.titleMedium,
            color = scheme.onSurface,
        )
        Text(
            text = "ConversationId: ${state.activeConversationId}",
            style = typography.bodySmall,
            color = extra.textSecondary,
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
) {
    val extra = BreezeTheme.extendedColors
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val isUser = message.role == Message.Role.User

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(if (isUser) shapes.bubbles.outgoing else shapes.bubbles.incoming)
                .background(if (isUser) extra.chatUserBubble else extra.chatAiBubble)
                .padding(horizontal = spacing.sm + spacing.micro, vertical = spacing.sm),
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
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(containerColor)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
    ) {
        Text(
            text = text,
            style = typography.labelMedium,
            color = contentColor,
        )
    }
}

private fun previewChatState(): BreezeChatState {
    val now = Clock.System.now()
    val conversationId = "preview-conversation"
    return BreezeChatState(
        conversations =
            listOf(
                Conversation(
                    id = conversationId,
                    title = "M3 Preview",
                    modelId = "breeze-echo",
                    updatedAt = now,
                )
            ),
        messages =
            listOf(
                Message(
                    id = "preview-user",
                    conversationId = conversationId,
                    role = Message.Role.User,
                    content = "帮我确认 M3 是否已经接通。",
                    createdAt = now,
                ),
                Message(
                    id = "preview-assistant",
                    conversationId = conversationId,
                    role = Message.Role.Assistant,
                    content = "现在预览展示的就是接线后的最终形态，运行时会改用真实 Room3 + MockEngine。",
                    createdAt = now,
                ),
            ),
        activeConversationId = conversationId,
        draft = "例如：再发一条验证消息",
        isSending = false,
        errorMessage = null,
        settings = BreezeSettingsSnapshot(),
        onDraftChange = {},
        onConversationSelected = {},
        onNewConversation = {},
        onSendMessage = {},
    )
}
