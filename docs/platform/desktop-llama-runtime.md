# Desktop llama.cpp Runtime

Breeze Desktop uses an in-process JNI bridge for on-device GGUF inference.
The runtime uses llama.cpp GPU offload when the selected build backend exposes a GPU
device. `auto` selects Metal on macOS, Vulkan on Windows/Linux, and CPU elsewhere.

The native runtime is built from a pinned submodule:

- Source: `third_party/llama.cpp`
- Pinned tag: `b9246`
- Pinned commit: `871b0b70f81d26494613ad7a9dcb933b1aec4611`

Normal Desktop runs do not build the native runtime. This keeps app startup builds offline
and avoids requiring CMake when the user is not using local models.

To build and bundle the Desktop runtime:

```bash
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true
```

GPU backend selection:

```bash
# Default: macOS=metal, Windows/Linux=vulkan
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true

# Explicit platform package backends
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=metal
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=vulkan
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=cuda
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=hip
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=sycl
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=opencl
./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true -PbreezeDesktopLlamaGpuBackend=cpu
```

Non-default GPU backends require the platform build image to provide the matching SDK
or driver development files, such as Vulkan SDK, CUDA Toolkit, ROCm/HIP, oneAPI/SYCL,
or OpenCL headers/libraries.

To make Desktop `run` and packaging tasks bundle the runtime automatically:

```bash
./gradlew :app:desktop:run -PbreezeBuildDesktopLlamaRuntime=true
```

To run a real GGUF smoke test:

```bash
./gradlew :data:jvmTest \
  -PbreezeBuildDesktopLlamaRuntime=true \
  -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
```

Without `breezeSmokeGgufPath`, the smoke test exits early and does not require a local
model file.

Runtime loading order:

1. `BREEZE_LLAMA_JNI_LIBRARY_PATH`, when set to an existing native library.
2. The app support runtime cache under Breeze model paths.
3. The bundled JVM resource emitted by `:data:jvmProcessResources`.

If the runtime is missing, the app can still run; selecting an on-device model will report
that the local runtime is unavailable until the native library is built or provided.
