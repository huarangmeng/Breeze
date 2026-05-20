package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import com.hrm.breeze.data.platform.resolveBreezeJvmAppSupportFile
import com.hrm.breeze.domain.model.InferenceRuntimeState
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

internal actual fun createOnDeviceRuntimeBridge(
    httpClient: HttpClient,
    modelPaths: BreezeModelPaths,
): OnDeviceRuntimeBridge = JvmLlamaCppRuntimeBridge(httpClient, modelPaths)

private class JvmLlamaCppRuntimeBridge(
    private val httpClient: HttpClient,
    private val modelPaths: BreezeModelPaths,
) : OnDeviceRuntimeBridge {
    private val sessionMutex = Mutex()
    private var activeSession: LlamaCppServerSession? = null

    override suspend fun ensureModelReady(request: OnDeviceRuntimeLaunchRequest): InferenceRuntimeState =
        runCatching {
            ensureSession(request)
            InferenceRuntimeState.Ready
        }.getOrElse {
            InferenceRuntimeState.Failed
        }

    override suspend fun requireEndpoint(request: OnDeviceRuntimeLaunchRequest): String = ensureSession(request).endpoint

    private suspend fun ensureSession(request: OnDeviceRuntimeLaunchRequest): LlamaCppServerSession =
        sessionMutex.withLock {
            val normalizedRequest = request.normalized()
            val reusableSession = activeSession
            if (reusableSession != null &&
                reusableSession.matches(normalizedRequest) &&
                reusableSession.process.isAlive &&
                reusableSession.isHealthy(httpClient)
            ) {
                return@withLock reusableSession
            }

            reusableSession?.stop()
            val binary = discoverLlamaServerBinary()
            val port = findAvailablePort()
            val logFile = prepareLogFile(normalizedRequest.modelId)
            val process =
                ProcessBuilder(buildCommand(binary, normalizedRequest, port))
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                    .start()
            val session =
                LlamaCppServerSession(
                    modelId = normalizedRequest.modelId,
                    modelPath = normalizedRequest.requireLocalPath(),
                    contextWindow = normalizedRequest.contextWindow,
                    endpoint = "http://127.0.0.1:$port/v1",
                    process = process,
                    logFile = logFile,
                )
            activeSession = session

            try {
                waitUntilReady(session)
                session
            } catch (throwable: Throwable) {
                session.stop()
                if (activeSession === session) {
                    activeSession = null
                }
                throw throwable
            }
        }

    private suspend fun waitUntilReady(session: LlamaCppServerSession) {
        val deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (!session.process.isAlive) {
                val exitCode = runCatching { session.process.exitValue() }.getOrNull()
                error(
                    buildString {
                        append("llama-server exited before becoming ready")
                        exitCode?.let {
                            append(" (exitCode=")
                            append(it)
                            append(')')
                        }
                        append(". Log: ")
                        append(session.logFile.absolutePath)
                    }
                )
            }

            when (val health = queryHealth(session.endpoint)) {
                ServerHealth.Ready -> return
                is ServerHealth.Failed -> {
                    error(
                        buildString {
                            append("llama-server failed to load model")
                            if (!health.details.isNullOrBlank()) {
                                append(": ")
                                append(health.details)
                            }
                            append(". Log: ")
                            append(session.logFile.absolutePath)
                        }
                    )
                }

                ServerHealth.Loading,
                ServerHealth.Unreachable,
                -> delay(HEALTH_POLL_INTERVAL_MILLIS)
            }
        }

        error("Timed out waiting for llama-server to become ready. Log: ${session.logFile.absolutePath}")
    }

    private suspend fun LlamaCppServerSession.isHealthy(httpClient: HttpClient): Boolean =
        when (queryHealth(endpoint)) {
            ServerHealth.Ready -> true
            ServerHealth.Loading,
            ServerHealth.Unreachable,
            is ServerHealth.Failed,
            -> false
        }

    private suspend fun queryHealth(endpoint: String): ServerHealth {
        val baseEndpoint = endpoint.removeSuffix("/v1")
        val candidates = listOf("$baseEndpoint/health", "$baseEndpoint/v1/health")
        var sawLoading = false
        for (url in candidates) {
            val response = runCatching { httpClient.get(url) }.getOrNull() ?: continue
            when (response.status) {
                HttpStatusCode.OK -> return ServerHealth.Ready
                HttpStatusCode.ServiceUnavailable -> sawLoading = true
                HttpStatusCode.InternalServerError -> return ServerHealth.Failed(response.bodyAsText())
            }
        }
        return if (sawLoading) ServerHealth.Loading else ServerHealth.Unreachable
    }

    private fun buildCommand(
        binary: File,
        request: OnDeviceRuntimeLaunchRequest,
        port: Int,
    ): List<String> =
        buildList {
            add(binary.absolutePath)
            add("--host")
            add(LLAMA_SERVER_HOST)
            add("--port")
            add(port.toString())
            add("--model")
            add(request.requireLocalPath())
            add("--alias")
            add(request.modelId)
            add("--ctx-size")
            add(request.contextWindow.toString())
            add("--parallel")
            add("1")
        }

    private fun discoverLlamaServerBinary(): File {
        val configuredPath = System.getenv(ENV_BREEZE_LLAMA_SERVER_PATH)
        configuredPath
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?.takeIf(File::isUsableExecutable)
            ?.let { return it }

        val bundledCandidates =
            listOf(
                resolveBreezeJvmAppSupportFile("runtime/llama-server"),
                resolveBreezeJvmAppSupportFile("runtime/llama-server.exe"),
                File("/opt/homebrew/bin/llama-server"),
                File("/usr/local/bin/llama-server"),
            )
        bundledCandidates.firstOrNull(File::isUsableExecutable)?.let { return it }

        findBinaryFromPath("llama-server")?.let { return it }
        findBinaryFromPath("llama-server.exe")?.let { return it }

        error(
            "Unable to find llama-server. Set $ENV_BREEZE_LLAMA_SERVER_PATH or install llama.cpp so llama-server is available in PATH."
        )
    }

    private fun findBinaryFromPath(fileName: String): File? {
        val pathEntries = System.getenv("PATH").orEmpty().split(File.pathSeparatorChar)
        return pathEntries
            .asSequence()
            .filter(String::isNotBlank)
            .map(::File)
            .map { dir -> File(dir, fileName) }
            .firstOrNull(File::isUsableExecutable)
    }

    private fun prepareLogFile(modelId: String): File {
        val logsDir = File(modelPaths.logs.toString()).apply { mkdirs() }
        return File(logsDir, "llama-server-${modelId.toSafeFileSegment()}.log")
    }
}

