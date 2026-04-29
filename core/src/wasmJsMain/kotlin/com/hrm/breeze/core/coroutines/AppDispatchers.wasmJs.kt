package com.hrm.breeze.core.coroutines

import kotlinx.coroutines.Dispatchers

private object WasmJsAppDispatchers : AppDispatchers {
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default

    // 浏览器只有单线程，io 退化为 Default。
    override val io = Dispatchers.Default
}

actual fun defaultAppDispatchers(): AppDispatchers = WasmJsAppDispatchers
