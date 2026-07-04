# UI implement — IS481

## Изменённые файлы

### `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`

Изменён единственный блок (строки ~181-194 после правки) — callsite
`AddLexemeMeaningRow`. Pre-IS481 callsite передавал `canAddDefinition`
напрямую из `lexemeState`; теперь AND'ится с `state.hasDefinitionComponent`.

#### Diff (логически)

**Было:**

```kotlin
if (lexemeState.canAddTranslation || lexemeState.canAddDefinition) {
    AddLexemeMeaningRow(
        canAddTranslation = lexemeState.canAddTranslation,
        canAddDefinition = lexemeState.canAddDefinition,
        enabled = !state.isPendingDbOp,
        onCreateTranslation = { sendMessage(Msg.CreateTranslation(lexemeState.id)) },
        onCreateDefinition = { sendMessage(Msg.CreateDefinition(lexemeState.id)) },
    )
}
```

**Стало:**

```kotlin
// IS481 (AGG-6) — chip «Определение» рендерится только
// если per-dictionary `hasDefinitionComponent == true` AND
// per-lexeme `canAddDefinition == true` (definition ещё не
// заполнен). Translation chip — без условия видимости.
val showAddTranslation = lexemeState.canAddTranslation
val showAddDefinition =
    lexemeState.canAddDefinition && state.hasDefinitionComponent
if (showAddTranslation || showAddDefinition) {
    AddLexemeMeaningRow(
        canAddTranslation = showAddTranslation,
        canAddDefinition = showAddDefinition,
        enabled = !state.isPendingDbOp,
        onCreateTranslation = { sendMessage(Msg.CreateTranslation(lexemeState.id)) },
        onCreateDefinition = { sendMessage(Msg.CreateDefinition(lexemeState.id)) },
    )
}
```

Изменения:
1. Введены local-vals `showAddTranslation` / `showAddDefinition` —
   `showAddDefinition` AND'ит per-lexeme `canAddDefinition` с
   per-dictionary `state.hasDefinitionComponent`.
2. Guard-condition `if (...)` использует local-vals — иначе при
   `canAddDefinition=true, hasDefinitionComponent=false` без other-chip
   add-row отрендерился бы как пустой FlowRow + divider.
3. Параметры `AddLexemeMeaningRow` получают local-vals (не raw `lexemeState.*`).
4. Comment с тэгом `IS481 (AGG-6)` объясняет смысл AND.

## Тесты

Запущена команда:

```bash
./gradlew :modules:screen:wordcard:testDebugUnitTest
```

Результат — все 16 test suites модуля PASS:

| Suite | Tests | Failures | Errors |
|---|---|---|---|
| LexemeExtTest | 3 | 0 | 0 |
| DefinitionManagementTest | 12 | 0 | 0 |
| TopBarExtTest | 2 | 0 | 0 |
| LexemeManagementTest | 7 | 0 | 0 |
| CloseTopBarMenuTest | 2 | 0 | 0 |
| NavigateBackTest | 3 | 0 | 0 |
| WordLoadedTest | 7 | 0 | 0 |
| WordExtTest | 6 | 0 | 0 |
| DatasourceEffectHandlerTest | 18 | 0 | 0 |
| NoOperationTest | 2 | 0 | 0 |
| UndoDeleteTest | 8 | 0 | 0 |
| SpecializedLexemeExtTest | 8 | 0 | 0 |
| DeleteWordDialogTest | 7 | 0 | 0 |
| OpenTopBarMenuTest | 2 | 0 | 0 |
| TranslationManagementTest | 15 | 0 | 0 |
| WordEditTest | 8 | 0 | 0 |
| **TOTAL** | **110** | **0** | **0** |

Exit code: 0. Только deprecation warnings (`@Deprecated Translation`/`Definition`
shim — ожидаемо, B4/C2 design decision, см. business_implement).

## Verification

Изменения **не** ломают existing reducer / handler / state логику:
- `WordCardState.hasDefinitionComponent` уже добавлено в `business_implement`
- `LexemeState.canAddDefinition` computed без изменений
- `AddLexemeMeaningRow` сигнатура без изменений (composable уже принимает Boolean)
- Reducer-вычисление `hasDefinitionComponent` покрыто `WordLoadedTest` (4 кейса)

## log_messages

- 1 callsite `AddLexemeMeaningRow` в `WordCardScreen.kt` изменён — AND `canAddDefinition` с `state.hasDefinitionComponent`
- 110 unit-тестов wordcard PASS, exit code 0
- composable `AddLexemeMeaningRow.kt` сам не менялся — уже принимал Boolean флаги

_model: claude-opus-4-7[1m]_
