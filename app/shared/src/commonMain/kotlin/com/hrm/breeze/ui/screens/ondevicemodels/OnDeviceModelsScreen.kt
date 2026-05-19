package com.hrm.breeze.ui.screens.ondevicemodels

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.hrm.breeze.domain.model.OnDeviceDownloadStatus
import com.hrm.breeze.generated.resources.Res
import com.hrm.breeze.generated.resources.delete_model
import com.hrm.breeze.generated.resources.download
import com.hrm.breeze.generated.resources.on_device_models
import com.hrm.breeze.generated.resources.on_device_models_description
import com.hrm.breeze.generated.resources.set_current_model
import com.hrm.breeze.generated.resources.status_downloaded
import com.hrm.breeze.generated.resources.status_downloading
import com.hrm.breeze.generated.resources.status_not_downloaded
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.theme.BreezeTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnDeviceModelsScreen(
    state: OnDeviceModelsUiState,
    onBack: () -> Unit,
    onDownload: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    embeddedMode: Boolean = false,
    showBackButton: Boolean = true,
) {
    val windowInfo = LocalWindowInfo.current
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography
    val extra = BreezeTheme.extendedColors

    val contentModifier =
        if (embeddedMode) {
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
        val surfaceModifier =
            if (embeddedMode) {
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
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (showBackButton) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterStart),
                            shape = shapes.medium,
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
                            text = stringResource(Res.string.on_device_models),
                            style = typography.titleLarge,
                            color = scheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(Res.string.on_device_models_description),
                            style = typography.bodySmall,
                            color = extra.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                state.statusMessage?.let { statusMessage ->
                    Text(
                        text = stringResource(statusMessage),
                        style = typography.bodySmall,
                        color = extra.textSecondary,
                    )
                }

                state.models.forEach { model ->
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
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            Text(
                                text = model.preset.displayName,
                                style = typography.titleMedium,
                                color = scheme.onSurface,
                            )
                            Text(
                                text = model.preset.description,
                                style = typography.bodySmall,
                                color = extra.textSecondary,
                            )
                            Text(
                                text =
                                    when (model.downloadStatus) {
                                        OnDeviceDownloadStatus.Downloaded -> stringResource(Res.string.status_downloaded)
                                        OnDeviceDownloadStatus.Downloading -> stringResource(Res.string.status_downloading)
                                        else -> stringResource(Res.string.status_not_downloaded)
                                    },
                                style = typography.bodySmall,
                                color = extra.textSecondary,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { onDownload(model.preset.id) },
                                    enabled = state.activePresetId != model.preset.id && model.downloadStatus != OnDeviceDownloadStatus.Downloaded,
                                    shape = shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = scheme.primary,
                                        contentColor = scheme.onPrimary,
                                    ),
                                ) {
                                    Text(stringResource(Res.string.download))
                                }
                                TextButton(
                                    onClick = { onSelect(model.preset.id) },
                                    enabled = state.activePresetId != model.preset.id && model.downloadStatus == OnDeviceDownloadStatus.Downloaded,
                                    shape = shapes.medium,
                                ) {
                                    Text(stringResource(Res.string.set_current_model))
                                }
                                TextButton(
                                    onClick = { onDelete(model.preset.id) },
                                    enabled = state.activePresetId != model.preset.id && model.downloadStatus == OnDeviceDownloadStatus.Downloaded,
                                    shape = shapes.medium,
                                ) {
                                    Text(stringResource(Res.string.delete_model))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
