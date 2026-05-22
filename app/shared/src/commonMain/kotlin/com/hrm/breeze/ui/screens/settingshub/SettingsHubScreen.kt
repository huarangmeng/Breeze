package com.hrm.breeze.ui.screens.settingshub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.breeze.generated.resources.*
import com.hrm.breeze.i18n.BreezeLanguagePreference
import com.hrm.breeze.i18n.languagePreferenceLabelRes
import com.hrm.breeze.ui.components.BreezePageHeader
import com.hrm.breeze.ui.theme.BreezeTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsHubScreen(
    selectedRoute: String,
    languagePreference: String,
    onLanguagePreferenceSelected: (String) -> Unit,
    onBackToChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onOpenOnDeviceModels: () -> Unit,
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
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Surface(
            color = scheme.surfaceVariant.copy(alpha = 0.48f),
            shape = shapes.large,
            border = BorderStroke(spacing.hairline, scheme.outlineVariant),
        ) {
            BreezePageHeader(
                title = stringResource(Res.string.settings),
                subtitle = stringResource(Res.string.settings_subtitle),
                showBackButton = true,
                onBack = onBackToChat,
                modifier = Modifier.padding(spacing.md),
            )
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
                    title = stringResource(Res.string.current_chat),
                    description = stringResource(Res.string.current_chat_description),
                    selected = selectedRoute == "chat",
                    onClick = onBackToChat,
                )
                SettingsHubItem(
                    title = stringResource(Res.string.history_list),
                    description = stringResource(Res.string.history_description),
                    selected = selectedRoute == "history",
                    onClick = onOpenHistory,
                )
                SettingsHubItem(
                    title = stringResource(Res.string.api_config),
                    description = stringResource(Res.string.api_config_description),
                    selected = selectedRoute == "api-config",
                    onClick = onOpenApiConfig,
                )
                SettingsHubItem(
                    title = stringResource(Res.string.model_settings),
                    description = stringResource(Res.string.model_settings_description),
                    selected = selectedRoute == "model-settings",
                    onClick = onOpenModelSettings,
                )
                SettingsHubItem(
                    title = stringResource(Res.string.on_device_models),
                    description = stringResource(Res.string.on_device_models_description),
                    selected = selectedRoute == "on-device-models",
                    onClick = onOpenOnDeviceModels,
                )
                SettingsHubItem(
                    title = stringResource(Res.string.more_features),
                    description = stringResource(Res.string.more_features_description),
                    selected = false,
                    onClick = {},
                )
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
                Text(
                    text = stringResource(Res.string.app_language),
                    style = typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.app_language_description),
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
                BreezeLanguagePreference.entries.forEach { preference ->
                    SettingsHubItem(
                        title = stringResource(languagePreferenceLabelRes(preference)),
                        description = if (preference == BreezeLanguagePreference.System) {
                            stringResource(Res.string.follow_system)
                        } else {
                            preference.storageValue
                        },
                        selected = BreezeLanguagePreference.fromStorageValue(languagePreference) == preference,
                        onClick = { onLanguagePreferenceSelected(preference.storageValue) },
                    )
                }
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
