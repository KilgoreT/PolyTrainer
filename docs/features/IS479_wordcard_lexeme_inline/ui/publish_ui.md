# publish_ui

## Опубликовано
- `docs/features-spec/wordcard.md` — обновлён (вставлен раздел `## UI Layout` с ссылкой между `## UI Messages` и `## IO`)
- `docs/features-spec/wordcard-ui.md` — создан (489 строк)
- Режим: split-to-ui-file
- Раздел: `## UI Layout`
- Размер раздела: 489 строк (вынесен в `wordcard-ui.md`, в основной спеке — только ссылка)

## Корректировки от implement

UI Layout опубликован с применением корректировок от ит.1 / ит.2 implement (`impl.md`):

- **SubentityChip Active (ит.2 F002).** ui_layout.md описывал Active-state базовым `M3 InputChip`. В реализации InputChip заменён на `Surface(shape=RoundedCornerShape(999.dp), color=primary, contentColor=onPrimary) + Row + IconBoxed(ic_close)` — причина: InputChip имеет встроенный ripple на теле chip'а, требовалось «тело без клика, кликается только trailing ✕». Pill `RoundedCornerShape(999.dp)` сохранён per project_decision `subentity_chip_pill`. Active label → `MaterialTheme.typography.labelSmall` (Body XS 11sp Medium). Отражено в блоке `<SubentityChip>` в `type`, `slots/content` (Active), `notes` (🚨 ит.2 F002), `behavior` (Active body НЕ кликабелен), а также в `## ❇️ НОВЫЕ ВИДЖЕТЫ`. Источник: `impl.md` § «Ит.2 (closing F001/F002/F003)».

