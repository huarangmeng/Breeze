package com.hrm.breeze.data

import coil3.ImageLoader
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.core.coroutines.defaultAppDispatchers
import com.hrm.breeze.data.image.createBreezeImageLoader
import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.data.network.KtorBreezeChatApi
import com.hrm.breeze.data.network.createBreezeHttpClient
import com.hrm.breeze.data.repository.ChatRepositoryImpl
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.settings.createBreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.driver.createPlatformSQLiteDriver
import com.hrm.breeze.domain.repository.ChatRepository
import io.ktor.client.HttpClient

class BreezeDataContainer(
    val httpClient: HttpClient,
    val database: BreezeDatabase,
    val settings: BreezeSettings,
    val imageLoader: ImageLoader,
    val chatRepository: ChatRepository,
) {
    companion object {
        fun create(
            dispatchers: AppDispatchers = defaultAppDispatchers(),
        ): BreezeDataContainer {
            val settings = createBreezeSettings()
            val httpClient = createBreezeHttpClient()
            val database = BreezeDatabase.build(
                driver = createPlatformSQLiteDriver(),
                dispatchers = dispatchers,
            )
            val chatApi: BreezeChatApi = KtorBreezeChatApi(
                httpClient = httpClient,
                endpointProvider = { settings.echoEndpoint },
            )
            val repository: ChatRepository = ChatRepositoryImpl(
                database = database,
                chatApi = chatApi,
                settings = settings,
                dispatchers = dispatchers,
            )
            return BreezeDataContainer(
                httpClient = httpClient,
                database = database,
                settings = settings,
                imageLoader = createBreezeImageLoader(httpClient),
                chatRepository = repository,
            )
        }
    }
}
