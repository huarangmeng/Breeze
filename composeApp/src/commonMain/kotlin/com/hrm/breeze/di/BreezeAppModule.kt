package com.hrm.breeze.di

import coil3.ImageLoader
import com.hrm.breeze.BreezeChatViewModel
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.image.createBreezeImageLoader
import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.data.network.KtorBreezeChatApi
import com.hrm.breeze.data.network.createBreezeHttpClient
import com.hrm.breeze.data.network.createMockBreezeHttpClient
import com.hrm.breeze.data.repository.ChatRepositoryImpl
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.createBreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.domain.repository.ChatRepository
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val USE_MOCK_ECHO_SERVICE = true

private val infrastructureModule =
    module {
        single<AppDispatchers> { defaultAppDispatchers() }
        single<BreezeSettings> { createBreezeSettings() }
        single<HttpClient> {
            if (USE_MOCK_ECHO_SERVICE) {
                createMockBreezeHttpClient()
            } else {
                createBreezeHttpClient()
            }
        }
        single<BreezeDatabase> {
            BreezeDatabase.create(dispatchers = get())
        }
        single<BreezeChatApi> {
            KtorBreezeChatApi(
                httpClient = get(),
                endpointProvider = get<BreezeSettings>()::getEchoEndpoint,
            )
        }
        single<ImageLoader> { createBreezeImageLoader(get()) }
        single<ChatRepository> {
            ChatRepositoryImpl(
                database = get(),
                chatApi = get(),
                settings = get(),
                dispatchers = get(),
            )
        }
    }

private val presentationModule =
    module {
        viewModel { BreezeChatViewModel(get(), get()) }
    }

val breezeAppModule =
    module {
        includes(infrastructureModule, presentationModule)
    }
