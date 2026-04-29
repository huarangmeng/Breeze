package com.hrm.breeze.data.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java

actual fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*> = Java
