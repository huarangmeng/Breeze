package com.hrm.breeze

import java.awt.Desktop
import java.io.File

internal actual fun openDirectoryForPath(path: String): Boolean =
    runCatching {
        val target = File(path)
        val directory = if (target.isDirectory) target else target.parentFile
        checkNotNull(directory) { "Directory is unavailable for path: $path" }
        check(directory.exists()) { "Directory does not exist: ${directory.absolutePath}" }
        Desktop.getDesktop().open(directory)
    }.isSuccess
