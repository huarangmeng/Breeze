# Third-party Source

This directory contains pinned third-party source submodules that Breeze builds as part
of its local runtime.

## llama.cpp

- Path: `third_party/llama.cpp`
- Upstream: https://github.com/ggml-org/llama.cpp
- Pinned tag: `b9246`
- Pinned commit: `871b0b70f81d26494613ad7a9dcb933b1aec4611`

This is a Git submodule pinned to a reviewed upstream tag. Keep the full upstream source
tree so Breeze can build platform GPU backends without repeatedly copying missing
backend files into the main repository.

The Desktop in-process llama runtime builds only from this pinned submodule. Do not use
Gradle or CMake network fetches for normal builds. Initialize submodules before building
the native runtime:

```bash
git submodule update --init --recursive third_party/llama.cpp
```

## Updating llama.cpp

Use an explicit upstream tag or commit. Do not track `master` in Gradle or CMake.

1. Fetch the reviewed upstream revision in the submodule:

   ```bash
   git -C third_party/llama.cpp fetch --tags origin
   git -C third_party/llama.cpp checkout <tag-or-commit>
   ```

2. Verify and record the pinned commit:

   ```bash
   git -C third_party/llama.cpp rev-parse HEAD
   ```

3. Keep Breeze's runtime CMake backend policy explicit:

   - Do not enable server, examples, tests, tools, or conversion utilities.
   - Use `-PbreezeDesktopLlamaGpuBackend=auto` by default. It maps macOS to Metal,
     Windows/Linux to Vulkan, and unknown hosts to CPU.
   - Build explicit platform packages with `metal`, `vulkan`, `cuda`, `hip`, `sycl`,
     `opencl`, or `cpu` only when the platform image has the matching SDK and runtime
     dependencies.

4. Rebuild and verify:

   ```bash
   ./gradlew :data:jvmProcessResources -PbreezeBuildDesktopLlamaRuntime=true
   ./gradlew :data:compileKotlinJvm :app:desktop:compileKotlin
   ```

   If a local GGUF model is available, also run:

   ```bash
   ./gradlew :data:jvmTest \
     -PbreezeBuildDesktopLlamaRuntime=true \
     -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
   ```

5. Check the generated native library dependencies. On macOS:

   ```bash
   otool -L data/build/processedResources/jvm/main/breeze-runtime/macos-arm64/libbreeze_llama_jni.dylib
   ```

   The Desktop build should not introduce unexpected runtime dependencies beyond system
   libraries, Metal/Accelerate on macOS, and libc++.
