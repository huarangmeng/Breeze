package com.hrm.breeze.ui.screens.modelsettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hrm.breeze.generated.resources.*
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.theme.BreezeTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun ModelSettingsScreen(
    state: ModelSettingsUiState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onModelIdChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onContextWindowChange: (Int) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
    embeddedMode: Boolean = false,
    showBackButton: Boolean = true,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing

    val contentModifier = if (embeddedMode) {
        modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxSize()
            .padding(spacing.lg)
    }

    Box(
        modifier = contentModifier,
        contentAlignment = if (embeddedMode) Alignment.TopStart else Alignment.Center,
    ) {
        val surfaceModifier = if (embeddedMode) {
            Modifier.fillMaxSize()
        } else {
            Modifier.widthIn(max = windowInfo.contentMaxWidth + windowInfo.contentMaxWidth / 2).fillMaxSize()
        }

        Surface(
            modifier = surfaceModifier,
            color = scheme.background,
            shape = shapes.large,
            border = if (embeddedMode) null else BorderStroke(spacing.hairline, scheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (embeddedMode) spacing.lg else spacing.xl)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                ModelSettingsHeader(
                    state = state,
                    previewMode = previewMode,
                    onBack = onBack,
                    showBackButton = showBackButton && !embeddedMode,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.surface,
                    shape = shapes.large,
                    border = BorderStroke(spacing.hairline, scheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.lg),
                    ) {
                        ModelSelectionSection(
                            state = state,
                            onModelIdChange = onModelIdChange,
                        )
                        ParameterRow(
                            title = stringResource(Res.string.temperature),
                            description = stringResource(Res.string.temperature_description),
                            valueLabel = formatTwoDecimals(state.temperature),
                            minLabel = "0",
                            maxLabel = "2",
                            sliderValue = state.temperature / 2f,
                            onSliderValueChange = { onTemperatureChange(it * 2f) },
                        )
                        ParameterRow(
                            title = stringResource(Res.string.top_p),
                            description = stringResource(Res.string.top_p_description),
                            valueLabel = formatTwoDecimals(state.topP),
                            minLabel = "0",
                            maxLabel = "1",
                            sliderValue = state.topP,
                            onSliderValueChange = onTopPChange,
                        )
                        ParameterRow(
                            title = stringResource(Res.string.max_tokens),
                            description = stringResource(Res.string.max_tokens_description),
                            valueLabel = state.maxTokens.toString(),
                            minLabel = "256",
                            maxLabel = "8192",
                            sliderValue = ((state.maxTokens - 256).toFloat() / (8192 - 256).toFloat()).coerceIn(0f, 1f),
                            onSliderValueChange = { onMaxTokensChange((256 + it * (8192 - 256)).roundToInt()) },
                        )
                        ParameterRow(
                            title = stringResource(Res.string.context_window_length),
                            description = stringResource(Res.string.context_window_length_description),
                            valueLabel = state.contextWindow.toString(),
                            minLabel = "1024",
                            maxLabel = "32768",
                            sliderValue = ((state.contextWindow - 1024).toFloat() / (32768 - 1024).toFloat()).coerceIn(0f, 1f),
                            onSliderValueChange = { onContextWindowChange((1024 + it * (32768 - 1024)).roundToInt()) },
                        )
                        ModelSettingsActions(
                            state = state,
                            onReset = onReset,
                            onSave = onSave,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSettingsHeader(
    state: ModelSettingsUiState,
    previewMode: Boolean,
    onBack: () -> Unit,
    showBackButton: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (showBackButton) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                shape = shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = scheme.surface,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Text("<")
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = stringResource(Res.string.model_parameters),
                style = typography.titleLarge,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (previewMode) stringResource(Res.string.preview_parameter_layout) else stringResource(Res.string.adjust_model_parameters),
                style = typography.bodySmall,
                color = extra.textSecondary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    Res.string.provider_label,
                    state.providerDisplayName.ifBlank { stringResource(Res.string.model_not_configured) },
                ),
                style = typography.bodySmall,
                color = extra.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModelSelectionSection(
    state: ModelSettingsUiState,
    onModelIdChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.model),
            style = typography.labelLarge,
            color = scheme.onSurface,
        )
        OutlinedTextField(
            value = state.selectedModelId,
            onValueChange = onModelIdChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            shape = shapes.input,
            label = { Text(stringResource(Res.string.model_id)) },
            placeholder = { Text("deepseek/deepseek-v4-flash:free") },
            supportingText = {
                Text(
                    text = stringResource(Res.string.model_id_description),
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            },
        )
    }
}

@Composable
private fun ParameterRow(
    title: String,
    description: String,
    valueLabel: String,
    minLabel: String,
    maxLabel: String,
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        color = scheme.surface,
        shape = shapes.medium,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(0.55f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Text(
                    text = title,
                    style = typography.labelLarge,
                    color = scheme.onSurface,
                )
                Text(
                    text = description,
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
            Column(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = minLabel,
                        style = typography.bodySmall,
                        color = extra.textSecondary,
                    )
                    Surface(
                        color = scheme.surfaceVariant,
                        shape = shapes.medium,
                    ) {
                        Text(
                            text = valueLabel,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                            style = typography.bodySmall,
                            color = scheme.onSurface,
                        )
                    }
                }
                Slider(
                    value = sliderValue,
                    onValueChange = onSliderValueChange,
                )
                Text(
                    text = maxLabel,
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
        }
    }
}

private fun formatTwoDecimals(value: Float): String {
    val scaled = (value * 100).roundToInt()
    val whole = scaled / 100
    val fraction = (scaled % 100).toString().padStart(2, '0')
    return "$whole.$fraction"
}

@Composable
private fun ModelSettingsActions(
    state: ModelSettingsUiState,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        state.statusMessage?.let { statusMessage ->
            Text(
                text = stringResource(statusMessage),
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onReset,
                enabled = state.hasUnsavedChanges && !state.isSaving,
                shape = shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = scheme.surfaceVariant,
                    contentColor = scheme.primary,
                ),
            ) {
                Text(stringResource(Res.string.reset_to_default))
            }
            Button(
                onClick = onSave,
                enabled = state.hasUnsavedChanges && !state.isSaving,
                shape = shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Text(if (state.isSaving) stringResource(Res.string.saving) else stringResource(Res.string.apply_settings))
            }
        }
    }
}

internal fun previewModelSettingsUiState(): ModelSettingsUiState =
    ModelSettingsUiState(
        providerDisplayName = "OpenAI",
        selectedModelId = "",
        hasUnsavedChanges = true,
    )
