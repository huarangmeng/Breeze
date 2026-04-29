package com.hrm.breeze.data.settings

import com.russhwolf.settings.Settings

private const val DEFAULT_NAMESPACE = "breeze.preferences"
private const val DEFAULT_ECHO_ENDPOINT = "https://example.invalid/v1/chat/echo"
private const val DEFAULT_MODEL_ID = "breeze-echo"

class BreezeSettings(
    private val delegate: Settings,
) {
    var echoEndpoint: String
        get() = delegate.getString(KEY_ECHO_ENDPOINT, DEFAULT_ECHO_ENDPOINT)
        set(value) = delegate.putString(KEY_ECHO_ENDPOINT, value)

    var currentModelId: String
        get() = delegate.getString(KEY_MODEL_ID, DEFAULT_MODEL_ID)
        set(value) = delegate.putString(KEY_MODEL_ID, value)

    var apiToken: String?
        get() = delegate.getStringOrNull(KEY_API_TOKEN)
        set(value) {
            if (value.isNullOrBlank()) {
                delegate.remove(KEY_API_TOKEN)
            } else {
                delegate.putString(KEY_API_TOKEN, value)
            }
        }

    companion object {
        private const val KEY_ECHO_ENDPOINT = "network.echo_endpoint"
        private const val KEY_MODEL_ID = "model.current_id"
        private const val KEY_API_TOKEN = "network.api_token"
    }
}

fun createBreezeSettings(
    namespace: String = DEFAULT_NAMESPACE,
): BreezeSettings = BreezeSettings(createPlatformSettings(namespace))

expect fun createPlatformSettings(namespace: String): Settings
