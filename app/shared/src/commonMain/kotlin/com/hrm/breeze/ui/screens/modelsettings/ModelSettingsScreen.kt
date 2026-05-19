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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.hrm.breeze.domain.model.LlmProviderId
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
    onModelSelected: (String) -> Unit,
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
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.9f) }
    var maxTokens by remember { mutableIntStateOf(2048) }
    var contextLength by remember { mutableIntStateOf(8192) }
    var streamOutput by remember { mutableStateOf(true) }

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
                if (!embeddedMode) {
                    ModelSettingsHeader(
                        state = state,
                        previewMode = previewMode,
                        onBack = onBack,
                        showBackButton = showBackButton,
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
                            .padding(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.lg),
                    ) {
                        ModelSelectionSection(
                            state = state,
                            onModelSelected = onModelSelected,
                        )
                        ParameterRow(
                            title = stringResource(Res.string.temperature),
                            description = stringResource(Res.string.temperature_description),
                            valueLabel = formatTwoDecimals(temperature),
                            minLabel = "0",
                            maxLabel = "2",
                            sliderValue = temperature / 2f,
                            onSliderValueChange = { temperature = it * 2f },
                        )
                        ParameterRow(
                            title = stringResource(Res.string.top_p),
                            description = stringResource(Res.string.top_p_description),
                            valueLabel = formatTwoDecimals(topP),
                            minLabel = "0",
                            maxLabel = "1",
                            sliderValue = topP,
                            onSliderValueChange = { topP = it },
                        )
                        ParameterRow(
                            title = stringResource(Res.string.max_tokens),
                            description = stringResource(Res.string.max_tokens_description),
                            valueLabel = maxTokens.toString(),
                            minLabel = "256",
                            maxLabel = "8192",
                            sliderValue = ((maxTokens - 256).toFloat() / (8192 - 256).toFloat()).coerceIn(0f, 1f),
                            onSliderValueChange = {
                                maxTokens = (256 + it * (8192 - 256)).roundToInt()
                            },
                        )
                        ParameterRow(
                            title = stringResource(Res.string.context_window_length),
                            description = stringResource(Res.string.context_window_length_description),
                            valueLabel = contextLength.toString(),
                            minLabel = "1024",
                            maxLabel = "32768",
                            sliderValue = ((contextLength - 1024).toFloat() / (32768 - 1024).toFloat()).coerceIn(0f, 1f),
                            onSliderValueChange = {
                                contextLength = (1024 + it * (32768 - 1024)).roundToInt()
                            },
                        )
                        StreamOutputRow(
                            checked = streamOutput,
                            onCheckedChange = { streamOutput = it },
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

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            TextButton(
                onClick = onBack,
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
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = stringResource(Res.string.model_parameters),
                style = typography.titleLarge,
                color = scheme.onBackground,
            )
            Text(
                text = if (previewMode) stringResource(Res.string.preview_parameter_layout) else stringResource(Res.string.adjust_model_parameters),
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            Text(
                text = stringResource(Res.string.provider_label, state.providerId.displayName),
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
    }
}

@Composable
private fun ModelSelectionSection(
    state: ModelSettingsUiState,
    onModelSelected: (String) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            state.availableModels.take(3).forEach { model ->
                val selected = model.id == state.selectedModelId
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModelSelected(model.id) },
                    color = if (selected) scheme.primaryContainer else scheme.surface,
                    shape = shapes.medium,
                    border = BorderStroke(
                        spacing.hairline,
                        if (selected) scheme.primary else scheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        Text(
                            text = model.title,
                            style = typography.labelLarge,
                            color = if (selected) scheme.primary else scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(model.descriptionRes),
                            style = typography.bodySmall,
                            color = extra.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
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
private fun StreamOutputRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Text(
                    text = stringResource(Res.string.stream_output),
                    style = typography.labelLarge,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.stream_output_description),
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
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
        selectedModelId = "gpt-4.1-mini",
        providerId = LlmProviderId.OpenAI,
        availableModels = listOf(
            ModelOption("gpt-4.1-mini", "GPT-4.1 mini", Res.string.model_desc_gpt41_mini),
            ModelOption("gpt-4.1", "GPT-4.1", Res.string.model_desc_gpt41),
            ModelOption("o4-mini", "o4-mini", Res.string.model_desc_o4_mini),
        ),
        hasUnsavedChanges = true,
    )
