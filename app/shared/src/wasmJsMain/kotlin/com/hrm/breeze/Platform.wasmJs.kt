package com.hrm.breeze

import kotlin.js.ExperimentalWasmJsInterop

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.WebWasm,
    displayName = "Web with Kotlin/Wasm",
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => (typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en'")
private external fun browserLanguage(): String

internal actual fun getSystemLanguageTag(): String = browserLanguage()
