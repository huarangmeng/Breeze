package com.hrm.breeze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
import com.hrm.breeze.ui.adaptive.ProvideWindowInfo
import com.hrm.breeze.ui.adaptive.WidthClass
import com.hrm.breeze.ui.theme.BreezeAppTheme
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
@Preview
fun App() {
    BreezeAppTheme {
        ProvideWindowInfo {
            BreezeDemoScreen()
        }
    }
}

@Composable
private fun BreezeDemoScreen() {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeContentPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
    ) {
        val layoutModifier = when (windowInfo.widthClass) {
            WidthClass.Compact -> Modifier.fillMaxWidth()
            WidthClass.Medium -> Modifier.fillMaxWidth()
            WidthClass.Expanded -> Modifier.fillMaxWidth(0.9f)
        }

        Column(
            modifier = layoutModifier,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            BreezeScreenHeader()

            when (windowInfo.paneMode) {
                PaneMode.Single -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        StatusChip("Compact", extra.info, extra.onInfo)
                        StatusChip("Touch", extra.success, extra.onSuccess)
                        StatusChip("Single Pane", extra.warning, extra.onWarning)
                    }
                    ChatPreviewCard()
                    ComposerCard()
                }

                PaneMode.ListDetail,
                PaneMode.Triple,
                -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        StatusChip(windowInfo.widthClass.name, extra.info, extra.onInfo)
                        StatusChip("Pointer", extra.success, extra.onSuccess)
                        StatusChip("ListDetail", extra.warning, extra.onWarning)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.Top,
                    ) {
                        AdaptivePane(
                            weight = 0.42f,
                            title = "Conversations",
                            subtitle = "Expanded 下展示列表面板，后续接 History / Chat 列表。",
                        ) {
                            ConversationListPreview()
                        }
                        AdaptivePane(
                            weight = 0.58f,
                            title = "Chat Detail",
                            subtitle = "详情面板保持消息预览和输入区，验证双栏布局切换。",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                                ChatPreviewCard()
                                ComposerCard()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreezeScreenHeader() {
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
            text = "Responsive preview: ${windowInfo.widthClass} / ${windowInfo.heightClass} / ${windowInfo.paneMode}",
            style = typography.bodyMedium,
            color = extra.textSecondary,
        )
    }
}

@Composable
private fun AdaptivePane(
    weight: Float,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Surface(
        modifier = Modifier.fillMaxWidth(weight),
        color = scheme.surface,
        shape = shapes.large,
        tonalElevation = spacing.hairline,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = {
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
            },
        )
    }
}

@Composable
private fun ConversationListPreview() {
    val extra = BreezeTheme.extendedColors
    val spacing = BreezeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        ConversationListItem(
            title = "Breeze Theme Review",
            summary = "Theme token baseline is ready, next step is responsive shell.",
            accent = extra.info,
        )
        ConversationListItem(
            title = "WindowInfo Integration",
            summary = "Desktop and web should reflect real width classes.",
            accent = extra.success,
        )
        ConversationListItem(
            title = "Navigation Planning",
            summary = "List / detail shell is reserved for M4.",
            accent = extra.warning,
        )
    }
}

@Composable
private fun ConversationListItem(
    title: String,
    summary: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(scheme.surfaceVariant)
            .padding(spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.xs)
                .clip(shapes.pill)
                .background(accent),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = title,
                style = typography.labelLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                style = typography.bodySmall,
                color = BreezeTheme.extendedColors.textSecondary,
            )
        }
    }
}

@Composable
private fun ChatPreviewCard() {
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        shape = shapes.large,
        tonalElevation = spacing.micro,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            MessageBubble(
                text = "帮我把骨架页切到真正的响应式窗口类。",
                background = extra.chatUserBubble,
                contentColor = extra.chatUserText,
                alignEnd = true,
            )
            MessageBubble(
                text = "现在 `LocalWindowInfo` 已经接通，Compact 是单栏，Expanded 是列表 + 详情双栏。",
                background = extra.chatAiBubble,
                contentColor = extra.chatAiText,
                alignEnd = false,
            )
            CodeSnippetCard(
                code = "LocalWindowInfo.current.widthClass\nProvideWindowInfo { App() }",
            )
        }
    }
}

@Composable
private fun ComposerCard() {
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = shapes.large,
        tonalElevation = spacing.hairline,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = "Composer",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.input)
                    .background(extra.chatInputBackground)
                    .border(spacing.hairline, extra.chatInputBorder, shapes.input)
                    .padding(horizontal = spacing.sm + spacing.micro, vertical = spacing.sm),
            ) {
                Text(
                    text = "Send a message...",
                    style = typography.bodyMedium,
                    color = extra.textTertiary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.micro),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                ) {
                    Text("发送")
                }
                Text(
                    text = "Errors keep `colorScheme.error` semantics.",
                    style = typography.bodySmall,
                    color = scheme.error,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
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

@Composable
private fun MessageBubble(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean,
) {
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .clip(if (alignEnd) shapes.bubbles.outgoing else shapes.bubbles.incoming)
                .background(background)
                .padding(horizontal = spacing.sm + spacing.micro, vertical = spacing.sm),
        ) {
            Text(
                text = text,
                style = typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun CodeSnippetCard(code: String) {
    val extra = BreezeTheme.extendedColors
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.codeBlock)
            .background(extra.codeBlockBackground)
            .border(spacing.hairline, extra.codeBlockBorder, shapes.codeBlock)
            .padding(spacing.sm + spacing.micro),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = "Theme tokens",
            style = typography.labelLarge,
            color = extra.textSecondary,
        )
        Text(
            text = code,
            style = typography.code,
            color = extra.textPrimary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(spacing.xs)
                    .clip(shapes.pill)
                    .background(MaterialTheme.colorScheme.error),
            )
            Spacer(modifier = Modifier.size(spacing.xs))
            Text(
                text = "Error and success colors stay semantic, not decorative.",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(spacing.micro))
    }
}
