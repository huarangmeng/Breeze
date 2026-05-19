package com.hrm.breeze.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null
    private val appLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = appLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val locale = value?.let(Locale::forLanguageTag) ?: defaultLocale!!
        Locale.setDefault(locale)
        return appLocale.provides(locale.toLanguageTag())
    }
}
