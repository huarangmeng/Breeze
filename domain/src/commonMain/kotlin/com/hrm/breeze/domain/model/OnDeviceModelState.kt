package com.hrm.breeze.domain.model

data class OnDeviceModelState(
    val preset: OnDeviceModelPreset,
    val downloadStatus: OnDeviceDownloadStatus = OnDeviceDownloadStatus.NotDownloaded,
    val runtimeState: InferenceRuntimeState = InferenceRuntimeState.Idle,
    val isCurrent: Boolean = false,
    val localPath: String? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val lastError: String? = null,
) {
    val isReadyForChat: Boolean
        get() = downloadStatus == OnDeviceDownloadStatus.Downloaded && localPath != null
}
