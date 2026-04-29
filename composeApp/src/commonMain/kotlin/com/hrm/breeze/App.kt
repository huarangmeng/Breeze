package com.hrm.breeze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hrm.breeze.ui.theme.BreezeAppTheme
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
@Preview
fun App() {
    BreezeAppTheme {
        BreezeDemoScreen()
    }
}

@Composable
private fun BreezeDemoScreen() {
    val scheme = MaterialTheme.colorScheme
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeContentPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Breeze",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Compose Multiplatform chat palette preview",
                style = MaterialTheme.typography.bodyMedium,
                color = extra.textSecondary,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Connected", extra.success, extra.onSuccess)
            StatusChip("Context", extra.info, extra.onInfo)
            StatusChip("Token Alert", extra.warning, extra.onWarning)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surface,
            contentColor = scheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MessageBubble(
                    text = "帮我把颜色系统改成更适合长时间阅读的 Breeze 风格。",
                    background = extra.chatUserBubble,
                    contentColor = extra.chatUserText,
                    alignEnd = true,
                )
                MessageBubble(
                    text = "已切换到与 Material 3 对齐的日夜间色板，主色固定为浅蓝，聊天气泡和语义提示色分层管理。",
                    background = extra.chatAiBubble,
                    contentColor = extra.chatAiText,
                    alignEnd = false,
                )
                CodeSnippetCard(
                    code = "MaterialTheme.colorScheme.primary\nBreezeTheme.extendedColors.chatUserBubble",
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Composer",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(extra.chatInputBackground)
                        .border(1.dp, extra.chatInputBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Send a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extra.textTertiary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                }
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (alignEnd) 18.dp else 6.dp,
                        bottomEnd = if (alignEnd) 6.dp else 18.dp,
                    ),
                )
                .background(background)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun CodeSnippetCard(code: String) {
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(extra.codeBlockBackground)
            .border(1.dp, extra.codeBlockBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Theme tokens",
            style = MaterialTheme.typography.labelLarge,
            color = extra.textSecondary,
        )
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium,
            color = extra.textPrimary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.error),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Error and success colors stay semantic, not decorative.",
                style = MaterialTheme.typography.bodySmall,
                color = extra.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
