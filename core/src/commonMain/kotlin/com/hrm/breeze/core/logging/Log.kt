package com.hrm.breeze.core.logging

import co.touchlab.kermit.Logger as KermitLogger

/**
 * 项目统一日志入口，默认委托给 Kermit。
 * 需要接入 Sentry / Bugsnag 时，扩展一个自定义 LogWriter 即可。
 */
object Log {
    fun d(tag: String, message: () -> String) = KermitLogger.d(tag = tag, messageString = message())
    fun i(tag: String, message: () -> String) = KermitLogger.i(tag = tag, messageString = message())
    fun w(tag: String, throwable: Throwable? = null, message: () -> String) =
        KermitLogger.w(tag = tag, throwable = throwable, messageString = message())
    fun e(tag: String, throwable: Throwable? = null, message: () -> String) =
        KermitLogger.e(tag = tag, throwable = throwable, messageString = message())
}
