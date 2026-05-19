package com.hrm.breeze

import platform.UIKit.UIDevice

internal actual fun getPlatform(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.IOS,
    displayName = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion,
)
