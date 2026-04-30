# Breeze 项目计划

本文档是 Breeze 的总体执行计划，按里程碑（M0 → M6）组织，每个里程碑下挂具体任务卡。与 [architecture/overview.md](architecture/overview.md) §11 的演进路线对齐，颗粒度更细。

- **任务状态**：⬜ 未开始 / 🟡 进行中 / ✅ 已完成 / ⏸ 暂挂
- **任务粒度**：每张卡能在 1~3 天独立完成并合入
- **任务 ID 规则**：`M<里程碑>-<序号>`，例如 `M1-3`
- 具体任务的详细设计（如果超出一段文字能说清的范围）写到 `docs/work-items/<ID>-<slug>.md`
- 选型性变更写 ADR 到 `docs/adr/NNNN-<slug>.md`

---

## M0 模块拆分 ✅

目标：建立 `:core` / `:core-ui` / `:domain` / `:data` / `:composeApp` 五模块骨架，确立分层与依赖方向。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M0-1  | 新增 `:core` 模块：`BreezeResult` / `AppDispatchers`（5 端 actual）/ `Log`     | ✅   |
| M0-2  | 新增 `:core-ui` 模块，迁入 `BreezeTheme` + `BreezeColors`，新增 `WindowInfo`  | ✅   |
| M0-3  | 新增 `:domain` 模块：`Message` / `Conversation` / `ChatRepository` / UseCase | ✅   |
| M0-4  | 新增 `:data` 模块：Ktor + Room3 + Settings + Coil 依赖装配，占位 Repository | ✅   |
| M0-5  | `settings.gradle.kts` include + `:composeApp` 改依赖新模块                    | ✅   |
| M0-6  | `libs.versions.toml` 补齐导航 / 网络 / 数据库 / 图片 / 日志 / 测试 / Markdown  | ✅   |
| M0-7  | 架构文档 `docs/architecture/overview.md` 落地                                 | ✅   |

---

## M1 主题基线 ✅

目标：补齐视觉 token 层，消除 `App.kt` 里的硬编码；确保所有页面视觉从主题层消费。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M1-1  | 新增 `BreezeShapes`（大/中/小圆角 + 气泡圆角），替换 `App.kt` 硬编码 `dp`       | ✅   |
| M1-2  | 新增 `BreezeSpacing`（xxs~xxl 阶梯），在 `BreezeTheme` 通过 CompositionLocal 暴露 | ✅   |
| M1-3  | 新增 `BreezeTypography`（title/body/label/code），接入 MaterialTheme.typography | ✅   |
| M1-4  | `App.kt` 里的所有硬编码颜色/圆角/间距切换到主题 token                          | ✅   |
| M1-5  | 补 `docs/design/theme-tokens.md`：token 命名、语义用途、扩展方式                | ✅   |

完成标准：`App.kt` 内搜不到 `.dp` 以外的硬编码颜色/圆角；新加页面只需 `BreezeTheme.xxx`。

---

## M2 响应式接通 ✅

目标：让 `LocalWindowInfo` 在各端宿主真实计算并 Provide，骨架页在 Compact / Expanded 间切换。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M2-1  | `:core-ui/adaptive` 增加 `rememberWindowInfo()` 助手，接入 `LocalWindowInfo`  | ✅   |
| M2-2  | Android 宿主：在 `MainActivity` 计算窗口尺寸并 Provide                         | ✅   |
| M2-3  | iOS 宿主：在 `MainViewController` 计算窗口尺寸并 Provide                       | ✅   |
| M2-4  | Desktop（jvmMain）：`main.kt` 监听窗口尺寸变化并 Provide                       | ✅   |
| M2-5  | Web（js / wasmJs）：监听 `window.innerWidth` 变化并 Provide                    | ✅   |
| M2-6  | 验证页：一个 sample screen 根据 `widthClass` 切换单栏 / 双栏布局                 | ✅   |
| M2-7  | 补 `docs/design/responsive.md`：断点、PaneMode、各端验证截图                     | ✅   |

完成标准：五端运行时 `WindowInfo.widthClass` 真实反映窗口尺寸，sample screen 能看到形态切换。

---

## M3 基础设施接入 ✅

目标：`:data` 从占位升级为真实实现；网络 + 数据库 + 设置 + 图片打通。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M3-1  | `:data/network/BreezeHttpClient.kt`：Ktor Client 工厂（content-neg + logging + sse）；`BreezeJson` 单例 | ✅   |
| M3-2  | 各端 Ktor 引擎 actual 工厂（OkHttp / Darwin / Java / JS）；wasmJs 方案定稿 | ✅   |
| M3-3  | `:data/storage`：Room3 数据库骨架 `BreezeDatabase`（`Conversation` + `Message` 两张表） | ✅   |
| M3-4  | 各端 `SQLiteDriver` 工厂：Android/JVM 用 bundled，iOS 用 bundled，Web 用 `WebWorkerSQLiteDriver` | ✅   |
| M3-5  | Web Worker 资源：`composeApp/webMain/resources` 里放 sqlite web worker js       | ✅   |
| M3-6  | `:data/settings/BreezeSettings`：封装 AndroidX DataStore 提供类型安全设置存储     | ✅   |
| M3-7  | `:data/image/BreezeImageLoader`：Coil3 `ImageLoader` 工厂（使用同一个 Ktor Client） | ✅   |
| M3-8  | `ChatRepositoryImpl`：走 Room3 + Ktor 的真实实现替换 `InMemoryChatRepository`   | ✅   |
| M3-9  | 网络与数据库方案说明已收敛到 `docs/architecture/overview.md`                     | ✅   |

完成情况：

