package com.hrm.breeze.ui.screens.apiconfig

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import com.hrm.breeze.generated.resources.*
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.theme.BreezeTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ApiConfigScreen(
    state: ApiConfigUiState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onEndpointChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
    onModelIdChange: (String) -> Unit,
    onTestConnection: () -> Unit,
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
                ApiConfigHeader(
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
                        ApiFieldSection(
                            state = state,
                            tokenVisible = tokenVisible,
                            onEndpointChange = onEndpointChange,
                            onApiTokenChange = onApiTokenChange,
                            onModelIdChange = onModelIdChange,
                            onTokenVisibilityChange = { tokenVisible = it },
                        )
                        ApiActionSection(
                            state = state,
                            onTestConnection = onTestConnection,
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
private fun ApiConfigHeader(
    onBack: () -> Unit,
    showBackButton: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

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
                text = stringResource(Res.string.api_configuration),
                style = typography.titleLarge,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
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
    onModelIdChange: (String) -> Unit,
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
            label = { Text(stringResource(Res.string.api_key)) },
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(
                    onClick = { onTokenVisibilityChange(!tokenVisible) },
                ) {
                    Text(if (tokenVisible) stringResource(Res.string.hide) else stringResource(Res.string.show))
                }
            },
        )

        OutlinedTextField(
            value = state.endpoint,
            onValueChange = onEndpointChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            shape = shapes.input,
            label = { Text(stringResource(Res.string.base_url)) },
            placeholder = { Text(stringResource(Res.string.base_url_hint)) },
        )
        OutlinedTextField(
            value = state.modelId,
            onValueChange = onModelIdChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            shape = shapes.input,
            label = { Text(stringResource(Res.string.model_id)) },
            placeholder = { Text(stringResource(Res.string.model_id_hint)) },
        )
    }
}

@Composable
private fun ApiActionSection(
    state: ApiConfigUiState,
    onTestConnection: () -> Unit,
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
        state.statusDetail?.let { statusDetail ->
            Text(
                text = statusDetail,
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
                onClick = onTestConnection,
                enabled = state.isFormComplete && !state.isSaving && !state.isTesting,
                shape = shapes.medium,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = scheme.surface,
                    contentColor = scheme.primary,
                ),
            ) {
                Text(
                    if (state.isTesting) {
                        stringResource(Res.string.testing_connection)
                    } else {
                        stringResource(Res.string.test_connection)
                    }
                )
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
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = state.isFormComplete && state.hasUnsavedChanges && !state.isSaving && !state.isTesting,
                    shape = shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                ) {
                    Text(if (state.isSaving) stringResource(Res.string.saving) else stringResource(Res.string.save))
                }
            }
        }
    }
}

internal fun previewApiConfigUiState(): ApiConfigUiState =
    ApiConfigUiState(
        endpoint = "",
        apiToken = "",
        modelId = "",
        hasUnsavedChanges = true,
    )
