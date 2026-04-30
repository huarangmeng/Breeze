package com.hrm.breeze.data.network

import com.hrm.breeze.core.logging.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.http.path
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString

private const val HTTP_LOG_TAG = "BreezeHttpClient"
const val BREEZE_MOCK_ECHO_ENDPOINT: String = "https://mock.breeze.local/v1/chat/echo"

expect fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*>

fun createBreezeHttpClient(
    engineFactory: HttpClientEngineFactory<*> = platformHttpClientEngineFactory(),
): HttpClient = HttpClient(engineFactory) {
    applyBreezeDefaults()
}

fun createMockBreezeHttpClient(): HttpClient = HttpClient(MockEngine) {
    applyBreezeDefaults()

    engine {
        addHandler { request ->
            if (request.method != HttpMethod.Post || request.url.toString() != BREEZE_MOCK_ECHO_ENDPOINT) {
                return@addHandler respond(
                    content = """{"message":"mock endpoint not found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

            val requestBody = request.body.readJsonBody()
            val echoRequest = BreezeJson.decodeFromString<ChatEchoRequest>(requestBody)
            val response =
                ChatEchoResponse(
                    message = "Breeze mock(${echoRequest.modelId}): ${echoRequest.text}",
                )

            respond(
                content = BreezeJson.encodeToString(response),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    }
}

private fun HttpClientConfig<*>.applyBreezeDefaults() {
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

private fun Any.readJsonBody(): String = when (this) {
    is TextContent -> text
    is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    else -> error("Unsupported mock request body: ${this::class.simpleName}")
}
