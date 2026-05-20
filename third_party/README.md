# Third-party Source

This directory vendors source code that Breeze builds as part of its local runtime.

## llama.cpp

- Path: `third_party/llama.cpp`
- Upstream: https://github.com/ggml-org/llama.cpp
- Pinned tag: `b9246`
- Pinned commit: `871b0b70f81d26494613ad7a9dcb933b1aec4611`

This is a trimmed source copy for Breeze's CPU-only Desktop JNI runtime. It keeps the
CMake files, public headers, `src`, `ggml`, and upstream license metadata required to
build `llama` and `ggml`. It intentionally omits upstream examples, tests, tools, docs,
conversion scripts, models, media, and other repository maintenance files.

The Desktop in-process llama runtime builds only from this vendored source. Do not use
Gradle or CMake network fetches for normal builds. When upgrading llama.cpp, refresh the
trimmed source from a reviewed upstream tag and update the pinned metadata here.

## Updating llama.cpp

Use an explicit upstream tag or commit. Do not track `master` in Gradle, CMake, or a
submodule.

1. Clone the reviewed upstream revision outside the repository:

   ```bash
   git clone --depth 1 --branch <tag> https://github.com/ggml-org/llama.cpp.git /tmp/breeze-llama.cpp
   ```

2. Replace `third_party/llama.cpp` with a clean archive of that revision:

   ```bash
   rm -rf third_party/llama.cpp
   mkdir -p third_party/llama.cpp
   git -C /tmp/breeze-llama.cpp archive HEAD | tar -x -C third_party/llama.cpp
   ```

3. Trim files that Breeze does not build or ship. Keep:

   - `CMakeLists.txt`
   - `cmake/`
   - `include/`
   - `src/`
   - `ggml/CMakeLists.txt`
   - `ggml/cmake/`
   - `ggml/include/`
   - `ggml/src/`
   - `LICENSE`, `AUTHORS`, `README.md`, and required `licenses/` entries

   Remove upstream examples, tests, tools, docs, conversion scripts, model samples,
   media, CI metadata, package manager files, and repository maintenance files. If a
   future llama.cpp CMake release requires a newly referenced file, add only that file
   or directory back and note why in this README.

4. Keep Breeze's runtime CMake CPU-only unless the app explicitly adds GPU support:

   - Do not enable server, examples, tests, tools, or conversion utilities.
   - Do not enable CUDA, Metal, Vulkan, OpenCL, or other GPU backends unless the Kotlin
     runtime and packaging path are updated for those dynamic dependencies.

5. Update the pinned tag and commit at the top of this file:

   ```bash
   git -C /tmp/breeze-llama.cpp rev-parse HEAD
   ```

6. Rebuild and verify:

   ```bash
   ./gradlew :data:copyDesktopLlamaRuntime -PbreezeBuildDesktopLlamaRuntime=true
   ./gradlew :data:compileKotlinJvm :app:desktop:compileKotlin
   ```

   If a local GGUF model is available, also run:

   ```bash
   ./gradlew :data:jvmTest \
     -PbreezeBuildDesktopLlamaRuntime=true \
     -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
   ```

7. Check the generated native library dependencies. On macOS:

   ```bash
   otool -L data/build/generated/llamaRuntime/resources/breeze-runtime/macos-arm64/libbreeze_llama_jni.dylib
   ```

   The CPU-only Desktop build should not introduce unexpected runtime dependencies
   beyond system libraries, Accelerate on macOS, and libc++.
