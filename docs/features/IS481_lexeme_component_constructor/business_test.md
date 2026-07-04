# Business test plan: IS481

## Решение: тесты нужны (yes)

**Обоснование:** IS481 вводит существенное новое business поведение:
- Generic component API в `WordCardUseCaseImpl` (`addLexemeWithBuiltInComponent` /
  `addLexemeWithUserDefinedComponent` / `addComponentValue` / `updateComponentValue` /
  `deleteComponentValue` / `getComponentTypes`) с atomic compound INSERT и lookup'ом
  user-defined типов.
- Маппер `LexemeApiEntity.toDomain()` (AGG-1, AGG-2) — нетривиальная shim-логика
  + debug-only consistency invariant (B7 / B8 / C11 / C12 / C13).
- Reducer вычисление `hasDefinitionComponent` из `Msg.WordLoaded.componentTypes`
  (AGG-6, F1 fix).
- `DatasourceEffectHandler` — sequential `LoadWord` (`getTermById` →
  `getComponentTypes` → `Msg.WordLoaded(term, types)`) + reroute
  `UpdateLexemeDefinition` / `RemoveDefinition` на generic API (Node 25).
- `QuizGameImpl.toQuizItem` переписан на `componentRefs` lookup с graceful skip
  вместо `throw IllegalArgumentException` (AGG-5).
- `QuizChatUseCaseImpl.getQuizConfig` — domain mapping path (AGG-10).
- Atomicity rollback на FK violation в `addLexemeWithBuiltInComponent` и
  `addDictionary` auto-INSERT default quiz config (F013, F015).
- `Lexeme.builtIn(BuiltInComponent)` extension в `modules/domain/lexeme` (Gap-7).

