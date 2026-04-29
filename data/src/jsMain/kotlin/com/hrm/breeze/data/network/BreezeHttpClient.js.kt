package com.hrm.breeze.data.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*> = Js
