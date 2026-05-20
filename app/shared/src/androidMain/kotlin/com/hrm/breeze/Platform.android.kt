package com.hrm.breeze

import android.os.LocaleList

internal actual fun getSystemLanguageTag(): String =
    LocaleList.getDefault().get(0)?.toLanguageTag().orEmpty().ifBlank { "en" }
