package com.hrm.breeze.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.data.conversation.ConversationContextAssembler
import com.hrm.breeze.data.conversation.ConversationContextManager
import com.hrm.breeze.data.conversation.ConversationSummarizer
import com.hrm.breeze.data.embedding.EmbeddingProvider
import com.hrm.breeze.data.llm.LlmCompletionRequest
import com.hrm.breeze.data.llm.LlmProvider
import com.hrm.breeze.data.llm.LlmProviderRegistry
import com.hrm.breeze.data.llm.LlmStreamDelta
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.entity.ConversationEntity
import com.hrm.breeze.data.storage.entity.ConversationSummaryEntity
import com.hrm.breeze.data.storage.entity.MessageEntity
import com.hrm.breeze.data.storage.createPlatformDatabaseBuilder
import com.hrm.breeze.data.rag.HybridRagRetriever
import com.hrm.breeze.data.rag.RagContextProvider
import com.hrm.breeze.data.rag.RagIndexer
import com.hrm.breeze.data.rag.RagStore
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplJvmTest {
    @Test
    fun sendMessagePersistsConversationAndMessages() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = ChatRepositoryTestDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-chat-repository-test")
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val modelConfigRepository = ModelConfigRepositoryImpl(database, settings, clock = Clock.System)
        val providerRegistry = LlmProviderRegistry(listOf(TestLocalProvider()))
        val clock =
            SequenceClock(
                instants =
                    listOf(
                        Instant.fromEpochMilliseconds(1_710_000_000_000),
                        Instant.fromEpochMilliseconds(1_710_000_000_500),
                    )
            )
        val repository =
            ChatRepositoryImpl(
                database = database,
                llmProviderRegistry = providerRegistry,
                modelConfigRepository = modelConfigRepository,
                settings = settings,
                conversationContextManager = createConversationContextManager(database, dispatchers),
                dispatchers = dispatchers,
                clock = clock,
            )

        modelConfigRepository.createAndActivateConfig(
            providerId = LlmProviderId.OpenAI,
            endpoint = "local://runtime",
            apiToken = null,
            modelId = "mock-model",
        )
        advanceUntilIdle()

        try {
            val emitted = repository.sendMessage(conversationId = "conversation-1", text = "hello breeze", reasoningEnabled = false).toList()
            advanceUntilIdle()

            val conversations = repository.observeConversations().first()
            val messages = repository.observeMessages("conversation-1").first()

            assertEquals(1, emitted.size)
            assertEquals("Breeze local(mock-model): hello breeze", emitted.single().content)

            assertEquals(1, conversations.size)
            assertEquals("hello breeze", conversations.single().title)
            assertEquals("mock-model", conversations.single().modelId)

            assertEquals(2, messages.size)
            assertEquals("hello breeze", messages.first().content)
            assertEquals("Breeze local(mock-model): hello breeze", messages.last().content)
            assertEquals(
                listOf(
                    com.hrm.breeze.domain.model.Message.Role.User,
                    com.hrm.breeze.domain.model.Message.Role.Assistant,
                ),
                messages.map { it.role },
            )
            assertTrue(messages.last().createdAt > messages.first().createdAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun sendMessagePersistsAssistantDraftAsStreamArrives() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = ChatRepositoryTestDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-chat-repository-stream-test")
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val modelConfigRepository = ModelConfigRepositoryImpl(database, settings, clock = Clock.System)
        val providerRegistry = LlmProviderRegistry(listOf(StreamingLocalProvider()))
        val clock =
            SequenceClock(
                instants =
                    listOf(
                        Instant.fromEpochMilliseconds(1_710_100_000_000),
                        Instant.fromEpochMilliseconds(1_710_100_000_500),
                    )
            )
        val repository =
            ChatRepositoryImpl(
                database = database,
                llmProviderRegistry = providerRegistry,
                modelConfigRepository = modelConfigRepository,
                settings = settings,
                conversationContextManager = createConversationContextManager(database, dispatchers),
                dispatchers = dispatchers,
                clock = clock,
            )

        modelConfigRepository.createAndActivateConfig(
            providerId = LlmProviderId.OpenAI,
            endpoint = "local://runtime",
            apiToken = null,
            modelId = "mock-model",
        )
        advanceUntilIdle()

        try {
            val emitted = repository.sendMessage(conversationId = "conversation-stream", text = "hello breeze", reasoningEnabled = false).toList()
            advanceUntilIdle()

            val messages = repository.observeMessages("conversation-stream").first()

            assertEquals(5, emitted.size)
            assertEquals("Breeze ", emitted[0].content)
            assertEquals("Breeze local(mock-model): hello breeze", emitted.last().content)
            assertEquals(2, messages.size)
            assertEquals("Breeze local(mock-model): hello breeze", messages.last().content)
        } finally {
            database.close()
        }
    }

    @Test
    fun sendMessagePersistsReasoningSeparatelyFromAnswer() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = ChatRepositoryTestDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-chat-repository-reasoning-test")
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val modelConfigRepository = ModelConfigRepositoryImpl(database, settings, clock = Clock.System)
        val providerRegistry = LlmProviderRegistry(listOf(ReasoningLocalProvider()))
        val clock =
            SequenceClock(
                instants =
                    listOf(
                        Instant.fromEpochMilliseconds(1_710_200_000_000),
                        Instant.fromEpochMilliseconds(1_710_200_000_500),
                    )
            )
        val repository =
            ChatRepositoryImpl(
                database = database,
                llmProviderRegistry = providerRegistry,
                modelConfigRepository = modelConfigRepository,
                settings = settings,
                conversationContextManager = createConversationContextManager(database, dispatchers),
                dispatchers = dispatchers,
                clock = clock,
            )

        modelConfigRepository.createAndActivateConfig(
            providerId = LlmProviderId.OpenAI,
            endpoint = "local://runtime",
            apiToken = null,
            modelId = "mock-model",
        )
        advanceUntilIdle()

        try {
            val emitted = repository.sendMessage(conversationId = "conversation-reasoning", text = "hello breeze", reasoningEnabled = true).toList()
            advanceUntilIdle()

            val messages = repository.observeMessages("conversation-reasoning").first()

            assertEquals(3, emitted.size)
            assertEquals("先分析问题", emitted[0].reasoningContent)
            assertEquals("先分析问题，再组织答案", emitted[1].reasoningContent)
            assertEquals("Final answer", emitted.last().content)
            assertEquals("先分析问题，再组织答案", emitted.last().reasoningContent)
            assertEquals(2, messages.size)
            assertEquals("Final answer", messages.last().content)
            assertEquals("先分析问题，再组织答案", messages.last().reasoningContent)
        } finally {
            database.close()
        }
    }

    @Test
    fun sendMessageUsesSummaryAndRecentMessagesForProviderContext() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = ChatRepositoryTestDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-chat-repository-context-test")
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val modelConfigRepository = ModelConfigRepositoryImpl(database, settings, clock = Clock.System)
        val provider = CapturingLocalProvider()
        val providerRegistry = LlmProviderRegistry(listOf(provider))
        val repository =
            ChatRepositoryImpl(
                database = database,
                llmProviderRegistry = providerRegistry,
                modelConfigRepository = modelConfigRepository,
                settings = settings,
                conversationContextManager = createConversationContextManager(database, dispatchers),
                dispatchers = dispatchers,
                clock = Clock.System,
            )

        modelConfigRepository.createAndActivateConfig(
            providerId = LlmProviderId.OpenAI,
            endpoint = "local://runtime",
            apiToken = null,
            modelId = "mock-model",
        )
        database.conversationDao().upsertConversation(
            ConversationEntity(
                id = "conversation-context",
                title = "context",
                modelId = "mock-model",
                updatedAtEpochMillis = 12,
            )
        )
        (1..12).forEach { index ->
            database.messageDao().insertMessage(
                MessageEntity(
                    id = "m$index",
                    conversationId = "conversation-context",
                    role = if (index % 2 == 0) "assistant" else "user",
                    content = "message-$index",
                    createdAtEpochMillis = index.toLong(),
                )
            )
        }
        database.conversationSummaryDao().upsertSummary(
            ConversationSummaryEntity(
                conversationId = "conversation-context",
                summary = "Earlier messages established the project constraints.",
                coveredUntilMessageCreatedAtEpochMillis = 8,
                coveredUntilMessageId = "m8",
                updatedAtEpochMillis = 20,
            )
        )
        advanceUntilIdle()

        try {
            repository.sendMessage(
                conversationId = "conversation-context",
                text = "new question",
                reasoningEnabled = false,
            ).toList()
            advanceUntilIdle()

            val chatRequest = provider.requests.first()

            assertTrue(chatRequest.messages.first().content.contains("Earlier messages established"))
            assertTrue(chatRequest.messages.any { it.content == "message-9" })
            assertTrue(chatRequest.messages.none { it.content == "message-8" })
            assertEquals("new question", chatRequest.messages.last().content)
        } finally {
            database.close()
        }
    }
}

