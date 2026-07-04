# UI layout — IS481

## Изменения

Single layout change в wordcard: chip «Определение» в `AddLexemeMeaningRow`
рендерится **условно** в зависимости от per-dictionary флага
`state.hasDefinitionComponent`. Translation chip без условия видимости —
built-in компонент, доступен в любом словаре.

## Layout-инвариант

```
state.hasDefinitionComponent == false ⇒
    chip «Определение» ОТСУТСТВУЕТ в composable-tree
    (НЕ disabled, НЕ greyed-out — полностью отсутствует)
```

Инвариант распространяется на add-row placeholder (новая лексема без
значения). Existing chip+text в `LexemeMeaningField` для уже заполненного
definition'а **не** скрывается этим флагом — данные остаются валидными до
явного `RemoveDefinition`.

## Композиция add-row после изменения

```
LexemeCard
└── (translation field, если есть)
└── (definition field, если есть)
└── if (showAddTranslation || showAddDefinition) AddLexemeMeaningRow
    │   showAddTranslation = lexemeState.canAddTranslation
    │   showAddDefinition  = lexemeState.canAddDefinition
    │                        && state.hasDefinitionComponent
    │
    └── Column
        ├── HorizontalDivider (align = End, 33% width)
        ├── Spacer(12dp)
        ├── FlowRow (Arrangement.End)
        │   ├── if (canAddTranslation) SubentityChip("Перевод", + icon)
        │   └── if (canAddDefinition) SubentityChip("Определение", + icon)
        └── Spacer(6dp)
└── DeleteLexemeButton
```

## Сценарии видимости

| canAddTranslation | canAddDefinition | hasDefinitionComponent | Render add-row | Chips видны |
|---|---|---|---|---|
| true | true | true | да | translation + definition |
| true | true | false | да | только translation |
| true | false | true | да | только translation |
| true | false | false | да | только translation |
| false | true | true | да | только definition |
| false | true | false | **нет** | — (skip add-row entirely) |
| false | false | * | нет | — |

Ключевой кейс: `canAddTranslation=false ∧ canAddDefinition=true ∧
hasDefinitionComponent=false` — без guard'а условия `if (...)` add-row
отрендерился бы с пустым FlowRow + видимым divider'ом. Поэтому условие
guard также переписывается на `showAddTranslation || showAddDefinition`.

## Translation chip без изменений

`canAddTranslation = lexemeState.canAddTranslation` (computed
`translation == null`). Translation — built-in компонент, всегда доступен.

## Layout-инвариант инициализации

`hasDefinitionComponent` стартует как `false` (default в `WordCardState`),
обновляется reducer'ом на `Msg.WordLoaded(word, componentTypes)`. До прихода
`WordLoaded` `state.wordState is WordState.NotLoaded` ⇒ блок add-row
вообще не рендерится (внешний guard `if (state.wordState is WordState.Loaded)`
в `WordCardScreen.kt:133`). Race condition исключён.

## log_messages

- chip «Определение» скрыт **полностью** (отсутствует в дереве), не disabled
- условие `if (lexemeState.canAddTranslation || lexemeState.canAddDefinition)` переписывается на use of `showAddTranslation || showAddDefinition` чтобы не рендерить пустой add-row + divider
- translation chip без изменений видимости — built-in компонент

_model: claude-opus-4-7[1m]_
