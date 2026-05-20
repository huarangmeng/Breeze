package com.hrm.breeze.platform

import android.os.Build

internal actual fun getPlatformInfo(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.Android,
    displayName = "Android ${Build.VERSION.SDK_INT}",
)
