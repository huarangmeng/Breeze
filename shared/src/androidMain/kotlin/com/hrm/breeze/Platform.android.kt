package com.hrm.breeze

import android.os.Build

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.Android,
    displayName = "Android ${Build.VERSION.SDK_INT}",
)
