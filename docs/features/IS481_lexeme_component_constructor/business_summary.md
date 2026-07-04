---
status: done
---

# Summary — business

## Что сделано

Реализован business слой IS481 — переход с translation/definition-specific API к generic
component-API + transitional shim для совместимости с не-тронутыми mate/UI/dictionaryTab.
26 узлов design tree, 4 модуля изменены, все unit-тесты PASS.

### Domain types — `modules/domain/lexeme/` (Layer 0, pure-JVM)

Создано 8 новых файлов:
- `BuiltInComponent.kt` — `enum class BuiltInComponent(val key: String) { TRANSLATION("translation") }` + `fromKey()` (AGG-1: definition мигрирует в user-defined).
- `ComponentTemplate.kt` — `enum { TEXT, LONG_TEXT, IMAGE }` + `fromKey()` с fallback на TEXT (forward-compat).
- `ComponentValueData.kt` — `sealed interface { TextValue(text), LongTextValue(text), ImageValue(uri) }`.
- `ComponentType.kt` — `data class ComponentType(id, systemKey: BuiltInComponent?, dictionaryId: Long?, name: String?, template, position, removeDate)` + `@JvmInline value class ComponentTypeId(id: Long)`.
- `ComponentValue.kt` — `data class ComponentValue(id, lexemeId, type: ComponentType, data)` + `ComponentValueId`. Multi-level relation: type — full embedded.
- `ComponentTypeRef.kt` — `sealed interface { BuiltIn(key: BuiltInComponent), UserDefined(name: String) }` (AGG-10 TODO на вынос в `modules/domain/quiz`).
- `QuizConfig.kt` — `data class QuizConfig(dictionaryId, quizMode, componentRefs: List<ComponentTypeRef>)`.
- `LexemeBuiltInExt.kt` — `fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? = components.firstOrNull { it.type.systemKey == key }`.

Модифицировано: `Lexeme.kt` — добавлено `components: List<ComponentValue>` (new SoT, ordered by `ComponentType.position`); поля `translation: Translation?` / `definition: Definition?` помечены `@Deprecated` (B4/C2 shim); value-классы `Translation` / `Definition` тоже `@Deprecated`.

### API DTOs — `core/core-db-api/` (Layer 2)

Gradle dep edge MIN-2: `api(project(":modules:domain:lexeme"))` для транзитивной видимости domain типов downstream.

Создано: `ComponentTypeApiEntity.kt`, `ComponentValueApiEntity.kt` (type — full embedded для multi-level @Relation), `QuizConfigApiEntity.kt` (componentRefs хранит domain sealed).

Модифицировано:
- `LexemeApiEntity.kt` — удалены поля `translation` / `definition` + extensions `canRemoveTranslation` / `canRemoveDefinition`; добавлено `components: List<ComponentValueApiEntity>`. `TranslationApiEntity` / `DefinitionApiEntity` value-classes оставлены для `@Deprecated` overloads.
- `CoreDbApi.LexemeApi` interface (AGG-6) — удалены `addLexeme(wordId, translation)`, `addLexeme(wordId, definition)`, `addLexemeWithDefinition`, `updateLexemeDefinition`; добавлены generic: `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent` (lookup по `(dictionaryId, name, systemKey=NULL)`), `addLexemeWithComponents` (atomic compound INSERT для MIN-9), `addComponentValue`, `updateComponentValue`, `deleteComponentValue` (возвращает count remaining), `getComponentTypes(dictionaryId)`, `getQuizConfig(dictionaryId, quizMode)`. Shim `@Deprecated`: `addLexemeWithTranslation`, `updateLexemeTranslation` (A3).

### Wordcard UseCase + mate — `modules/screen/wordcard/`

- `entity/Term.kt` — добавлено `dictionaryId: Long` (F1 fix), источник `WordApiEntity.dictionaryId`.
- `deps/WordCardUseCase.kt` interface (AGG-6) — удалены `addLexemeDefinition`, `deleteLexemeDefinition`, `RemoveDefinitionResult`; добавлены generic + `RemoveComponentResult { ComponentRemoved(lexeme), LexemeCascadeRemoved }`; новый `getComponentTypes(dictionaryId): List<ComponentType>` (public — handler `LoadWord` вызывает); shim `addLexemeTranslation` / `deleteLexemeTranslation` + `RemoveTranslationResult` сохранены.
- `mate/State.kt` — добавлен `hasDefinitionComponent: Boolean = false` (explicit field, не computed; project memory feedback_explicit_state_flags).
- `mate/Message.kt` — `Msg.WordLoaded(word: Term, componentTypes: List<ComponentType>)` (F1 fix: transport types через Msg для reducer).
- `mate/WordCardReducer.kt` — branch `WordLoaded` вычисляет `hasDefinitionComponent = componentTypes.any { it.systemKey == null && it.name == "Definition" }`. Read-only-on-load.
- `mate/DatasourceEffectHandler.kt`:
  - `LoadWord` → sequential prefetch: `getTermById` → `getComponentTypes(term.dictionaryId)` → `Msg.WordLoaded(term, types)`.
  - `UpdateLexemeDefinition` → reroute на `addLexemeWithUserDefinedComponent(name="Definition", TextValue(...))`.
  - `RemoveDefinition` → reroute на `deleteDefinitionComponent(lexemeId)` shim в UseCase; маппинг `RemoveComponentResult` → existing `Msg.DefinitionDeleted` / `Msg.LexemeCascadeRemovedWithUndo`.

