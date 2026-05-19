package com.hrm.breeze

import android.os.Build
import android.os.LocaleList

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.Android,
    displayName = "Android ${Build.VERSION.SDK_INT}",
)

internal actual fun getSystemLanguageTag(): String =
    LocaleList.getDefault().get(0)?.toLanguageTag().orEmpty().ifBlank { "en" }
