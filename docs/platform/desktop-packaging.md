# Desktop Packaging

`Breeze` 的 Desktop 分发目前基于 Compose Desktop `nativeDistributions`，目标产物包括：

- macOS `DMG`
- Windows `MSI`
- Linux `DEB`

当前仓库已经提供两个便捷脚本：

- `scripts/package-macos-dmg.sh`
- `scripts/package-windows-msi.ps1`

## 主机限制

- `DMG` 必须在 macOS 主机上打包
- `MSI` 必须在 Windows 主机上打包

这是 Compose Desktop / `jpackage` 的平台限制，不建议尝试跨平台直接产出安装包。

## 使用方式

### macOS

发布版：

```bash
./scripts/package-macos-dmg.sh
```

调试版：

```bash
./scripts/package-macos-dmg.sh debug
```

默认调用：

- 发布版：`:app:desktop:packageReleaseDmg`
- 调试版：`:app:desktop:packageDmg`

### Windows

发布版：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows-msi.ps1
```

调试版：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows-msi.ps1 -BuildKind debug
```

默认调用：

- 发布版：`:app:desktop:packageReleaseMsi`
- 调试版：`:app:desktop:packageMsi`

## 产物位置

脚本会在构建完成后打印产物路径。

默认可在以下目录查找：

- `app/desktop/build/compose/binaries/`

## 混淆与加密建议

### 需要马上做的吗

当前阶段不建议把“安装包加密”当成打包必做项。

原因：

- `DMG` / `MSI` 本身不是安全边界，做安装包层面的加密意义不大
- Desktop 客户端一旦本地运行，代码和资源最终都会落到用户设备上
- 如果把真实密钥放进客户端，即使加密或混淆，也不能阻止有经验的用户提取

### 当前更重要的事情

对外分发时，比“加密安装包”更重要的是：

- macOS 做代码签名与 notarization
- Windows 做代码签名，减少 SmartScreen 告警
- 不在客户端内硬编码真实服务端密钥
- 敏感配置尽量走服务端签发或用户本地输入

### 要不要做代码混淆

如果只是内部使用、个人项目、早期验证版本，可以先不做。

如果后续准备公开分发，并且你确实担心 JVM 层代码被直接反编译，可以考虑只对发布版增加混淆，但要注意：

- 当前仓库已有 `packageReleaseDmg` / `packageReleaseMsi` / `proguardReleaseJars` 任务
- 目前 `app/desktop/build.gradle.kts` 还没有接自定义 Desktop ProGuard 规则
- 一旦开启较强混淆，需要重点回归 Compose、Koin、Kotlinx Serialization、Room、JNI 入口是否被错误裁剪

也就是说，发布版“可做适度混淆”，但它不是当前打包能否正常安装的前置条件。

### 模型与本地资源要不要加密

如果以后要分发本地模型、词表或其他大资源：

- 不建议把“资源加密”作为主要防护手段
- 应优先考虑安装后下载、许可控制、版本校验和完整性校验
- 资源一旦需要在本地解密运行，密钥和解密逻辑仍然会落到客户端

## 当前建议

现阶段建议采用这套顺序：

1. 先稳定 `DMG` / `MSI` 可安装、可启动
2. 再补 macOS notarization 和 Windows 签名
3. 最后再视外发范围决定是否给 Desktop 发布版补 ProGuard 混淆规则
