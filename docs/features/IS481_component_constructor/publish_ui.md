# publish_ui

## Опубликовано

- `docs/features-spec/component-constructor.md` — обновлён.
- Режим: `update-section` (вставка нового раздела между существующими `## UI Messages` и `## IO`).
- Раздел: `## UI Layout`.
- Размер раздела: ~496 строк (inline; ≤ 500 — без выноса в отдельный `*-ui.md`).
- Размер спеки после публикации: 1366 строк (`wc -l`).
- Канон H2-порядка после публикации: `Бизнес-описание → User Stories → State → UI Messages → UI Layout → IO → UseCase → Тестовые сценарии` (соответствует canonical schema из `steps/publish_ui.md`).

## Корректировки от implement

Применены изменения из `ui_implement.md` к опубликованному snapshot'у `ui_layout.md`:

- **F158 — MVP scope=Global only.** `CreateComponentDialog` опубликован без `scope_slot` и без callback'ов `onScopeGlobal` / `onScopePerDict` / `onOpenDictionaryPicker`. Reducer hardcode'ит scope при `OpenCreateDialog` по контексту экрана (Manager → `Scope.Global`; PerDict → `Scope.PerDictionaries([currentDict])`). В спеке оставлена явная справка о том, что `Msg.CreateScopeChange` остаётся в контракте, но UI его не отправляет.
- **F162 — `LexemeRadioRow` primitive.** В `CreateComponentDialog.structure` template radio-group: `variant=LexemeRadioMenuItem` заменён на `variant=LexemeRadioRow`. Primitive описан в § Новые виджеты (живёт в `:modules:core:ui`, используется обоими экранами).
- **F163 — `ErrorStateWidget` + Retry.** На обоих экранах (Manager / PerDict) в `## Карта экрана` добавлена ветка `!isLoading && (userDefinedTypes|items) == null` → `<ErrorStateWidget>` с `onRetry → Msg.OnRetryClick`. Описание примитива `<ErrorStateWidget>` добавлено в раздел «Анализ виджетов» — generic primitive в `:modules:core:ui`.
- **F161 — i18n PerDictRow.** В `<PerDictRowWidget>.structure.meta_slot.usage_text` источник заменён на `R.string.per_dict_row_value_count(row.valueCount)` (locale-aware форматирование) вместо hardcoded `"${row.valueCount} values"`.

Так же убраны из опубликованной структуры элементы, которые больше не существуют (parity с реальным кодом):

- `scope_slot` (включая `global_option` / `per_dict_option` / `per_dict_picker`) — удалён.
- Строки `_picker_label` / `_scope_global` / `_scope_per_dict` упоминались в исходном `ui_layout.md` (`structure:`); в спеке заменены на `components_button_create` / `components_button_save` где применимо. Snackbar / Error retry строки описаны как `components_*_load_failed` / `components_error_retry`.
- F167 (iter 3): добавлен `DatasourceEffect.LoadAllUserDefinedTypes` / `.LoadComponentsForDictionary` в spec sealed, IO-note обновлён (Retry flow re-subscribe).
