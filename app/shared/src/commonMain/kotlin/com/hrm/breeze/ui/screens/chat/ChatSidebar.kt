package com.hrm.breeze.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrm.breeze.domain.model.Conversation
import com.hrm.breeze.generated.resources.Res
import com.hrm.breeze.generated.resources.api_config
import com.hrm.breeze.generated.resources.current_chat
import com.hrm.breeze.generated.resources.earlier
import com.hrm.breeze.generated.resources.empty_history_hint
import com.hrm.breeze.generated.resources.history_conversations
import com.hrm.breeze.generated.resources.model_label
import com.hrm.breeze.generated.resources.model_settings
import com.hrm.breeze.generated.resources.more_features
import com.hrm.breeze.generated.resources.new_chat
import com.hrm.breeze.generated.resources.on_device_models
import com.hrm.breeze.generated.resources.recent_30_days
import com.hrm.breeze.generated.resources.recent_7_days
import com.hrm.breeze.generated.resources.today
import com.hrm.breeze.generated.resources.yesterday
import com.hrm.breeze.ui.navigation.ApiConfig
import com.hrm.breeze.ui.navigation.Chat
import com.hrm.breeze.ui.navigation.ModelSettings
import com.hrm.breeze.ui.navigation.OnDeviceModels
import com.hrm.breeze.ui.theme.BreezeTheme
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DesktopSidebar(
    state: ChatUiState,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onOpenOnDeviceModels: () -> Unit,
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
                label = stringResource(Res.string.on_device_models),
                selected = selectedDesktopRoute == OnDeviceModels.routePattern,
                onClick = onOpenOnDeviceModels,
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

const val DAY_MILLIS = 86_400_000L
