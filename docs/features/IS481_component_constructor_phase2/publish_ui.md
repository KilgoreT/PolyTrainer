# publish_ui

## Опубликовано

- `docs/features-spec/component-constructor.md` — `## UI Layout` раздел заменён финальным snapshot phase 2 (~430 строк, было ~262). Канонический порядок разделов сохранён (`# Component Constructor` → `Бизнес-описание` → `User Stories` → `State` → `UI Messages` → **`UI Layout`** → `IO` → `UseCase` → `Тестовые сценарии`).

## Корректировки от implement

Финальный snapshot отличается от `ui_layout.md` (UI design) по следующим nontrivial решениям, принятым на implement-стадии (отражено в спеке отдельным подразделом «Корректировки от implement»):

1. **`lexemeLabel` placeholder** — host резолвит через `context.getString(R.string.components_edit_lexeme_label, id)` = "Lexeme #N" / "Лексема №N"; lambda вызывается из non-@Composable scope. Backlog: реальный label через UseCase query.
2. **`onShowAllImpacted` — no-op** в обоих screen'ах (drill-in destination — backlog).
3. **`DictionaryRef.name` field** verified Read'ом из `DictionaryApiEntity.kt` (не `.title` как было в design tree).
4. **`HostVariant` enum** (Manager | PerDict) declared inline в `CreateComponentDialog.kt` (один dialog = один файл с display-only DTO'шками). PerDict передаёт пустые collections + no-op callbacks.
5. **Плоские примитивы для всех Tier 1/2 widgets** — design tree открыл 3 варианта (coupling / lift-types / flatten); выбран flatten. Hosts раскладывают mate state на mount-site (Dependency Rule).
6. **`DeletionImpactRef` display-only DTO** (counts only) declared в `DeleteComponentConfirmDialog.kt`; host маппит full `DeletionImpact` → counts.
7. **`EditNameError.toLabelRes()` host-local** — private extension в каждом screen, не выносится в shared module (`EditNameError` живёт в двух package'ах mate'ов).
8. **`LexemeStyle.LabelM/LabelL` fallback** — отсутствует в проекте → label → `BodyS`, label-large → `BodyLBold`. `ComponentBlock` name_slot использует `BodyS+onSurfaceVariant`.
9. **`PrimaryTextButtonWidget` без StringRes args** → drill-in button использует M3 `TextButton` + `stringResource(id, totalCount)` (backlog: overload с vararg formatArgs).
10. **`ImpactedLexemesPreview` sealed → 3 плоских параметра** на widget-уровне (`inlineIds`, `totalCount`, `showAllVisible`); reducer/host маппит sealed на mount-site.

Дополнительно отражено: 8 новых strings ключей (`components_edit_*`, `components_create_field_scope`, `components_create_scope_*`), переезд 16 widgets в `:modules:widget:component_widgets`, миграция `onEdit→Msg.OpenEditDialog` в Row widgets.

## log_messages

- Read ui_layout.md (720 строк) + ui_implement.md (104 строки) + existing spec (1471 строк, по страницам); канонический порядок разделов проверен — OK.
- Edit заменил блок `## UI Layout` (lines 493-753) на финальный snapshot с интегрированными implement-корректировками (10 пунктов выделены отдельным подразделом + интегрированы в Анализ виджетов / API виджетов).

_model: claude-opus-4-7[1m]_