- `BreezeDataContainer` 已接入 `composeApp` 根入口，默认使用仓库内 `MockEngine` 回显链路。
- `App.kt` 已从静态 demo 切为真实会话列表 + 消息区，消息发送会写入 Room3，并通过 `KtorBreezeChatApi` 回写 assistant 消息。
- `:data` 已补 `ChatRepositoryImplJvmTest`，覆盖“持久化 -> 请求 -> 回写消息”主链路。
- 已验证 `:data:jvmTest`、`:composeApp:compileKotlinJvm`、`:androidApp:assembleDebug` 通过。

完成标准：Desktop 和 Android 运行后，能把一条消息持久化到 Room3、通过 Ktor 发到回显链路、再读取回来；同时 `:data` 具备最小自动化验证。以上能力现已接通。

---

## M4 导航 + Feature 拆分 ✅

目标：接入 navigation-compose，把 `chat / history / api / modelsettings` 四个 feature 的骨架页落地；视情况拆 `:feature:*` 模块。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M4-1  | `:core-ui/navigation/Destination.kt`：sealed 基础类型、类型安全参数约定          | ✅   |
| M4-2  | `:composeApp/navigation/BreezeNavHost.kt`：集中路由表与导航装配                   | ✅   |
| M4-3  | Chat 骨架页：`ChatScreen` + `ChatViewModel` + `ChatRoute`                       | ✅   |
| M4-4  | History 骨架页：同上                                                           | ✅   |
| M4-5  | ApiConfig 骨架页：同上                                                         | ✅   |
| M4-6  | ModelSettings 骨架页：同上                                                     | ✅   |
| M4-7  | 根 `BreezeApp()`：Expanded 下 ListDetail 骨架（列表 + 详情），Compact 下单栏      | ✅   |
| M4-8  | 当 feature 达到 3 个以上真实实现时，评估拆 `:feature:chat` 等模块（ADR 0003）   | ✅   |
| M4-9  | 补 `docs/design/navigation.md`：路由表、深链规则、返回栈策略                     | ✅   |

完成标准：四个 feature 页可通过导航相互跳转；每个页面都有独立 ViewModel；响应式在页面内生效。

---

## M5 多 Provider 🟡

目标：在 `:data/llm/` 支持多家 LLM Provider，UI 与 domain 不感知 Provider 差异。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M5-1  | `:domain/model/LlmProviderId.kt`、`:domain/model/ModelProfile.kt`：Provider 抽象 | ✅   |
| M5-2  | `:data/llm/LlmProvider.kt`：Provider 统一接口（`complete` / `stream`）          | ✅   |
| M5-3  | `OpenAIProvider`：chat/completions + 流式 SSE                                  | ⬜   |
| M5-4  | `AnthropicProvider`：messages API                                              | ⬜   |
| M5-5  | `LocalProvider`（Ollama 兼容）：本地 HTTP                                      | 🟡   |
| M5-6  | `LlmProviderRegistry`：按 `ModelProfile.providerId` 路由到具体 Provider          | ✅   |
| M5-7  | ApiConfig 页：新增/编辑 Provider + API Key；Settings 持久化                      | ✅   |
| M5-8  | ModelSettings 页：参数（temperature / top_p / max_tokens）持久化                 | ⬜   |
| M5-9  | 错误映射：把 Provider 特定错误统一映射成 `BreezeResult.Failure` + 面向用户文案   | ⬜   |

完成标准：切换 Provider + Model 后，UI / ViewModel 无改动；流式响应正常出字。

---

## M6 生产化 ⬜

目标：稳定性与可观测性到可发布状态。

| ID    | 任务                                                                          | 状态 |
| ----- | ----------------------------------------------------------------------------- | ---- |
| M6-1  | 网络层重试 + 超时 + 断流恢复                                                   | ⬜   |
| M6-2  | 全局错误边界 `BreezeErrorBoundary`：未捕获异常 → Snackbar + 上报                 | ⬜   |
| M6-3  | `Logger` 分级 + 生产环境过滤；Android/iOS/Desktop 日志落地方式                   | ⬜   |
| M6-4  | `ErrorReporter` 接口落地（默认 no-op；可挂 Sentry/Bugsnag，由 ADR 决定）         | ⬜   |
| M6-5  | 性能：冷启动、消息列表滚动、流式渲染的基线埋点                                   | ⬜   |
| M6-6  | 发布配置：Android release 签名、iOS archive、Desktop dmg/msi、Web 生产包         | ⬜   |
| M6-7  | `docs/platform/` 平台差异清单（API、权限、能力 gap）                             | ⬜   |
| M6-8  | README 写清各端本地运行 + 发布流程                                              | ⬜   |

完成标准：四端各有一次可发布构建产物；崩溃能在日志 / 上报端定位到源码位置。

---

## 里程碑依赖关系

```
M0 ──► M1 ──► M2 ──┐
                   ├──► M4 ──► M5 ──► M6
M0 ──────► M3 ─────┘
```

- M3（基础设施）不依赖 M1/M2，可以与 M1/M2 并行
- M4（导航 + feature）依赖 M2（响应式）和 M3（真实数据）
- M5（多 Provider）依赖 M3（网络）和 M4（ApiConfig 页）
- M6 在全部 feature 稳定后收尾

---

## 规则与守则

- 每张任务卡开工前：先在本文件把状态从 ⬜ 改为 🟡，完成后改为 ✅
- 任务范围超过一段文字能说清的：在 `docs/work-items/<ID>-<slug>.md` 写详细设计
- 遇到选型决策：不在 PR 里讨论，写 `docs/adr/NNNN-<slug>.md`（Context → Decision → Consequences）
- 每完成一个里程碑：同步更新 [architecture/overview.md](architecture/overview.md) 的"当前差距"清单
- 不引入未经本计划登记的新目录 / 新库（对应 [AGENTS.md §3.2](../AGENTS.md)）
