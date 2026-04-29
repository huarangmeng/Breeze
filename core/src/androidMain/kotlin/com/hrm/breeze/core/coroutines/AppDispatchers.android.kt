package com.hrm.breeze.core.coroutines

import kotlinx.coroutines.Dispatchers

private object AndroidAppDispatchers : AppDispatchers {
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default
    override val io = Dispatchers.IO
}

actual fun defaultAppDispatchers(): AppDispatchers = AndroidAppDispatchers
