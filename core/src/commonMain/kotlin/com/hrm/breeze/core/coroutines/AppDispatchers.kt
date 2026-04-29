package com.hrm.breeze.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * 统一的调度器入口。所有 suspend 代码通过它拿 dispatcher，不要直接用 Dispatchers.IO 等。
 */
interface AppDispatchers {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

expect fun defaultAppDispatchers(): AppDispatchers
