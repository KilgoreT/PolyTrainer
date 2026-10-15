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

## Флоу фичи

Новая фича, подфича, «флоу» → читать `docs/handbook/guides/feature-flow.md` и идти по нему: бриф и `decisions.md` → разрез и фикстуры из прода → продуктовая спека в бэкенде (`backend/`) и экранная в приложении → план → TDD и покрытие по стороне → ручники на девайсе → коммит по решению юзера → PR и релиз.

Этапы ведёт главная сессия сама. Суб-агенты — только на ревью спеки и плана; промпты в `backend/agents/prompts`, правки промптов через `backend/agents/LEDGER.md`. Все доки на русском, код и идентификаторы на английском. Доки отдельно не коммитятся — только вместе с кодом.

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
**Куда коммитить?**
1. В ветку фичи, вторым коммитом.
2. В отдельную ветку.
3. Поправить, напиши что.
```

Рекомендуемый вариант — первым; вопрос — последним в сообщении, один за раз.
