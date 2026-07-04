---
status: done
---

# Summary — ui

## Что сделано

Реализован UI слой IS481 — chip «Определение» в `AddLexemeMeaningRow`
скрывается, если у словаря отсутствует user-defined тип `Definition`.

### Изменения

- **`modules/screen/wordcard/.../WordCardScreen.kt`** — единственный
  callsite `AddLexemeMeaningRow` (строки ~181-194). Введены local-vals
  `showAddTranslation` (= `lexemeState.canAddTranslation`) и
  `showAddDefinition` (= `lexemeState.canAddDefinition &&
  state.hasDefinitionComponent`). Параметры composable и условие
  guard `if (...)` используют local-vals.

### Источник `hasDefinitionComponent`

Поле `WordCardState.hasDefinitionComponent` уже добавлено в
`business_implement` (см. `State.kt:18-24`). Reducer вычисляет его
на `Msg.WordLoaded(word, componentTypes)`:

```kotlin
hasDefinitionComponent = componentTypes.any {
    it.systemKey == null && it.name == "Definition"
}
```

UI только читает state — никакой логики не реализует.

### Тесты

`./gradlew :modules:screen:wordcard:testDebugUnitTest`:
- 16 suites, 110 tests, 0 failures, 0 errors.
- Reducer-вычисление `hasDefinitionComponent` покрыто `WordLoadedTest`
  (4 кейса) — closed в business sub-flow.

## Ключевые решения

- **Chip полностью скрыт, не disabled.** AGG-6 интент — если у словаря
  нет definition-типа, у пользователя нет даже визуального намёка на эту
  функциональность. Disabled chip создавал бы шум в UI.
- **Translation chip без изменений видимости.** Translation —
  built-in `TRANSLATION` компонент, доступен в любом словаре по контракту
  (AGG-1).
- **AND, не OR.** Per-dictionary флаг `state.hasDefinitionComponent`
  AND'ится с per-lexeme `canAddDefinition` (= `definition == null`).
  Если у лексемы уже есть definition — chip не нужен (нечего добавлять);
  если у словаря нет definition-типа — chip тоже не нужен.
- **Guard `if (...)` тоже использует local-vals.** Без этого при
  `canAddTranslation=false, canAddDefinition=true, hasDefinitionComponent=false`
  add-row отрендерился бы с пустым FlowRow + видимым divider.
- **Composable `AddLexemeMeaningRow` не менялся.** Уже принимал Boolean
  параметры и условно рендерил chip-ы. Менять только callsite —
  минимальное вмешательство.

## Артефакты

- [`ui_walkthrough.md`](ui_walkthrough.md) — discovery 2 локаций рендера chip definition + verify через Read.
- [`ui_layout.md`](ui_layout.md) — layout-инвариант + матрица сценариев видимости (7 строк).
- [`ui_design_tree.md`](ui_design_tree.md) — DAG из 1 узла `[~]` (`WordCardScreen.kt`).
- [`ui_implement.md`](ui_implement.md) — diff + test results (110/0/0).
- [`publish_ui.md`](publish_ui.md) — отчёт публикации (UI-делта уже в `wordcard.md` § «UI Layout»).
- Modified Composable: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`.

_model: claude-opus-4-7[1m]_