private fun createDatabase(
    tempDirectory: String,
    dispatchers: AppDispatchers,
): BreezeDatabase =
    BreezeDatabase.build(
        builder = createPlatformDatabaseBuilder("$tempDirectory/breeze-test.db"),
        driver = BundledSQLiteDriver(),
        dispatchers = dispatchers,
    )

private fun createSettings(
    tempDirectory: String,
): BreezeSettings = BreezeSettings(
    dataStore =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "$tempDirectory/breeze-settings.preferences_pb".toPath() },
        )
)

private fun createConversationContextManager(
    database: BreezeDatabase,
    dispatchers: AppDispatchers,
): ConversationContextManager =
    RagStore(
        documentDao = database.ragDocumentDao(),
        chunkDao = database.ragChunkDao(),
    ).let { ragStore ->
        val embeddingProvider = TestEmbeddingProvider()
    ConversationContextManager(
        summaryDao = database.conversationSummaryDao(),
        contextAssembler = ConversationContextAssembler(),
        conversationSummarizer = ConversationSummarizer(database.conversationSummaryDao()),
            ragContextProvider = RagContextProvider(HybridRagRetriever(embeddingProvider, ragStore)),
            ragIndexer = RagIndexer(ragStore, embeddingProvider),
        backgroundScope = CoroutineScope(dispatchers.io),
    )
    }

