# ui_layout review

## Итерация 1 (2026-05-23T03:12:45-0600)

### F001 [architect] critical

**Description:** В Карте экрана `LexemeItemWidget` содержит `LexemeTitleWidget` и `Surface(Card)` как прямых детей контейнера-обёртки, но в Анализе виджета его `type` указан как `M3 Surface (Card) + outer Column` — Карта показывает только один Surface (внутренний), внешний Surface(Card) из Анализа в Карте отсутствует, рассинхрон структуры.

**Status:** approved

**Verdict:** Карта не показывает outer Column из анализа — структурный рассинхрон между map и Анализом.

### F002 [architect] critical

**Description:** В Карте экрана узел `🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗜𝘁𝗲𝗺𝗪𝗶𝗱𝗴𝗲𝘁>` имеет первого ребёнка `🔄 <𝗟𝗲𝘅𝗲𝗺𝗲𝗧𝗶𝘁𝗹𝗲𝗪𝗶𝗱𝗴𝗲𝘁>` и второго `⚙️ Surface (Card)` — между ними нет общего Column-родителя, но в Анализе явно указан "outer Column (заголовок + тело)"; структура Карты не отражает контейнер.

**Status:** approved

**Verdict:** В Карте LexemeTitleWidget и Surface(Card) поданы как прямые дети LexemeItemWidget без общего Column-родителя, противоречит анализу "outer Column (заголовок + тело)".

### F003 [architect] minor

**Description:** `LexemeValueFieldWidget.titleRes` в Анализе указывает на ключ `word_card_bottom_translation` — суффикс `_bottom` остался от удалённого bottom-sheet-механизма, ресурс-имя протекает контекст устаревшего паттерна, который фича как раз убирает.

**Status:** approved

**Verdict:** Scope явно упоминает ревизию ключей word_card_bottom_*, артефакт использует их без 🚨/ℹ️-пометки об устаревании контекста.

### F004 [architect] minor

**Description:** `LexemeTitleWidget.source` помечен как «проектное решение», но в `notes` ссылается на Figma `9154:82519` (items + button «Удалить») — формально это виджет с Figma-источником и отходом, корректнее `source: figma 9154:82519` + 🚨 в notes (уже стоит), либо явно зафиксировать что Figma-источника нет.

**Status:** approved

**Verdict:** Per § 4 source "проектное решение" допустим только для ❇️-виджетов; LexemeTitleWidget — 🔄 с Figma-референсом в notes, должен иметь figma source.

### F005 [architect] minor

**Description:** Иконка `ic_more_horizonral` (опечатка `horizonral` вместо `horizontal`) фигурирует и в Анализе `LexemeTitleWidget`, и в секции «ИКОНКИ К ИМПОРТУ» — имя ресурса вне скоупа IS479, но протекает в артефакт как канон; стоит отметить опечатку как ℹ️ или зафиксировать в Backlog.

**Status:** rejected

**Verdict:** Имя ресурса вне скоупа IS479, артефакт корректно цитирует существующее имя — это не проёб шага.

## Итерация 2 (2026-05-23T03:26:59-0600)

### PASS [architect]

Все 4 approved findings ит.1 (F001, F002, F003, F004) закрыты. F005 rejected — не трогался. Артефакт принят.

## Итерация 3 (2026-05-23T03:35:28-0600)

### F006 [user-manual] critical

**Description:** AddLexemeWidget зафиксирован как inline-кнопка в Column (не в `Scaffold.floatingActionButton`), с придуманным обоснованием "inline-механика IS479". Это противоречит project_decision #4 из plan.context (`fab_scaffold_slot`: "AddLexeme — FAB icon-only в Scaffold.floatingActionButton"), Figma `9154:82532` (icon-only FAB) и промпту шага (артефакт = финальное состояние ПОСЛЕ реализации фичи, не текущий код). Sub-agent зафиксировал состояние **текущего кода** (где AddLexemeWidget ещё inline) вместо **целевого финала** (FAB в Scaffold-slot). Architect не поймал.

**Status:** approved

**Verdict:** Явное нарушение project_decision из контекста + неправильная интерпретация семантики артефакта (snapshot финального состояния, не текущего кода).
