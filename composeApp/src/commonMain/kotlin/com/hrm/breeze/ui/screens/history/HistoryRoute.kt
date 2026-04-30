package com.hrm.breeze.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryRoute(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    HistoryScreen(
        modifier = modifier,
        state = uiState,
        onConversationSelected = viewModel::onConversationSelected,
    )
}
