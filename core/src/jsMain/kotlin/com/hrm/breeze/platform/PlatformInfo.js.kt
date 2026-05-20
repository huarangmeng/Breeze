package com.hrm.breeze.platform

internal actual fun getPlatformInfo(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.WebJs,
    displayName = "Web with Kotlin/JS",
)
