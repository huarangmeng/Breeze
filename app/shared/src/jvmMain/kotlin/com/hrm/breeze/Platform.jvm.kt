package com.hrm.breeze

import java.util.Locale

internal actual fun getSystemLanguageTag(): String =
    Locale.getDefault().toLanguageTag().ifBlank { "en" }
