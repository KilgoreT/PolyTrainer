# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PolyTrainer — Android-приложение для изучения слов (словарь + квизы). Kotlin, Jetpack Compose, многомодульная архитектура.

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug

# Lint
./gradlew :app:lintDebug

# All unit tests
./gradlew testDebugUnitTest

# Tests for specific module
./gradlew :modules:screen:quiztab:testDebugUnitTest

# Single test class
./gradlew :modules:screen:quiztab:testDebugUnitTest --tests "*.QuizTabReducerTest"
```

CI pipeline (GitHub Actions): Lint → Unit Tests → Build APK. Triggers on branches `IS*` and `MT*`.

## Architecture: TEA (The Elm Architecture) via Mate framework

Custom TEA implementation in `modules/core/mate/`. Every feature screen follows this loop:

```
UI → Message → Reducer → (new State, Set<Effect>)
                                       ↓
                               EffectHandler → Message → back to Reducer
```

### Key types per feature module

| File | Role |
|------|------|
| `State.kt` | Immutable data class + pure extension functions for state transforms |
| `Message.kt` | Sealed interface: `UiMsg` (user actions), `DatasourceMsg` (effect results) |
| `Reducer.kt` | Pure function `(State, Msg) → ReducerResult<State, Effect>` |
| `*EffectHandler.kt` | Executes side effects, emits result Messages |
| `ViewModel.kt` | Wraps `Mate` instance, exposes `state: StateFlow` and `accept(Msg)` |

### Reducer conventions

- `ReducerResult` = `Pair<State, Set<Effect>>`. Helpers: `.state()` (no effects), `.effects()`.
- Reducer is pure — no side effects, no coroutines.
- State updates via copy + extension functions (e.g., `state.loadLang()`, `state.selectLang()`).
- Lifecycle events handled via `UiMsg.LifeCycleEvent`.

### Testing TEA

- `MateTestHelper` — helper in `core/mate` for reducer tests.
- Extensions: `testReduce()`, `testScenario()`, `assertNoEffects()`, `assertSingleEffect<T>()`.
- Reference example: `MateReducerTestExample.kt` in `core/mate`.

## Module Structure

```
modules/
├── core/          — mate (TEA framework), theme, ui, tools
├── screen/        — feature screens (each with own TEA loop)
├── widget/        — reusable UI components (some with own Mate)
├── datasource/    — DataStore prefs
└── library/       — flags (country icons)
```

Feature modules depend on core modules. Core modules never depend on features. Splash screen uses simple ViewModel without TEA (lightweight exception).

## Dependency Management

Version catalogs in `deps/*.versions.toml` (not default `gradle/`):
- `project.versions.toml` — AGP, Kotlin, KSP
- `compose.versions.toml` — Compose, Navigation
- `datastore.versions.toml` — Room, DataStore, Paging
- `di.versions.toml` — Dagger 2

DI: Dagger 2 with KSP. ViewModels use factory pattern.

## Code Standards

- **Max line length:** 120 characters.
- **Language:** Code, comments, docs — English. Chat responses — Russian.
- **Extension test files:** `*ExtTest.kt` / `*ExtTests.kt` in `ext/` folder.
- Extension tests must verify immutability of all untouched state fields.
- Test ordering: Boundary → Standard → Edge cases. Numbered in class doc comment.
- Test names: `should [expected] when [condition]`.
- Assertion messages must include expected vs actual values, under 80 chars.

## TEA Feature File Layout

```
feature/
├── ui/
│   ├── ViewModel.kt
│   └── Screen.kt
├── logic/
│   ├── Reducer.kt
│   ├── State.kt
│   ├── Message.kt
│   └── EffectHandlers.kt
└── deps/
    └── UseCase.kt
```
