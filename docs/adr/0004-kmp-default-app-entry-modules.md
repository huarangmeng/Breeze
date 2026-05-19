# ADR 0004: Adopt KMP App Entry Modules

## Context

JetBrains updated the default Kotlin Multiplatform project structure in May 2026. The new default separates shared KMP library code from runnable app entry points. This also matches the AGP 9 direction: Android application entry points should live in an Android app module, not inside the shared KMP library module.

Before this ADR, Breeze had already extracted an Android app entry module, but the old `:composeApp` module still carried three responsibilities:

- shared Compose UI and presentation code
- Desktop JVM executable and packaging
- Web JS / Wasm executable entry points and resources

## Decision

Rename the old shared `:composeApp` module to `:app:shared`, then extract the remaining runnable entry points under `app/`:

- `:app:android` depends on `:app:shared`
- `:app:desktop` owns the Compose Desktop `main()` and native distribution configuration
- `:app:web` owns JS / Wasm executable targets, `index.html`, styles, and SQLite worker resources
- `:app:ios` continues to consume the static framework from `:app:shared`; the framework binary name remains `ComposeApp` to avoid Swift-side import churn

The `:app:shared` module remains the shared Compose Multiplatform library for root `App()`, navigation, DI, ViewModels, Routes, and screens.

## Consequences

Positive:

- App entry points now have single-purpose Gradle modules.
- `:app:shared` no longer contains desktop packaging or Web executable configuration.
- The module graph matches the current KMP default structure and leaves a clearer path for future feature modularization.

Tradeoffs:

- Existing run configurations must point to `:app:desktop` and `:app:web`.
- Docs and scripts that referenced `:composeApp` need to move to `:app:shared`, `:app:desktop`, or `:app:web` depending on intent.
