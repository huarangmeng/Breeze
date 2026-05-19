package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.domain.model.OnDeviceModelPreset

object OnDeviceModelCatalog {
    val presets: List<OnDeviceModelPreset> =
        listOf(
            OnDeviceModelPreset(
                id = "qwen2_5_0_5b_q4_k_m",
                displayName = "Qwen2.5 0.5B",
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                fileSizeBytes = 491_000_000L,
                description = "适合离线简单聊天、摘要和命令理解，速度快，占用低。复杂问题建议使用更大的模型。",
            ),
            OnDeviceModelPreset(
                id = "qwen2_5_1_5b_q4_k_m",
                displayName = "Qwen2.5 1.5B",
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                fileSizeBytes = 1_000_000_000L,
                description = "适合离线聊天、摘要、改写和 App 内助手。比轻量模型更聪明，但下载和内存占用更高。",
            ),
            OnDeviceModelPreset(
                id = "qwen2_5_coder_1_5b_q4_k_m",
                displayName = "Qwen2.5 Coder 1.5B",
                downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
                fileName = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
                fileSizeBytes = 1_120_000_000L,
                description = "适合解释代码、生成小函数、修复简单错误和回答开发问题。",
            ),
            OnDeviceModelPreset(
                id = "smollm2_360m_q8_0",
                displayName = "SmolLM2 360M",
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
                fileName = "smollm2-360m-instruct-q8_0.gguf",
                fileSizeBytes = 386_000_000L,
                description = "适合简单离线任务、短文本处理和低端设备。中文和复杂推理能力有限。",
            ),
        )

    fun requirePreset(presetId: String): OnDeviceModelPreset =
        presets.firstOrNull { it.id == presetId }
            ?: error("Unknown on-device preset: $presetId")
}
