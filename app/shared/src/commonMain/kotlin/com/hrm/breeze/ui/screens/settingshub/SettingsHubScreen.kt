package com.hrm.breeze.ui.screens.settingshub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun SettingsHubScreen(
    selectedRoute: String,
    onBackToChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
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
                TextButton(onClick = onBackToChat, shape = shapes.pill) {
                    Text("返回")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "设置",
                        style = typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "选择要进入的功能页",
                        style = typography.bodySmall,
                        color = extra.textSecondary,
                    )
                }
                Box(modifier = Modifier.padding(horizontal = spacing.sm))
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surface,
            shape = shapes.large,
            border = BorderStroke(spacing.hairline, scheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SettingsHubItem(
                    title = "当前对话",
                    description = "返回主对话工作区",
                    selected = selectedRoute == "chat",
                    onClick = onBackToChat,
                )
                SettingsHubItem(
                    title = "历史列表",
                    description = "进入历史路由并查看会话列表",
                    selected = selectedRoute == "history",
                    onClick = onOpenHistory,
                )
                SettingsHubItem(
                    title = "API 配置",
                    description = "打开 Provider 与鉴权配置页",
                    selected = selectedRoute == "api-config",
                    onClick = onOpenApiConfig,
                )
                SettingsHubItem(
                    title = "模型设置",
                    description = "打开模型参数与输出偏好页",
                    selected = selectedRoute == "model-settings",
                    onClick = onOpenModelSettings,
                )
                SettingsHubItem(
                    title = "更多能力",
                    description = "后续用于承载更多 tab",
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SettingsHubItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val containerColor = when {
        selected -> scheme.primaryContainer
        hovered -> scheme.surfaceVariant.copy(alpha = 0.46f)
        else -> scheme.surfaceVariant.copy(alpha = 0.28f)
    }
    val borderColor = when {
        selected -> scheme.primary.copy(alpha = 0.35f)
        hovered -> scheme.outline.copy(alpha = 0.22f)
        else -> scheme.outlineVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        color = containerColor,
        shape = shapes.medium,
        border = BorderStroke(
            spacing.hairline,
            borderColor,
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = title,
                style = typography.titleMedium,
                color = if (selected) scheme.primary else scheme.onSurface,
            )
            Text(
                text = description,
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
    }
}
