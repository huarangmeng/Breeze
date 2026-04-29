package com.hrm.breeze.core.coroutines

import kotlinx.coroutines.Dispatchers

private object IosAppDispatchers : AppDispatchers {
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default

    // iOS 没有 Dispatchers.IO，网络/磁盘 IO 走 Default 即可（K/N 线程池）。
    override val io = Dispatchers.Default
}

actual fun defaultAppDispatchers(): AppDispatchers = IosAppDispatchers
