# ADR 0004: Adopt KMP App Entry Modules

## Context

JetBrains updated the default Kotlin Multiplatform project structure in May 2026. The new default separates shared KMP library code from runnable app entry points. This also matches the AGP 9 direction: Android application entry points should live in an Android app module, not inside the shared KMP library module.

Before this ADR, Breeze had already extracted `:androidApp`, but the old `:composeApp` module still carried three responsibilities:

- shared Compose UI and presentation code
- Desktop JVM executable and packaging
- Web JS / Wasm executable entry points and resources

## Decision

Rename the old shared `:composeApp` module to `:shared`, then extract the remaining runnable entry points:

- `:androidApp` depends on `:shared`
- `:desktopApp` owns the Compose Desktop `main()` and native distribution configuration
- `:webApp` owns JS / Wasm executable targets, `index.html`, styles, and SQLite worker resources
- `:iosApp` continues to consume the static framework from `:shared`; the framework binary name remains `ComposeApp` to avoid Swift-side import churn

The `:shared` module remains the shared Compose Multiplatform library for root `App()`, navigation, DI, ViewModels, Routes, and screens.

## Consequences

Positive:

- App entry points now have single-purpose Gradle modules.
- `:shared` no longer contains desktop packaging or Web executable configuration.
- The module graph matches the current KMP default structure and leaves a clearer path for future feature modularization.

Tradeoffs:

- Existing run configurations must point to `:desktopApp` and `:webApp`.
- Docs and scripts that referenced `:composeApp` need to move to `:shared`, `:desktopApp`, or `:webApp` depending on intent.
