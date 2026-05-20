package com.hrm.breeze.runtime.llama

import com.hrm.breeze.platform.PlatformKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmLlamaRuntimePathTest {
    @Test
    fun computesWindowsPlatformSegment() {
        assertEquals(
            expected = "windows-x64",
            actual = runtimePlatformSegment(platformKind = PlatformKind.Windows, archName = "amd64"),
        )
        assertEquals(
            expected = "windows-arm64",
            actual = runtimePlatformSegment(platformKind = PlatformKind.Windows, archName = "aarch64"),
        )
    }

    @Test
    fun resolvesWindowsRuntimeDirectoryFromAppData() {
        val directory =
            resolveJvmRuntimeDirectory(
                userHome = "C:/Users/Breeze",
                platformKind = PlatformKind.Windows,
                appData = "C:/Users/Breeze/AppData/Roaming",
                xdgDataHome = null,
            )

        assertEquals(
            expected = File("C:/Users/Breeze/AppData/Roaming/Breeze/models/runtime/native").path,
            actual = directory.path,
        )
    }

    @Test
    fun fallsBackToWindowsRoamingDirectoryWhenAppDataIsMissing() {
        val directory =
            resolveJvmRuntimeDirectory(
                userHome = "C:/Users/Breeze",
                platformKind = PlatformKind.Windows,
                appData = null,
                xdgDataHome = null,
            )

        assertEquals(
            expected = File("C:/Users/Breeze/AppData/Roaming/Breeze/models/runtime/native").path,
            actual = directory.path,
        )
    }

    @Test
    fun computesBundledWindowsRuntimeResourcePath() {
        assertEquals(
            expected = "/breeze-runtime/windows-x64/breeze_llama_jni.dll",
            actual = bundledRuntimeResourcePath(
                platformSegment = "windows-x64",
                fileName = "breeze_llama_jni.dll",
            ),
        )
    }
}
