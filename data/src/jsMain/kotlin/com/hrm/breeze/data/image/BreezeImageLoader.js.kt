package com.hrm.breeze.data.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient

@OptIn(ExperimentalCoilApi::class)
actual fun createBreezeImageLoader(
    httpClient: HttpClient,
): ImageLoader = ImageLoader.Builder(PlatformContext.INSTANCE)
    .components {
        add(KtorNetworkFetcherFactory(httpClient))
    }
    .build()
