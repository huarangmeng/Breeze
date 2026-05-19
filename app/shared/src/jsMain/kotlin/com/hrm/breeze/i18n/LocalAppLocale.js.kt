package com.hrm.breeze.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => (typeof window !== 'undefined' && window.__customLocale) ? window.__customLocale : ((typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en')")
private external fun resolvedBrowserLocale(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(value) => { if (typeof window !== 'undefined') { window.__customLocale = value; } }")
private external fun setCustomLocale(value: String?)

actual object LocalAppLocale {
    private val appLocale = staticCompositionLocalOf { resolvedBrowserLocale() }

    actual val current: String
        @Composable get() = appLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        setCustomLocale(value)
        return appLocale.provides(value ?: resolvedBrowserLocale())
    }
}
