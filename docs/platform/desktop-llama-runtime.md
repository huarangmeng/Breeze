# Desktop llama.cpp Runtime

Breeze Desktop uses an in-process JNI bridge for on-device GGUF inference.
The runtime uses llama.cpp GPU offload when the selected build backend exposes a GPU
device. `auto` selects Metal on macOS, Vulkan on Windows/Linux, and CPU elsewhere.

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

To run a real GGUF smoke test:

```bash
./gradlew :runtime:llama:jvmTest \
  -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
```

Without `breezeSmokeGgufPath`, the smoke test exits early and does not require a local
model file.

Runtime loading order:

1. `BREEZE_LLAMA_JNI_LIBRARY_PATH`, when set to an existing native library.
2. The app support runtime cache under Breeze model paths.
3. The bundled JVM resource emitted by `:runtime:llama:jvmProcessResources`.

When no explicit library path is configured, Breeze compares the bundled runtime with the
cached copy under app support and refreshes that cache automatically if the bundled one
changes. This prevents stale CPU-only libraries from shadowing newer GPU-enabled builds.

If the runtime is missing, the app can still run; selecting an on-device model will report
that the local runtime is unavailable until the native library is built or provided.
