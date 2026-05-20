package com.hrm.breeze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.BreezeSettingsSnapshot
import com.hrm.breeze.di.breezeAppModule
import com.hrm.breeze.i18n.BreezeI18nProvider
import com.hrm.breeze.i18n.BreezeLanguagePreference
import com.hrm.breeze.navigation.BreezeNavHost
import com.hrm.breeze.ui.adaptive.ProvideWindowInfo
import com.hrm.breeze.ui.screens.apiconfig.ApiConfigRoute
import com.hrm.breeze.ui.screens.apiconfig.ApiConfigScreen
import com.hrm.breeze.ui.screens.apiconfig.previewApiConfigUiState
import com.hrm.breeze.ui.screens.chat.ChatRoute
import com.hrm.breeze.ui.screens.chat.ChatScreen
import com.hrm.breeze.ui.screens.chat.previewChatUiState
import com.hrm.breeze.ui.screens.history.HistoryRoute
import com.hrm.breeze.ui.screens.history.HistoryScreen
import com.hrm.breeze.ui.screens.history.previewHistoryUiState
import com.hrm.breeze.ui.screens.modelsettings.ModelSettingsRoute
import com.hrm.breeze.ui.screens.modelsettings.ModelSettingsScreen
import com.hrm.breeze.ui.screens.modelsettings.previewModelSettingsUiState
import com.hrm.breeze.ui.screens.ondevicemodels.OnDeviceModelsRoute
import com.hrm.breeze.ui.screens.ondevicemodels.OnDeviceModelsScreen
import com.hrm.breeze.ui.screens.ondevicemodels.OnDeviceModelsUiState
import com.hrm.breeze.ui.theme.BreezeAppTheme
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    previewMode: Boolean = false,
) {
    BreezeAppTheme {
        AppContent(previewMode = previewMode)
    }
}

