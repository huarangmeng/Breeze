package com.hrm.breeze.data.network

import com.hrm.breeze.core.logging.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

private const val HTTP_LOG_TAG = "BreezeHttpClient"

expect fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*>

fun createBreezeHttpClient(
    engineFactory: HttpClientEngineFactory<*> = platformHttpClientEngineFactory(),
): HttpClient = HttpClient(engineFactory) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(BreezeJson)
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Log.d(HTTP_LOG_TAG) { message }
            }
        }
        level = LogLevel.INFO
    }

    install(SSE)

    defaultRequest {
        headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        contentType(ContentType.Application.Json)
    }
}
