package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.data.platform.BreezeModelPaths
import io.ktor.client.statement.HttpStatement
import okio.Path

internal expect fun ensureModelDirectories(modelPaths: BreezeModelPaths)

internal expect fun deleteModelFile(path: Path)

internal expect fun modelFileExists(path: Path): Boolean

internal expect suspend fun persistStatementToFile(
    statement: HttpStatement,
    tempPath: Path,
    finalPath: Path,
    onProgress: suspend (downloadedBytes: Long, totalBytes: Long?) -> Unit,
): Long
