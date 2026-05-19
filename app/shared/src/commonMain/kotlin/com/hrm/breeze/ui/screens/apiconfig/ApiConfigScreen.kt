package com.hrm.breeze.ui.screens.apiconfig

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun ApiConfigScreen(
    state: ApiConfigUiState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onProviderSelected: (LlmProviderId) -> Unit,
    onEndpointChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
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
    var tokenVisible by remember { mutableStateOf(false) }

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
            Modifier.widthIn(max = windowInfo.contentMaxWidth + windowInfo.contentMaxWidth / 3).fillMaxSize()
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
                    ApiConfigTopBar(
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
                        if (!embeddedMode) {
                            ApiIntroSection()
                        }
                        ApiFieldSection(
                            state = state,
                            tokenVisible = tokenVisible,
                            onEndpointChange = onEndpointChange,
                            onApiTokenChange = onApiTokenChange,
                            onTokenVisibilityChange = { tokenVisible = it },
                        )
                        ProviderGrid(
                            state = state,
                            onProviderSelected = onProviderSelected,
                        )
                        ProviderNoticeCard(state.providerNotice)
                        ApiActionSection(
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
private fun ApiConfigTopBar(
    state: ApiConfigUiState,
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
                text = "API Configuration",
                style = typography.titleLarge,
                color = scheme.onBackground,
            )
            Text(
                text = if (previewMode) "Preview provider settings" else "Enter your API details to connect to the model service.",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
            Text(
                text = "Current Provider: ${state.selectedProviderId.displayName}",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
    }
}

@Composable
private fun ApiIntroSection() {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = scheme.primaryContainer,
            shape = shapes.medium,
        ) {
            Text(
                text = "[]",
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.md),
                style = typography.labelLarge,
                color = scheme.primary,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = "Configure Your API",
                style = typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "Your API configuration is stored locally on this device and is never uploaded.",
                style = typography.bodySmall,
                color = extra.textSecondary,
            )
        }
    }
}

@Composable
private fun ApiFieldSection(
    state: ApiConfigUiState,
    tokenVisible: Boolean,
    onEndpointChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
    onTokenVisibilityChange: (Boolean) -> Unit,
) {
    val spacing = BreezeTheme.spacing
    val shapes = BreezeTheme.shapes

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        OutlinedTextField(
            value = state.apiToken,
            onValueChange = onApiTokenChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            shape = shapes.input,
            label = { Text("API Key") },
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(
                    onClick = { onTokenVisibilityChange(!tokenVisible) },
                ) {
                    Text(if (tokenVisible) "Hide" else "Show")
                }
            },
        )

        OutlinedTextField(
            value = state.endpoint,
            onValueChange = onEndpointChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            shape = shapes.input,
            label = { Text("Base URL") },
            placeholder = { Text("https://api.example.com/v1") },
        )
    }
}

@Composable
private fun ProviderGrid(
    state: ApiConfigUiState,
    onProviderSelected: (LlmProviderId) -> Unit,
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
            text = "Model Type",
            style = typography.labelLarge,
            color = scheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            state.availableProviders.forEach { providerId ->
                val selected = providerId == state.selectedProviderId
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProviderSelected(providerId) },
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
                            text = providerId.displayName,
                            style = typography.labelLarge,
                            color = if (selected) scheme.primary else scheme.onSurface,
                        )
                        Text(
                            text = providerDescription(providerId),
                            style = typography.bodySmall,
                            color = extra.textSecondary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderNoticeCard(
    notice: String,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    Surface(
        color = scheme.surfaceVariant,
        shape = shapes.medium,
        border = BorderStroke(spacing.hairline, scheme.outlineVariant),
    ) {
        Text(
            text = notice,
            modifier = Modifier.padding(spacing.md),
            style = typography.bodySmall,
            color = extra.textSecondary,
        )
    }
}

@Composable
private fun ApiActionSection(
    state: ApiConfigUiState,
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
        if (state.statusMessage != null) {
            Text(
                text = state.statusMessage,
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
                onClick = {},
                shape = shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = scheme.surface,
                    contentColor = scheme.primary,
                ),
            ) {
                Text("Test Connection")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                TextButton(
                    onClick = onReset,
                    enabled = state.hasUnsavedChanges && !state.isSaving,
                    shape = shapes.medium,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = scheme.surfaceVariant,
                        contentColor = scheme.onSurface,
                    ),
                ) {
                    Text("Cancel")
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
                    Text(if (state.isSaving) "Saving..." else "Save")
                }
            }
        }
    }
}

private fun providerDescription(providerId: LlmProviderId): String = when (providerId) {
    LlmProviderId.Local -> "Local echo chain"
    LlmProviderId.OpenAI -> "Hosted provider"
    LlmProviderId.Anthropic -> "Hosted provider"
}

internal fun previewApiConfigUiState(): ApiConfigUiState =
    ApiConfigUiState(
        selectedProviderId = LlmProviderId.OpenAI,
        endpoint = "https://api.openai.com/v1",
        apiToken = "sk-preview-token",
        hasUnsavedChanges = true,
        providerNotice = "OpenAI 当前仍走兼容桥接；切换 Provider 的交互和持久化已经接通。",
    )
