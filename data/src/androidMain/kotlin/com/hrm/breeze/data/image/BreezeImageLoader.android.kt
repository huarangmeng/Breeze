package com.hrm.breeze.data.image

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.hrm.breeze.data.platform.requireBreezeAndroidContext
import io.ktor.client.HttpClient

@OptIn(ExperimentalCoilApi::class)
actual fun createBreezeImageLoader(
    httpClient: HttpClient,
): ImageLoader = ImageLoader.Builder(requireBreezeAndroidContext())
    .components {
        add(KtorNetworkFetcherFactory(httpClient))
    }
    .build()