`DatasourceEffect` sealed sigs не менялись — все изменения в `onEffect` теле.

### Quiz session — `modules/screen/quiz/chat/`

- `deps/QuizChatUseCase.kt` — добавлен `suspend fun getQuizConfig(dictionaryId: Long, quizMode: String = "write"): QuizConfig?` (AGG-5, F2 fix: возвращает domain, не ApiEntity).
- `quiz/QuizGameImpl.kt`:
  - `fetchData` — pre-fetch `QuizConfig` ОДИН РАЗ на session (F5 no N+1), передача в `toQuizItem(componentRefs, ...)`.
  - `WriteQuiz.toQuizItem(componentRefs: List<ComponentTypeRef>, ...): QuizItem?` — резолв каждого ref в `lexeme.components` (BuiltIn по `systemKey`, UserDefined по `(systemKey == null, name == ref.name)`), graceful skip `null` если resolved пуст. Order priority по `componentRefs` (F4). Удалён `throw IllegalArgumentException("No translation or definition")`. `.map` → `.mapNotNull`.

### App layer — `app/`

- `mapper/LexemeMapper.kt` (AGG-2) — все API → Domain мапперы IS481 в одном файле: `ComponentTypeApiEntity.toDomain()`, `ComponentValueApiEntity.toDomain()`, `QuizConfigApiEntity.toDomain()`, `LexemeApiEntity.toDomain()` со shim translation (built-in `TRANSLATION` lookup) / definition (user-defined `systemKey=null, name="Definition"` lookup, AGG-1).
- `di/module/wordCard/WordCardUseCaseImpl.kt` — translation shim wrappers делегируют на generic; `restoreLexeme` impl переписан на atomic compound `addLexemeWithComponents` (MIN-9); `deleteDefinitionComponent` shim с lookup внутри impl; `TermApiEntity.toDomainEntity()` пробрасывает `word.dictionaryId`; helper `deleteLexemeComponentBy<R>(...)` устранил copy-paste translation/definition (iter 2 F6).
- `di/module/quizchat/QuizChatUseCaseImpl.kt` — добавлены инжекты `lexemeApi`, `logger`; override `getQuizConfig` → `lexemeApi.getQuizConfig(...).toDomain()` (try/catch + null + logger.e).

### Data layer stub (`core/core-db-impl/`)

`CoreDbApiImpl.LexemeApiImpl` переписан под новый interface через legacy `lexemes.translation` / `.definition` колонки + synthetic component types/values (id `-1L` TRANSLATION, `-2L` Definition). `getComponentTypes` возвращает synthetic `[TRANSLATION, Definition]`; `getQuizConfig` — дефолтный `[BuiltIn(TRANSLATION)]`. Это **временный bridge** — data sub-flow заменит на честные DAO calls после миграции M11→M12 (5 known limitations задокументированы в `business_implement.md`).

### Тесты

| Module | Suites | Tests | Status |
|---|---|---|---|
| `:modules:domain:lexeme` | 1 (LexemeBuiltInExtTest) | 5 | PASS |
| `:modules:screen:wordcard` | 14 | 110 | PASS |
| `:modules:screen:quiz:chat` | 3 | 12 | PASS |
| `:app` | 7 | 59 | PASS |

Покрытие: shim consistency invariant (B7/B8/C11/C12/C13), `LexemeBuiltInExt` (Gap-7), `WordCardUseCaseImpl` generic methods (13 кейсов incl. atomicity F013), `QuizChatUseCaseImpl.getQuizConfig`, `QuizGameImpl.toQuizItem` (8 кейсов incl. graceful skip + F4 order), `DatasourceEffectHandlerTest` (LoadWord sequential prefetch + reroute generic), `WordLoadedTest` (4 новых `hasDefinitionComponent` кейса).

### Log-точки

- `LogTags.WORDCARD` (existing) — все generic UseCase методы через `logger.e` on exception path (`WordCardUseCaseImpl`).
- `LogTags.QUIZ_CHAT` — добавлен в `QuizChatUseCaseImpl.getQuizConfig` для null-on-error.
- `LexemeLogger.w(... "no quiz config for dict $dictionaryId")` в `QuizGameImpl.fetchData` если `getQuizConfig` вернул null.

## Ключевые решения

