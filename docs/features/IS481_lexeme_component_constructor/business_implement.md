# Business implement: IS481 Lexeme Component Constructor

## Summary

Реализован business слой IS481 согласно `business_design_tree.md` (26 узлов).
Все unit-тесты на изменённых модулях прошли (домен/lexeme, wordcard, quiz/chat,
app). Data layer (DAO/Migration) — out of scope, временные stub'ы в
`CoreDbApiImpl.LexemeApiImpl` через legacy `lexemes.translation`/`lexemes.definition`
колонки + synthetic component types/values для совместимости с новой shape API.

## Изменённые / созданные файлы

### `modules/domain/lexeme` (Layer 0 — domain types)

**Создано:**
- `BuiltInComponent.kt` (enum только `TRANSLATION` — AGG-1)
- `ComponentTemplate.kt` (enum TEXT/LONG_TEXT/IMAGE)
- `ComponentValueData.kt` (sealed: TextValue/LongTextValue/ImageValue)
- `ComponentType.kt` (data + `ComponentTypeId` value class)
- `ComponentValue.kt` (data + `ComponentValueId` value class)
- `ComponentTypeRef.kt` (sealed BuiltIn/UserDefined + AGG-10 TODO)
- `QuizConfig.kt` (data + AGG-10 TODO)
- `LexemeBuiltInExt.kt` (extension `Lexeme.builtIn(BuiltInComponent)`)

**Модифицировано:**
- `Lexeme.kt` — добавлен `components: List<ComponentValue>`, поля `translation` / `definition` помечены `@Deprecated` (B4/C2 shim). Value-классы `Translation`/`Definition` тоже `@Deprecated`.
- `build.gradle.kts` — добавлен `testImplementation("junit:junit:4.13.2")`

**Тесты:** `LexemeBuiltInExtTest.kt` (5 кейсов, Gap-7).

### Gradle deps (Layer 1 — MIN-2)

- `core/core-db-api/build.gradle.kts` — `api(project(":modules:domain:lexeme"))` для транзитивной видимости domain типов downstream-модулям.

### `core/core-db-api` (Layer 2 — API DTOs)

**Создано:**
- `ComponentTypeApiEntity.kt`
- `ComponentValueApiEntity.kt`
- `QuizConfigApiEntity.kt`

**Модифицировано:**
- `LexemeApiEntity.kt` — удалены поля `translation` / `definition`, добавлено `components: List<ComponentValueApiEntity>`. Value-классы `TranslationApiEntity`/`DefinitionApiEntity` оставлены для @Deprecated shim'ов. Удалены extension `canRemoveTranslation` / `canRemoveDefinition`.
- `CoreDbApi.kt::LexemeApi` interface полностью переработан (AGG-6):
  - удалены: `addLexeme(wordId, translation)`, `addLexeme(wordId, definition)`, `addLexemeWithDefinition`, `updateLexemeDefinition`
  - добавлены generic: `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `addLexemeWithComponents`, `addComponentValue`, `updateComponentValue`, `deleteComponentValue`, `getComponentTypes`, `getQuizConfig`
  - shim: `addLexemeWithTranslation` / `updateLexemeTranslation` @Deprecated (A3)

### `core/core-db-impl` (compile stub for data sub-flow)

- `CoreDbApiImpl.kt::LexemeApiImpl` — переписан под новый interface. Generic методы реализованы через legacy translation/definition колонки + synthetic component types/values (id < 0). `getComponentTypes` возвращает synthetic `[TRANSLATION, Definition]` для совместимости с UI (hasDefinitionComponent = true). `getQuizConfig` возвращает дефолтный `[BuiltIn(TRANSLATION)]`. Data sub-flow заменит на честные DAO calls.
- `LexemeDbEntity.kt::toApiEntity()` — переписан на конструирование synthetic `ComponentValueApiEntity` из legacy translation/definition колонок.

### `modules/screen/wordcard` (Layer 3-7)

**Модифицировано:**
- `entity/Term.kt` — добавлен `dictionaryId: Long` (F1 fix).
- `deps/WordCardUseCase.kt` — interface полностью переработан (AGG-6):
  - удалены: `addLexemeDefinition`, `deleteLexemeDefinition`, `RemoveDefinitionResult`
  - добавлены generic: `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `addComponentValue`, `updateComponentValue`, `deleteComponentValue`, `deleteDefinitionComponent` (shim для handler), `getComponentTypes`
  - новый sealed `RemoveComponentResult` (ComponentRemoved / LexemeCascadeRemoved)
  - shim: `addLexemeTranslation`, `deleteLexemeTranslation`, `RemoveTranslationResult` сохранены
- `mate/State.kt` — добавлен `hasDefinitionComponent: Boolean = false` (AGG-6 UI блок).
- `mate/Message.kt` — `Msg.WordLoaded` расширен `componentTypes: List<ComponentType>` (F1 fix).
- `mate/WordCardReducer.kt` — branch `Msg.WordLoaded` теперь вычисляет `hasDefinitionComponent` из `message.componentTypes`.
- `mate/DatasourceEffectHandler.kt`:
  - `LoadWord` → sequential prefetch `getTermById` → `getComponentTypes(term.dictionaryId)` → `Msg.WordLoaded(term, types)` (F1 fix)
  - `UpdateLexemeDefinition` → reroute на `addLexemeWithUserDefinedComponent("Definition", ...)` (AGG-6)
  - `RemoveDefinition` → reroute на `deleteDefinitionComponent(lexemeId)` с маппингом `RemoveComponentResult` → existing Msg

