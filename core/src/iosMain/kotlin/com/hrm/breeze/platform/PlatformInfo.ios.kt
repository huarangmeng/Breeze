package com.hrm.breeze.platform

import platform.UIKit.UIDevice

internal actual fun getPlatformInfo(): PlatformInfo = PlatformInfo(
    kind = PlatformKind.IOS,
    displayName = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion,
)
