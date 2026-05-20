package com.hrm.breeze

import platform.Foundation.NSUserDefaults

internal actual fun getSystemLanguageTag(): String =
    (NSUserDefaults.standardUserDefaults.stringArrayForKey("AppleLanguages")?.firstOrNull() as? String)
        .orEmpty()
        .ifBlank { "en" }
