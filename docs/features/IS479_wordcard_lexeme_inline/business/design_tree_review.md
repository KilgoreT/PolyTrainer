## Итерация 1 (2026-05-19T22:20:00-0600)

architect: 2 critical + 2 minor.

### F076 [architect] critical

**Description:** UI-файлы (`WordCardScreen.kt`, `LexemeItemWidget.kt`, `AddLexemeBottomWidget.kt`) ссылаются на удаляемые типы (`UiMsg.ShowNotification(text, show)`, `ExitTranslationEditMode`, `OpenAddLexemeDialog`, `EnableTranslationCreation`, `AddLexemeBottomState`). После применения business-узлов модуль не скомпилируется.

**Status:** accepted

**Verdict:** Conductor decision — accept intermediate compile-break. Business sub-flow завершается с broken build, UI sub-flow стартует сразу после и восстанавливает компиляцию под inline-механику (Figma). Решение зафиксировано в `design_tree.md` ремаркой «Compile-break — accepted intermediate state».

### F077 [architect] critical

**Description:** `WordCardUseCaseImpl` декларирует инъекцию `database: AppDatabase` для `withTransaction`. `CoreDbApi` не предоставляет ни `AppDatabase`, ни transactional API. F064 atomicity физически нереализуем без расширения data-слоя.

**Status:** rejected

**Verdict:** ЛОЖНЫЙ critical. Проверено: `CoreDbApi.LexemeApi` уже содержит перегрузку `addLexeme(wordId, translation: TranslationApiEntity): Long` (`CoreDbApi.kt:80-83`) и `addLexeme(wordId, definition: DefinitionApiEntity): Long` (`CoreDbApi.kt:85-88`). Impl в `CoreDbApiImpl.kt:216-242` делает **один** `wordDao.addLexeme(LexemeDb(wordId, translation=..., addDate))` — атомарно по построению (один INSERT в Room). F064 закрывается переключением `WordCardUseCaseImpl.addLexemeTranslation(lexemeId=null, ...)` на использование существующей перегрузки. Никакого расширения `CoreDbApi` / `AppDatabase`-инъекции не нужно. Узел `WordCardUseCaseImpl` в design_tree ит.2 детализируется через `lexemeApi.addLexeme(wordId, TranslationApiEntity(translation))`.

### F078 [architect] minor

**Description:** Тест `DeleteWordDialogTest.kt` (#11) depends `[1, 4]`, фикстуры опираются на `WordState.Loaded` — фактическая зависимость от #1 (State) усилена, не отражена в DAG.

**Status:** approved

**Verdict:** Точечная правка design_tree: пометить #11 (и #10/#15) зависимостью от #1 как «фикстуры используют sealed `WordState.Loaded`».

### F079 [architect] minor

**Description:** `UiEffectHandler.kt` (#5 [-]) содержит `sealed interface UiEffect` — если есть ссылки извне модуля, lurking ref.

**Status:** rejected

**Verdict:** Проверено reviewer'ом — ссылок на `UiEffect` извне модуля нет. Удаление безопасно. Зависимость #6 → #5 корректна.

---

## Итог ит.1 (close)

- 1 critical accepted as conductor decision (F076 compile-break).
- 1 critical rejected (F077 — existing API уже покрывает atomicity).
- 1 minor approved → точечная правка DAG (F078).
- 1 minor rejected (F079).

Design_tree обновлён в ит.2 conductor-патчем без отдельного запуска execute субагента (правки точечные, описаны в этом review).
