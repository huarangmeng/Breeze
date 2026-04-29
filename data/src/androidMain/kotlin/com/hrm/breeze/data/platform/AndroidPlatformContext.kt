package com.hrm.breeze.data.platform

import android.content.Context

private var appContext: Context? = null

fun initializeBreezeAndroidPlatform(context: Context) {
    appContext = context.applicationContext
}

internal fun requireBreezeAndroidContext(): Context = requireNotNull(appContext) {
    "Breeze Android 平台上下文尚未初始化，请先调用 initializeBreezeAndroidPlatform(context)。"
}
