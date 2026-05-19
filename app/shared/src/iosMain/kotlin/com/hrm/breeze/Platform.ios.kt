package com.hrm.breeze

import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.IOS,
    displayName = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion,
)

internal actual fun getSystemLanguageTag(): String =
    (NSUserDefaults.standardUserDefaults.stringArrayForKey("AppleLanguages")?.firstOrNull() as? String)
        .orEmpty()
        .ifBlank { "en" }
