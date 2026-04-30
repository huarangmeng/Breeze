package com.hrm.breeze.ui.screens.modelsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun ModelSettingsScreen(
    state: ModelSettingsUiState,
    onModelSelected: (String) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    previewMode: Boolean = false,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeContentPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = windowInfo.contentMaxWidth)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ModelSettingsHeader(
                state = state,
                previewMode = previewMode,
            )
            ModelSelectionSection(
                state = state,
                onModelSelected = onModelSelected,
            )
            ParameterPlaceholderSection()
            PresetPlaceholderSection()
            ModelSettingsActions(
                state = state,
                onReset = onReset,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun ModelSettingsHeader(
    state: ModelSettingsUiState,
    previewMode: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs + spacing.micro)) {
        Text(
            text = "Model Settings",
            style = typography.titleLarge,
            color = scheme.onBackground,
        )
        Text(
            text = if (previewMode) "Preview: 模型选择骨架" else "Runtime: 模型选择骨架",
            style = typography.bodyMedium,
            color = extra.textSecondary,
        )
        Text(
            text = "当前 Provider: ${state.providerId.displayName}",
            style = typography.bodySmall,
            color = extra.textSecondary,
        )
    }
}

@Composable
private fun ModelSelectionSection(
    state: ModelSettingsUiState,
    onModelSelected: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

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
                text = "Model",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "根据当前 Provider 展示推荐模型列表；当前阶段只持久化模型 ID。",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            state.availableModels.forEach { model ->
                val selected = model.id == state.selectedModelId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .clickable { onModelSelected(model.id) },
                    color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
                    shape = shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        Text(
                            text = model.title,
                            style = typography.labelLarge,
                            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                        )
                        Text(
                            text = model.description,
                            style = typography.bodySmall,
                            color = if (selected) scheme.onPrimaryContainer else extra.textSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Model ID: ${model.id}",
                            style = typography.bodySmall,
                            color = if (selected) scheme.onPrimaryContainer else extra.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterPlaceholderSection() {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

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
                text = "Generation Parameters",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            listOf(
                "temperature: M5-8 再接持久化与输入控件",
                "top_p: M5-8 再接持久化与输入控件",
                "max_tokens: M5-8 再接持久化与输入控件",
            ).forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.surfaceVariant,
                    shape = shapes.medium,
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(spacing.sm),
                        style = typography.bodySmall,
                        color = extra.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetPlaceholderSection() {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

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
                text = "Presets",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "M4-6 先落骨架，预设管理和参数方案在 M5-8 再接真实持久化。",
                style = typography.bodySmall,
                color = extra.textSecondary,
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
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

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
            if (state.statusMessage != null) {
                Text(
                    text = state.statusMessage,
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
            Text(
                text = if (state.hasUnsavedChanges) "存在未保存的模型选择" else "当前模型已同步到设置层",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            Button(
                onClick = onSave,
                enabled = state.hasUnsavedChanges && !state.isSaving,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
            ) {
                Text(if (state.isSaving) "保存中..." else "保存模型")
            }
            TextButton(
                onClick = onReset,
                enabled = state.hasUnsavedChanges && !state.isSaving,
            ) {
                Text("恢复最近保存")
            }
        }
    }
}

internal fun previewModelSettingsUiState(): ModelSettingsUiState =
    ModelSettingsUiState(
        selectedModelId = "gpt-4.1-mini",
        providerId = com.hrm.breeze.domain.model.LlmProviderId.OpenAI,
        availableModels = listOf(
            ModelOption("gpt-4.1-mini", "GPT-4.1 mini", "预览占位模型"),
            ModelOption("gpt-4.1", "GPT-4.1", "预览占位模型"),
        ),
        hasUnsavedChanges = true,
        statusMessage = "切换 Provider 后，这里的模型列表会跟随更新。",
    )
