package com.hrm.breeze.data.platform

import okio.Path.Companion.toPath

actual fun createBreezeModelPaths(): BreezeModelPaths =
    BreezeModelPaths(
        root = "breeze-models".toPath(),
        files = "breeze-models/files".toPath(),
        temp = "breeze-models/tmp".toPath(),
        logs = "breeze-models/logs".toPath(),
    )
