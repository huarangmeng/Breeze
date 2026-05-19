# Learnings

Corrections, insights, and knowledge gaps captured during development.

**Categories**: correction | insight | knowledge_gap | best_practice

---

## [LRN-20260519-001] correction

**Logged**: 2026-05-19T12:58:41+00:00
**Priority**: high
**Status**: resolved
**Area**: frontend

### Summary
KMP 中超过 5 路的 Flow combine 不能直接写成多参数 lambda，否则可能命中 Array<Any?> 重载导致 Android 编译失败

### Details
这次修改 `ApiConfigViewModel` 时，把多个 StateFlow 直接通过单个 `combine(...)` 合并，并写成 7 个参数的 lambda。`compileKotlinMetadata` 通过了，但 `:app:android:compileDebugKotlin` 失败，报错显示命中了 `suspend (Array<Any?>) -> R` 的重载，后续所有参数类型推断都失效。

### Suggested Action
在 KMP 共享层里，多个 Flow 的组合优先拆成两段，或先合成中间 data class，再继续 combine，避免依赖不稳定的重载推断。

### Metadata
- Source: user_feedback
- Related Files: app/shared/src/commonMain/kotlin/com/hrm/breeze/ui/screens/apiconfig/ApiConfigViewModel.kt
- Tags: kotlin, flow, combine, kmp, compile

### Resolution
- **Resolved**: 2026-05-19T12:58:41+00:00
- **Notes**: 将表单状态先合成为 `DraftFormState`，再与状态消息继续 `combine`；Android 编译已通过。

---
