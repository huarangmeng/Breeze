package com.hrm.breeze

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.WebJs,
    displayName = "Web with Kotlin/JS",
)
