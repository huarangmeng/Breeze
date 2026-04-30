# ADR 0003: Feature Module Split Timing

## Context

`Breeze` 已经在 `:composeApp` 内落地了 4 个真实 feature 页面：

- `chat`
- `history`
- `apiconfig`
- `modelsettings`

同时，当前模块边界已经比较清晰：

- `:core-ui` 承载主题、响应式和导航基础类型
- `:domain` 承载模型与 repository 接口
- `:data` 承载 repository 实现、Settings、网络与存储
- `:composeApp` 负责根 `App()`、`NavHost`、Koin 与 ViewModel/Route 装配

当前 feature 代码虽然在目录上已经收敛到 `composeApp/src/commonMain/kotlin/com/hrm/breeze/ui/screens/*`，但它们仍然共享以下装配条件：

- 同一套 `Koin` presentation module
- 同一个 `BreezeNavHost`
- 同一批 Compose / Navigation / Lifecycle 依赖
- 大量共享的 `BreezeTheme` / `LocalWindowInfo` / navigation contract

现阶段如果直接拆出 `:feature:chat`、`:feature:history` 等模块，会带来新的 Gradle 模块配置、KMP target 对齐、依赖转发和 DI 装配成本，但还不能明显降低耦合或缩短变更半径。

## Decision

当前阶段 **暂不拆** `:feature:*` 模块，继续保持：

- feature 目录级拆分留在 `:composeApp`
- `Route + ViewModel + Screen` 作为 feature 边界
- `Destination` 保留在 `:core-ui/navigation`
- 根导航与根布局继续由 `:composeApp/navigation/BreezeNavHost.kt` 统一装配

只有在出现以下任一信号时，再启动 feature 模块拆分：

1. 某个 feature 出现独立的测试、假实现或预览依赖，开始拖累其他 feature 编译与装配
2. 某个 feature 需要独立资源、独立导航子图或明显不同的依赖集合
3. `composeApp` 的 presentation 层开始出现大面积跨 feature 回调耦合，单文件装配明显失控
4. 团队需要多人并行开发同一批 feature，模块边界能显著降低冲突面

## Consequences

正面：

- 继续保持当前 KMP / CMP 构建配置的收敛，避免为拆模块引入机械性成本
- 先把页面职责、状态边界和导航文档做稳，再决定是否升级为 Gradle 边界
- 当前 feature 仍然能通过目录和命名完成较好的可导航性

负面：

- `:composeApp` 仍然同时承担根装配与 feature presentation 代码
- 未来如果 feature 继续膨胀，再拆模块时仍需要一次迁移成本

后续动作：

- 在 `M5` 继续观察 provider、settings、history 等功能增长是否会推高拆模块收益
- 如果触发上述信号，再新增后续 ADR 记录具体模块切分方案与依赖方向

