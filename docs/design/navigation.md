# Breeze Navigation

本文档记录 `Breeze` 当前导航结构、根布局切换规则、返回栈策略与深链边界。它描述的是当前已落地实现，不替代后续更细的 feature IA 设计。

## 1. 当前结构

当前导航基于 `androidx.navigation:navigation-compose` 的 Compose Multiplatform 版本，分两层：

- `:core-ui/navigation/Destination.kt`：定义共享 destination contract
- `:composeApp/navigation/BreezeNavHost.kt`：定义根级 `NavHost` 与顶层导航壳

当前顶层 destination 为：

| Destination | Route           | 角色 |
| ----------- | --------------- | ---- |
| `Chat` | `chat` | 主聊天页 |
| `History` | `history` | 历史会话浏览页 |
| `ApiConfig` | `api-config` | Provider 与鉴权配置页 |
| `ModelSettings` | `model-settings` | 模型选择与参数骨架页 |

当前还预留了一个带参数的非顶层 destination：

| Destination | Route Pattern | 用途 |
| ----------- | ------------- | ---- |
| `ChatThread(conversationId)` | `chat/thread/{conversationId}` | 后续用于直接进入指定会话线程 |

## 2. 根布局

`BreezeNavHost` 统一消费 `LocalWindowInfo.current`，根据 `paneMode` 选择根布局：

- `PaneMode.Single`
  - 对应 `Compact / Medium`
  - 使用顶部横向导航条
  - 当前内容以单栏方式占满主体区域
- `PaneMode.ListDetail`
  - 对应 `Expanded`
  - 使用左侧 feature 列表 + 右侧详情区
  - 左右栏比例当前使用 `Modifier.weight()` 控制，避免固定宽度导致溢出

这意味着：

- 根导航壳只决定“怎么切 feature”
- 各 feature 仍需自己根据 `LocalWindowInfo.current` 做页面内响应式

## 3. 导航原则

当前遵循以下约束：

- 顶层页面使用 `TopLevelDestination`
- 无参数顶层路由统一使用稳定字符串常量，不在调用处手写 route
- 带参数页面通过 destination 类型上的 builder 生成 route，避免 stringly-typed 拼接
- `Route` 负责 ViewModel 装配，`Screen` 不直接参与导航控制

当前顶层导航入口来源于 `BreezeDestinations.topLevel`，根壳的顶部导航条和宽屏左栏列表都消费同一份 destination 列表，避免双份配置漂移。

## 4. 返回栈策略

当前顶层跳转由 `BreezeNavHost.navigateTo()` 统一执行，策略如下：

- `launchSingleTop = true`
  - 避免重复点击当前顶层页时在栈中压入同一路由
- `restoreState = true`
  - 返回已访问过的顶层页时尽量恢复该目的地的状态
- `popUpTo(graphStartRoute) { saveState = true }`
  - 顶层切换时把返回栈收敛到图起点
  - 同时保存已有目的地状态，避免每次切换都完全冷启动

当前这套策略的意图是：

- 顶层导航表现更接近 tab / section 切换
- 不把四个顶层页堆成很深的返回链
- 保留页面状态恢复的空间

## 5. 深链边界

当前代码层只定义了 `ChatThread(conversationId)` 这样的参数路由能力，但还没有建立完整深链策略，因此目前结论是：

- 顶层 destination 暂不承诺外部深链入口
- 参数 destination 暂不承诺 URL scheme、Web deep link 或通知跳转
- 真正接入深链前，需要先补：
  - 参数合法性约束
  - 非法 conversationId 的回退策略
  - 宿主侧 URL / intent 映射规则

后续如果要开放深链，建议优先从 `ChatThread(conversationId)` 开始，并新增对应 ADR。

## 6. 当前限制

当前导航实现仍有这些明确边界：

- 还没有嵌套子图
- 还没有 detail back stack 专项策略
- 还没有浏览器 URL 同步策略
- 还没有 `ChatThread` 的真实页面接入
- `Medium` 当前仍复用单栏壳，没有单独抽屉导航

这些限制是当前阶段的有意收敛，不视为缺陷；等 `M5` 之后再根据复杂度继续演进。
