# Approved findings — scope_analysis iter 4

## F025 [architect] minor — approved

**Что чинить:** В Note F022 (cross-reference в § Tests / комментарии после переноса тестов) исправить typo `whenEditUpgradesIsMulli_*` → `whenEditUpgradesIsMulti_*` (canonical форма уже в § Tests).

**Verdict:** реальный typo cross-reference расходится с canonical именем — copy-paste-ошибка iter 4.

## F026 [architect] minor — approved

**Что чинить:** Зафиксировать тип для `availableDictionaries`. Два варианта:
- **(а)** Использовать existing `DictionaryApiEntity` из `core-db-api` — `availableDictionaries: List<DictionaryApiEntity>`, `flowDictionaries(): Flow<List<DictionaryApiEntity>>`. UseCaseImpl делегирует на `dictionaryApi.flowDictionaryList()` без mapping.
- **(б)** Ввести NEW domain тип `DictionaryEntry` в `modules/domain/lexeme/` (или новый `modules/domain/dictionary/`) с mapping `DictionaryApiEntity → DictionaryEntry` в UseCaseImpl.

Best-guess: (а) для MVP (минимум surface area), unless domain нуждается в proecting (например для filtering / sorting / aliasing). Зафиксировать решение в § Затронутые файлы (Data API / Business / Domain), убрать упоминания `DictionaryEntry` либо явно объявить NEW.

**Verdict:** Тип DictionaryEntry упоминается в State/UseCase сигнатурах, но не существует в domain/API — реальная неувязка scope, нужна фиксация (использовать DictionaryApiEntity либо NEW domain тип с placement).

## F027 [qa_engineer] critical — approved

**Что чинить:** Привести API-level `EditComponentOutcome` к существующему pattern (Create/Rename/SoftDelete API outcome):
- Убрать `NameEmpty` из API variants (валидация на UseCaseImpl: `trimmed.isBlank()` → domain `EditOutcome.NameEmpty` без обращения к API).
- Убрать `Failure(cause: Throwable)` из API variants (try-catch в UseCaseImpl, mapping exception → domain `EditOutcome.Failure(cause)`).
- API `EditComponentOutcome` остаётся с: `Success / SameScopeCollision / CrossScopeCollision / CardinalityDowngradeBlocked(impactedLexemeIds) / TemplateImmutable / BuiltInProtected / Removed` (7 вариантов, не 9).

**Verdict:** Расхождение API-level outcome list с existing pattern — implementer не знает по какому контракту писать, реальная проблема scope.

## F028 [qa_engineer] critical — approved

**Что чинить:** В § Tests Reducer block добавить тест на `EditResult.Failure`:
- `whenEditResultFailure_thenDialogClosed_andGenericErrorSnackbarEmitted` (либо явный outcome `whenEditResultFailure_thenDialogRetained_andErrorInlineShown` если решено retain — выбрать один pattern и зафиксировать в § Аспекты `edit_component`).

Best-guess: close dialog + generic snackbar (parity с Rename/Delete Failure handling, минимальный UI complexity для MVP).

**Verdict:** Reducer-реакция на EditResult.Failure не специфицирована в § Tests — реальный пробел test coverage в области ответственности шага.

## F030 [qa_engineer] minor — approved

**Что чинить:** В § Tests Reducer block (ComponentsManagerReducerTest) добавить тест-инвариант:
- `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState` — Msg.DictionariesLoaded(updated) НЕ затрагивает EditDialogState (только CreateDialogState filtering). Регрессия если общая Reducer-ветка случайно применит фильтр к Edit.

**Verdict:** invariant заявлен в § Аспекты но не покрыт тестом — реальный gap consistency между аспектом и § Tests.

---

## Rejected (iter 4, для трассировки)

- **F029** rejected — «no-op Submit» short-circuit vs UPDATE — sub-flow design decision, не классификация scope.
