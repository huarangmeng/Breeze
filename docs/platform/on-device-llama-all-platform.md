# On-Device llama Runtime

本文档是 `Breeze` 端侧 `llama.cpp` runtime 的唯一平台文档，统一覆盖：

- 全平台目标架构
- 当前实现状态
- Desktop / Android / iOS 的构建与打包链路
- 后续 Web 路线与剩余工作

约束继承自仓库现状与既有决策：

- 端侧推理必须集成在应用进程内，不能依赖外部进程或独立服务
- 禁止为平台适配直接修改 `third_party/llama.cpp` 源码
- GPU backend 必须按平台自动选择，不能要求用户手动切换核心构建开关
- native/runtime 产物必须随应用一起打包，并在运行时自动刷新旧缓存

## 总览

`runtime/api` 已提供共享接口 `OnDeviceRuntime`，`runtime/llama` 当前已经形成四类 runtime family：

- `Desktop JVM`
- `Android JNI`
- `Apple Native`
- `WebAssembly`

其中真正已打通的，是前三层中的共享编排层和大部分 native family seam：

1. 共享编排层

- 位置：`runtime/llama` 的 `commonMain`
- 职责：请求归一化、ChatML prompt 拼装、流式协议、能力探测接口

2. 平台 adapter 层

- 位置：各平台 source set
- 职责：把共享请求转换成该平台可调用的 native interface

3. native/runtime 产物层

- 位置：Gradle / CMake / Xcode / Kotlin/Native cinterop
- 职责：编译 `third_party/llama.cpp`、选择 backend、输出随应用分发的 runtime 产物

## 当前状态

### 已完成

- `runtime/api` 已暴露 runtime capability，UI 与仓库层可在 unsupported 平台提前降级
- `commonMain` 已统一收敛：
  - request 归一化
  - ChatML prompt 组装
  - capability 接口
- Desktop JVM family 已形成完整 JNI 路径
- Android JNI family 已接入真实 Kotlin bridge 与宿主 native build 配置
- Apple Native family 已接入真实 Kotlin/Native bridge、Apple static runtime 构建与 cinterop

### 各平台状态

#### Desktop JVM

- `macOS`：已可用，默认 backend 为 `metal`
- `Windows`：已纳入一等目标，默认 backend 为 `vulkan`
- `Linux`：同属 Desktop family，默认 backend 为 `vulkan`

当前 Desktop 侧已具备：

- `breeze_llama_jni.cpp` JNI bridge
- `:runtime:llama` 的 `jvmProcessResources` 资源打包
- 运行时缓存刷新与旧缓存覆盖
- Windows 路径/资源规则测试
- Windows MSI 打包前置校验

#### Android JNI

- `OnDeviceRuntimeBridge.android.kt` 已从占位改为真实 JNI adapter
- Android 宿主 `app/android` 已配置 NDK、ABI 与 `externalNativeBuild`
- 复用 `runtime/llama/src/androidMain/cpp/CMakeLists.txt` 构建 `libbreeze_llama_jni.so`

当前剩余问题主要不是代码缺口，而是环境验证：

- 本机 Android NDK license 未接受时，`assembleDebug` 会被环境阻塞
- Android 真机 smoke test 尚未补齐

#### Apple Native

- `OnDeviceRuntimeBridge.ios.kt` 已从占位改为真实 adapter
- 已新增：
  - `breeze_llama_apple_bridge.h`
  - `breeze_llama_apple_bridge.mm`
  - `breeze_llama_apple.def`
- `runtime/llama/build.gradle.kts` 已增加：
  - `iosArm64` 原生构建任务
  - `iosSimulatorArm64` 原生构建任务
  - Apple static bridge 打包
  - Kotlin/Native cinterop 与 linker 配置

当前已验证：

- `:runtime:llama:linkDebugFrameworkIosSimulatorArm64`
- `:runtime:llama:linkDebugFrameworkIosArm64`

#### WebAssembly

- `jsMain` / `wasmJsMain` 仍是占位实现
- 尚未接入：
  - Wasm runtime
  - Web Worker 推理
  - OPFS / IndexedDB GGUF 持久化

## Runtime Capability

`runtime/api` 现在暴露的是结构化 capability，而不是让上层只能在运行时报错：

