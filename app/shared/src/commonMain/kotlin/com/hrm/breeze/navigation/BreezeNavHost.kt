package com.hrm.breeze.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hrm.breeze.ui.adaptive.LocalWindowInfo
import com.hrm.breeze.ui.adaptive.PaneMode
import com.hrm.breeze.ui.navigation.ApiConfig
import com.hrm.breeze.ui.navigation.Chat
import com.hrm.breeze.ui.navigation.History
import com.hrm.breeze.ui.navigation.ModelSettings
import com.hrm.breeze.ui.navigation.OnDeviceModels
import com.hrm.breeze.ui.navigation.SettingsHub
import com.hrm.breeze.ui.navigation.TopLevelDestination
import com.hrm.breeze.ui.screens.settingshub.SettingsHubScreen
import com.hrm.breeze.ui.theme.BreezeTheme

@Composable
fun BreezeNavHost(
    modifier: Modifier = Modifier,
    startDestination: TopLevelDestination = Chat,
    languagePreference: String = "system",
    onLanguagePreferenceSelected: (String) -> Unit = {},
    chatContent: @Composable (
        selectedDesktopRoute: String,
        onOpenSettings: () -> Unit,
        onSelectChatTab: () -> Unit,
        onOpenApiConfig: () -> Unit,
        onOpenModelSettings: () -> Unit,
        onOpenOnDeviceModels: () -> Unit,
        embeddedApiConfigContent: @Composable () -> Unit,
        embeddedModelSettingsContent: @Composable () -> Unit,
        embeddedOnDeviceModelsContent: @Composable () -> Unit,
    ) -> Unit,
    historyContent: @Composable (
        onNewConversation: () -> Unit,
        onBackToChat: () -> Unit,
        onOpenApiConfig: () -> Unit,
        onOpenModelSettings: () -> Unit,
    ) -> Unit = { _, _, _, _ ->
        FeaturePlaceholderScreen(
            title = "History",
            description = "历史页面还未接入。",
        )
    },
    apiConfigContent: @Composable (
        onBack: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenModelSettings: () -> Unit,
        embeddedMode: Boolean,
    ) -> Unit = { _, _, _, _ ->
        FeaturePlaceholderScreen(
            title = "API Configuration",
            description = "API 配置页面还未接入。",
        )
    },
    modelSettingsContent: @Composable (
        onBack: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenApiConfig: () -> Unit,
        embeddedMode: Boolean,
    ) -> Unit = { _, _, _, _ ->
        FeaturePlaceholderScreen(
            title = "Model Parameters",
            description = "模型参数页面还未接入。",
        )
    },
    onDeviceModelsContent: @Composable (
        onBack: () -> Unit,
        embeddedMode: Boolean,
    ) -> Unit = { _, _ ->
        FeaturePlaceholderScreen(
            title = "On-device Models",
            description = "端侧模型页面还未接入。",
        )
    },
) {
    val navController = rememberNavController()
    val scheme = MaterialTheme.colorScheme
    val windowInfo = LocalWindowInfo.current
    val desktopLikeLayout = windowInfo.paneMode != PaneMode.Single

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeDrawingPadding(),
    ) {
        FeatureNavHost(
            navController = navController,
            startDestination = startDestination,
            chatContent = chatContent,
            historyContent = historyContent,
            apiConfigContent = apiConfigContent,
            modelSettingsContent = modelSettingsContent,
            onDeviceModelsContent = onDeviceModelsContent,
            desktopLikeLayout = desktopLikeLayout,
            languagePreference = languagePreference,
            onLanguagePreferenceSelected = onLanguagePreferenceSelected,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun FeatureNavHost(
    navController: NavHostController,
    startDestination: TopLevelDestination,
    chatContent: @Composable (
        selectedDesktopRoute: String,
        onOpenSettings: () -> Unit,
        onSelectChatTab: () -> Unit,
        onOpenApiConfig: () -> Unit,
        onOpenModelSettings: () -> Unit,
        onOpenOnDeviceModels: () -> Unit,
        embeddedApiConfigContent: @Composable () -> Unit,
        embeddedModelSettingsContent: @Composable () -> Unit,
        embeddedOnDeviceModelsContent: @Composable () -> Unit,
    ) -> Unit,
    historyContent: @Composable (
        onNewConversation: () -> Unit,
        onBackToChat: () -> Unit,
        onOpenApiConfig: () -> Unit,
        onOpenModelSettings: () -> Unit,
    ) -> Unit,
    apiConfigContent: @Composable (
        onBack: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenModelSettings: () -> Unit,
        embeddedMode: Boolean,
    ) -> Unit,
    modelSettingsContent: @Composable (
        onBack: () -> Unit,
        onOpenHistory: () -> Unit,
        onOpenApiConfig: () -> Unit,
        embeddedMode: Boolean,
    ) -> Unit,
    onDeviceModelsContent: @Composable (
        onBack: () -> Unit,
        embeddedMode: Boolean,
    ) -> Unit,
    desktopLikeLayout: Boolean,
    languagePreference: String,
    onLanguagePreferenceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRoute by rememberSaveable {
        mutableStateOf(
            when (startDestination.routePattern) {
                History.routePattern -> History.routePattern
                ApiConfig.routePattern -> ApiConfig.routePattern
                ModelSettings.routePattern -> ModelSettings.routePattern
                OnDeviceModels.routePattern -> OnDeviceModels.routePattern
                else -> Chat.routePattern
            },
        )
    }
    val desktopPanelRoute = when (selectedRoute) {
        ApiConfig.routePattern -> ApiConfig.routePattern
        ModelSettings.routePattern -> ModelSettings.routePattern
        OnDeviceModels.routePattern -> OnDeviceModels.routePattern
        else -> Chat.routePattern
    }
    val navStartDestination = if (desktopLikeLayout && startDestination.routePattern != History.routePattern) {
        Chat.routePattern
    } else {
        startDestination.routePattern
    }

    NavHost(
        navController = navController,
        startDestination = navStartDestination,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Chat.routePattern) {
            chatContent(
                desktopPanelRoute,
                { navController.navigate(SettingsHub.routePattern) },
                { selectedRoute = Chat.routePattern },
                { selectedRoute = ApiConfig.routePattern },
                { selectedRoute = ModelSettings.routePattern },
                { selectedRoute = OnDeviceModels.routePattern },
                {
                    apiConfigContent(
                        {},
                        { navController.navigate(History.routePattern) },
                        { selectedRoute = ModelSettings.routePattern },
                        true,
                    )
                },
                {
                    modelSettingsContent(
                        {},
                        { navController.navigate(History.routePattern) },
                        { selectedRoute = ApiConfig.routePattern },
                        true,
                    )
                },
                {
                    onDeviceModelsContent(
                        {},
                        true,
                    )
                },
            )
        }
        composable(SettingsHub.routePattern) {
            SettingsHubScreen(
                selectedRoute = selectedRoute,
                languagePreference = languagePreference,
                onLanguagePreferenceSelected = onLanguagePreferenceSelected,
                onBackToChat = {
                    selectedRoute = Chat.routePattern
                    navController.navigate(Chat.routePattern)
                },
                onOpenHistory = {
                    selectedRoute = History.routePattern
                    navController.navigate(History.routePattern)
                },
                onOpenApiConfig = {
                    selectedRoute = ApiConfig.routePattern
                    if (desktopLikeLayout) {
                        navController.navigate(Chat.routePattern)
                    } else {
                        navController.navigate(ApiConfig.routePattern)
                    }
                },
                onOpenModelSettings = {
                    selectedRoute = ModelSettings.routePattern
                    if (desktopLikeLayout) {
                        navController.navigate(Chat.routePattern)
                    } else {
                        navController.navigate(ModelSettings.routePattern)
                    }
                },
                onOpenOnDeviceModels = {
                    selectedRoute = OnDeviceModels.routePattern
                    if (desktopLikeLayout) {
                        navController.navigate(Chat.routePattern)
                    } else {
                        navController.navigate(OnDeviceModels.routePattern)
                    }
                },
            )
        }
        composable(History.routePattern) {
            historyContent(
                {
                    selectedRoute = Chat.routePattern
                    navController.navigate(Chat.routePattern)
                },
                {
                    selectedRoute = Chat.routePattern
                    navController.navigate(Chat.routePattern)
                },
                {
                    if (desktopLikeLayout) {
                        selectedRoute = ApiConfig.routePattern
                        navController.navigate(Chat.routePattern)
                    } else {
                        selectedRoute = ApiConfig.routePattern
                        navController.navigate(ApiConfig.routePattern)
                    }
                },
                {
                    if (desktopLikeLayout) {
                        selectedRoute = ModelSettings.routePattern
                        navController.navigate(Chat.routePattern)
                    } else {
                        selectedRoute = ModelSettings.routePattern
                        navController.navigate(ModelSettings.routePattern)
                    }
                },
            )
        }
        if (!desktopLikeLayout) {
            composable(ApiConfig.routePattern) {
                selectedRoute = ApiConfig.routePattern
                apiConfigContent(
                    {
                        selectedRoute = Chat.routePattern
                        navController.navigate(Chat.routePattern)
                    },
                    {
                        selectedRoute = History.routePattern
                        navController.navigate(History.routePattern)
                    },
                    {
                        selectedRoute = ModelSettings.routePattern
                        navController.navigate(ModelSettings.routePattern)
                    },
                    false,
                )
            }
            composable(ModelSettings.routePattern) {
                selectedRoute = ModelSettings.routePattern
                modelSettingsContent(
                    {
                        selectedRoute = Chat.routePattern
                        navController.navigate(Chat.routePattern)
                    },
                    {
                        selectedRoute = History.routePattern
                        navController.navigate(History.routePattern)
                    },
                    {
                        selectedRoute = ApiConfig.routePattern
                        navController.navigate(ApiConfig.routePattern)
                    },
                    false,
                )
            }
            composable(OnDeviceModels.routePattern) {
                selectedRoute = OnDeviceModels.routePattern
                onDeviceModelsContent(
                    {
                        selectedRoute = Chat.routePattern
                        navController.navigate(Chat.routePattern)
                    },
                    false,
                )
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
    val spacing = BreezeTheme.spacing
    val typography = BreezeTheme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface)
            .padding(spacing.xl),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
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