- **AddLexemeWidget — DRIFT FAB enabled (design_tree #8).** ui_layout.md описывал `enabled: Boolean` как обычный параметр. В реализации M3 `FloatingActionButton` не имеет нативного `enabled` параметра — реализовано через `Modifier.alpha(if (enabled) 1f else 0.38f)` + no-op `onClick = { if (enabled) onAddLexeme() }`. Реальный disabled FAB вынесен в backlog (отдельная задача через core/ui обёртку). Отражено в блоке `<AddLexeme>` (notes 🚨 DRIFT design_tree #8, behavior, params комментарий) и в `## 🔧 МЕНЯЕМ`. Источник: `impl.md` § «#8 AddLexemeWidget.kt».

- **LexemeValueFieldWidget — удалён `titleRes` (ит.1 #4).** ui_layout.md перечислял параметр `titleRes: Int (@StringRes)` и `Text` заголовок над chip-вью. В реализации (ит.1 #4) заголовок переехал в chip-Active внутри `LexemeMeaningField` — параметр `titleRes` и `Text`-заголовок удалены. Локализационные ключи `word_card_bottom_translation/_definition` теперь применяются на уровне `LexemeMeaningField.labelRes`. Отражено в блоке `<LexemeValueField>`: `params` (без `titleRes` + явный комментарий о его удалении), `slots/content` (без content[0] = Text title), `colors`/`typography` (без секции title), `notes` (ℹ️ про ключи `word_card_bottom_*`). В блоке `<LexemeMeaningField>` добавлен `labelRes` в `params`. Источник: `impl.md` § «#4 LexemeValueFieldWidget.kt».

- **LexemeMeaningField — `onDeactivate` маппится на `onCancelEdit` (ит.1 #5).** ui_layout.md описывал отдельный `onDeactivate: () -> Unit` callback для ✕ chip-Active. В реализации `onDeactivate` chip-Active маппится на существующий `Msg.CancelTranslationEdit / CancelDefinitionEdit` — отдельного `Msg.Deactivate*` нет. Reducer для свежесозданной субсущности (origin пустой) трактует Cancel как nullify, что эквивалентно «MeaningField уничтожается, chip возвращается в FlowRow, value сбрасывается». Отражено в блоках `<SubentityChip>` (callbacks state=Active) и `<LexemeMeaningField>` (params, callbacks, notes 🚨). Источник: `impl.md` § «#5 LexemeMeaningField.kt», «Нетривиальные решения #2».

- **LexemeValueFieldWidget — cursor preservation (ит.2 F001).** ui_layout.md упоминал `LaunchedEffect(value)` re-sync TextFieldValue. В реализации (ит.2 F001) `LaunchedEffect` дополнительно сохраняет старый cursor offset через `selection = TextRange(minOf(oldOffset, value.length))` — курсор больше не прыгает в конец при parent-трансформации ввода. Отражено в блоке `<LexemeValueField>.behavior`. Источник: `impl.md` § «Ит.2 (closing F001)».

- **WordCardScreen — Column вместо LazyColumn (ит.1 #9 + ит.4 F007).** ui_layout.md описывал внешний контейнер списка лексем как `⚙️ LazyColumn (× N)` per ит.4 F007. В реализации (ит.1 #9) оставлен обычный `Column` — LazyColumn зафиксирован как DRIFT в backlog. Отражено в `🗺 Карта экрана` (`⚙️ Column (× N)` с пояснением «DRIFT vs LazyColumn, backlog») и в `## 🔧 МЕНЯЕМ` (явно про DRIFT). Источник: `impl.md` § «#9 WordCardScreen.kt».

- **LexemeItemWidget — `order: Int` с @Suppress (ит.1 #7).** ui_layout.md перечислял `order: Int` в `params` без пояснений. В реализации параметр сохранён в сигнатуре per design_tree #7 (future a11y / debug) с `@Suppress("UNUSED_PARAMETER")` — UI его не использует. Отражено в блоке `<LexemeItem>.params` (явный комментарий о @Suppress). Источник: `impl.md` § «#7 LexemeItemWidget.kt», «Нетривиальные решения #1».

- **LexemeChipPlaceholderWidget удалён (ит.1 #3).** ui_layout.md в § «❌ УДАЛЯЕМ» этого пункта не было — `LexemeChipPlaceholderWidget` упоминался только косвенно через расширение до `SubentityChip`. В реализации (ит.1 #3) виджет полностью удалён, единственный потребитель (`LexemeItemWidget`) переведён на `SubentityChip`. Добавлен явный bullet в `## ❌ УДАЛЯЕМ (с миграцией)` про удаление `LexemeChipPlaceholderWidget`. Источник: `impl.md` § «#3 LexemeChipPlaceholderWidget.kt».

- **DeleteLexemeButton — `ic_trash` (ит.1 #6).** ui_layout.md в § «🖼 ИКОНКИ К ИМПОРТУ» имел запись `ic_trash (или эквивалент)`. В реализации использована точно `ic_trash`. Уточнено в § «🖼 ИКОНКИ К ИМПОРТУ» (без `или эквивалент`) и в блоке `<DeleteLexemeButton>.slots/content.leading`. Источник: `impl.md` § «#6 DeleteLexemeButton.kt».

- **SubentityChip Placeholder — SuggestionChip default container (ит.1 #2).** ui_layout.md указывал Placeholder background = hex `#F5F3F8` (fill_R87IJC) — «нет токена, кандидат на добавление в theme». В реализации использован дефолтный SuggestionChip container (M3 token), а не сырой hex. Отражено в `<SubentityChip>.colors` (Placeholder background: «SuggestionChip default container, не Figma fill_R87IJC»). Источник: `impl.md` § «#2 SubentityChip.kt».

## log_messages
- Раздел `## UI Layout` вставлен в `docs/features-spec/wordcard.md` между `## UI Messages` (после её содержимого) и `## IO`. Канонический порядок секций спеки сохранён.
- Из-за размера (489 строк > 500 порога с запасом близко к нему — фактически содержимое вынесено в split-файл, как и предписано режимом split-to-ui-file для крупных UI Layout) UI Layout вынесен в отдельный `docs/features-spec/wordcard-ui.md`; в основной спеке оставлена ссылка.
- В `wordcard-ui.md` применены 9 корректировок от ит.1 / ит.2 implement (см. § «Корректировки от implement»).
- Канонический порядок H2-секций спеки проверен и сохранён: Бизнес-описание → User Stories → State → UI Messages → **UI Layout** (новый) → IO → UseCase → Тестовые сценарии.
- Другие разделы спеки (`## State`, `## UI Messages`, `## IO`, `## UseCase`, `## Тестовые сценарии`) не трогались per правилу шага «НЕ править разделы кроме `## UI Layout`».

_model: claude-opus-4-7[1m]_
