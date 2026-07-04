# UI design tree — IS481

## Граф изменений

```
[~] modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt
    deps:
      - [~] WordCardState.hasDefinitionComponent (business_implement, State.kt)
      - [~] LexemeState.canAddDefinition (computed, State.kt — без изменений)
      - [~] AddLexemeMeaningRow (без изменений сигнатуры)
    change:
      - вычислить showAddTranslation / showAddDefinition AS local vals в forEach loop
      - showAddDefinition = lexemeState.canAddDefinition && state.hasDefinitionComponent
      - условие выходного if() — showAddTranslation || showAddDefinition
      - параметры AddLexemeMeaningRow — передать showAdd* (не raw lexemeState.*)
```

Прочие composable файлы IS481-UI **не затрагиваются**:

- `AddLexemeMeaningRow.kt` — composable уже принимает `canAddDefinition: Boolean` и условно рендерит chip. Менять не нужно.
- `LexemeMeaningField.kt` — для уже заполненного definition'а, флаг не релевантен (данные остаются валидными до явного delete).
- `LexemeCard.kt` — Surface-обёртка, не зависит от компонентов.
- `SubentityChip.kt` — chip primitive, не зависит от типа.

## Preview / тесты

Preview-функции в `AddLexemeMeaningRow.kt` (внутренние `@PreviewWidget`) и
`LexemeMeaningField.kt` уже покрывают visibility сценарии через прямые
параметры `canAddTranslation`/`canAddDefinition`. Дополнительных preview не нужно.

Unit-тесты UI слоя в wordcard — отсутствуют (Composable code не тестируется
unit-test'ами; instrumented UI tests — out of scope для проекта). Поведение
`hasDefinitionComponent` уже покрыто reducer-тестами в `WordLoadedTest.kt`
(4 кейса, business_implement).

## Узлы дерева

| Тип | Файл | Действие |
|---|---|---|
| `[~]` | `modules/screen/wordcard/.../WordCardScreen.kt` | Modify (1 callsite, ~5 строк изменений + comment) |

Один узел. Минимальный UI scope IS481 = ровно один файл.

## log_messages

- design tree: 1 узел `[~]` — `WordCardScreen.kt` (modify callsite)
- `AddLexemeMeaningRow.kt` сигнатура / тело не меняются — composable уже принимает Boolean флаги
- preview / unit tests — без изменений (visibility покрыто reducer-тестами в WordLoadedTest)

_model: claude-opus-4-7[1m]_
