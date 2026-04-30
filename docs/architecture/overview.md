# Breeze 架构总览

本文档是 `Breeze` 仓库的架构基线。它描述骨架、分层与库选型，具体页面设计放 `docs/design/`，选型变动放 `docs/adr/`。

与 [AGENTS.md](file:///Users/bytedance/AndroidStudioProjects/Breeze/AGENTS.md) 配合：AGENTS.md 管协作规则，本文档管代码放哪里。

适用范围：Android / iOS / macOS / Windows / Web（JS + Wasm）。

---

## 1. 目标与非目标

- 一套 UI 覆盖五端
- 颜色/圆角/间距/字型只在主题层演化
- 按窗口尺寸响应式，不按平台分叉
- 文档分目录归档

非目标：每端各写一套 UI、把桌面稿当作全端唯一布局、把业务下沉到宿主。

---

## 2. 模块划分

目标拓扑：

```
:androidApp ──┐
:iosApp     ──┴──► :composeApp ──► :core-ui ──► :core
                              ├──► :domain  ──► :core
                              └──► :data    ──► :domain, :core
```

依赖方向由 Gradle 边界强制：

| 模块          | 类型              | 职责                                                                 |
| ------------- | ----------------- | -------------------------------------------------------------------- |
| `:androidApp` | Android App       | Activity 包装、启动入口                                              |
| `:iosApp`     | Xcode 工程        | `UIViewController` 包装、启动入口                                    |
| `:composeApp` | KMP + CMP 壳      | 根 `App()`、NavHost、Koin 模块、ViewModel/Route 装配                 |
| `:core-ui`    | KMP + CMP lib     | `BreezeTheme`、`adaptive`、通用组件、Compose 生态（navigation/coil/markdown） |
| `:domain`     | KMP lib（纯 Kotlin） | 模型、用例、Repository **接口**；禁止 Compose / Ktor / SQLDelight |
| `:data`       | KMP lib           | Repository 实现、Ktor、Settings（后续 SQLDelight）                   |
| `:core`       | KMP lib           | `BreezeResult`、`AppDispatchers`、`Log`                              |

Feature 数量上来之后再拆 `:feature:chat` 等，现在先不拆；当前评估结论见 [ADR 0003](file:///Users/bytedance/AndroidStudioProjects/Breeze/docs/adr/0003-feature-module-split-timing.md)。

所有 KMP 模块统一 source set：`commonMain / androidMain / iosMain / jvmMain / jsMain / wasmJsMain`，业务默认写 `commonMain`，遇到平台 API 再下沉（对应 `AGENTS.md §3.1`）。

---

## 3. 目录落地（包根 `com.hrm.breeze`）

```
:core
└── core/
    ├── BreezeResult.kt
    ├── coroutines/AppDispatchers.kt  (expect + 各平台 actual)
    └── logging/Log.kt                (委托 Kermit)

:core-ui
└── ui/
    ├── theme/        BreezeTheme, BreezeColors, (BreezeShapes/Spacing/Typography 待补)
    ├── adaptive/     WindowInfo / WidthClass / PaneMode / LocalWindowInfo
    ├── components/   (待建)
    └── navigation/   (待建：Destination 基础类型)

:domain
└── domain/
    ├── model/       Message, Conversation
    ├── repository/  ChatRepository (接口)
    └── usecase/     SendMessageUseCase

:data
└── data/
    ├── llm/         (待建：OpenAIProvider 等)
    ├── image/       BreezeImageLoader expect/actual
    ├── network/     BreezeHttpClient, BreezeChatApi, BreezeJson
    ├── settings/    BreezeSettings
    ├── storage/     Room3 + SQLiteDriver 工厂
    └── repository/  ChatRepositoryImpl / InMemoryChatRepository

:composeApp
└── breeze/
    ├── App.kt       根 Composable（迁出 theme 后使用 :core-ui）
    ├── Platform.kt  现有 expect
    ├── di/          Koin 模块与运行时装配
    └── navigation/  路由与 NavHost
```

### 各层约束

- **`:core-ui/theme`**：视觉 token 唯一出处；语义色保持语义
- **`:core-ui/adaptive`**：唯一管理 WindowInfo；其它地方只读 `LocalWindowInfo.current`，不判 `Platform.isAndroid`
- **`:core-ui/components`**：纯展示、受控；禁止硬编码颜色/圆角/间距
- **feature screens**：`XxxScreen` 只消费 state 派发 event；`XxxRoute` 做装配
- **`:domain`**：纯 Kotlin；接口在 domain、实现在 data（依赖倒置）
- **`:data`**：跨平台差异通过 `expect/actual` 收敛
- **`:core`**：不依赖任何业务模块

---

## 4. 响应式

断点（`AGENTS.md §4`）：

- `Compact < 600dp`
- `Medium 600~839dp`
- `Expanded ≥ 840dp`

单一事实源：`:core-ui/adaptive/WindowInfo` + `LocalWindowInfo`（已落地）。底层窗口分类使用官方 `Material3 Adaptive` 的 `currentWindowAdaptiveInfoV2()`，由 `Breeze WindowInfo` 再包装成项目语义。布局形态：Compact 单栏、Medium 单栏+抽屉、Expanded 双栏/三栏。

交互底线：触控命中 ≥ `44dp`；关键操作不依赖 hover；底部输入不被手势区遮挡；`Expanded` 限制阅读宽度。

---

## 5. 数据流

```
ChatScreen ─(event)─► ChatViewModel ─► SendMessageUseCase ─► ChatRepository(接口)
                                                                    ▲
                                                       ChatRepositoryImpl (:data)
                                                         ├─ Ktor / BreezeChatApi
                                                         └─ Room3 / Settings
```

约束：UI 只订阅 state；ViewModel 只调 UseCase；Repository 接口在 `:domain`、实现在 `:data`。

---

## 6. 平台差异（expect / actual）

最小暴露面：

- `:core/coroutines/AppDispatchers`（已落地，各端 actual 齐全）
- `:core/logging/Log`（委托 Kermit，无需 expect）
- `:data`（网络引擎、存储驱动按端提供）
- `:composeApp/Platform`（保留现状）

持久化目录策略见 [persistence-paths.md](file:///Users/bytedance/AndroidStudioProjects/Breeze/docs/platform/persistence-paths.md)，用于统一 `Room3` 与 `DataStore` 在各平台上的本地路径语义。

禁止 UI 层出现 `if (Platform.isAndroid)`，差异转成 `WindowInfo` 或主题 token。

---

## 7. 三方库选型

全部要求 KMP 全端可用。版本见 [libs.versions.toml](file:///Users/bytedance/AndroidStudioProjects/Breeze/gradle/libs.versions.toml)。

| 能力       | 选型                                                 | 所在模块          |
| ---------- | ---------------------------------------------------- | ----------------- |
| UI         | Compose Multiplatform                                | `:core-ui` / 宿主 |
| 响应式     | Material3 Adaptive + `WindowSizeClass`              | `:core-ui/adaptive` |
| 导航       | `androidx.navigation:navigation-compose`（CMP 版）   | `:core-ui`        |
| 依赖注入   | Koin（`koin-core` + `koin-compose` + `koin-compose-viewmodel`） | `:composeApp` |
| ViewModel  | `androidx.lifecycle:lifecycle-viewmodel*`            | `:composeApp`     |
| 协程       | `kotlinx-coroutines`                                 | `:core` / `:data` |
| 时间       | `kotlinx-datetime`                                   | `:core` / `:domain` |
| 序列化     | `kotlinx-serialization-json`                         | `:data`           |
| 网络       | Ktor 3（core + content-negotiation + logging + sse） | `:data`           |
| 网络引擎   | OkHttp（Android）/ Darwin（iOS）/ Java（JVM）/ JS    | `:data` 各 actual |
| 数据库     | Room3 + `androidx.sqlite`                            | `:data/storage/`  |
| KV 设置    | AndroidX DataStore Preferences                       | `:data/settings/` |
| 图片加载   | Coil 3（`coil-compose` + `coil-network-ktor3`）      | `:core-ui` / `:data` |
| Markdown   | `io.github.huarangmeng:markdown-parser/runtime/renderer` `1.2.6` | `:core-ui` |
| 日志       | Kermit                                               | `:core`           |
| 测试       | `kotlin-test` + `kotlinx-coroutines-test` + Turbine  | 各模块 `commonTest` |

模块间依赖约定：`:domain` 禁止引入 Compose / Ktor / Room3 / Serialization；`:core` 禁止依赖业务模块。

---

## 8. 导航

- `androidx.navigation:navigation-compose` 的 CMP 版
- `Destination` 基础类型已落在 `:core-ui/navigation/`
- 路由表与 `NavHost` 已落在 `:composeApp/navigation/`
- 根布局由 `BreezeNavHost` 统一切换 `Compact/Medium` 单栏与 `Expanded` ListDetail
- 宿主只创建根 `App()` 并传入系统级依赖

---

## 9. 测试

- `commonTest`：`:domain` 用例、ViewModel 行为（`TestDispatcher` + Turbine）
- `:data`：Ktor `MockEngine` + `ChatRepositoryImpl` 主链路验证（JVM 优先）
- UI 测试等组件稳定后再加

---

## 10. 构建（与 `AGENTS.md §7` 对齐）

| 目标     | 命令                                                  |
| -------- | ----------------------------------------------------- |
| Android  | `./gradlew :androidApp:assembleDebug`                 |
| Desktop  | `./gradlew :composeApp:run`                           |
| Web Wasm | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`   |
| Web JS   | `./gradlew :composeApp:jsBrowserDevelopmentRun`       |
| iOS      | 打开 `iosApp/` 用 Xcode                               |

---

## 11. 演进路线

1. **M0 模块拆分** ✅（本次完成）：`:core` / `:core-ui` / `:domain` / `:data` 落地；`theme/` 迁入 `:core-ui`；`WindowInfo` 骨架就位；`libs.versions.toml` 补齐
2. **M1 主题基线** ✅（本次完成）：补 `BreezeShapes` / `BreezeSpacing` / `BreezeTypography`，清掉 `App.kt` 残留硬编码，并补 `docs/design/theme-tokens.md`
3. **M2 响应式接通** ✅（本次完成）：引入官方 `Material3 Adaptive`，共享层统一映射 `WindowInfo`；骨架页切换 Compact / Expanded；补 `docs/design/responsive.md`
4. **M3 基础设施** ✅：Ktor / Room3 / Settings / Coil3 已接入 `:data`，并通过 `App` + `MockEngine` + Room3 完成最小闭环
5. **M4 导航 + feature 拆分**：navigation-compose 接入；按需拆 `:feature:chat` 等
6. **M5 多 Provider**：`:data/llm/` 引入 OpenAI / 本地等
7. **M6 生产化**：错误边界、网络重试、日志、遥测

每个里程碑完成后同步 `docs/architecture/` / `docs/adr/` / `docs/platform/`。

---

## 12. 何时写 ADR

放 `docs/adr/NNNN-<slug>.md`，采用 Context → Decision → Consequences。触发点：

- 导航方案变化（首轮 navigation-compose 接入时写新的 ADR）
- 网络 / 持久化库变化
- 新增跨端 `expect` 族
- 断点 / 窗口类定义变化
- 新增或下线平台目标

---

## 13. 当前差距

- [ ] `:core-ui/components` / `:core-ui/navigation` 尚空
- [ ] `:composeApp` 仍同时承载根装配与 feature presentation，后续继续观察是否触发 ADR 0003 的拆分条件
- [ ] `docs/platform/`、`docs/work-items/` 目录尚未建立
