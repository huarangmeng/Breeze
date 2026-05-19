package com.hrm.breeze.data.platform

import okio.Path.Companion.toPath

actual fun createBreezeModelPaths(): BreezeModelPaths {
    val root = resolveBreezeJvmAppSupportFile("models").absolutePath.toPath()
    return BreezeModelPaths(
        root = root,
        files = "${root}/files".toPath(),
        temp = "${root}/tmp".toPath(),
        logs = "${root}/logs".toPath(),
    )
}
