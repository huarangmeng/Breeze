# Breeze

`Breeze` 是一个基于 Kotlin Multiplatform + Compose Multiplatform 构建的多端 LLM 平台，目标是在一套共享 UI 和共享业务逻辑的基础上，同时覆盖：

- Android
- iOS
- macOS
- Windows
- Web

当前工程已经具备 Android、iOS、Web、Wasm、JVM Desktop 的基础目标配置，其中桌面端原生分发已配置：

- macOS `DMG`
- Windows `MSI`
- Linux `DEB`

## 项目目标

`Breeze` 不只是一个桌面聊天工具，而是一个需要兼顾手机、平板、桌面和浏览器窗口尺寸变化的跨端 LLM 客户端。项目重点包括：

- 统一的 Breeze 品牌视觉和主题系统
- 多模型 API 接入与本地配置管理
- 聊天、历史会话、API 配置、模型参数等核心页面
- 基于 `commonMain` 的最大化 UI 复用
- 面向不同屏幕尺寸的响应式布局，而不是仅适配桌面宽屏

## 设计稿来源

当前设计参考图位于 [`img/`](./img)：

- [`img/chat.png`](./img/chat.png)：主聊天页
- [`img/chat history.png`](./img/chat%20history.png)：会话历史页
- [`img/api.png`](./img/api.png)：API 配置页
- [`img/model settings.png`](./img/model%20settings.png)：模型参数页

这些设计稿已经定义了项目的视觉语言：

- 浅色低对比背景
- 卡片式容器
- 大圆角和轻阴影
- 桌面端双栏或中心表单布局

但这些稿件目前只覆盖桌面视图，后续实现不能直接按桌面布局照搬到手机端。

## 响应式适配策略

项目默认采用“按窗口宽度分级”的跨平台适配方式，而不是按平台单独写页面：

- `Compact`：`< 600dp`
- `Medium`：`600dp - 839dp`
- `Expanded`：`>= 840dp`

对应原则如下：

- `Compact`
  - 优先单栏布局
  - 侧栏改为抽屉、独立页面或弹层
  - 输入区和底部操作区优先保证触达
- `Medium`
  - 使用可折叠侧栏或分栏布局
  - 控制内容最大宽度，避免横向过散
- `Expanded`
  - 延续设计稿中的双栏、居中卡片、宽屏留白
  - 对正文阅读区和表单区设置最大宽度，避免无限拉伸

页面级适配要求：

- 聊天页：手机端单栏，历史记录通过抽屉或独立页进入；桌面端双栏
- 历史页：手机端改为全屏列表，桌面端保留说明区 + 列表区
- API 配置页：手机端表单全宽，模型卡片改为堆叠或自适应网格
- 模型参数页：手机端将“左说明 + 右控件”改为纵向排布

完整协作约束请参考根目录 [`AGENTS.md`](./AGENTS.md)。

## 模块结构

### `composeApp`

共享 Compose Multiplatform 应用模块，承载绝大多数跨端 UI 和共享逻辑。

- [`composeApp/src/commonMain`](./composeApp/src/commonMain)：所有平台共享的 UI、状态、主题和业务代码
- [`composeApp/src/androidMain`](./composeApp/src/androidMain)：Android 专属实现
- [`composeApp/src/iosMain`](./composeApp/src/iosMain)：iOS 专属实现
- [`composeApp/src/jvmMain`](./composeApp/src/jvmMain)：桌面端实现
- [`composeApp/src/jsMain`](./composeApp/src/jsMain)：JS Web 目标
- [`composeApp/src/wasmJsMain`](./composeApp/src/wasmJsMain)：Wasm Web 目标
- [`composeApp/src/webMain`](./composeApp/src/webMain)：Web 入口资源与页面宿主

### `androidApp`

Android 应用入口模块。

### `iosApp`

iOS 应用入口与 Xcode 工程。即使 UI 主要共享在 Compose 中，iOS 仍需要这里作为原生宿主。

### `img`

UI 设计参考图目录。这里只提供桌面视图，不能视为移动端最终布局定义。

## 当前代码状态

当前 `commonMain` 已经有一套 `BreezeTheme` 主题基础和演示页面，用于验证颜色与基础组件风格。后续建议优先补齐以下能力：

- 自适应窗口尺寸分类与布局决策层
- 统一导航结构
- 聊天页正式布局
- 会话历史页
- API 配置页
- 模型参数页

## 文档组织

仓库文档遵循“根目录保留入口，长期文档统一归档”的规则：

- [`README.md`](./README.md)：项目入口说明
- [`AGENTS.md`](./AGENTS.md)：仓库协作与代理操作手册
- [`docs/`](./docs)：后续设计说明、架构文档、适配方案、任务计划的统一归档目录

如果后续需要新增页面适配方案、实施计划或模块说明，请优先放到 [`docs/`](./docs) 下，而不是直接放到根目录。

## 运行方式

### Android

macOS / Linux:

```bash
./gradlew :composeApp:assembleDebug
```

Windows:

```bash
.\gradlew.bat :composeApp:assembleDebug
```

### Desktop (JVM)

macOS / Linux:

```bash
./gradlew :composeApp:run
```

Windows:

```bash
.\gradlew.bat :composeApp:run
```

### Web (Wasm)

macOS / Linux:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Windows:

```bash
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

### Web (JS)

macOS / Linux:

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

Windows:

```bash
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun
```

### iOS

在 IDE 中使用对应运行配置，或直接打开 [`iosApp/`](./iosApp) 并通过 Xcode 运行。

## 协作约定

- 品牌命名统一使用 `Breeze`
- 响应式规则优先沉淀到共享层，不在页面里散落硬编码断点
- 新页面实现时必须同时考虑手机、平板、桌面三种宽度行为
- 视觉风格以 `img/` 中的设计稿为基准，但布局结构允许针对移动端重排
- 文档、设计约束和代码实现要同步演进

## 参考资料

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
