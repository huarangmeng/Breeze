package com.hrm.breeze.ui.screens.ondevicemodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnDeviceModelsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    embeddedMode: Boolean = false,
    showBackButton: Boolean = true,
    viewModel: OnDeviceModelsViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    OnDeviceModelsScreen(
        modifier = modifier,
        state = uiState,
        onBack = onBack,
        onDownload = viewModel::onDownload,
        onSelect = viewModel::onSelect,
        onDelete = viewModel::onDelete,
        embeddedMode = embeddedMode,
        showBackButton = showBackButton,
    )
}
