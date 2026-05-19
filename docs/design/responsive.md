# Breeze Responsive

本文档记录 `M2` 响应式接通阶段的实现基线，目标是让 `Breeze` 在 Android、iOS、Desktop、Web 上统一消费同一份窗口信息模型。

## 1. 实现策略

- 底层窗口分类使用官方 `org.jetbrains.compose.material3.adaptive:adaptive`
- 共享层通过 `currentWindowAdaptiveInfoV2()` 获取官方 `WindowAdaptiveInfo`
- `Breeze` 自己保留 `WindowInfo` 作为项目语义包装层
- 页面层只读取 `LocalWindowInfo.current`，不直接依赖平台 API 和官方 adaptive 细节

这意味着：

- 官方能力负责 `WindowSizeClass`
- `Breeze` 负责 `PaneMode`、`contentMaxWidth`、`isTouchPreferred`

## 2. 断点

窗口断点与 `AGENTS.md` 保持一致：

- `Compact < 600dp`
- `Medium 600dp - 839dp`
- `Expanded >= 840dp`

高度分级沿用官方窗口类常量：

- `Compact < 480dp`
- `Medium 480dp - 899dp`
- `Expanded >= 900dp`

## 3. WindowInfo 映射

当前 `WindowInfo` 暴露：

- `widthClass`
- `heightClass`
- `paneMode`
- `contentMaxWidth`
- `isTouchPreferred`

当前映射规则：

- `Compact` -> `PaneMode.Single`
- `Medium` -> `PaneMode.Single`
- `Expanded` -> `PaneMode.ListDetail`

当前内容宽度策略：

- `Compact` -> 使用容器实时宽度
- `Medium` -> `720.dp`
- `Expanded` -> `840.dp`

输入偏好策略：

- Android / iOS / JS / Wasm 默认 `true`
- Desktop JVM 默认 `false`

## 4. 当前根布局

当前由 `app/shared/navigation/BreezeNavHost.kt` 统一消费 `LocalWindowInfo`，根布局规则为：

- `Compact` / `Medium`：单栏导航壳，顶部横向 destination bar + 当前 feature 内容
- `Expanded`：`ListDetail` 根骨架，左侧 feature 列表 + 右侧当前详情页
- feature 页面内部继续各自消费 `LocalWindowInfo.current` 做页面级响应式

`M2` 的验证页职责已经在 `M4-7` 被正式根布局取代；页面内响应式则继续由各个 feature 自己负责。

## 5. 宿主接入

当前各宿主继续只负责启动共享 `App()`：

- Android：`MainActivity`
- iOS：`MainViewController`
- Desktop：`main.kt`
- Web JS / Wasm：`app/web/src/webMain/main.kt`

窗口尺寸不再由每个宿主单独手算，而是统一由 Compose + Material3 Adaptive 在共享层感知，再映射进 `Breeze WindowInfo`。

## 6. WindowManager Core

当前阶段不显式引 `WindowManager Core` 到项目代码中。

原因：

- `adaptive` 已经传递依赖 `org.jetbrains.androidx.window:window-core`
- `M2` 只需要标准窗口类，不需要折叠铰链和 embedding 能力

后续如果进入折叠屏专项，才评估是否把 posture / hinge 信息扩展进 `WindowInfo`。

## 7. Chat UI 分级策略

Chat 是当前主要工作台，不能把桌面双栏直接缩小到移动端。页面按 `WindowInfo.widthClass` 采用以下策略：

- `Compact`
  - 使用单栏结构，隐藏桌面侧栏。
  - 顶部保留轻量头部，包含设置、新建和当前模型切换入口。
  - 欢迎提示 chips 自动变为单列，避免横向溢出。
  - Composer 工具按钮允许折行，输入区宽度跟随屏幕并保留安全边距。
- `Medium`
  - 继续使用单栏主工作区，不强制显示侧栏。
  - 内容最大宽度受 `contentMaxWidth` 和页面 `widthIn` 约束，避免阅读行过长。
  - 可在后续根据导航复杂度评估抽屉或分段导航。
- `Expanded`
  - 使用左侧固定品牌/导航侧栏，品牌保持左上。
  - 主区顶部展示当前模型切换入口，点击可直接切换同 Provider 下的模型，或进入模型设置页。
  - 消息区、欢迎态和 Composer 分别限制最大宽度，超宽窗口只增加留白，不拉长文本行。

这套策略的底线是：移动端优先保证可读、可点、可输入；桌面端优先保证工作区效率和信息层级。
