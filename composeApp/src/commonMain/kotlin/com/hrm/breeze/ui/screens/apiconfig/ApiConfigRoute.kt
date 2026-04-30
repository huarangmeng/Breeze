package com.hrm.breeze.ui.screens.apiconfig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ApiConfigRoute(
    modifier: Modifier = Modifier,
    viewModel: ApiConfigViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    ApiConfigScreen(
        modifier = modifier,
        state = uiState,
        onProviderSelected = viewModel::onProviderSelected,
        onEndpointChange = viewModel::onEndpointChange,
        onApiTokenChange = viewModel::onApiTokenChange,
        onReset = viewModel::onReset,
        onSave = viewModel::onSave,
    )
}
