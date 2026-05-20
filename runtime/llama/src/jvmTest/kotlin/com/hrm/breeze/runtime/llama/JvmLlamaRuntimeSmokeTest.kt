package com.hrm.breeze.runtime.llama

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class JvmLlamaRuntimeSmokeTest {
    @Test
    fun streamsTokensFromConfiguredGguf() =
        runTest {
            val modelPath = System.getProperty("breezeSmokeGgufPath") ?: return@runTest
            val runtimeManager = OnDeviceRuntimeManager()

            val chunks =
                runtimeManager.streamCompletion(
                    modelId = "smoke-test",
                    localPath = modelPath,
                    messages =
                        listOf(
                            LlamaMessage(
                                role = LlamaMessage.Role.User,
                                content = "Say hello in one short sentence.",
                            )
                        ),
                    temperature = 0f,
                    topP = 1f,
                    maxTokens = 16,
                    contextWindow = 512,
                ).take(16).toList()

            assertTrue(chunks.joinToString(separator = "").isNotBlank())
        }
}
