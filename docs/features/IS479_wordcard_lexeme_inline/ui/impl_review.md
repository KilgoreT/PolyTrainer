# impl review

## Итерация 1 (2026-05-23T13:39:31-0600)

### PASS [architect]

Все 12 архитектурных проверок пройдены.

### F001 [senior] minor

**Description:** `LexemeValueFieldWidget.EditRow` синхронизирует `fieldValue` через `LaunchedEffect(value)` без сравнения по `selection` — если parent трансформирует ввод (trim/normalize), курсор прыгнет в конец из-за `TextRange(value.length)`. Сейчас reducer не трансформирует, но контракт хрупкий.

**Status:** approved

**Verdict:** Реальная хрупкость контракта в зоне шага implement — LaunchedEffect(value) без учёта selection приведёт к скачку курсора при любой трансформации ввода parent'ом.

### F002 [senior] minor

**Description:** `SubentityChip` state=Active использует `InputChip(onClick = {})` — chip остаётся кликабельным (ripple срабатывает) при тапе по телу без действия. Для пользователя визуально неотличимо от disabled-кнопки. Стоит либо отключить ripple, либо документировать что вся интерактивность только через ✕.

**Status:** approved

**Verdict:** onClick = {} оставляет ripple на Active-чипе, что создаёт ложный аффорданс кликабельности при отсутствии действия — это UX-дефект реализации в зоне шага.

### F003 [senior] minor

**Description:** `@file:OptIn(ExperimentalMaterial3Api::class)` в `SubentityChip.kt` и `LexemeValueFieldWidget.kt` — `InputChip`/`SuggestionChip` уже стабильны в текущей версии M3, opt-in может быть лишним.

**Status:** approved

**Verdict:** Лишние OptIn-аннотации — мусор в реализованном артефакте, проверить и убрать если компилятор не требует — в зоне ответственности шага implement.

### F004 [senior] minor

**Description:** `@Suppress("UNUSED_PARAMETER") order: Int` в `LexemeItemWidget` — параметр объявлен ради будущего a11y/debug, но сейчас мёртвый.

**Status:** rejected

**Verdict:** Параметр order оставлен по закрытому project_decision для будущего a11y — documented decision, удалять/доделывать сейчас out-of-scope.

## Итерация 2 (2026-05-23T13:58:46-0600)

### PASS [architect]

Все архитектурные проверки пройдены.

### PASS [senior]

F001 (cursor offset preserved), F002 (Surface+Row без ripple), F003 (@OptIn убран) — закрыты корректно. Best practices, leaks, lifecycle, naming — без замечаний.
