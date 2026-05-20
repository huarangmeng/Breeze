package com.hrm.breeze.data.rag

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.data.embedding.EmbeddingProvider
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.createPlatformDatabaseBuilder
import com.hrm.breeze.data.storage.entity.MessageEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HybridRagRetrieverJvmTest {
    @Test
    fun retrievesIndexedConversationTurnWithLexicalFallbackWhenEmbeddingIsNotReady() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-rag-test")
        val database =
            BreezeDatabase.build(
                builder = createPlatformDatabaseBuilder("$tempDirectory/breeze-rag-test.db"),
                driver = BundledSQLiteDriver(),
                dispatchers = dispatchers,
            )
        val store = RagStore(database.ragDocumentDao(), database.ragChunkDao())
        val embeddingProvider = NotReadyEmbeddingProvider()
        val indexer = RagIndexer(store, embeddingProvider)
        val retriever = HybridRagRetriever(embeddingProvider, store)

        try {
            indexer.indexConversationTurn(
                conversationId = "conversation-1",
                userMessage =
                    MessageEntity(
                        id = "user-1",
                        conversationId = "conversation-1",
                        role = "user",
                        content = "怎么配置 Qwen3 Embedding 做本地 RAG？",
                        createdAtEpochMillis = 1,
                    ),
                assistantMessage =
                    MessageEntity(
                        id = "assistant-1",
                        conversationId = "conversation-1",
                        role = "assistant",
                        content = "下载 GGUF 后通过 llama.cpp embedding runtime 生成向量。",
                        createdAtEpochMillis = 2,
                    ),
            )

            val result = retriever.retrieve("Qwen3 Embedding 本地向量", RetrievalScope(topK = 3))

            assertEquals(1, result.chunks.size)
            assertTrue(result.chunks.single().content.contains("Qwen3 Embedding"))
            assertTrue(result.chunks.single().score > 0f)
        } finally {
            database.close()
        }
    }
}

private class NotReadyEmbeddingProvider : EmbeddingProvider {
    override val modelId: String = "not-ready"
    override val dimension: Int = 0

    override suspend fun isReady(): Boolean = false

    override suspend fun embed(texts: List<String>): List<FloatArray> = emptyList()
}

private class TestDispatchers(
    dispatcher: CoroutineDispatcher,
) : AppDispatchers {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
