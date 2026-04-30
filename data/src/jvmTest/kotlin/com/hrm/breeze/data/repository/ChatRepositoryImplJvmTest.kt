package com.hrm.breeze.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.hrm.breeze.core.coroutines.AppDispatchers
import com.hrm.breeze.data.llm.LlmProviderRegistry
import com.hrm.breeze.data.llm.LocalProvider
import com.hrm.breeze.data.network.BREEZE_MOCK_ECHO_ENDPOINT
import com.hrm.breeze.data.network.BreezeChatApi
import com.hrm.breeze.data.network.KtorBreezeChatApi
import com.hrm.breeze.data.network.createMockBreezeHttpClient
import com.hrm.breeze.data.settings.BreezeSettings
import com.hrm.breeze.data.storage.BreezeDatabase
import com.hrm.breeze.data.storage.createPlatformDatabaseBuilder
import com.hrm.breeze.domain.model.LlmProviderId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
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
        val dispatchers = TestAppDispatchers(dispatcher)
        val tempDirectory = Files.createTempDirectory("breeze-chat-repository-test")
        val database = createDatabase(tempDirectory.toString(), dispatchers)
        val settings = createSettings(tempDirectory.toString())
        val httpClient = createMockBreezeHttpClient()
        val chatApi: BreezeChatApi = KtorBreezeChatApi(httpClient) { BREEZE_MOCK_ECHO_ENDPOINT }
        val providerRegistry = LlmProviderRegistry(listOf(LocalProvider(chatApi)))
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
                settings = settings,
                dispatchers = dispatchers,
                clock = clock,
            )

        settings.updateCurrentProviderId(LlmProviderId.Local)
        settings.updateCurrentModelId("mock-model")
        advanceUntilIdle()

        try {
            val emitted = repository.sendMessage(conversationId = "conversation-1", text = "hello breeze").toList()
            advanceUntilIdle()

            val conversations = repository.observeConversations().first()
            val messages = repository.observeMessages("conversation-1").first()

            assertEquals(1, emitted.size)
            assertEquals("Breeze mock(mock-model): hello breeze", emitted.single().content)

            assertEquals(1, conversations.size)
            assertEquals("hello breeze", conversations.single().title)
            assertEquals("mock-model", conversations.single().modelId)

            assertEquals(2, messages.size)
            assertEquals("hello breeze", messages.first().content)
            assertEquals("Breeze mock(mock-model): hello breeze", messages.last().content)
            assertEquals(
                listOf(
                    com.hrm.breeze.domain.model.Message.Role.User,
                    com.hrm.breeze.domain.model.Message.Role.Assistant,
                ),
                messages.map { it.role },
            )
            assertTrue(messages.last().createdAt > messages.first().createdAt)
        } finally {
            httpClient.close()
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

private class TestAppDispatchers(
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
