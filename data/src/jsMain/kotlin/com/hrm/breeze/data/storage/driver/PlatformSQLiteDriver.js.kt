package com.hrm.breeze.data.storage.driver

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlin.js.ExperimentalWasmJsInterop
import org.w3c.dom.Worker

private const val DATABASE_WORKER_SCRIPT = "breeze-sqljs-worker.js"

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url) => new Worker(url, { type: 'module' })")
private external fun createModuleWorker(url: String): Worker

actual fun createPlatformSQLiteDriver(): SQLiteDriver =
    WebWorkerSQLiteDriver(createModuleWorker(DATABASE_WORKER_SCRIPT))
