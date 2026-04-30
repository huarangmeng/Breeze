# 持久化路径策略

本文档记录 `Breeze` 在各平台上的本地持久化路径规则，适用于以下模块：

- `:data/storage` 的 `Room3`
- `:data/settings` 的 AndroidX `DataStore`

目标：

- 让数据库与设置文件遵循同一套平台目录语义
- 避免把内部状态写入面向用户的文档目录或临时目录
- 让 `commonMain` 只保留共享配置，把具体文件系统差异收敛到平台 source set

## 适用原则

- Android：使用应用私有目录
- iOS：使用 `Application Support`
- JVM Desktop：按操作系统使用平台约定的应用数据目录
- JS / Wasm：沿用当前 Web 端运行时默认相对路径策略

这些规则同时约束 `Room3` 和 `DataStore`，避免两套持久化实现各自演化出不同目录语义。

## 平台映射

| 平台 | 数据库 / 设置目录 | 说明 |
| --- | --- | --- |
| Android | `Context.filesDir` / `Context.getDatabasePath(name)` | 应用私有目录，不暴露给用户文档空间 |
| iOS | `Application Support` | 适合应用内部状态，不应写入 `Documents` |
| macOS | `~/Library/Application Support/Breeze` | 与系统桌面应用习惯一致 |
| Windows | `%APPDATA%/Breeze` | 与 Roaming AppData 约定一致 |
| Linux | `$XDG_DATA_HOME/Breeze` 或 `~/.local/share/Breeze` | 遵循 XDG 目录规范 |
| JS / Wasm | 当前运行时相对路径 / Web driver 约定 | 受宿主和浏览器运行时限制，暂不统一到本地文件系统语义 |

## 当前实现

`Room3`：

- `commonMain` 只负责 `RoomDatabase.Builder` 的共享配置，例如 `setDriver()`、`setQueryCoroutineContext()`、`fallbackToDestructiveMigration()`
- 各平台 source set 负责创建 `Room.databaseBuilder(...)`

`DataStore`：

- `commonMain` 只负责 `PreferenceDataStoreFactory.createWithPath(...)`
- 各平台 source set 负责提供 `createPlatformSettingsPath(...)`

## 目录选择说明

### 为什么 iOS 不用 `Documents`

`Documents` 更适合用户可见、可导出的内容。`Breeze` 的数据库和设置属于应用内部状态，写入 `Application Support` 更符合系统语义，也能避免把内部文件暴露成“用户文档”。

### 为什么 JVM 不用临时目录

临时目录可能被系统清理，不适合长期保存聊天数据库和设置。`Application Support` / `AppData` / `XDG_DATA_HOME` 才是桌面应用的稳定存储目录。

### 为什么 Android 继续使用应用私有目录

Android 的应用私有目录已经符合内部状态存储要求，不需要额外引入新的目录抽象。

### 为什么 JS / Wasm 先不强行统一

Web 平台的持久化依赖浏览器能力和当前 driver 实现，不存在与原生平台完全等价的“应用支持目录”。在没有稳定宿主抽象前，保持当前实现比伪造目录语义更安全。

## 代码入口

- `Room3` 公共配置：[BreezeDatabase.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/commonMain/kotlin/com/hrm/breeze/data/storage/BreezeDatabase.kt)
- `Room3` Android 路径：[BreezeDatabase.android.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/androidMain/kotlin/com/hrm/breeze/data/storage/BreezeDatabase.android.kt)
- `Room3` iOS 路径：[BreezeDatabase.ios.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/iosMain/kotlin/com/hrm/breeze/data/storage/BreezeDatabase.ios.kt)
- `Room3` JVM 路径：[BreezeDatabase.jvm.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/jvmMain/kotlin/com/hrm/breeze/data/storage/BreezeDatabase.jvm.kt)
- `DataStore` 公共配置：[BreezeSettings.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/commonMain/kotlin/com/hrm/breeze/data/settings/BreezeSettings.kt)
- `DataStore` iOS 路径：[BreezeSettings.ios.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/iosMain/kotlin/com/hrm/breeze/data/settings/BreezeSettings.ios.kt)
- `DataStore` JVM 路径：[BreezeSettings.jvm.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/jvmMain/kotlin/com/hrm/breeze/data/settings/BreezeSettings.jvm.kt)
- 平台路径辅助层：
  - [BreezePlatformPaths.ios.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/iosMain/kotlin/com/hrm/breeze/data/platform/BreezePlatformPaths.ios.kt)
  - [BreezePlatformPaths.jvm.kt](file:///Users/bytedance/AndroidStudioProjects/Breeze/data/src/jvmMain/kotlin/com/hrm/breeze/data/platform/BreezePlatformPaths.jvm.kt)
