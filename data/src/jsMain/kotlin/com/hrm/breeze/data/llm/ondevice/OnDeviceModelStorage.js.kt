package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import io.ktor.client.statement.HttpStatement
import okio.Path

internal actual fun ensureModelDirectories(modelPaths: BreezeModelPaths) = Unit

internal actual fun deleteModelFile(path: Path) = Unit

internal actual fun modelFileExists(path: Path): Boolean = false

internal actual suspend fun persistStatementToFile(
    statement: HttpStatement,
    tempPath: Path,
    finalPath: Path,
    onProgress: suspend (downloadedBytes: Long, totalBytes: Long?) -> Unit,
): Long = error("On-device model file persistence is not implemented on JS yet")