@Composable
private fun AppContent(
    previewMode: Boolean,
) {
    if (previewMode) {
        BreezeI18nProvider(languageTag = BreezeLanguagePreference.System.storageValue) {
            ProvideWindowInfo {
                BreezeNavHost(
                    languagePreference = BreezeLanguagePreference.System.storageValue,
                    onLanguagePreferenceSelected = {},
                    chatContent = { selectedDesktopRoute, onOpenSettings, onSelectChatTab, onOpenApiConfig, onOpenModelSettings, onOpenOnDeviceModels, embeddedApiConfigContent, embeddedModelSettingsContent, embeddedOnDeviceModelsContent ->
                        ChatScreen(
                            state = previewChatUiState(),
                            onDraftChange = {},
                            onReasoningEnabledChange = {},
                            onConversationSelected = {},
                            onNewConversation = {},
                            onModelSelected = {},
                            onSendMessage = {},
                            selectedDesktopRoute = selectedDesktopRoute,
                            onOpenSettings = onOpenSettings,
                            onSelectChatTab = onSelectChatTab,
                            onOpenApiConfig = onOpenApiConfig,
                            onOpenModelSettings = onOpenModelSettings,
                            onOpenOnDeviceModels = onOpenOnDeviceModels,
                            embeddedApiConfigContent = embeddedApiConfigContent,
                            embeddedModelSettingsContent = embeddedModelSettingsContent,
                            embeddedOnDeviceModelsContent = embeddedOnDeviceModelsContent,
                            previewMode = true,
                        )
                    },
                    historyContent = { onNewConversation, onBackToChat, onOpenApiConfig, onOpenModelSettings ->
                        HistoryScreen(
                            state = previewHistoryUiState(),
                            onConversationSelected = {},
                            onNewConversation = onNewConversation,
                            onBackToChat = onBackToChat,
                            onOpenApiConfig = onOpenApiConfig,
                            onOpenModelSettings = onOpenModelSettings,
                            previewMode = true,
                        )
                    },
                    apiConfigContent = { onBack, onOpenHistory, onOpenModelSettings, embeddedMode ->
                        ApiConfigScreen(
                            state = previewApiConfigUiState(),
                            onBack = onBack,
                            onOpenHistory = onOpenHistory,
                            onEndpointChange = {},
                            onApiTokenChange = {},
                            onModelIdChange = {},
                            onTestConnection = {},
                            onReset = {},
                            onSave = {},
                            previewMode = true,
                            embeddedMode = embeddedMode,
                            showBackButton = !embeddedMode,
                        )
                    },
                    modelSettingsContent = { onBack, onOpenHistory, onOpenApiConfig, embeddedMode ->
                        ModelSettingsScreen(
                            state = previewModelSettingsUiState(),
                            onBack = onBack,
                            onOpenHistory = onOpenHistory,
                            onOpenApiConfig = onOpenApiConfig,
                            onModelIdChange = {},
                            onTemperatureChange = {},
                            onTopPChange = {},
                            onMaxTokensChange = {},
                            onContextWindowChange = {},
                            onReset = {},
                            onSave = {},
                            previewMode = true,
                            embeddedMode = embeddedMode,
                            showBackButton = !embeddedMode,
                        )
                    },
                    onDeviceModelsContent = { onBack, embeddedMode ->
                        OnDeviceModelsScreen(
                            state = OnDeviceModelsUiState(),
                            onBack = onBack,
                            onDownload = {},
                            onSelect = {},
                            onDelete = {},
                            embeddedMode = embeddedMode,
                            showBackButton = !embeddedMode,
                        )
                    },
                )
            }
        }
        return
    }

    KoinApplication(
        configuration =
            koinConfiguration {
                modules(breezeAppModule)
            }
    ) {
        ProvideWindowInfo {
            BreezeRuntimeApp()
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App(previewMode = true)
}

@Composable
private fun BreezeRuntimeApp() {
    val httpClient = koinInject<HttpClient>()
    val settings = koinInject<BreezeSettings>()
    val settingsSnapshot by settings.snapshot.collectAsState(initial = BreezeSettingsSnapshot())
    val scope = rememberCoroutineScope()

    DisposableEffect(httpClient) {
        onDispose {
            httpClient.close()
        }
    }

    BreezeI18nProvider(languageTag = settingsSnapshot.appLanguageTag) {
        BreezeNavHost(
            languagePreference = settingsSnapshot.appLanguageTag,
            onLanguagePreferenceSelected = { languageTag ->
                scope.launch {
                    settings.updateAppLanguageTag(languageTag)
                }
            },
            chatContent = { selectedDesktopRoute, onOpenSettings, onSelectChatTab, onOpenApiConfig, onOpenModelSettings, onOpenOnDeviceModels, embeddedApiConfigContent, embeddedModelSettingsContent, embeddedOnDeviceModelsContent ->
                ChatRoute(
                    selectedDesktopRoute = selectedDesktopRoute,
                    onOpenSettings = onOpenSettings,
                    onSelectChatTab = onSelectChatTab,
                    onOpenApiConfig = onOpenApiConfig,
                    onOpenModelSettings = onOpenModelSettings,
                    onOpenOnDeviceModels = onOpenOnDeviceModels,
                    embeddedApiConfigContent = embeddedApiConfigContent,
                    embeddedModelSettingsContent = embeddedModelSettingsContent,
                    embeddedOnDeviceModelsContent = embeddedOnDeviceModelsContent,
                )
            },
            historyContent = { onNewConversation, onBackToChat, onOpenApiConfig, onOpenModelSettings ->
                HistoryRoute(
                    onNewConversation = onNewConversation,
                    onBackToChat = onBackToChat,
                    onOpenApiConfig = onOpenApiConfig,
                    onOpenModelSettings = onOpenModelSettings,
                )
            },
            apiConfigContent = { onBack, onOpenHistory, onOpenModelSettings, embeddedMode ->
                ApiConfigRoute(
                    onBack = onBack,
                    onOpenHistory = onOpenHistory,
                    embeddedMode = embeddedMode,
                    showBackButton = !embeddedMode,
                )
            },
            modelSettingsContent = { onBack, onOpenHistory, onOpenApiConfig, embeddedMode ->
                ModelSettingsRoute(
                    onBack = onBack,
                    onOpenHistory = onOpenHistory,
                    onOpenApiConfig = onOpenApiConfig,
                    embeddedMode = embeddedMode,
                    showBackButton = !embeddedMode,
                )
            },
            onDeviceModelsContent = { onBack, embeddedMode ->
                OnDeviceModelsRoute(
                    onBack = onBack,
                    embeddedMode = embeddedMode,
                    showBackButton = !embeddedMode,
                )
            },
        )
    }
}
