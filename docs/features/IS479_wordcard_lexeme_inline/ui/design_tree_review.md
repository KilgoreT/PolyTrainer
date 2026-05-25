# design_tree review

## Итерация 1 (2026-05-23T04:44:38-0600)

### F001 [architect] critical

**Description:** design_tree удаляет `LexemeValueFieldWidget.titleRes` без зарегистрированного project_decision — ui_layout явно перечисляет titleRes в params, а UX-обоснование «дубль title+chip» — это новое решение, которое должно сначала попасть либо в project_decisions, либо в ui_layout (его параметр titleRes снять), иначе artifact'ы остаются рассинхронизированными.

**Status:** rejected

**Verdict:** design_tree явно фиксирует drift в notes #4 и log_messages с UX-обоснованием «дубль title+chip», а декларация project_decisions — артефакт ui_layout-шага, не design_tree; зафиксированный drift достаточен для синхронизации.

### F002 [architect] critical

**Description:** SubentityChip/LexemeMeaningField сигнатуры расходятся с ui_layout — там `kind: SubentityKind` (Translation/Definition), в design_tree `labelRes: Int`; смена доменной модели на @StringRes теряет семантический тип kind и не отражена в project_decisions.

**Status:** rejected

**Verdict:** `@StringRes labelRes` — это техническая форма передачи доменного kind (Translation/Definition) в Compose-виджет через ресурс-ключи, доменный тип не теряется, маппинг kind→labelRes тривиально делает call-site в LexemeItemWidget; смены доменной модели нет.

### F003 [architect] minor

**Description:** LazyColumn → Column drift в #9 зафиксирован в log_messages, но обоснование «LazyColumn не работает внутри scrollable» — backlog-cleanup без project_decision; стоит либо завести decision, либо явно отметить как известный долг в ui_layout.

**Status:** rejected

**Verdict:** drift LazyColumn→Column явно зафиксирован в log_messages и в #9 как backlog-cleanup с обоснованием (parent verticalScroll конфликтует с LazyColumn) — этого достаточно на уровне design_tree.

### F004 [architect] minor

**Description:** иконка DeleteLexemeButton оставлена «выбор в impl» (ic_circle_delete | ic_close), хотя ui_layout указывает `ic_trash (или эквивалент)` — лучше зафиксировать конкретный drawable на этапе design_tree, чтобы impl не принимал UX-решение.

**Status:** approved

**Verdict:** design_tree должен фиксировать конкретный drawable, а не оставлять «выбор в impl» между ic_circle_delete и ic_close — это UX-решение, утечка которого в impl противоречит ответственности шага.

### F005 [architect] minor

**Description:** AddLexemeWidget.enabled реализуется через alpha+no-op (M3 FAB не имеет native enabled) — это поведенческое отклонение от ui_layout (`enabled: Boolean` подразумевает стандартный disabled), стоит зафиксировать в notes ui_layout либо как project_decision.

**Status:** approved

**Verdict:** M3 FloatingActionButton не имеет нативного enabled, а реализация через alpha+no-op onClick — поведенческое отклонение от ui_layout (`enabled: Boolean`), которое должно быть зафиксировано как drift/decision в design_tree, а не оставлено замечанием в pseudocode.

## Итерация 2 (2026-05-23T04:55:00-0600)

### PASS [architect]

F004 (DeleteLexemeButton icon = ic_trash) и F005 (AddLexeme.enabled через alpha+no-op как behavioral DRIFT) закрыты. Артефакт принят.