**Тесты переписаны:**
- `WordLoadedTest.kt` — добавлены кейсы для `hasDefinitionComponent` (4 новых: empty / only TRANSLATION / TRANSLATION+Definition / Other user-defined)
- `DatasourceEffectHandlerTest.kt` — FakeUseCase обновлён под новый interface; добавлены кейсы LoadWord sequential prefetch, UpdateLexemeDefinition reroute, RemoveDefinition reroute (ComponentRemoved / LexemeCascadeRemoved / null)
- `LexemeManagementTest.kt` / `WordEditTest.kt` — миграция Term/Lexeme конструкторов под новый shape

### `modules/screen/quiz/chat` (Layer 8)

**Модифицировано:**
- `deps/QuizChatUseCase.kt` — добавлен `getQuizConfig(dictionaryId, quizMode)`.
- `quiz/QuizGameImpl.kt`:
  - `fetchData` pre-fetch `QuizConfig` через `quizChatUseCase.getQuizConfig` (AGG-5, F5 no N+1)
  - `WriteQuiz.toQuizItem(componentRefs, ...)` переписан: резолв `componentRefs` в `lexeme.components`, graceful skip `null` (F2) вместо удалённого `throw IllegalArgumentException`. Сохраняет order priority (F4).
  - `.map` → `.mapNotNull` в `fetchData`

**Тесты:**
- `QuizGameImplTest.kt` (новый, 8 кейсов): translation/definition matched, graceful skip null, empty refs/components, F4 order priority
- `QuizGameImplEmptyListTest.kt` — добавлен mock для `getQuizConfig`

### `app/`

**Модифицировано:**
- `mapper/LexemeMapper.kt` (AGG-2) — все API → Domain мапперы IS481:
  - `ComponentTypeApiEntity.toDomain()`, `ComponentValueApiEntity.toDomain()`, `QuizConfigApiEntity.toDomain()`
  - `LexemeApiEntity.toDomain()` со shim translation/definition через built-in lookup + debug-only consistency invariant (B7)
- `di/module/wordCard/WordCardUseCaseImpl.kt` — переписан под новый interface:
  - Translation shim wrapper'ы делегируют на generic
  - `restoreLexeme` impl на atomic compound `addLexemeWithComponents` (MIN-9)
  - `deleteDefinitionComponent` — lookup внутри impl, delete + cascade decision
  - `TermApiEntity.toDomainEntity()` пробрасывает `word.dictionaryId` → `Term.dictionaryId` (F1 fix)
- `di/module/quizchat/QuizChatUseCaseImpl.kt` — добавлены инжекты `lexemeApi`, `logger`. Override `getQuizConfig` делегирует на `lexemeApi.getQuizConfig(...).toDomain()`.

**Тесты:**
- `LexemeMapperTest.kt` — переписан под новый shape API (10 кейсов B7/B8/C11/C12/C13)
- `WordCardUseCaseImplTest.kt` (новый, 13 кейсов): addLexemeWithBuiltIn (happy/exception), addLexemeWithUserDefined (lookup miss / happy), addComponentValue (happy/exception), getComponentTypes (empty/full/exception), translation shim delegation, deleteDefinitionComponent (no-def/ComponentRemoved/LexemeCascadeRemoved)
- `QuizChatUseCaseImplTest.kt` — добавлены 3 кейса getQuizConfig (happy/null/exception); обновлён конструктор useCase под новые deps

## Test results

| Module | Suites | Tests | Status |
|---|---|---|---|
| `:modules:domain:lexeme` | 1 (LexemeBuiltInExtTest) | 5 | PASS |
| `:modules:screen:wordcard` | 14 (incl. DatasourceEffectHandlerTest, WordLoadedTest, …) | ~110 | PASS |
| `:modules:screen:quiz:chat` | 3 (QuizGameImplTest + EmptyListTest + ExampleUnitTest) | 12 | PASS |
| `:app` | 7 (incl. LexemeMapperTest, WordCardUseCaseImplTest, QuizChatUseCaseImplTest) | 59 | PASS |

Все test suite показывают `failures="0" errors="0"`.

## Известные ограничения (для data sub-flow)

1. **LexemeApiImpl** — generic методы реализованы через legacy `lexemes.translation` / `.definition` колонки. Не настоящая `component_values` таблица. Data sub-flow заменит после миграции M11→M12.
2. **`addComponentValue` для arbitrary type** — поддерживает только synthetic id `-1L` (TRANSLATION) и `-2L` (Definition). Прочие throw `NotImplementedError`.
3. **`getComponentTypes`** — возвращает synthetic `[TRANSLATION, Definition]` для каждого словаря. После миграции — DAO query.
4. **`getQuizConfig`** — возвращает дефолтный `[BuiltIn(TRANSLATION)]`. После миграции — JSON parse из `quiz_configs` таблицы.
5. **`addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` для existing lexemeId** — путь требует `getComponentTypes` lookup, который вернёт synthetic — UI flow работает на synthetic id'ах.

