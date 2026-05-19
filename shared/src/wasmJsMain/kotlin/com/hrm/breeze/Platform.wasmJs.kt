package com.hrm.breeze

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.WebWasm,
    displayName = "Web with Kotlin/Wasm",
)
