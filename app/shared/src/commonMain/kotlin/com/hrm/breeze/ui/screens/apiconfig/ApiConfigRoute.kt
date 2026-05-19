package com.hrm.breeze.ui.screens.apiconfig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ApiConfigRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    embeddedMode: Boolean = false,
    showBackButton: Boolean = true,
    viewModel: ApiConfigViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(viewModel, onBack) {
        viewModel.closePageEvent.collect {
            onBack()
        }
    }

    ApiConfigScreen(
        modifier = modifier,
        state = uiState,
        onBack = onBack,
        onOpenHistory = onOpenHistory,
        onEndpointChange = viewModel::onEndpointChange,
        onApiTokenChange = viewModel::onApiTokenChange,
        onModelIdChange = viewModel::onModelIdChange,
        onTestConnection = viewModel::onTestConnection,
        onReset = viewModel::onReset,
        onSave = viewModel::onSave,
        embeddedMode = embeddedMode,
        showBackButton = showBackButton,
    )
}
