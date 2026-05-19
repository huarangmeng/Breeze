package com.hrm.breeze.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrm.breeze.generated.resources.Res
import com.hrm.breeze.generated.resources.current_model
import com.hrm.breeze.generated.resources.model_not_configured
import com.hrm.breeze.generated.resources.model_settings_menu
import com.hrm.breeze.ui.theme.BreezeTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CompactChatHeader(
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
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val switcherMaxWidth = (maxWidth - 42.dp - 42.dp - spacing.xs - spacing.xs - spacing.xs - spacing.xs)
                .coerceAtLeast(116.dp)

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
                    modifier = Modifier.widthIn(max = switcherMaxWidth),
                    compact = true,
                )
                HeaderActionButton(label = "+", onClick = onNewConversation)
            }
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
internal fun ModelSwitcher(
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
    val selectedTitle = state.settings.currentModelId.ifBlank { stringResource(Res.string.model_not_configured) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .widthIn(min = 116.dp)
                .heightIn(min = 34.dp)
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
