# Docs

这里是 `Breeze` 仓库的长期文档归档目录。

## 目标

`docs/` 用于承接除根目录入口文档之外的大部分说明材料，避免文档继续散落在仓库根目录，便于检索、归档和后续扩展。

## 放什么

建议把以下内容放到 `docs/`：

- 设计说明
- 页面适配方案
- 架构说明
- ADR
- 平台差异记录
- 任务拆解与实施计划
- 排查记录与结论

## 建议结构

后续可逐步建立以下子目录：

- `docs/design/`
- `docs/architecture/`
- `docs/adr/`
- `docs/platform/`
- `docs/work-items/`

当前如果只需要新增一篇文档，也优先放在 `docs/` 下的合适位置，而不是继续放到根目录。

## 已有文档

- [plan.md](plan.md)：项目总体计划（M0 → M6 里程碑与任务卡）
- [architecture/overview.md](architecture/overview.md)：仓库架构基线（顶层模块、分层、响应式、演进路线）
- [design/theme-tokens.md](design/theme-tokens.md)：主题 token 基线（颜色、圆角、间距、字型与扩展规则）
- [design/responsive.md](design/responsive.md)：响应式基线（官方 adaptive、WindowInfo 包装层与布局切换规则）

## 根目录保留项

根目录建议只保留高层入口文档：

- `README.md`
- `AGENTS.md`

其他长期 Markdown 文档默认都应进入 `docs/`。
