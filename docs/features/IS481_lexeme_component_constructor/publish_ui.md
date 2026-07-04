# Publish UI — IS481

## Резюме

UI-делта IS481 **уже опубликована** в спеке wordcard.md в фазе
`business_publish_spec`. Отдельной UI-only публикации **нет** —
изменения видимости chip учтены в основной спеке.

## Опубликованные ссылки

### `docs/features-spec/wordcard.md` § «UI Layout» (опубликовано business_publish_spec)

```markdown
## UI Layout

См. подробную UI-разметку: [wordcard-ui.md](wordcard-ui.md).

Chip «Определение» в `AddLexemeMeaningRow` / `LexemeMeaningField`
скрывается если `state.hasDefinitionComponent == false`. Translation
chip без условия видимости.
```

### `docs/features-spec/wordcard.md` § «State» — поле hasDefinitionComponent (опубликовано)

```kotlin
/**
 * Per-dictionary флаг наличия user-defined типа `name="Definition", system_key=NULL`.
 * Управляет видимостью chip «Определение». Explicit field, заполняется один раз
 * на `Msg.WordLoaded`. Composable AND'ит с per-lexeme `canAddDefinition`.
 */
val hasDefinitionComponent: Boolean = false,
```

### `docs/features-spec/wordcard.md` § Инварианты (опубликовано)

```
11. `hasDefinitionComponent == true` ⇔ при load `componentTypes`
    (список `ComponentType` словаря лексемы) содержит запись
    `name="Definition", systemKey=null`.
```

## UI-only артефакт публикации

Нет отдельной UI-спеки (типа `wordcard-ui.md` extra-файла). Делта layout
уже в основной wordcard.md спеке. PUML диаграмма UI flow — не публикуется
в IS481 (Figma не используется, диаграмма не требовалась в scope).

## Соответствие impl ↔ спека

| Спека (wordcard.md) | Impl (WordCardScreen.kt) | Статус |
|---|---|---|
| chip «Определение» скрывается if `hasDefinitionComponent == false` | local-val `showAddDefinition = lexemeState.canAddDefinition && state.hasDefinitionComponent` | ✅ |
| translation chip без условия видимости | `showAddTranslation = lexemeState.canAddTranslation` | ✅ |
| AND с per-lexeme `canAddDefinition` | `lexemeState.canAddDefinition && state.hasDefinitionComponent` | ✅ |
| `hasDefinitionComponent` read-only-on-load | source — `state.hasDefinitionComponent` (заполняется reducer'ом на `Msg.WordLoaded`) | ✅ |
| Translation chip без изменений | callsite не трогает поведение translation | ✅ |

## Артефакты публикации

- Спека: [`docs/features-spec/wordcard.md`](../../features-spec/wordcard.md) — уже опубликована business_publish_spec.
- UI implementation: [WordCardScreen.kt](../../../modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt) — изменения в текущем UI sub-flow.

## log_messages

- UI-делта учтена в `wordcard.md` § «UI Layout» + § «State» + § Инварианты (опубликовано business_publish_spec)
- Отдельной UI-спеки и PUML нет — Figma не используется в IS481
- impl ↔ спека соответствует 1:1 (5 пунктов)

_model: claude-opus-4-7[1m]_
