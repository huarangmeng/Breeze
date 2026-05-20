package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.domain.model.OnDeviceModelPreset
import com.hrm.breeze.domain.model.OnDeviceModelKind

object OnDeviceModelCatalog {
    val presets: List<OnDeviceModelPreset> =
        listOf(
            OnDeviceModelPreset(
                id = "qwen3_embedding_0_6b_q8_0",
                displayName = "Qwen3 Embedding 0.6B",
                downloadUrl = "https://huggingface.co/Qwen/Qwen3-Embedding-0.6B-GGUF/resolve/main/Qwen3-Embedding-0.6B-Q8_0.gguf",
                fileName = "qwen3-embedding-0.6b-q8_0.gguf",
                fileSizeBytes = 610_000_000L,
                recommendedContextWindow = 8192,
                minimumRamGb = 4,
                kind = OnDeviceModelKind.Embedding,
                description = "Qwen3 本地向量模型，用于 Breeze RAG 语义检索。未下载或加载失败时会自动退回轻量 lexical 检索。",
            ),
            OnDeviceModelPreset(
                id = "qwen3_0_6b_q4_k_m",
                displayName = "Qwen3 0.6B",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_K_M.gguf",
                fileName = "qwen3-0.6B-Q4_K_M.gguf",
                fileSizeBytes = 397_000_000L,
                description = "新版千问轻量模型，适合中文离线聊天、摘要、简单问答和命令理解。体积较小，适合作为默认轻量模型候选。",
            ),
            OnDeviceModelPreset(
                id = "qwen3_1_7b_q4_k_m",
                displayName = "Qwen3 1.7B",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf",
                fileName = "qwen3-1.7b-q4_k_m.gguf",
                fileSizeBytes = 1_100_000_000L,
                description = "新版千问 1.7B 模型，中文和多语言能力更强，适合较好的手机做离线聊天、总结和改写。比 0.6B 更聪明，但占用更高。",
            ),
            OnDeviceModelPreset(
                id = "smollm2_360m_q8_0",
                displayName = "SmolLM2 360M",
                downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
                fileName = "smollm2-360m-instruct-q8_0.gguf",
                fileSizeBytes = 386_000_000L,
                description = "适合简单离线任务、短文本处理和低端设备。中文和复杂推理能力有限。",
            ),
            OnDeviceModelPreset(
                id = "llama3_2_1b_q4_k_m",
                displayName = "Llama 3.2 1B",
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
                fileName = "llama-3.2-1b-instruct-q4_k_m.gguf",
                fileSizeBytes = 808_000_000L,
                description = "Meta 的轻量英文通用模型，适合英文聊天、摘要、简单问答和离线助手。中文场景建议优先使用 Qwen。",
            ),
            OnDeviceModelPreset(
                id = "llama3_2_3b_q4_k_m",
                displayName = "Llama 3.2 3B",
                downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                fileName = "llama-3.2-3b-instruct-q4_k_m.gguf",
                fileSizeBytes = 2_000_000_000L,
                description = "Meta Llama 3.2 的 3B 指令模型，英文通用能力比 1B 更好，适合高端手机或平板。文件和内存占用较高，不建议作为默认下载。",
            ),
            OnDeviceModelPreset(
                id = "gemma3_1b_it_q4_k_m",
                displayName = "Gemma 3 1B",
                downloadUrl = "https://huggingface.co/unsloth/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf",
                fileName = "gemma-3-1b-it-q4_k_m.gguf",
                fileSizeBytes = 806_000_000L,
                description = "Google Gemma 系列小模型，适合英文问答、摘要和轻量助手。中文能力一般，适合作为英文备用模型。",
            ),
            OnDeviceModelPreset(
                id = "gemma4_e2b_it_q4_k_m",
                displayName = "Gemma 4 E2B",
                downloadUrl = "https://huggingface.co/bartowski/google_gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf",
                fileName = "gemma-4-e2b-it-q4_k_m.gguf",
                fileSizeBytes = 3_110_000_000L,
                description = "Google Gemma 4 E2B 指令模型，适合英文问答、摘要、改写和轻量推理。文件约 3.11GB，内存占用高，建议仅在高端手机、平板或桌面设备上使用。",
            )
        )
    val chatPresets: List<OnDeviceModelPreset> = presets.filter { preset -> preset.kind == OnDeviceModelKind.Chat }

    val embeddingPresets: List<OnDeviceModelPreset> = presets.filter { preset -> preset.kind == OnDeviceModelKind.Embedding }

    fun requirePreset(presetId: String): OnDeviceModelPreset =
        presets.firstOrNull { it.id == presetId }
            ?: error("Unknown on-device preset: $presetId")

    fun findPreset(presetId: String): OnDeviceModelPreset? = presets.firstOrNull { it.id == presetId }
}
