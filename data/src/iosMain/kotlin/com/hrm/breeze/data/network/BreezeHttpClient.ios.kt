package com.hrm.breeze.data.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin
