package com.hrm.breeze.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hrm.breeze.ui.navigation.ApiConfig
import com.hrm.breeze.ui.navigation.BreezeDestinations
import com.hrm.breeze.ui.navigation.Chat
import com.hrm.breeze.ui.navigation.History
import com.hrm.breeze.ui.navigation.ModelSettings
import com.hrm.breeze.ui.navigation.TopLevelDestination
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun BreezeNavHost(
    modifier: Modifier = Modifier,
    startDestination: TopLevelDestination = Chat,
    chatContent: @Composable () -> Unit,
    historyContent: @Composable () -> Unit = {
        FeaturePlaceholderScreen(
            title = "History",
            description = "M4-4 会把历史列表与详情页骨架接入这里。",
        )
    },
    apiConfigContent: @Composable () -> Unit = {
        FeaturePlaceholderScreen(
            title = "API Config",
            description = "M4-5 会把 Provider 配置与鉴权设置页接入这里。",
        )
    },
    modelSettingsContent: @Composable () -> Unit = {
        FeaturePlaceholderScreen(
            title = "Model Settings",
            description = "M4-6 会把模型参数与预设管理页接入这里。",
        )
    },
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDestination.routePattern

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BreezeTheme.spacing.md),
    ) {
        TopLevelNavigationBar(
            currentRoute = currentRoute,
            onDestinationClick = { destination ->
                if (destination.routePattern == currentRoute) {
                    return@TopLevelNavigationBar
                }

                navController.navigate(destination.routePattern) {
                    launchSingleTop = true
                    restoreState = true

                    val graphStartRoute = navController.graph.startDestinationRoute
                    if (graphStartRoute != null) {
                        popUpTo(graphStartRoute) {
                            saveState = true
                        }
                    }
                }
            },
        )

        NavHost(
            navController = navController,
            startDestination = startDestination.routePattern,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Chat.routePattern) { chatContent() }
            composable(History.routePattern) { historyContent() }
            composable(ApiConfig.routePattern) { apiConfigContent() }
            composable(ModelSettings.routePattern) { modelSettingsContent() }
        }
    }
}

@Composable
private fun TopLevelNavigationBar(
    currentRoute: String,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = shapes.large,
        tonalElevation = spacing.hairline,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = spacing.xs, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            BreezeDestinations.topLevel.forEach { destination ->
                val selected = currentRoute == destination.routePattern
                TextButton(
                    onClick = { onDestinationClick(destination) },
                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor =
                                if (selected) {
                                    scheme.primaryContainer
                                } else {
                                    scheme.surface
                                },
                            contentColor =
                                if (selected) {
                                    scheme.onPrimaryContainer
                                } else {
                                    scheme.onSurface
                                },
                        ),
                    shape = shapes.pill,
                ) {
                    Text(
                        text = destination.title,
                        style = typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePlaceholderScreen(
    title: String,
    description: String,
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = BreezeTheme.shapes
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = scheme.surface,
        shape = shapes.large,
        tonalElevation = spacing.hairline,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surface)
                .padding(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = title,
                    style = typography.titleLarge,
                    color = scheme.onSurface,
                )
                Text(
                    text = description,
                    style = typography.bodyMedium,
                    color = BreezeTheme.extendedColors.textSecondary,
                )
            }
        }
    }
}