- 是否支持端侧 `llama`
- family 类型
- 默认 backend
- 支持的 backend 集合
- 是否支持模型文件持久化
- 不支持时的原因

这使得：

- `OnDeviceModelsScreen` 可以禁用下载/选择按钮
- `ChatViewModel` 可以在发送前直接显示 unavailable reason
- unsupported 平台不会再等到 `streamCompletion()` 才失败

## 平台架构

### Shared Llama Core

平台无关逻辑统一收敛在 `commonMain`：

- `OnDeviceRuntimeLaunchRequest`
- `OnDeviceRuntimeCompletionRequest`
- 请求校验
- ChatML prompt 组装
- capability 对外接口

这样 Android / iOS / Desktop 只需要实现底层 adapter，不再复制上层推理协议。

### Native Adapter Families

按桥接方式分 family，而不是按平台零碎复制：

- `Jvm/JNI` family：macOS / Windows / Linux
- `Android/JNI` family：Android
- `Apple Native` family：iOS
- `WebAssembly` family：Web

每个 family 只对共享层暴露统一的 `load / generate / cancel / unload` 语义。

## 构建与打包

### Desktop JVM

Desktop runtime 使用 vendored `llama.cpp` 构建当前 host 平台动态库，并打进 `:runtime:llama` 的 JVM resources。

来源：

- Source: `third_party/llama.cpp`
- Pinned tag: `b9246`
- Pinned commit: `871b0b70f81d26494613ad7a9dcb933b1aec4611`

若项目同步时缺失 submodule，根 Gradle 会尝试自动执行：

```bash
git submodule update --init --recursive -- third_party/llama.cpp
```

直接构建并打包 Desktop runtime：

```bash
./gradlew :runtime:llama:jvmProcessResources
```

校验当前 host runtime 已进入 JVM resources：

```bash
./gradlew :runtime:llama:verifyDesktopLlamaBundledRuntime
```

GPU backend 选择：

```bash
# 默认: macOS=metal, Windows/Linux=vulkan
./gradlew :runtime:llama:jvmProcessResources

# 显式指定 backend
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=metal
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=vulkan
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=cuda
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=hip
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=sycl
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=opencl
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=cpu
```

Desktop 运行：

```bash
./gradlew :app:desktop:run
```

Windows MSI 打包必须在 Windows host 上执行：

```bash
./gradlew :runtime:llama:verifyDesktopLlamaBundledRuntime
./gradlew :app:desktop:packageMsi
```

若显式设置：

- `-PbreezeDesktopLlamaTargetOs`
- `-PbreezeDesktopLlamaTargetArch`

目标仍必须与当前 build host 一致；Desktop llama runtime 目前不支持跨 host 交叉打包。

Desktop smoke test：

```bash
./gradlew :runtime:llama:jvmTest \
  -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
```

不传 `breezeSmokeGgufPath` 时，smoke test 会提前退出，不要求本地模型文件。

#### Desktop 运行时加载顺序

1. `BREEZE_LLAMA_JNI_LIBRARY_PATH`
2. Breeze app support 下的 runtime cache
3. `:runtime:llama:jvmProcessResources` 打进包里的 JVM resource

当未配置显式库路径时，运行时会比较 bundled runtime 与本地缓存，不一致时自动覆盖旧缓存，避免旧的 CPU-only 动态库遮蔽新的 GPU build。

### Android

Android runtime 仍使用 JNI 方案，不重写纯 Kotlin 推理层。

当前代码路径：

- Kotlin adapter：`runtime/llama/src/androidMain/kotlin/.../OnDeviceRuntimeBridge.android.kt`
- Native build：`runtime/llama/src/androidMain/cpp/CMakeLists.txt`
- Android host 打包：`app/android/build.gradle.kts`

当前 ABI 策略：

- `arm64-v8a`
- `x86_64`

当前 Android 宿主会在 `app/android` 中构建并打包 `.so`，而不是要求手动复制 native 库。

Android 构建命令：

```bash
./gradlew :app:android:assembleDebug
```

当前环境注意项：

- 机器必须安装并接受对应 NDK license
- 若 NDK 未准备好，Gradle 会在 Android module 配置阶段失败

### iOS

iOS 使用 `Apple Native + Kotlin/Native cinterop`，不走 JNI。

