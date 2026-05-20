package com.hrm.breeze.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.hrm.breeze.generated.resources.*
import com.hrm.breeze.getSystemLanguageTag
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class BreezeLanguagePreference(val storageValue: String) {
    System("system"),
    SimplifiedChinese("zh-Hans"),
    TraditionalChinese("zh-Hant"),
    English("en"),
    Japanese("ja");

    companion object {
        fun fromStorageValue(value: String?): BreezeLanguagePreference =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

@Composable
fun BreezeI18nProvider(
    languageTag: String,
    systemLanguageTag: String = getSystemLanguageTag(),
    content: @Composable () -> Unit,
) {
    val localeTag = remember(languageTag, systemLanguageTag) {
        resolveResourceLocaleTag(
            preference = BreezeLanguagePreference.fromStorageValue(languageTag),
            systemLanguageTag = systemLanguageTag,
        )
    }

    CompositionLocalProvider(LocalAppLocale provides localeTag) {
        key(localeTag) {
            content()
        }
    }
}

private fun resolveResourceLocaleTag(
    preference: BreezeLanguagePreference,
    systemLanguageTag: String,
): String =
    when (preference) {
        BreezeLanguagePreference.System -> resolveSystemResourceLocaleTag(systemLanguageTag)
        BreezeLanguagePreference.SimplifiedChinese -> "zh-CN"
        BreezeLanguagePreference.TraditionalChinese -> "zh-TW"
        BreezeLanguagePreference.English -> "en"
        BreezeLanguagePreference.Japanese -> "ja"
    }

private fun resolveSystemResourceLocaleTag(systemLanguageTag: String): String {
    val normalized = systemLanguageTag.lowercase().replace('_', '-')
    return when {
        normalized.startsWith("zh-hant") ||
            normalized.startsWith("zh-tw") ||
            normalized.startsWith("zh-hk") ||
            normalized.startsWith("zh-mo") -> "zh-TW"
        normalized.startsWith("zh") -> "zh-CN"
        normalized.startsWith("ja") -> "ja"
        else -> "en"
    }
}

fun languagePreferenceLabelRes(preference: BreezeLanguagePreference): StringResource =
    when (preference) {
        BreezeLanguagePreference.System -> Res.string.language_preference_system
        BreezeLanguagePreference.SimplifiedChinese -> Res.string.language_preference_simplified_chinese
        BreezeLanguagePreference.TraditionalChinese -> Res.string.language_preference_traditional_chinese
        BreezeLanguagePreference.English -> Res.string.language_preference_english
        BreezeLanguagePreference.Japanese -> Res.string.language_preference_japanese
    }

@Composable
fun promptSuggestionTexts(): List<String> = listOf(
    stringResource(Res.string.prompt_suggestion_1),
    stringResource(Res.string.prompt_suggestion_2),
    stringResource(Res.string.prompt_suggestion_3),
    stringResource(Res.string.prompt_suggestion_4),
    stringResource(Res.string.prompt_suggestion_5),
)
