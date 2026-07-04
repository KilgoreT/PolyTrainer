# UI walkthrough — IS481

## Scope discovery

UI scope IS481 — chip visibility «Определение» в wordcard. Translation chip
без изменений. Figma не используется. UI-делта уже описана в опубликованной
спеке [`docs/features-spec/wordcard.md`](../../features-spec/wordcard.md) §
«UI Layout» (после `business_publish_spec`).

## Локация рендера chip «Определение»

Chip «Определение» рендерится **в двух местах** wordcard composable-tree:

### 1. Add-row placeholder (новая лексема без значения)

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddLexemeMeaningRow.kt`

```kotlin
internal fun AddLexemeMeaningRow(
    canAddTranslation: Boolean,
    canAddDefinition: Boolean,
    enabled: Boolean,
    onCreateTranslation: () -> Unit,
    onCreateDefinition: () -> Unit,
) {
    Column(...) {
        ...
        FlowRow(...) {
            if (canAddTranslation) {
                SubentityChip(labelRes = R.string.word_card_bottom_translation, ...)
            }
            if (canAddDefinition) {
                SubentityChip(labelRes = R.string.word_card_bottom_definition, ...)
            }
        }
        ...
    }
}
```

Composable **уже** имеет параметр `canAddDefinition: Boolean` и рендерит chip
условно через `if (canAddDefinition)`. Дополнять сам composable не нужно —
достаточно правильно вычислить значение `canAddDefinition` на стороне callsite.

### 2. Edit field существующей лексемы

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeMeaningField.kt`

Используется для **уже заполненного** значения (chip+inline-editable текст).
`hasDefinitionComponent` для этого composable **не релевантен**: поле
показывается только если `lexemeState.definition != null` (значение уже есть
в БД). Если у словаря потом удалить definition-тип, существующие значения
останутся валидными до удаления данных. Скрытие add-chip достаточно — новые
definition'ы создавать нельзя.

## Callsite (где AND'ить hasDefinitionComponent с canAddDefinition)

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`

Линии 181-189 (pre-IS481):

```kotlin
if (lexemeState.canAddTranslation || lexemeState.canAddDefinition) {
    AddLexemeMeaningRow(
        canAddTranslation = lexemeState.canAddTranslation,
        canAddDefinition = lexemeState.canAddDefinition,
        enabled = !state.isPendingDbOp,
        ...
    )
}
```

Здесь `lexemeState.canAddDefinition` (computed `definition == null`)
нужно AND'ить с `state.hasDefinitionComponent` (per-dictionary флаг из mate
state) — это место единственное изменение в UI слое IS481.

## State.kt — источник флага

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`

Поле `hasDefinitionComponent: Boolean = false` уже добавлено в `WordCardState`
в `business_implement` (строки 18-24). KDoc явно описывает контракт:
"Composable AND'ит с per-lexeme `canAddDefinition` для chip visibility".

## Verdict

UI sub-flow требует **единственного точечного изменения** в `WordCardScreen.kt`:
заменить `canAddDefinition = lexemeState.canAddDefinition` на
`canAddDefinition = lexemeState.canAddDefinition && state.hasDefinitionComponent`.
Также обновить условие `if (...)` для соответствия — иначе add-row может
отрендериться с обоими false (пустой FlowRow + divider).

## log_messages

- chip «Определение» рендерится в `AddLexemeMeaningRow.kt` через `if (canAddDefinition)` — самого composable менять не нужно
- единственный callsite в `WordCardScreen.kt:181-189` — AND'ить `canAddDefinition` с `state.hasDefinitionComponent`
- `LexemeMeaningField.kt` (заполненный definition) не зависит от `hasDefinitionComponent` — данные остаются до явного удаления

_model: claude-opus-4-7[1m]_
