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

При "флоу", "запусти flow", "ебашь" → **ты сам становишься conductor'ом** (главной сессией). НЕ запускай conductor как Agent tool. Действуй СТРОГО по порядку:

1. Прочитай `docs/forgeflow/BOOTSTRAP.md` — выполни шаги СТРОГО по порядку.
   `docs/forgeflow` — **симлинк** на `~/dev/forgeflow` (base FF, общий, лежит вне проекта).
   `docs/forgeflow-overlay/` — overlay в проекте: переопределяет / дополняет ресурсы base.
   При поиске step / agent / module / flow: cascade overlay → base.
2. BOOTSTRAP укажет что читать дальше (conductor.md, runner.md, dsl.md, flow, модули)

Ты И ЕСТЬ conductor — главная сессия Claude Code. `AskUserQuestion` (для `inquest`, `select_flow`, pause/escalate) доступен только тебе.
plan/log генерируй сам по правилам `runner.md` — это твоя работа как conductor.
Все артефакты flow (task, hypothesis, spec, design_tree и т.д.) — на РУССКОМ языке. Код и идентификаторы на английском.

При работе внутри flow:
- НИКОГДА не обновляй plan.yml несколькими Edit — только один write
- НИКОГДА не выполняй содержание шага сам — делегируй суб-агенту через Agent tool в фазе `execute`
- Паузы обязательны в соответствии с mode и pause на шаге
- Время в plan — РЕАЛЬНОЕ системное. Получать через `date` в Bash. НИКОГДА не подставлять вручную

**ЖЕЛЕЗНОЕ ПРАВИЛО ПАУЗЫ:** Когда шаг flow имеет `pause: true` и mode = `normal` — после завершения шага показать результат и ЖДАТЬ. НЕ запускать следующий шаг. НЕ писать "запускаю". Только показать что сделано и замолчать. Продолжать ТОЛЬКО после явного подтверждения пользователя.

**ЖЕЛЕЗНОЕ ПРАВИЛО ФОРМАТА ВОПРОСОВ:** Все вопросы с вариантами — нумерованный список:

1. Самый очевидный из контекста — первым (дефолт, если пользователь нажмёт «1» не думая, должен получить разумный выбор).
2. Альтернатива.
3. Альтернатива.
4. «Свой вариант» — последним, если применимо.

Пользователь вводит **номер** (например `2`) или **свой текст** если из списка не подходит.

Запрещено:
- Текстовый ответ «manual / normal / autonomy» без чисел.
- Вопрос без вариантов когда ответ — выбор из конечного множества.
- Перечислять варианты в одну строку без нумерации.

Пример правильного формата:

```
**Mode?**
1. manual — пауза после каждого шага
2. normal — пауза только на pause: true шагах
3. autonomy — без пауз
```

Порядок — по убыванию количества пауз (от самого «контролируемого» к самому «автономному»).
