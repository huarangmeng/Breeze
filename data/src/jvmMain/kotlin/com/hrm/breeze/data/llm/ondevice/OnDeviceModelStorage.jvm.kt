package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import okio.FileSystem
import okio.Path
import okio.buffer

internal actual fun ensureModelDirectories(modelPaths: BreezeModelPaths) {
    val fileSystem = FileSystem.SYSTEM
    fileSystem.createDirectories(modelPaths.root)
    fileSystem.createDirectories(modelPaths.files)
    fileSystem.createDirectories(modelPaths.temp)
    fileSystem.createDirectories(modelPaths.logs)
}

internal actual fun deleteModelFile(path: Path) {
    FileSystem.SYSTEM.delete(path, mustExist = false)
}

internal actual fun modelFileExists(path: Path): Boolean = FileSystem.SYSTEM.exists(path)

internal actual suspend fun persistStatementToFile(
    statement: HttpStatement,
    tempPath: Path,
    finalPath: Path,
    onProgress: suspend (downloadedBytes: Long, totalBytes: Long?) -> Unit,
): Long {
    val fileSystem = FileSystem.SYSTEM
    fileSystem.delete(tempPath, mustExist = false)
    var downloaded = 0L
    statement.execute { response ->
        val totalBytes = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        val sink = fileSystem.sink(tempPath).buffer()
        try {
            val channel = response.bodyAsChannel()
            while (true) {
                val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                if (bytesRead <= 0) {
                    break
                }
                sink.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                onProgress(downloaded, totalBytes)
            }
        } finally {
            sink.close()
        }
        fileSystem.atomicMove(tempPath, finalPath)
    }
    return downloaded
}

private const val DOWNLOAD_BUFFER_SIZE = 8 * 1024
