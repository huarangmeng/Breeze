package com.hrm.breeze.ui.screens.apiconfig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ApiConfigRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenModelSettings: () -> Unit = {},
    embeddedMode: Boolean = false,
    showBackButton: Boolean = true,
    viewModel: ApiConfigViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    ApiConfigScreen(
        modifier = modifier,
        state = uiState,
        onBack = onBack,
        onOpenHistory = onOpenHistory,
        onOpenModelSettings = onOpenModelSettings,
        onProviderSelected = viewModel::onProviderSelected,
        onEndpointChange = viewModel::onEndpointChange,
        onApiTokenChange = viewModel::onApiTokenChange,
        onReset = viewModel::onReset,
        onSave = viewModel::onSave,
        embeddedMode = embeddedMode,
        showBackButton = showBackButton,
    )
}
