package com.hrm.breeze

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => (typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en'")
private external fun browserLanguage(): String

internal actual fun getSystemLanguageTag(): String = browserLanguage()
