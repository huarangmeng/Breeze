package com.hrm.breeze

internal actual fun getPlatform(): PlatformInfo {
    val osName = System.getProperty("os.name").orEmpty()
    val javaVersion = System.getProperty("java.version").orEmpty()
    val kind = when {
        osName.startsWith("Mac", ignoreCase = true) -> PlatformKind.MacOS
        osName.startsWith("Windows", ignoreCase = true) -> PlatformKind.Windows
        osName.startsWith("Linux", ignoreCase = true) -> PlatformKind.Linux
        else -> PlatformKind.Unknown
    }
    return PlatformInfo(
        kind = kind,
        displayName = "$osName / Java $javaVersion",
    )
}