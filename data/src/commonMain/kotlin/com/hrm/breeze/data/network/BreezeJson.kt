package com.hrm.breeze.data.network

import kotlinx.serialization.json.Json

val BreezeJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    isLenient = true
}
