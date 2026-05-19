package com.hrm.breeze.data.llm.ondevice

import com.hrm.breeze.domain.model.InferenceRuntimeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OnDeviceRuntimeManager {
    suspend fun ensureModelReady(localPath: String?): InferenceRuntimeState =
        if (localPath.isNullOrBlank()) {
            InferenceRuntimeState.Failed
        } else {
            InferenceRuntimeState.Ready
        }

    fun streamCompletion(
        modelId: String,
        prompt: String,
    ): Flow<String> = flow {
        val chunks =
            listOf(
                "Breeze 端侧模型(",
                modelId,
                ") 已接管当前会话。",
                "\n\n",
                "当前仓库已打通下载、选择与 provider 路由，",
                "下一步只需要把这里替换成 llama.cpp 原生 token 输出即可。",
                "\n\n用户输入：",
                prompt,
            )
        chunks.forEach { chunk ->
            delay(24)
            emit(chunk)
        }
    }
}
