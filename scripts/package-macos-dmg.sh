#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_KIND="${1:-release}"

case "${BUILD_KIND}" in
  release)
    GRADLE_TASK=":app:desktop:packageReleaseDmg"
    ;;
  debug|dev)
    GRADLE_TASK=":app:desktop:packageDmg"
    ;;
  *)
    echo "Unsupported build kind: ${BUILD_KIND}" >&2
    echo "Usage: $(basename "$0") [release|debug]" >&2
    exit 1
    ;;
esac

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "macOS DMG packaging must run on a macOS host." >&2
  exit 1
fi

# Compose Desktop 的 packageDmg/checkRuntime 依赖 jpackage。
# Android Studio 自带的 JBR 不包含 jpackage，需要切到带 jpackage 的 JDK（>=17）。
# 注意：Gradle 会读取 .gradle/config.properties 里的 java.home（IDE 固化的 JBR 路径），
#       仅设置 JAVA_HOME 不够，必须显式传 -Dorg.gradle.java.home。
PACKAGING_JDK=""
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/jpackage" ]]; then
  PACKAGING_JDK="${JAVA_HOME}"
elif CANDIDATE="$(/usr/libexec/java_home -v 21 2>/dev/null)" && [[ -x "${CANDIDATE}/bin/jpackage" ]]; then
  PACKAGING_JDK="${CANDIDATE}"
elif CANDIDATE="$(/usr/libexec/java_home -v 17+ 2>/dev/null)" && [[ -x "${CANDIDATE}/bin/jpackage" ]]; then
  PACKAGING_JDK="${CANDIDATE}"
else
  echo "No JDK with 'jpackage' found. Install a full JDK (>=17) such as Temurin/Oracle JDK 21." >&2
  exit 1
fi

export JAVA_HOME="${PACKAGING_JDK}"
echo "Using JDK with jpackage: ${PACKAGING_JDK}"

echo "Packaging Breeze DMG with task ${GRADLE_TASK}"
# --no-configuration-cache 避免复用之前用 JBR 计算出的缓存条目。
# JAVA_HOME 会被 app/desktop/build.gradle.kts 读取，强制把
# Compose Desktop 的 javaHome 指向带 jpackage 的 JDK。
(cd "${REPO_ROOT}" && ./gradlew \
  --no-configuration-cache \
  "${GRADLE_TASK}")

ARTIFACT_ROOT="${REPO_ROOT}/app/desktop/build/compose/binaries"
echo
echo "Build finished. DMG artifacts:"
find "${ARTIFACT_ROOT}" -type f -name "*.dmg" -print | sort
