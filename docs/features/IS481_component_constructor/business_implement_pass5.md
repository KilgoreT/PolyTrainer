# business_implement_pass5.md — IS481 cc, Pass 5 (migration call-sites)

**Status: done.** Migration M12 → M13 (business-side): `ComponentValueData` → `TemplateValues`/`Primitive` + `ComponentTypeApiEntity` field rename `removeDate → removedAt` + new fields `isMulti/createdAt/updatedAt` + 8 existing test files rebind. Узлы #49-#54 design_tree + F099 (test rebind) + F117 (drop local toDomain).

## Scope (как было запланировано)

- #49 `WordCardUseCaseImpl.kt` — 4× `ComponentValueData.TextValue(...)` + 5× `data: ComponentValueData` parameter rebind.
- #50 `WordCardUseCase.kt` (interface) — 5× `data: ComponentValueData` → `TemplateValues`.
- #51 `wordcard/mate/DatasourceEffectHandler.kt` — 1× `ComponentValueData.TextValue`.
- #52 `app/.../mapper/LexemeMapper.kt` — 2× `ComponentValueData.TextValue` cast + `ComponentTypeApiEntity.toDomain()` mapping rename + new fields propagation.
- #53 `QuizGameImpl.kt` — 2× `ComponentValueData.TextValue`/`LongTextValue` cast → `TextValues`. LongTextValue branch dropped (M13 template consolidation `long_text → text`, F046).
- #54 `Lexeme.kt` — `@Deprecated` text refresh (косметика).
- `ComponentValue.kt` — rebind `data: ComponentValueData` → `TemplateValues`.

## F117 — drop local `toDomain` extensions

- `app/.../componentsmanager/ComponentsManagerUseCaseImpl.kt`: удалён `private fun ComponentTypeApiEntity.toDomain()`, заменён на `import me.apomazkin.polytrainer.mapper.toDomain`.
- `app/.../perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt`: то же.

## F099 — test files mechanical rebind (8 files, не 6)

Кроме перечисленных 6 в prompt'е — два дополнительных файла зацеплены через transitivity (`ComponentType` field expansion с required `createdAt`/`updatedAt`):

1. `app/.../WordCardUseCaseImplTest.kt` (~15 refs) — `TextValues` rebind + `createdAt`/`updatedAt` для api-entity test helpers + `ComponentValueId` import cleanup.
2. `app/.../LexemeMapperTest.kt` (~14 refs) — `TextValues` + один `ImageValues` (was `ComponentValueData.ImageValue("file://img")`) + test helpers update.
3. `modules/screen/wordcard/src/test/.../mate/DatasourceEffectHandlerTest.kt` (~12 refs) — sub-replace `ComponentValueData → TemplateValues` в FakeUseCase signatures + 1 `TextValues` assertion fix (was `TextValue.text`, теперь `TextValues.value.value`).
4. `modules/screen/wordcard/src/test/.../mate/WordLoadedTest.kt` — `ComponentType` helpers (3) + Date `now` helper.
5. `modules/screen/quiz/chat/src/test/.../QuizGameImplTest.kt` — `TextValues` + `createdAt`/`updatedAt` для inline `ComponentType(...)`.
6. `modules/screen/quiz/chat/src/test/.../QuizGameImplFetchDataTest.kt` — `TextValues` + `createdAt`/`updatedAt` для val `translationType` / `definitionType`.
7. `modules/screen/quiz/chat/src/test/.../logic/ChatReducerTest.kt` — `createdAt`/`updatedAt` для 2 `ComponentType` helpers + `import java.util.Date`.
8. `modules/screen/quiz/chat/src/test/.../logic/DatasourceEffectHandlerTest.kt` — `createdAt`/`updatedAt` для inline `translationType` val.
9. `modules/screen/quiz/chat/src/test/.../logic/QuizPickerFlowHandlerTest.kt` — то же.
10. `modules/domain/lexeme/src/test/.../LexemeBuiltInExtTest.kt` (~3 refs) — `TextValues` + `createdAt`/`updatedAt` для 2 ComponentType helpers.
11. `modules/domain/lexeme/src/test/.../ComponentTypeRefExtTest.kt` — `createdAt`/`updatedAt` для 2 helpers + `import java.util.Date`.

(#4, #7-#11 — discovery during compile; не было в prompt 6-file list, но обязательны иначе `compileTestKotlin` fail.)

## Tests результат

| Module | Task | Result |
|---|---|---|
| `:modules:domain:lexeme` | `:test` | ✅ PASS |
| `:modules:screen:wordcard` | `:testDebugUnitTest` | ✅ PASS |
| `:modules:screen:quiz:chat` | `:testDebugUnitTest` | ✅ PASS |
| `:modules:screen:components_manager` | `:testDebugUnitTest` | ✅ PASS |
| `:modules:screen:per_dictionary_components` | `:testDebugUnitTest` | ✅ PASS |

## НЕ сделано (по плану)

- `ComponentValueData.kt` (#6) — НЕ удалён; data_design_tree удаляет это финальным узлом после data-side migrations.
- `:core:core-db-impl` — НЕ компилируется (broken Pass 1 breaking changes); восстанавливает data_implement.
- `:app:testDebugUnitTest` — НЕ запускался (зависит от `:core:core-db-impl`).
- `:app:assembleDebug` — НЕ запускался (тот же reason).

## Нетривиальные решения / отклонения

1. **`TemplateValues` flat vs nested.** Prompt предписывал `TemplateValues.TextValues(...)` (nested), но реальный код в `modules/domain/lexeme/TemplateValues.kt` (Pass 1) объявляет `TextValues` и `ImageValues` как **top-level** data classes : `TemplateValues`. Использовал реальный API — `TextValues(value = Primitive.Text(s))`. Реально (Verify-library-API-through-source).
2. **`ImageValues` migration.** В `LexemeMapperTest.kt` был тест C12 с `ComponentValueData.ImageValue("file://img")` — перевёл на `ImageValues(value = Primitive.Image(...))`. Тест проверяет defensive cast `(it.data as? TextValues)?.value?.value` → null при non-text → корректность сохранена.
3. **`LongTextValue` removal.** В `QuizGameImpl.kt` была fallback-ветка `?: (source.data as? ComponentValueData.LongTextValue)?.text` — удалил вместе с импортом. Per design_tree #53: «LongTextValue полностью упразднён (M13 template consolidation `long_text → text`, F046); fallback-ветка удаляется».
4. **F117 завершён.** Local `private fun ComponentTypeApiEntity.toDomain()` extensions в обоих UseCase impl files удалены; canonical `me.apomazkin.polytrainer.mapper.toDomain` импортируется. Pass 1-2 это и ожидали (F134 retrofit comment в коде).
5. **Required ctor params в test helpers.** `ComponentType`/`ComponentTypeApiEntity` имеют `createdAt: Date` / `updatedAt: Date` без defaults (по design_tree #7/#19) — пришлось обновить 11 test files, не 6.

## История ревью

(пусто — review iter для Pass 5 запускается после этого doc)
