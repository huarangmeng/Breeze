package com.hrm.breeze.ui.screens.modelsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ModelSettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenApiConfig: () -> Unit = {},
    embeddedMode: Boolean = false,
    showBackButton: Boolean = true,
    viewModel: ModelSettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    ModelSettingsScreen(
        modifier = modifier,
        state = uiState,
        onBack = onBack,
        onOpenHistory = onOpenHistory,
        onOpenApiConfig = onOpenApiConfig,
        onModelIdChange = viewModel::onModelIdChange,
        onReset = viewModel::onReset,
        onSave = viewModel::onSave,
        embeddedMode = embeddedMode,
        showBackButton = showBackButton,
    )
}
