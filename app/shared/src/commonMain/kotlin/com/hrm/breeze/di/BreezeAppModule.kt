package com.hrm.breeze.di

import coil3.ImageLoader
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.image.createBreezeImageLoader
import com.hrm.breeze.data.llm.LlmProviderRegistry
import com.hrm.breeze.data.llm.LocalProvider
import com.hrm.breeze.data.llm.OpenAiCompatibleProvider
import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.data.network.KtorBreezeChatApi
import com.hrm.breeze.data.network.KtorOpenAiCompatibleChatApi
import com.hrm.breeze.data.network.OpenAiCompatibleChatApi
import com.hrm.breeze.data.network.createBreezeHttpClient
import com.hrm.breeze.data.repository.ChatRepositoryImpl
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.createBreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.domain.model.LlmProviderId
import com.hrm.breeze.domain.repository.ChatRepository
import com.hrm.breeze.ui.screens.apiconfig.ApiConfigViewModel
import com.hrm.breeze.ui.screens.chat.ChatViewModel
import com.hrm.breeze.ui.screens.history.HistoryViewModel
import com.hrm.breeze.ui.screens.modelsettings.ModelSettingsViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val infrastructureModule =
    module {
        single<AppDispatchers> { defaultAppDispatchers() }
        single<BreezeSettings> { createBreezeSettings() }
        single<HttpClient> { createBreezeHttpClient() }
        single<BreezeDatabase> {
            BreezeDatabase.create(dispatchers = get())
        }
        single<BreezeChatApi> {
            KtorBreezeChatApi(
                httpClient = get(),
                endpointProvider = get<BreezeSettings>()::getEchoEndpoint,
            )
        }
        single<OpenAiCompatibleChatApi> { KtorOpenAiCompatibleChatApi(get()) }
        single { LocalProvider() }
        single {
            LlmProviderRegistry(
                providers = listOf(
                    get<LocalProvider>(),
                    OpenAiCompatibleProvider(LlmProviderId.OpenAI, get(), get()),
                    OpenAiCompatibleProvider(LlmProviderId.Anthropic, get(), get()),
                ),
            )
        }
        single<ImageLoader> { createBreezeImageLoader(get()) }
        single<ChatRepository> {
            ChatRepositoryImpl(
                database = get(),
                llmProviderRegistry = get(),
                settings = get(),
                dispatchers = get(),
            )
        }
    }

private val presentationModule =
    module {
        viewModel { ChatViewModel(get(), get()) }
        viewModel { HistoryViewModel(get()) }
        viewModel { ApiConfigViewModel(get(), get()) }
        viewModel { ModelSettingsViewModel(get()) }
    }

val breezeAppModule =
    module {
        includes(infrastructureModule, presentationModule)
    }
