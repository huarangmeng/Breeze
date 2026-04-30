package com.hrm.breeze.ui.screens.apiconfig

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun ApiConfigScreen(
    state: ApiConfigUiState,
    onProviderSelected: (LlmProviderId) -> Unit,
    onEndpointChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
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
            ApiConfigHeader(
                state = state,
                previewMode = previewMode,
            )
            ApiConfigProviderSection(
                state = state,
                onProviderSelected = onProviderSelected,
            )
            ApiConfigCredentialsSection(
                state = state,
                onEndpointChange = onEndpointChange,
                onApiTokenChange = onApiTokenChange,
            )
            ApiConfigActions(
                state = state,
                onReset = onReset,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun ApiConfigHeader(
    state: ApiConfigUiState,
    previewMode: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs + spacing.micro)) {
        Text(
            text = "API Config",
            style = typography.titleLarge,
            color = scheme.onBackground,
        )
        Text(
            text = if (previewMode) "Preview: Provider 配置与鉴权表单" else "Runtime: Provider 配置与鉴权表单",
            style = typography.bodyMedium,
            color = extra.textSecondary,
        )
        Text(
            text = "当前 Provider: ${state.selectedProviderId.displayName}",
            style = typography.bodySmall,
            color = extra.textSecondary,
        )
    }
}

@Composable
private fun ApiConfigProviderSection(
    state: ApiConfigUiState,
    onProviderSelected: (LlmProviderId) -> Unit,
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
                text = "Provider",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "Provider 选择会写入共享设置层；当前阶段 Hosted Provider 先走兼容桥接，后续再接真实 API。",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            state.availableProviders.forEach { providerId ->
                val selected = providerId == state.selectedProviderId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.medium)
                        .clickable { onProviderSelected(providerId) },
                    color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
                    shape = shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        Text(
                            text = providerId.displayName,
                            style = typography.labelLarge,
                            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                        )
                        Text(
                            text = providerDescription(providerId),
                            style = typography.bodySmall,
                            color = if (selected) scheme.onPrimaryContainer else extra.textSecondary,
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = scheme.surfaceVariant,
                shape = shapes.medium,
            ) {
                Text(
                    text = state.providerNotice,
                    modifier = Modifier.padding(spacing.sm),
                    style = typography.bodySmall,
                    color = extra.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ApiConfigCredentialsSection(
    state: ApiConfigUiState,
    onEndpointChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
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
                text = "Credentials",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "当前先统一持久化 Base URL 和 API Key，后续再细化为 provider 专属字段。",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            OutlinedTextField(
                value = state.endpoint,
                onValueChange = onEndpointChange,
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.input,
                label = { Text("Endpoint / Base URL") },
                placeholder = { Text("例如：https://api.openai.com/v1") },
                enabled = !state.isSaving,
                minLines = 2,
            )
            OutlinedTextField(
                value = state.apiToken,
                onValueChange = onApiTokenChange,
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.input,
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                enabled = !state.isSaving,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
    }
}

@Composable
private fun ApiConfigActions(
    state: ApiConfigUiState,
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
                text = if (state.hasUnsavedChanges) "存在未保存修改" else "当前配置已同步到持久化设置层",
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
                Text(if (state.isSaving) "保存中..." else "保存配置")
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

private fun providerDescription(providerId: LlmProviderId): String = when (providerId) {
    LlmProviderId.Local -> "继续使用仓库内 mock/local 兼容链路。"
    LlmProviderId.OpenAI -> "预留 OpenAI provider 选择入口，真实适配器稍后接入。"
    LlmProviderId.Anthropic -> "预留 Anthropic provider 选择入口，真实适配器稍后接入。"
}

internal fun previewApiConfigUiState(): ApiConfigUiState =
    ApiConfigUiState(
        selectedProviderId = LlmProviderId.OpenAI,
        endpoint = "https://api.openai.com/v1",
        apiToken = "sk-preview-token",
        hasUnsavedChanges = true,
        providerNotice = "OpenAI 当前仍走兼容桥接；切换 Provider 的交互和持久化已经接通。",
    )
