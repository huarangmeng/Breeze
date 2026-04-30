package com.hrm.breeze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import com.hrm.breeze.di.breezeAppModule
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
import com.hrm.breeze.ui.theme.BreezeAppTheme
import io.ktor.client.HttpClient
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    previewMode: Boolean = false,
) {
    if (previewMode) {
        BreezeAppTheme {
            ProvideWindowInfo {
                BreezeNavHost(
                    chatContent = {
                        ChatScreen(
                            state = previewChatUiState(),
                            onDraftChange = {},
                            onConversationSelected = {},
                            onNewConversation = {},
                            onSendMessage = {},
                            previewMode = true,
                        )
                    },
                    historyContent = {
                        HistoryScreen(
                            state = previewHistoryUiState(),
                            onConversationSelected = {},
                            previewMode = true,
                        )
                    },
                    apiConfigContent = {
                        ApiConfigScreen(
                            state = previewApiConfigUiState(),
                            onProviderSelected = {},
                            onEndpointChange = {},
                            onApiTokenChange = {},
                            onReset = {},
                            onSave = {},
                            previewMode = true,
                        )
                    },
                    modelSettingsContent = {
                        ModelSettingsScreen(
                            state = previewModelSettingsUiState(),
                            onModelSelected = {},
                            onReset = {},
                            onSave = {},
                            previewMode = true,
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
        BreezeAppTheme {
            ProvideWindowInfo {
                BreezeRuntimeApp()
            }
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

    DisposableEffect(httpClient) {
        onDispose {
            httpClient.close()
        }
    }

    BreezeNavHost(
        chatContent = {
            ChatRoute()
        },
        historyContent = {
            HistoryRoute()
        },
        apiConfigContent = {
            ApiConfigRoute()
        },
        modelSettingsContent = {
            ModelSettingsRoute()
        },
    )
}
