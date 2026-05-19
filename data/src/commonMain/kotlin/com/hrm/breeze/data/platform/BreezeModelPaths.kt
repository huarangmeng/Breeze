package com.hrm.breeze.data.platform

import okio.Path

data class BreezeModelPaths(
    val root: Path,
    val files: Path,
    val temp: Path,
    val logs: Path,
)

expect fun createBreezeModelPaths(): BreezeModelPaths
