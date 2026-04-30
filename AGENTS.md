# AGENTS.md

本文件只保留在 Breeze 仓库中工作的必要规则。详细方案、页面设计和实施计划不要继续堆在这里，统一归档到 `docs/`。

## 1. 仓库目标

`Breeze` 是一个基于 Kotlin Multiplatform + Compose Multiplatform 的多端 LLM 平台，目标覆盖：

- Android
- iOS
- macOS
- Windows
- Web

当前阶段的优先级：

- 建立可复用的共享 UI 结构
- 建立统一的主题与设计 token
- 建立面向多尺寸窗口的响应式布局
- 建立清晰的文档归档体系

## 2. 仓库地图

- `composeApp/`：共享 UI 和主要业务代码
- `androidApp/`：Android 宿主
- `iosApp/`：iOS 宿主
- `img/`：桌面版设计参考图
- `docs/`：长期文档归档目录
- `README.md`：项目入口说明
- `AGENTS.md`：仓库协作规则

`composeApp/src` 的基本职责：

- `commonMain`：共享 UI、状态、主题、业务逻辑
- `androidMain` / `iosMain` / `jvmMain` / `jsMain` / `wasmJsMain`：平台差异代码
- `webMain`：Web 宿主资源

## 3. 核心规则

### 3.1 CommonMain First

- 业务 UI、页面状态、导航模型、设计 token 优先放在 `commonMain`
- 只有平台 API、宿主生命周期、权限和系统集成才放到平台 source set
- 不要在 `androidMain` 或 `iosMain` 长出第二套页面体系

### 3.2 保持收敛

- 新代码尽量向以下目录收敛：`ui/adaptive`、`ui/components`、`ui/screens`、`ui/navigation`、`ui/state`、`theme`
- 不要新增含糊目录名，例如 `temp`、`newui`、`utils2`
- 不要把颜色、圆角、断点、间距散落硬编码在多个页面

### 3.3 命名与主题

- 品牌命名统一使用 `Breeze`
- 新增视觉 token 优先放入主题层
- 优先复用现有 `BreezeTheme`
- 语义色保持语义用途，不要把错误色、成功色当装饰色

## 4. 响应式规则

- 桌面设计稿只定义视觉语言，不定义所有端的最终布局
- 适配优先看窗口尺寸，不优先按平台分叉
- 不要把桌面稿直接缩小塞给手机端

默认宽度分级：

- `Compact`：`< 600dp`
- `Medium`：`600dp - 839dp`
- `Expanded`：`>= 840dp`

共享层应集中管理窗口信息，例如：

- `widthClass`
- `heightClass`
- `isTouchPreferred`
- `contentMaxWidth`
- `paneMode`

交互底线：

- 触控命中面积不少于 `44dp`
- 关键操作不能依赖 hover 才可见
- 底部输入框和主操作区不能被手势区遮挡
- 超宽页面要限制阅读宽度

## 5. 设计稿使用方式

当前参考图：

- `img/chat.png`
- `img/chat history.png`
- `img/api.png`
- `img/model settings.png`

这些设计稿只定义以下内容：

- 浅色、低对比背景
- 大圆角卡片
- 轻边框与轻阴影
- 桌面双栏或中心表单的视觉节奏

更细的页面适配规则、信息架构和实现方案应写入 `docs/`，不要继续扩写到 `AGENTS.md`。

## 6. 文档规则

- 根目录只保留入口文档：`README.md`、`AGENTS.md`
- 设计说明、实施计划、适配方案、模块说明、ADR、排查记录统一放到 `docs/`
- 结构变化、平台范围变化、断点变化、运行命令变化时，同步更新文档

建议的 `docs/` 结构：

- `docs/README.md`
- `docs/design/`
- `docs/architecture/`
- `docs/adr/`
- `docs/platform/`
- `docs/work-items/`

## 7. 构建与运行

Android:

```bash
./gradlew :androidApp:assembleDebug
```

Desktop:

```bash
./gradlew :composeApp:run
```

Web Wasm:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Web JS:

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

iOS：

- 从 IDE 使用对应运行配置
- 或打开 `iosApp/` 用 Xcode 运行

如果只改了文档，不要为了形式主义去跑整套耗时构建。

## 8. 完成标准

- 与当前仓库结构一致
- 不引入新的命名漂移
- 不破坏多端共享方向
- 不把桌面稿误当成全平台唯一布局
- 相关文档同步到正确位置
