#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LLAMA_DIR="${REPO_ROOT}/third_party/llama.cpp"
DEFAULT_TAG="b9279"

TARGET_REF="${1:-$DEFAULT_TAG}"

if [[ ! -d "${LLAMA_DIR}" ]]; then
  echo "Missing llama.cpp checkout at ${LLAMA_DIR}" >&2
  echo "Run: git submodule update --init --recursive -- third_party/llama.cpp" >&2
  exit 1
fi

if ! git -C "${LLAMA_DIR}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "third_party/llama.cpp is not a git work tree" >&2
  exit 1
fi

echo "Fetching upstream tags for llama.cpp..."
git -C "${LLAMA_DIR}" fetch --tags origin

if ! git -C "${LLAMA_DIR}" rev-parse --verify "${TARGET_REF}^{commit}" >/dev/null 2>&1; then
  echo "Unknown llama.cpp ref: ${TARGET_REF}" >&2
  exit 1
fi

TAG_NAME=""
if git -C "${LLAMA_DIR}" rev-parse "refs/tags/${TARGET_REF}" >/dev/null 2>&1; then
  TAG_NAME="${TARGET_REF}"
fi

TARGET_COMMIT="$(git -C "${LLAMA_DIR}" rev-parse "${TARGET_REF}^{commit}")"
TARGET_SUBJECT="$(git -C "${LLAMA_DIR}" show -s --format='%s' "${TARGET_COMMIT}")"

echo "Pinning llama.cpp to:"
if [[ -n "${TAG_NAME}" ]]; then
  echo "  tag:    ${TAG_NAME}"
fi
echo "  commit: ${TARGET_COMMIT}"
echo "  title:  ${TARGET_SUBJECT}"

git -C "${LLAMA_DIR}" checkout "${TARGET_COMMIT}"

cat <<EOF

llama.cpp has been pinned in the local checkout.

Next steps:
  1. Update docs/README pin metadata if needed.
  2. Review the diff under third_party/llama.cpp.
  3. Commit the superproject pointer change:
       git add third_party/llama.cpp
       git commit -m "Pin llama.cpp to ${TAG_NAME:-$TARGET_COMMIT}"
  4. Rebuild and verify:
       ./gradlew :runtime:llama:jvmProcessResources
       ./gradlew :data:compileKotlinJvm :app:desktop:compileKotlin
       ./gradlew :runtime:llama:linkDebugFrameworkIosSimulatorArm64
       ./gradlew :runtime:llama:linkDebugFrameworkIosArm64
       ./gradlew :app:android:assembleDebug

Optional smoke test:
       ./gradlew :runtime:llama:jvmTest -DbreezeSmokeGgufPath="/absolute/path/to/model.gguf"
EOF