private class ChatRepositoryTestDispatchers(
    dispatcher: CoroutineDispatcher,
) : AppDispatchers {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}

private class SequenceClock(
    private val instants: List<Instant>,
) : Clock {
    private var index: Int = 0

    override fun now(): Instant {
        val current = instants[index.coerceAtMost(instants.lastIndex)]
        index += 1
        return current
    }
}

private class StreamingLocalProvider : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.OpenAI

    override fun stream(request: LlmCompletionRequest) = flowOf(
        LlmStreamDelta(contentDelta = "Breeze "),
        LlmStreamDelta(contentDelta = "local("),
        LlmStreamDelta(contentDelta = request.model.id),
        LlmStreamDelta(contentDelta = "): "),
        LlmStreamDelta(contentDelta = request.messages.last().content),
    )
}

private class TestLocalProvider : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.OpenAI

    override fun stream(request: LlmCompletionRequest) = flowOf(
        LlmStreamDelta(contentDelta = "Breeze local(${request.model.id}): ${request.messages.last().content}"),
    )
}

private class ReasoningLocalProvider : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.OpenAI

    override fun stream(request: LlmCompletionRequest) = flowOf(
        LlmStreamDelta(reasoningDelta = "先分析问题"),
        LlmStreamDelta(reasoningDelta = "，再组织答案"),
        LlmStreamDelta(contentDelta = "Final answer"),
    )
}

private class CapturingLocalProvider : LlmProvider {
    override val id: LlmProviderId = LlmProviderId.OpenAI
    val requests = mutableListOf<LlmCompletionRequest>()

    override fun stream(request: LlmCompletionRequest) = flowOf(
        LlmStreamDelta(contentDelta = "captured"),
    ).also {
        requests += request
    }
}

private class TestEmbeddingProvider : EmbeddingProvider {
    override val modelId: String = "test-embedding"
    override val dimension: Int = 0

    override suspend fun isReady(): Boolean = false

    override suspend fun embed(texts: List<String>): List<FloatArray> = emptyList()
}
