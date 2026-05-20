# Debug Session: local-send-fail [OPEN]

## Summary
- Symptom: Desktop 端已下载安装端侧模型后，聊天发送提示“消息发送失败，请稍后重试”。
- Scope: 本地 `llama.cpp` / `llama-server` Desktop 推理链路。
- Status: superseded-by-architecture-correction

## Correction
- 用户明确纠正：`llama.cpp` 需要集成在 Breeze 应用内，由 Breeze 安装/打包/加载，不应依赖用户安装外部 `llama-server`，也不应通过外部进程解决。
- 因此本次调试中关于外部 `llama-server` 缺失的结论只解释了旧实现为什么失败，不再作为目标修复方向。

## Hypotheses
- H1: `llama-server` 二进制未找到或不可执行，导致本地运行时根本没有启动。
- H2: `llama-server` 已启动但模型加载失败，健康检查一直不是 ready。
- H3: 本地 HTTP 端点已启动，但 `OpenAI-compatible` 请求参数或路径不兼容，导致 `/v1/chat/completions` 返回错误。
- H4: 模型文件路径、alias 或上下文窗口参数不合法，导致运行时在首条消息时崩溃。
- H5: UI 层把底层错误吞掉了，只显示通用发送失败，真实异常需要从日志与运行时输出中恢复。

## Evidence Log
- E1: Local model file exists at `/Users/bytedance/Library/Application Support/Breeze/models/files/smollm2-360m-instruct-q8_0.gguf`.
- E2: Local runtime log directory exists but contains no `llama-server-*.log` file.
- E3: `which llama-server` returns not found; `/opt/homebrew/bin/llama-server` and `/usr/local/bin/llama-server` are also absent.
- E4: `ChatViewModel` currently converts any lower-level exception into generic `status_send_failed`, so UI hides the real cause.

## Hypothesis Status
- H1: confirmed by E2 + E3.
- H2: not reached; process never started.
- H3: not reached; no local server endpoint was created.
- H4: model file exists, so path/download is not the primary blocker.
- H5: confirmed by E4.

## Next Step
- Remove the external `llama-server` process bridge.
- Replace it with an application-owned in-process native runtime bridge.
