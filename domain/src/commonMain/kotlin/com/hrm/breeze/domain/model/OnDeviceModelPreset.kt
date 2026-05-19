package com.hrm.breeze.domain.model

data class OnDeviceModelPreset(
    val id: String,
    val displayName: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val sha256: String? = null,
    val recommendedContextWindow: Int = 2048,
    val recommendedTemperature: Float = 0.7f,
    val recommendedTopP: Float = 0.9f,
    val minimumRamGb: Int = 4,
    val description: String = "",
)
