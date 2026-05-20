package com.hrm.breeze.platform

enum class PlatformKind {
    Android,
    IOS,
    MacOS,
    Windows,
    Linux,
    WebJs,
    WebWasm,
    Unknown,
}

data class PlatformInfo(
    val kind: PlatformKind,
    val displayName: String,
) {
    val isDesktop: Boolean
        get() = kind == PlatformKind.MacOS || kind == PlatformKind.Windows || kind == PlatformKind.Linux

    val isMacDesktop: Boolean
        get() = kind == PlatformKind.MacOS
}

val platformInfo: PlatformInfo by lazy(::getPlatformInfo)

internal expect fun getPlatformInfo(): PlatformInfo
