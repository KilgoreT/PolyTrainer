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

## ForgeFlow Framework

При "флоу", "запусти flow", "ебашь" → запусти conductor'а как суб-агента (Agent tool). В промпте conductor'а ОБЯЗАТЕЛЬНО укажи пошагово:

1. Прочитай `docs/forgeflow/BOOTSTRAP.md` — выполни шаги СТРОГО по порядку
2. BOOTSTRAP укажет что читать дальше (conductor.md, runner.md, dsl.md, flow, модули)

НИКОГДА не выполняй flow сам — ТОЛЬКО через conductor-агента.
НИКОГДА не генерируй plan/log сам — conductor делает это по своим правилам.
Все артефакты flow — на РУССКОМ языке. Код и идентификаторы на английском.

При работе внутри flow:
- НИКОГДА не обновляй plan.yml несколькими Edit — только один write
- НИКОГДА не выполняй содержание шага сам — делегируй суб-агенту
- Паузы обязательны в соответствии с mode и pause на шаге
- Время в plan — РЕАЛЬНОЕ системное. Получать через `date` в Bash. НИКОГДА не подставлять вручную

**ЖЕЛЕЗНОЕ ПРАВИЛО ПАУЗЫ:** Когда шаг flow имеет `pause: true` и mode = `normal` — после завершения шага показать результат и ЖДАТЬ. НЕ запускать следующий шаг. НЕ писать "запускаю". Только показать что сделано и замолчать. Продолжать ТОЛЬКО после явного подтверждения пользователя.