Все компоненты unit-testable (pure Kotlin / mock'аемые dependencies); risk
silent breakage если не покрыты — высокий (shim → mate → UI, FK race conditions).

## Категории тестов

### 1. Mapper tests (`app/src/test/java/me/apomazkin/polytrainer/mapper/LexemeMapperTest.kt`)

**Цель:** проверить `LexemeApiEntity.toDomain()` и связанные мапперы
(`ComponentTypeApiEntity.toDomain`, `ComponentValueApiEntity.toDomain`,
`QuizConfigApiEntity.toDomain`).

**Setup:** тест выставляет `BuildConfig.DEBUG = true` через test fixture
(`@BeforeClass` / reflection set), либо shim consistency invariant вынесена в
pure-function `verifyShimConsistency(lexeme)` без `BuildConfig` guard — тогда
вызывается явно в тесте. Финальное решение — в плане реализации.

Сценарии:
- **Shim consistency invariant (B7)** — параметризованный тест на матрице
  комбинаций: translation-only / translation+definition / user-defined-only /
  empty / **built-in+user-defined combination** (5-й кейс per 06_mapping_design.md:617):
  `lexeme.translation?.value == components.firstOrNull{TRANSLATION}?.data.text`
  и `lexeme.definition?.value == components.firstOrNull{systemKey=null, name="Definition"}?.data.text`.
- **Orphan lexeme (B8 / C11)** — `components.isEmpty()` → `translation=null`,
  `definition=null`, `components=emptyList()`.
- **Multi-component lexeme (C13)** — несколько user-defined типов; shim definition
  — только тот что `name="Definition"`, остальные user-defined игнорируются.
- **Malformed ComponentValueData (C12)** — defensive: cast `data as? TextValue` →
  null если не TextValue, не crash.
- **Translation built-in lookup** — `systemKey == TRANSLATION`, не `name=="translation"`.

### 2. WordCardUseCaseImpl tests (`app/src/test/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImplTest.kt`)

**Цель:** проверить новые generic переопределения + переписку translation shim.

Сценарии:
- `addLexemeWithBuiltInComponent` — happy path (new lexeme, existing lexeme),
  null on `lexemeApi` exception, **FK violation rollback (F013)**.
- `addLexemeWithUserDefinedComponent` — happy path; **lookup miss** (тип
  `name="Definition"` не существует в словаре → возвращает **null без crash**,
  UseCase impl логирует через `logger.e(LogTags.WORDCARD, "...")`); null on FK
  violation.
- `addComponentValue` / `updateComponentValue` — happy path, null-on-error.
- `deleteComponentValue` — case A: `RemoveComponentResult.ComponentRemoved` (есть
  оставшиеся компоненты); case B: `LexemeCascadeRemoved` (последний компонент);
  case C: null on error.
- `restoreLexeme` — atomic compound INSERT через `addLexemeWithComponents` (MIN-9):
  - translation-only restore;
  - translation + definition restore (требует `getComponentTypes` + user-defined
    "Definition" lookup);
  - rollback при FK violation на втором компоненте — ни `lexemes`, ни
    `write_quiz`, ни component_values не остаются.
- `getComponentTypes` — empty dict / только TRANSLATION built-in / built-in +
  user-defined Definition.
- `addLexemeTranslation` / `deleteLexemeTranslation` — shim wrapper'ы делегируют
  на `addLexemeWithBuiltInComponent` / `deleteComponentValue`, маппят результат в
  `RemoveTranslationResult`.
- `TermApiEntity.toDomainEntity()` — `Term.dictionaryId` корректно проброшен из
  `WordApiEntity.dictionaryId`.

### 3. WordCardReducer tests (расширение existing + новый класс)

**Цель:** проверить вычисление `hasDefinitionComponent` на `Msg.WordLoaded`.

**Расположение:** расширить existing `WordLoadedTest.kt`
(`modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt`)
— добавить кейсы с `Msg.WordLoaded(term, componentTypes=...)` payload. Если
`hasDefinitionComponent` вычисляется в отдельной reducer-branch / helper —
создать `HasDefinitionComponentTest.kt` рядом для изолированного покрытия.

Сценарии:
- `Msg.WordLoaded(term, componentTypes=emptyList)` → `state.hasDefinitionComponent == false`.
- `Msg.WordLoaded(term, componentTypes=[TRANSLATION built-in])` →
  `hasDefinitionComponent == false`.
- `Msg.WordLoaded(term, componentTypes=[TRANSLATION, UserDefined name="Definition"])` →
  `hasDefinitionComponent == true`.
- `Msg.WordLoaded(term, componentTypes=[UserDefined name="Other"])` →
  `hasDefinitionComponent == false` (не любой user-defined, только `name="Definition"`).
- Никакие другие поля State не подменяются на `WordLoaded` — `lexemeList` /
  `wordState` заполняются как раньше (existing `WordLoadedTest` assertions).
- Прочие Msg (`RefreshTranslation`, `RefreshDefinition`, etc.) — НЕ трогают
  `hasDefinitionComponent` (read-only-on-load).

### 4. QuizChatUseCaseImpl tests (`app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt`)

**Цель:** проверить новый `getQuizConfig` + mapping path.

Сценарии:
- `getQuizConfig(dictionaryId, "write")` — happy path: `LexemeApi.getQuizConfig`
  возвращает ApiEntity → mapped via `QuizConfigApiEntity.toDomain()` → domain
  `QuizConfig`.
- Missing row — `lexemeApi.getQuizConfig` → null → UseCase returns null
  (F1 нарушение, не crash).
- Exception в `lexemeApi.getQuizConfig` → null-on-error + logger.e (паттерн проекта).
- Возвращаемый `QuizConfig.componentRefs` — domain sealed `ComponentTypeRef`,
  не ApiEntity (тип проверяется через `is BuiltIn` / `is UserDefined`).

### 5. QuizGameImpl.toQuizItem tests (`modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/quiz/QuizGameImplTest.kt`)

**Цель:** проверить `componentRefs` lookup + graceful skip (AGG-5, F5).

Сценарии:
- `componentRefs=[BuiltIn(TRANSLATION)]` + lexeme contains translation component
  → `QuizItem` returned, не null.
- `componentRefs=[BuiltIn(TRANSLATION), UserDefined("Definition")]` + lexeme
  missing definition → **graceful skip (null)**, не crash. Заменяет удалённый
  `throw IllegalArgumentException`.
- `componentRefs=[UserDefined("Definition")]` + lexeme has definition component
  → `QuizItem` with definition data.
- `componentRefs=emptyList()` → null (нечего показывать).
- **`lexeme.components` empty + `componentRefs` non-empty** → graceful skip (null);
  никакого NPE / throw, no resolved values.
- **Order priority (F4)** — `componentRefs=[A, B]` vs `[B, A]` → порядок в
  `QuizItem.components` соответствует `componentRefs` order, не порядку
  `lexeme.components`.
- `componentRefs=[BuiltIn(TRANSLATION), UserDefined("Definition")]` + lexeme
  имеет оба → both в QuizItem в правильном order.

### 6. LexemeBuiltInExt tests (`modules/domain/lexeme/src/test/java/me/apomazkin/lexeme/LexemeBuiltInExtTest.kt`, Gap-7)

**Цель:** покрыть extension `Lexeme.builtIn(BuiltInComponent)`.

Сценарии:
- `Lexeme.builtIn(TRANSLATION)` — returns `ComponentValue` если translation
  component присутствует в `components`.
- `Lexeme.builtIn(TRANSLATION)` — null если translation отсутствует.
- Multiple components (built-in + user-defined) — `builtIn(TRANSLATION)` игнорирует
  user-defined типы (только `systemKey == TRANSLATION`).
- Empty `components=emptyList()` → `builtIn(TRANSLATION) == null`.

### 7. Atomicity rollback tests

**Цель:** проверить транзакционную целостность generic-методов (F013 / F015).

Сценарии — частично инлайнены в категории 2/4, выделены как **integration tests**
для прозрачности (формально живут в категориях 2 и data sub-flow, но
концептуально atomicity):

- **F013 — `addLexemeWithBuiltInComponent` rollback** — стимулировать FK violation
  на `component_type_id` (передать несуществующий built-in type) → assert
  `lexemes`, `write_quiz`, `component_values` не содержат новых строк после
  exception. Часть `WordCardUseCaseImplTest` либо отдельный DAO-test (data sub-flow).
- **F015 — `WordDao.addDictionary` + `insertDefaultQuizConfig` rollback** —
  триггер **только FK violation**. `insertDefaultQuizConfig` пишет hardcoded JSON
  для `componentRefs`, malformation невозможна (JSON формируется в коде, не
  user input). Тест: FK violation на parent dict insert → assert `dictionaries`
  row не создан, F1 invariant держится (AGG-4 реверс). Часть data sub-flow
  (DAO-test), упомянуто здесь для трейсбилити.

### 8. DatasourceEffectHandler tests (расширение existing)

**Файл:** `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt`
(existing, расширить). FakeUseCase обновляется под новый `WordCardUseCase`
interface (`getComponentTypes`, `addLexemeWithUserDefinedComponent`,
`deleteComponentValue`, `addLexemeWithComponents`).

**Цель:** покрыть Node 25 — F1 fix sequential prefetch + reroute generic API.

Сценарии:
- **LoadWord sequential prefetch (F1 fix)** — FakeUseCase сетит `getTermByIdImpl`
  → возвращает `term` с `dictionaryId=42`, `getComponentTypesImpl(42)` →
  возвращает `[TRANSLATION, UserDefined("Definition")]`. Handler:
  - assert вызов `getTermById(wordId)` **до** `getComponentTypes(term.dictionaryId)`
    (порядок через ordered list / counter в Fake);
  - assert single `Msg.WordLoaded(term, types)` sent с обоими параметрами;
  - assert `Msg.WordNotFound` если `getTermById` → null (никакого вызова
    `getComponentTypes` после null term).
- **LoadWord exception на `getComponentTypes`** — `getTermById` ok,
  `getComponentTypes` throws → handler ловит → `Msg.WordNotFound` (либо
  `WordLoaded(term, emptyList())` — финальное решение в impl шаге, тест
  фиксирует выбранное поведение).
- **UpdateLexemeDefinition reroute** — Fake `addLexemeWithUserDefinedComponentImpl`
  заасертить вызов с `name="Definition"`, `data=TextValue(effect.definition)`;
  на успех → `Msg.RefreshDefinition(lexemeId, value)`; на null →
  `Msg.ShowNotification` / `ShowError`.
- **RemoveDefinition reroute** — Fake `deleteComponentValueImpl` / shim
  `deleteDefinitionComponent` — assert lookup по lexemeId. Path A
  (`ComponentRemoved`) → `Msg.DefinitionDeleted`. Path B (`LexemeCascadeRemoved`)
  → `Msg.LexemeCascadeRemovedWithUndo`. Path C (null) → `Msg.ShowError`.
- **RestoreLexeme — atomic compound INSERT (MIN-9) verify через mock UseCase** —
  FakeUseCase `restoreLexemeImpl` ловит `(wordId, translation, definition)` →
  assert один вызов с обоими полями (не два последовательных). Handler не
  раскладывает на translate+define — atomicity делегирована в UseCase impl,
  которая делает `addLexemeWithComponents` (см. категория 2).

## Test gaps batch (alignment Test gaps)

| Gap | Категория | Test | Tracking |
|---|---|---|---|
| B7 | Mapper | shim consistency invariant (BuildConfig.DEBUG=true setup либо pure-fn вынос; 5 кейсов incl. built-in+user-defined) | category 1 |
| B8 / C11 | Mapper | orphan lexeme empty components | category 1 |
| C12 | Mapper | malformed `ComponentValueData` defensive cast | category 1 |
| C13 | Mapper | multi-component lookup correctness | category 1 |
| F013 | Atomicity | `addLexemeWithBuiltInComponent` FK rollback | category 2 / 7 |
| F015 | Atomicity | `addDictionary` auto-INSERT rollback (FK-only trigger; hardcoded JSON) | category 7 (delegated to data sub-flow) |
| Gap-7 | Domain ext | `LexemeBuiltInExt` | category 6 |
| F1 fix | Handler | LoadWord sequential prefetch + reroute generic | category 8 |

## Verification

Проверка существования через `Read` / `Grep` / `Glob` при implement шаге.
**НЕ `Bash`** (запрет на grep/sed/find через bash из CLAUDE.md global).

## Подтверждение

business_test.md создан. Категории тестов перечислены с конкретными file
locations. Решение «тесты нужны» обосновано. Test gaps batch (B7/B8+C11/C12/C13)
+ F013/F015 + Gap-7 + F1-handler явно трассированы.

## log_messages

- iter 2: добавлена Категория 8 `DatasourceEffectHandler tests` (LoadWord prefetch + Update/Remove reroute + RestoreLexeme MIN-9)
- Категория 1: B7 invariant — BuildConfig.DEBUG=true setup или pure-fn вынос; добавлен 5-й built-in+user-defined кейс
- Категория 3: расширение existing `WordLoadedTest.kt`; F015 trigger — FK-only (hardcoded JSON), addLexemeWithUserDefinedComponent lookup miss — null без crash + logger.e

_model: claude-opus-4-7[1m]_
