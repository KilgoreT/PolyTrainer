# Review: ui_implement.md

## Итерация 1 (2026-06-17T18:00:00-06:00)

### PASS [architect]

### F161 [senior] critical

**Description:** `PerDictRowWidget.kt:114` hardcode `"${row.valueCount} values"` без string resource — localization bug.

**Status:** approved

**Verdict:** Заменить на `stringResource(R.string.per_dict_row_value_count, row.valueCount)` (с plurals если применимо). Добавить strings в en + ru-rRU.

### F162 [senior] critical

**Description:** `LexemeRadioMenuItem` (DropdownMenuItem обёртка) использован в CreateComponentDialog внутри Column диалога — material3 semantic misuse. a11y-tree role=menuitem вне меню.

**Status:** approved

**Verdict:** Заменить на `Row` + `RadioButton` + `Text` либо новый primitive `LexemeRadioRow` в `:modules:core:ui` (без DropdownMenuItem). Применить в обоих CM/PerDict CreateComponentDialog.

### F163 [senior] minor

**Description:** `TypesLoadFailed` → `isLoading=false, rows=null` — все три ветки `when` мимо → пустой Box. UX recovery невозможен без kill-restart.

**Status:** approved

**Verdict:** Добавить error state branch в Screen: показать error message + Retry button (dispatch `Msg.Retry` либо аналог; если Msg.Retry не существует — добавить либо использовать снова `OpenScreen` Init). Альтернатива: показать pre-existing ComponentsEmptyStateWidget с error variant.

### F164 [senior] minor

**Description:** AssistChip closing-paren indent сдвинут.

**Status:** rejected

**Verdict:** Стилистика, ktlint/detekt не ловит — out of scope.

## Итоги итерации 1

- **Approved:** 2 critical (F161, F162) + 1 minor (F163). 1 rejected (F164).
- **Решение:** есть approved critical → reset streak, repeat iter 2.
