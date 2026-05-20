package com.hrm.breeze

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Window as AwtWindow

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Breeze",
    ) {
        DisposableEffect(window) {
            configureBreezeMacOsTransparentTitleBar(window)
            onDispose {
            }
        }

        App()
    }
}

private fun configureBreezeMacOsTransparentTitleBar(window: AwtWindow) {
    if (!platformInfo.isMacDesktop) {
        return
    }

    val rootPane = (window as? javax.swing.JFrame)?.rootPane ?: return
    rootPane.putClientProperty("apple.awt.fullWindowContent", true)
    rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
    rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
}
