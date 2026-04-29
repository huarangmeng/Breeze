package com.hrm.breeze.data.image

import coil3.ImageLoader
import io.ktor.client.HttpClient

expect fun createBreezeImageLoader(
    httpClient: HttpClient,
): ImageLoader
