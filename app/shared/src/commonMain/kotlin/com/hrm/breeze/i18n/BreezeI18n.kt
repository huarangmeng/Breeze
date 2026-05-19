package com.hrm.breeze.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.hrm.breeze.domain.model.LlmProviderId
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

fun providerDescriptionRes(providerId: LlmProviderId): StringResource =
    when (providerId) {
        LlmProviderId.Local -> Res.string.provider_description_local
        LlmProviderId.OpenAI -> Res.string.provider_description_openai
        LlmProviderId.Anthropic -> Res.string.provider_description_anthropic
    }

fun providerNoticeRes(providerId: LlmProviderId): StringResource =
    when (providerId) {
        LlmProviderId.Local -> Res.string.provider_notice_local
        LlmProviderId.OpenAI -> Res.string.provider_notice_openai
        LlmProviderId.Anthropic -> Res.string.provider_notice_anthropic
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
fun modelDescriptionText(modelId: String): String =
    when (modelId) {
        "breeze-echo" -> stringResource(Res.string.model_desc_breeze_echo)
        "qwen2.5:7b" -> stringResource(Res.string.model_desc_qwen25_7b)
        "llama3.2:3b" -> stringResource(Res.string.model_desc_llama32_3b)
        "gpt-4.1-mini" -> stringResource(Res.string.model_desc_gpt41_mini)
        "gpt-4.1" -> stringResource(Res.string.model_desc_gpt41)
        "o4-mini" -> stringResource(Res.string.model_desc_o4_mini)
        "claude-3-5-haiku-latest" -> stringResource(Res.string.model_desc_claude_35_haiku_latest)
        "claude-3-7-sonnet-latest" -> stringResource(Res.string.model_desc_claude_37_sonnet_latest)
        "claude-opus-4-1" -> stringResource(Res.string.model_desc_claude_opus_41)
        else -> modelId
    }

@Composable
fun promptSuggestionTexts(): List<String> = listOf(
    stringResource(Res.string.prompt_suggestion_1),
    stringResource(Res.string.prompt_suggestion_2),
    stringResource(Res.string.prompt_suggestion_3),
    stringResource(Res.string.prompt_suggestion_4),
    stringResource(Res.string.prompt_suggestion_5),
)