- **AGG-1 — definition мигрирует в user-defined per-dictionary.** `BuiltInComponent` содержит только `TRANSLATION`. Definition lookup в маппере: `systemKey == null && name == "Definition"`. Снимает coupling «фиксированные 2 поля → схема» с domain.
- **AGG-2 — все API→Domain мапперы IS481 в `app/.../mapper/LexemeMapper.kt`.** Existing patterns в проекте раздваиваются (LexemeMapper.kt отдельным файлом vs inline в QuizChatUseCaseImpl); IS481 консолидирует свои в один файл.
- **AGG-5 — `QuizConfig` pre-fetch один раз на session.** F5 no N+1: `QuizGameImpl.fetchData` вызывает `getQuizConfig` один раз, передаёт `componentRefs` в `toQuizItem` для каждой лексемы.
- **AGG-6 — definition-specific API УДАЛЁН, translation-specific — `@Deprecated` shim.** Translation остаётся built-in (compat shim wraps generic); definition вызывается через generic `addLexemeWithUserDefinedComponent` / `deleteComponentValue` напрямую. Снимает мёртвый код.
- **AGG-10 — `QuizConfig` / `ComponentTypeRef` живут в `modules/domain/lexeme` с TODO.** Backlog «Quiz config UX» вынесет в `modules/domain/quiz` отдельно — out of scope IS481.
- **B4/C2 — `Lexeme.translation`/`.definition` остаются как `@Deprecated` nullable поля.** Не computed extensions. Хранятся как поля (заполняются маппером) для compat с `copy(translation = X)` в mate. Триггер удаления — backlog «Wordcard mate refactor».
- **MIN-9 — `restoreLexeme` impl atomic compound INSERT.** Через DAO default-method `WordDao.addLexemeWithComponents` (одна транзакция `lexemes + write_quiz + N component_values`). FK violation → rollback всей транзакции. Сигнатура UseCase сохранена (B4/C2). Compound DAO-метод — один callsite (только из `restoreLexeme`), наружу не выставлен.
- **F1 fix — public `getComponentTypes` UseCase.** Types нужны в reducer'е через `Msg`, не внутри UseCase impl; option «private helper внутри `getTermById`» отвергнута. `Msg.WordLoaded` расширен `componentTypes: List<ComponentType>`. Sequential prefetch в handler `LoadWord`.

### Вне scope (передано в data / ui sub-flow или backlog)

- **Data sub-flow**: миграция M11→M12 (создание таблиц `component_types`, `component_values`, `quiz_configs`); честные DAO calls вместо synthetic-id stub; `addDictionary` auto-INSERT default `quiz_configs` row (AGG-4); `getLexemeIdByComponentValueId` DAO lookup (для F4 — `ComponentRemoved` non-last path).
- **UI sub-flow**: chip «Определение» visibility через `state.hasDefinitionComponent` AND `canAddDefinition` в `AddLexemeMeaningRow` / `LexemeMeaningField`.
- **Backlog «Wordcard mate refactor»**: переход `LexemeState.translation/definition` → коллекция `components`; универсальный `ComponentChip`; удаление translation shim wrappers + `Translation`/`Definition` value-classes из domain.
- **Backlog «Quiz config UX»**: вынос `QuizConfig` / `ComponentTypeRef` в `modules/domain/quiz`; UI редактирования quiz config.

## Артефакты

- [`business_walkthrough.md`](business_walkthrough.md) — discovery текущего code state pre-IS481 (22 секции: CoreDbApi/Lexeme/Term/Msg/State/UI/quiz инвентаризация + вердикт «аналог не найден»).
- [`business_contract.md`](business_contract.md) — дельта-контракт (State / Msg / Effect / UseCase / Domain types). Approved iter 1.
- [`business_contract_review.md`](business_contract_review.md) — closure verification F1-F5 (все iter 1 findings закрыты). Verdict: approved.
- [`business_contract_spec.md`](business_contract_spec.md) — полная спека wordcard.md (569 строк): User Stories, State + invariants, UI Messages + reducer rules, IO Effects + handler table, UseCase interface + atomicity contracts, Mappers, Transitional API shim, тестовые сценарии. META `spec_filename: wordcard.md`.
- [`business_design_tree.md`](business_design_tree.md) — граф из 26 узлов (9 `[+]` domain types + 1 `[~]` gradle + 3 `[+]` API DTO + 11 `[~]` modifications + 2 stub). Все `[~]` verified через Read.
- [`business_test.md`](business_test.md) — 8 категорий test plan (Mapper, WordCardUseCaseImpl, Reducer, QuizChatUseCaseImpl, QuizGameImpl, LexemeBuiltInExt, Atomicity, DatasourceEffectHandler) + trace табл. Test gaps (B7/B8/C11/C12/C13/F013/F015/Gap-7/F1).
- [`business_implement.md`](business_implement.md) — отчёт реализации с iter 2 fixes (F1-F6 critical), test results 4 модуля PASS.
- [`publish_spec.md`](publish_spec.md) — отчёт публикации `docs/features-spec/wordcard.md` (overwrite, +78 строк); анализ iter 2 fixes (все impl-details, спека not affected).
- Опубликованная спека: [`docs/features-spec/wordcard.md`](../../features-spec/wordcard.md).

_model: claude-opus-4-7[1m]_