Это **временный bridge**: production runtime UI компилируется и не падает; только реальное хранение компонентов произвольных типов out-of-scope business sub-flow.

## iter 2 fixes

Применены critical-fixes по итогам review iter 1.

### F1 — `updateComponentValue` мёртвый код `null/null`

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`

Было: обе ветки `if/else` возвращали `null` (tautology). Стало: явный TODO с пометкой «requires DAO-level `getLexemeIdByComponentValueId(componentValueId)` lookup until data sub-flow». Различены success (`updated > 0`) vs failure (`updated <= 0`) — оба пока возвращают `null`, но семантика и блок-комментарий о причине ясны. Generic path не вызывается в IS481 (callers идут через lexemeId-based методы).

### F2 — `LexemeMapper.toDomain` debug consistency check — tautology

Файл: `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt`

Применён вариант (a): удалён весь debug-check блок. `expectedT`/`expectedD` вычислялись тем же `toTranslationShim()`/`toDefinitionShim()` что и сам shim — проверка `check(translationShim?.value == expectedT?.value)` была тривиально-всегда-true. Удалён unused `BuildConfig` import. Сохранён комментарий в KDoc что invariant закрывается в mate refactor (B4/C2 backlog).

### F3 — `addLexemeWithBuiltInComponent` для existing lexemeId — missing UPDATE branch

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`

Добавлена симметричная с user-defined веткой логика: lookup existing component_value по `systemKey`, если есть — `updateComponentValue`, иначе `addComponentValue`. До iter 2 INSERT-only path упал бы на реальной DAO с UNIQUE(lexeme_id, type_id) violation (текущий stub `LexemeApiImpl` маппит synthetic `-1L` на `updateLexemeTranslation`, поэтому ошибка проявилась бы только после data sub-flow).

### F4 — `deleteComponentValue` returns null on success-with-remaining

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`

Без lexemeId невозможно собрать `ComponentRemoved(lexeme)` через `getLexemeById`. Сохранён `null` для non-last path + добавлен явный TODO о требовании DAO-level `getLexemeIdByComponentValueId(componentValueId)` lookup. `LexemeCascadeRemoved` для last-component path работает корректно. Generic метод не вызывается напрямую в IS481 — callers идут через `deleteDefinitionComponent(lexemeId)`. **Возражение iter 2:** «полноценный fix» по спецификации требует API extension в `core-db-api`/`core-db-impl` (новый метод `getLexemeIdByComponentValue`) — out of scope business sub-flow, отложено в data sub-flow.

### F5 — KDoc `resolveDictionaryIdForLexeme` не соответствует impl

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`

KDoc обещал per-lexeme resolution через `termApi.getTermById`, но impl просто `delegates to current dictionary`. Переписан KDoc: указано что текущая реализация — best-effort через current dictionary (корректна для word card сцены), и TODO о замене на DAO `getDictionaryIdForLexeme(lexemeId)` после data sub-flow.

### F6 — copy-paste delete translation / delete definition

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`

Выделен private generic helper `deleteLexemeComponentBy<R>(lexemeId, logLabel, predicate, onRemoved, onCascade)`. `deleteLexemeTranslation` и `deleteDefinitionComponent` теперь thin-wrappers, отличаются только predicate + result factory'ами. Удалён дубликат «lookup → delete → cascade decision» pattern (~30 строк).

## Test results — iter 2

| Module | Suites | Tests | Status |
|---|---|---|---|
| `:modules:domain:lexeme` | 1 (LexemeBuiltInExtTest) | 5 | PASS |
| `:modules:screen:wordcard` | 14 (incl. DatasourceEffectHandlerTest, WordLoadedTest, …) | 110 | PASS |
| `:modules:screen:quiz:chat` | 3 (QuizGameImplTest + EmptyListTest + ExampleUnitTest) | 12 | PASS |
| `:app` | 7 (incl. LexemeMapperTest, WordCardUseCaseImplTest, QuizChatUseCaseImplTest) | 59 | PASS |

Все test suites показывают `failures="0" errors="0"`. Существующие тесты `deleteLexemeTranslation` / `deleteDefinitionComponent` прошли после refactor на helper — поведение сохранено byte-for-byte.

## log_messages

- Реализованы 26 узлов design tree: 9 новых domain types + 1 gradle edge + 3 новых API DTO + 11 modified files + 2 test stub files. Все тесты PASS на 4 модулях.
- AGG-1/2/5/6/10/12, MIN-2/9, F1 fix, A3 shim применены. `core-db-api` использует `api(project(...))` для транзитивной видимости domain.
- Data layer stub в `LexemeApiImpl` через synthetic component ids (-1L TRANSLATION, -2L Definition) + legacy translation/definition колонки. Data sub-flow заменит на честные DAO calls после миграции M11→M12.

_model: claude-opus-4-7[1m]_
