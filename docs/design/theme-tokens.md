# Breeze Theme Tokens

本文档定义 `Breeze` 当前主题 token 的命名方式、语义边界和扩展规则，作为 `docs/plan.md` 中 `M1` 的落地产物。

## 1. 目标

- 把颜色、圆角、间距、字型收敛到 `:core-ui/theme`
- 让页面只消费 `BreezeTheme.xxx`，不直接散落硬编码
- 为后续 `M2` 响应式和 `M4` feature 页面提供稳定视觉基线

## 2. Token 分层

当前主题层暴露四组 token：

- `BreezeTheme.colorScheme`：Material 3 语义色，承接 `primary / surface / error` 等系统语义
- `BreezeTheme.extendedColors`：聊天气泡、输入框、代码块等 Breeze 业务语义色
- `BreezeTheme.shapes`：大中小圆角、胶囊形状、输入框和消息气泡形状
- `BreezeTheme.spacing`：`xxs ~ xxl` 间距阶梯，附带 `hairline / micro` 这类基础尺寸
- `BreezeTheme.typography`：标题、正文、标签、代码字型

原则：优先复用 Material 语义；只有 Material 不足以表达业务语义时，才扩展到 `extendedColors`。

## 3. 命名规则

- 颜色命名按用途，而不是按视觉印象：例如 `chatUserBubble`，不要用 `lightBlue2`
- 圆角命名按语义层级：`small / medium / large`，不要写 `radius16`
- 间距命名按阶梯：`xxs / xs / sm / md / lg / xl / xxl`
- 字型命名按阅读角色：`titleLarge / bodyMedium / labelSmall / code`

## 4. 使用约束

- 页面优先从 `BreezeTheme.spacing` 取 padding、gap、border 宽度
- 页面优先从 `BreezeTheme.shapes` 取容器和气泡圆角
- 页面正文优先从 `BreezeTheme.typography` 取样式
- 错误、成功、警告等反馈色保留语义用途，不作为装饰主色
- `MaterialTheme.colorScheme.error` 继续保留给错误态，不能挪作普通强调色

## 5. 扩展方式

新增 token 时遵循以下顺序：

1. 先判断现有 `colorScheme / shapes / typography / spacing` 是否足够
2. 如果是业务语义色缺失，追加到 `BreezeExtendedColors`
3. 如果是共享容器形状缺失，追加到 `BreezeShapes`
4. 如果是重复出现的尺寸节奏，追加到 `BreezeSpacing`
5. 如果是重复出现的文本角色，追加到 `BreezeTypography`

不要在单个页面里先硬编码、后迁移；应先补 token，再消费 token。

## 6. 当前基线

当前已落地：

- 聊天气泡的 `incoming / outgoing` 圆角
- 输入框与代码块容器形状
- `hairline / micro / xxs ~ xxl` 间距阶梯
- `title / body / label / code` 基础字型

后续如果引入更多组件，应继续向 `:core-ui/theme` 收敛，避免在 feature 层再次出现第二套视觉体系。
