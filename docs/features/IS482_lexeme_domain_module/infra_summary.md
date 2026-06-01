---
status: done
---

# Summary — infra (IS482)

## Что сделано

Sub-flow `infra` для IS482 реализовал инфраструктуру под новый pure-Kotlin модуль `:modules:domain:lexeme` и зарегистрировал его во всех консьюмерах. Содержимое доменных типов (`Lexeme`, `LexemeId`, `Translation`, `Definition`, mapper) сюда не входило — это business-слой.

**Создано:**
- `modules/domain/lexeme/build.gradle.kts` — 13 строк, 1-в-1 копия `modules/core/logger/build.gradle.kts`: plugin `org.jetbrains.kotlin.jvm`, `java { sourceCompatibility/targetCompatibility = VERSION_17 }`, `kotlin { jvmToolchain(17) }`. Без `android { }`, без `dependencies { }`, без `namespace`/proguard/manifest — pure-Kotlin domain не зависит ни от чего проектного.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` — placeholder (package declaration + одна строка комментария). Фиксирует package `me.apomazkin.lexeme` (соответствует `docs/guides/naming.md`) и облегчает IDE-индексацию; business sub-flow перепишет.

**Изменено (5 файлов, по одной строке каждый):**
- `settings.gradle.kts` — добавлен блок `//Domain` с `include(":modules:domain:lexeme")` между `//Libraries` и `//Old`. Стиль комментария-разделителя симметричен существующим (`//Core`, `//Features`, `//Widget`, `//Libraries`, `//Old`).
- `app/build.gradle.kts` — `implementation(project("path" to ":modules:domain:lexeme"))` между `:modules:datasource:prefs` и `:core:core-resources`. Прямая dep (не через api-экспозицию feature) — нужна для общего mapper'а `LexemeApiEntity.toDomainEntity()` и трёх `UseCaseImpl` в `app/`.
- `modules/screen/wordcard/build.gradle.kts` — `implementation(...)` после `:modules:core:logger`.
- `modules/screen/quiz/chat/build.gradle.kts` — `implementation(...)` после `:modules:datasource:prefs`.
- `modules/screen/dictionaryTab/build.gradle.kts` — `implementation(...)` после `:core:core-resources`. TODO-комментарии (про «избавиться от сущностей ui») оставлены как есть — частично адресуются IS482, но `LexemeUiItem` остаётся, второй TODO — out of scope.

**Конвенции применённые:**
- Map-style синтаксис `project("path" to ":...")` для всех проектных deps (проектная конвенция).
- `implementation` (не `api`) во всех консьюмерах — изоляция домена не требует транзитивной экспозиции.
- Mapper API↔domain — в `app/`, не в domain (`docs/guides/data-layer.md`).

**Проверки прошли:**
- `./gradlew :modules:domain:lexeme:dependencies --console=plain` → BUILD SUCCESSFUL. Граф нового модуля содержит только `kotlin-stdlib:2.2.0 → annotations:13.0`, проектных deps нет.
- `./gradlew :modules:screen:wordcard:assembleDebug --console=plain` → BUILD SUCCESSFUL (exit 0). Compile errors отсутствуют; pre-existing предупреждения `@param`-targeting не связаны с IS482.

## Ключевые решения

- **Pure-Kotlin domain** (`org.jetbrains.kotlin.jvm`, без `com.android.library` / `kotlin.android` / Compose / ksp). Domain не зависит от Android SDK и DI-генерации.
- **Mapper в `app/`** (не в domain). Соответствует `docs/guides/data-layer.md` ("API → Domain в UseCase модуле") и сохраняет domain без зависимостей.
- **Map-style `project("path" to ":...")`** — проектная конвенция для всех existing deps.
- **CI workflow не трогаем** — вне scope IS482. `testDebugUnitTest` не покрывает `kotlin.jvm` (есть только `test`); если business sub-flow добавит юнит-тесты в `modules/domain/lexeme/src/test/` — follow-up задача расширит `on_feature_push.yml` на `./gradlew test testDebugUnitTest`.
- **Placeholder Lexeme.kt создан** — несмотря на то, что `kotlin.jvm` не падает без source-файлов. Причина: IDE-индексация package и Gradle source dir, business sub-flow перепишет.
- **Версия `kotlin.jvm` 1.9.10 vs `kotlin.android` 2.0.20** — не выравниваем, следуем logger-прецеденту. Если в business sub-flow возникнут проблемы binary compat — финализация поднимет версию.

## Артефакты

- `docs/features/IS482_lexeme_domain_module/infra_design_tree.md` — DAG из 6 узлов (1 создание + 5 правок), обоснование зависимостей, явные исключения (CI, proguard, manifest, DI, source-файлы).
- `docs/features/IS482_lexeme_domain_module/infra_implement.md` — описание имплементации по узлам, валидация (gradle sync + feature build, оба BUILD SUCCESSFUL), нетривиальные решения, нулевые отклонения от design_tree.

## Что НЕ делалось

- **Конкретные `.kt` типы** (`Lexeme`, `LexemeId`, `Translation`, `Definition`, mapper `LexemeApiEntity.toDomainEntity()`) — business sub-flow.
- **`LexemeUiItem` UI-слой** — остаётся в `dictionaryTab` без изменений (UI-обёртки — отдельная задача, второй TODO в `dictionaryTab/build.gradle.kts`).
- **БД / data слой** — не задеты (scope изоляции domain от data сохранён).
- **DI-файлы** (`app/.../di/module/...`) — не входят в infra; domain без Dagger-аннотаций.
- **CI workflow** (`.github/workflows/on_feature_push.yml`) — вне scope (см. ключевые решения).
- **Удаление локальных `wordcard.entity.Lexeme` / `quiz.chat.entity.Lexeme`** — это business-слой (`.kt`-файлы), не infra.
- **Тесты для infra-слоя** — не применимы (Gradle wiring не unit-тестируется); тесты mapper будут в business_test.

_model: Opus 4.7 (1M context)_