当前代码路径：

- Kotlin adapter：`runtime/llama/src/iosMain/kotlin/.../OnDeviceRuntimeBridge.ios.kt`
- cinterop 定义：`runtime/llama/src/iosMain/cinterop/breeze_llama_apple.def`
- Apple bridge：`runtime/llama/src/iosMain/native/breeze_llama_apple_bridge.h`
- Apple bridge 实现：`runtime/llama/src/iosMain/native/breeze_llama_apple_bridge.mm`

当前 bridge 对外收敛为稳定 C ABI：

- `breeze_llama_load_model`
- `breeze_llama_unload`
- `breeze_llama_start_generation`
- `breeze_llama_next_token`
- `breeze_llama_cancel`
- `breeze_llama_generation_free`

Apple 构建任务会分别产出：

- `iosArm64`
- `iosSimulatorArm64`

验证命令：

```bash
./gradlew :runtime:llama:linkDebugFrameworkIosSimulatorArm64
./gradlew :runtime:llama:linkDebugFrameworkIosArm64
```

当前 backend 策略：

- 真机默认优先 `metal`
- 模拟器构建允许回落 CPU

当前 Xcode 宿主仍通过 `:app:shared:embedAndSignAppleFrameworkForXcode` 驱动 KMP framework；iOS runtime 已能在 `runtime/llama` 级别完成原生构建和 link 验证。

### WebAssembly

Web 仍是后续平台，不建议与移动端并行继续扩张。

仍需补齐：

- `llama.cpp` 的 Wasm runtime
- `Web Worker` 推理线程
- WebGPU 优先与 CPU Wasm 兜底
- OPFS / IndexedDB 模型持久化

## 实施顺序

推荐继续按 family 推进，而不是按平台散点补丁：

### Phase 1: Desktop JVM

- 完成 `macOS + Windows` 构建、打包、smoke test 基线
- 补 Linux 兼容性收尾

### Phase 2: Android JNI

- 补 Android 真机 smoke test
- 处理宿主 NDK 环境与 CI 基线

### Phase 3: Apple Native

- 补 iOS smoke test
- 验证 Xcode 宿主完整运行链路
- 补文档与可能的宿主集成说明

### Phase 4: WebAssembly

- 设计 Worker runtime
- 落地 GGUF Web 存储与 smoke test

## 文件级入口

当前最关键的实现入口：

- `runtime/api/src/commonMain/.../OnDeviceRuntime.kt`
- `runtime/llama/src/commonMain/.../OnDeviceRuntimeManager.kt`
- `runtime/llama/src/jvmMain/.../OnDeviceRuntimeBridge.jvm.kt`
- `runtime/llama/src/androidMain/.../OnDeviceRuntimeBridge.android.kt`
- `runtime/llama/src/androidMain/cpp/CMakeLists.txt`
- `runtime/llama/src/iosMain/.../OnDeviceRuntimeBridge.ios.kt`
- `runtime/llama/src/iosMain/cinterop/breeze_llama_apple.def`
- `runtime/llama/src/iosMain/native/breeze_llama_apple_bridge.h`
- `runtime/llama/src/iosMain/native/breeze_llama_apple_bridge.mm`
- `runtime/llama/build.gradle.kts`
- `app/android/build.gradle.kts`
- `app/shared/.../ChatViewModel.kt`
- `app/shared/.../OnDeviceModelsScreen.kt`

## 不建议的路径

- 不建议修改 `third_party/llama.cpp` 源码来修平台问题
- 不建议把 iOS 塞进 JNI/JVM 语义
- 不建议让 Web 直接复用 native 平台的模型存储假设
- 不建议让上层业务代码理解 native 资源路径、C++ 类型或 Xcode/CMake 细节

## 结论

`Breeze` 的端侧 `llama.cpp` 已不再是“只有 macOS 跑通”的状态，而是已经形成一套统一上层编排、按 native family 分治的 runtime 架构：

- Desktop JVM：已形成可交付基线
- Android JNI：代码链路已接通，待环境与真机验证补齐
- Apple Native：构建与推理链路已接通，待宿主级验证与 smoke test 补齐
- WebAssembly：仍未开始

后续应继续围绕这份单文档推进，避免再拆出按平台重复描述的平行文档。
