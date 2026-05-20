# Desktop llama.cpp Runtime

Breeze Desktop uses an in-process JNI bridge for on-device GGUF inference.
The runtime uses llama.cpp GPU offload when the selected build backend exposes a GPU
device. `auto` selects Metal on macOS, Vulkan on Windows/Linux, and CPU elsewhere.

Desktop support is intended to cover both macOS and Windows as first-class targets.
Linux stays in the same Desktop runtime family, but follows after macOS and Windows
for validation and compatibility hardening.

The native runtime is built from a pinned submodule:

- Source: `third_party/llama.cpp`
- Pinned tag: `b9246`
- Pinned commit: `871b0b70f81d26494613ad7a9dcb933b1aec4611`

When the Gradle project syncs and `third_party/llama.cpp` is missing, the root Gradle
build now attempts to initialize that submodule automatically with:

```bash
git submodule update --init --recursive -- third_party/llama.cpp
```

This keeps Android Studio / IntelliJ sync aligned with later native builds. The sync
machine still needs `git` available on `PATH` or exposed through `BREEZE_GIT`.

The `:runtime:llama` module now builds and bundles the current platform native runtime by default.
This keeps the packaged app aligned with the active build target so users do not need an
extra Gradle property just to get the correct GPU backend.

To build and bundle the Desktop runtime directly:

```bash
./gradlew :runtime:llama:jvmProcessResources
```

To verify that the current host runtime was actually copied into JVM resources:

```bash
./gradlew :runtime:llama:verifyDesktopLlamaBundledRuntime
```

GPU backend selection:

```bash
# Default: macOS=metal, Windows/Linux=vulkan
./gradlew :runtime:llama:jvmProcessResources

# Explicit platform package backends
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=metal
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=vulkan
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=cuda
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=hip
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=sycl
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=opencl
./gradlew :runtime:llama:jvmProcessResources -PbreezeDesktopLlamaGpuBackend=cpu
```

Non-default GPU backends require the platform build image to provide the matching SDK
or driver development files, such as Vulkan SDK, CUDA Toolkit, ROCm/HIP, oneAPI/SYCL,
or OpenCL headers/libraries.

Desktop `run` and packaging tasks pick up the bundled runtime automatically through the
runtime module JVM resources pipeline, so no extra switch is required:

```bash
./gradlew :app:desktop:run
```

Windows MSI packaging stays host-specific. The JNI runtime is bundled from the current
build host, so a real Windows MSI must be built on a Windows host:

```bash
./gradlew :runtime:llama:verifyDesktopLlamaBundledRuntime
./gradlew :app:desktop:packageMsi
```

If you explicitly set `-PbreezeDesktopLlamaTargetOs` or `-PbreezeDesktopLlamaTargetArch`,
the target must still match the current build host. Cross-compiling the Desktop llama
runtime is not supported yet; Windows DLLs and MSI artifacts must be produced on Windows.

To run a real GGUF smoke test:

```bash
./gradlew :runtime:llama:jvmTest \
  -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
```

Without `breezeSmokeGgufPath`, the smoke test exits early and does not require a local
model file.

JVM tests also cover the Desktop runtime path rules used by Windows packaging and cache
installation, including:

- `windows-x64` / `windows-arm64` platform segment mapping
- `%APPDATA%/Breeze/models/runtime/native` cache resolution
- bundled JVM resource path mapping for `breeze_llama_jni.dll`

Runtime loading order:

1. `BREEZE_LLAMA_JNI_LIBRARY_PATH`, when set to an existing native library.
2. The app support runtime cache under Breeze model paths.
3. The bundled JVM resource emitted by `:runtime:llama:jvmProcessResources`.

When no explicit library path is configured, Breeze compares the bundled runtime with the
cached copy under app support and refreshes that cache automatically if the bundled one
changes. This prevents stale CPU-only libraries from shadowing newer GPU-enabled builds.

If the runtime is missing, the app can still run; selecting an on-device model will report
that the local runtime is unavailable until the native library is built or provided.