private data class LlamaCppServerSession(
    val modelId: String,
    val modelPath: String,
    val contextWindow: Int,
    val endpoint: String,
    val process: Process,
    val logFile: File,
) {
    fun matches(request: OnDeviceRuntimeLaunchRequest): Boolean =
        modelId == request.modelId &&
            modelPath == request.requireLocalPath() &&
            contextWindow == request.contextWindow

    fun stop() {
        if (!process.isAlive) {
            return
        }
        process.destroy()
        if (!runCatching { process.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS) }.getOrDefault(false)) {
            process.destroyForcibly()
            runCatching { process.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        }
    }
}

private sealed interface ServerHealth {
    data object Ready : ServerHealth

    data object Loading : ServerHealth

    data object Unreachable : ServerHealth

    data class Failed(
        val details: String?,
    ) : ServerHealth
}

private fun OnDeviceRuntimeLaunchRequest.normalized(): OnDeviceRuntimeLaunchRequest {
    require(contextWindow > 0) { "Context window must be positive" }
    val localPath = requireLocalPath()
    check(File(localPath).isFile) { "Downloaded on-device model file is missing: $localPath" }
    return copy(localPath = localPath)
}

private fun OnDeviceRuntimeLaunchRequest.requireLocalPath(): String =
    checkNotNull(localPath?.takeIf(String::isNotBlank)) { "Missing local model file path" }

private fun File.isUsableExecutable(): Boolean = isFile && canExecute()

private fun findAvailablePort(): Int = ServerSocket(0).use { it.localPort }

private fun String.toSafeFileSegment(): String = replace(Regex("[^a-zA-Z0-9._-]"), "_")

private const val ENV_BREEZE_LLAMA_SERVER_PATH = "BREEZE_LLAMA_SERVER_PATH"
private const val LLAMA_SERVER_HOST = "127.0.0.1"
private const val STARTUP_TIMEOUT_MILLIS = 180_000L
private const val HEALTH_POLL_INTERVAL_MILLIS = 500L
private const val PROCESS_STOP_TIMEOUT_SECONDS = 5L
