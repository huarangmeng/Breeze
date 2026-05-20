package com.hrm.breeze.platform

internal actual fun getPlatformInfo(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.WebWasm,
    displayName = "Web with Kotlin/Wasm",
)
