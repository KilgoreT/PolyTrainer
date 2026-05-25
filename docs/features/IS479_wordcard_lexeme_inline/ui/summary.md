---
status: done
---

# Summary — UI sub-flow (IS479 wordcard lexeme inline)

UI sub-flow закрыл переход карточки слова на inline-механику добавления лексемы:
bottom-sheet удалён (business sub-flow), визуальный layout приведён к Figma
`9154:82509` — FAB в Scaffold-слоте, единый Surface(Card) лексемы с FlowRow
placeholder-chip'ов и footer-кнопкой удаления, двусостоятельный `SubentityChip`
(Placeholder / Active) + проектная обёртка `LexemeMeaningField`. Все 14 узлов
design_tree применены, ⋮-меню инфраструктура лексемы вычищена, тесты модуля
113/113 PASS, компиляция `:modules:screen:wordcard` восстановлена.

Контракт sub-flow выполнен. Реализация уложилась в UI-слой как и предполагалось
`02_scope.md` — `mate/*`, `WordCardViewModel.kt`, `WordCardUseCaseImpl.kt`,
`mate/UiEffectHandler.kt [-]` и удалённая директория `widget/addlexeme/*` были
закрыты business sub-flow до старта UI и явно вне scope UI (см. `business/summary.md`).

## Что сделано

### Артефакт ui_layout (`ui/ui_layout.md`, 501 строка, 5 итераций)

Финальный UI snapshot карточки слова после IS479. Источники: Figma frame
`9154:82509` (через `../figma_dump.json`), `02_scope.md`, `business/summary.md`,
текущий код виджетов.

Зафиксированы:
- **Карта экрана**: `Scaffold(topBar, snackbarHost, floatingActionButton = AddLexeme)`
  → `Box(imePadding+navigationBarsPadding)` → `Column(verticalScroll)` →
  `WordFieldWidget` + `Spacer(8)` + `LazyColumn(× N) LexemeItemWidget`. Внутри
  `LexemeItemWidget` (единый Surface/Card): `∀ active LexemeMeaningField` →
  `FlowRow placeholder SubentityChip` → `DeleteLexemeButton` footer.
- **Анализ виджетов** (9 блоков): TopBar (out-of-scope), WordField (out-of-scope),
  LexemeItem, DeleteLexemeButton, SubentityChip, LexemeMeaningField,
  LexemeValueField, AddLexeme (FAB), ConfirmDeleteWord (out-of-scope).
- **НОВЫЕ ВИДЖЕТЫ**: `SubentityChip`, `LexemeMeaningField`, `DeleteLexemeButton`.
- **МЕНЯЕМ**: `AddLexemeWidget` (Button → FAB), `LexemeItemWidget` (структура),
  `LexemeValueFieldWidget` (chip+input), `WordCardScreen` (FAB-slot).
- **УДАЛЯЕМ (с миграцией)**: `AddLexemeBottomWidget`, `LexemeMeaningWidget`,
  `ActionsWidget`, `UiEffectHandler`, `PrimaryLongFabWidget`+`LexemeLongFab`,
  `AddLexemeBottomState`+ext-функции, 4 Msg-варианта, `LexemeTitleWidget` +
  3 menu-item.
- **2 DRIFT (out-of-scope)**: TopBar (вариант "with btn" Figma не применён),
  WordField (tui-input → проектный LexemeEditableText).

5 итераций ui_layout (F001..F008): синхронизация Карты и Анализа
LexemeItemWidget (F001/F002), пометка о `word_card_bottom_*` ключах (F003),
fix `LexemeTitleWidget.source` (F004), возврат FAB в Scaffold-слот (F006),
LazyColumn в Карте (F007), добавление `LexemeMeaningField` как самостоятельного
блока (F008), ручной перепис LexemeItem по UX-задумке example (ит.5).

### Артефакт design_tree (`ui/design_tree.md`, 14 узлов, 2 итерации)

DAG из 14 узлов с детальным описанием каждого изменения (pseudocode, сигнатуры,
ссылки на guide):

```
[~] #0  core/core-resources/.../values/strings.xml         (word_card_lexeme_remove EN)
[~] #1  core/core-resources/.../values-ru-rRU/strings.xml  (word_card_lexeme_remove RU)
[+] #2  modules/screen/wordcard/.../widget/lexeme/SubentityChip.kt
[-] #3  modules/screen/wordcard/.../widget/lexeme/LexemeChipPlaceholderWidget.kt
[~] #4  modules/screen/wordcard/.../widget/lexeme/LexemeValueFieldWidget.kt
[+] #5  modules/screen/wordcard/.../widget/lexeme/LexemeMeaningField.kt
[+] #6  modules/screen/wordcard/.../widget/lexeme/DeleteLexemeButton.kt
[~] #7  modules/screen/wordcard/.../widget/lexeme/LexemeItemWidget.kt
[~] #8  modules/screen/wordcard/.../widget/AddLexemeWidget.kt
[~] #9  modules/screen/wordcard/.../WordCardScreen.kt
[-] #10 modules/screen/wordcard/.../widget/lexeme/LexemeTitleWidget.kt
[-] #11 modules/screen/wordcard/.../widget/lexeme/AddTranslationLexemeMenuItem.kt
[-] #12 modules/screen/wordcard/.../widget/lexeme/AddDefinitionLexemeMenuItem.kt
[-] #13 modules/screen/wordcard/.../widget/lexeme/DeleteLexemeMenuItem.kt
```

5 групп параллелизма (корни → SubentityChip-зависимые → LexemeItemWidget reshape
→ WordCardScreen FAB-slot → cleanup LexemeTitleWidget+menu-items). Циклов нет.

Зафиксированы:
- **Out-of-scope блок**: `mate/*`, `WordCardViewModel.kt`, `WordCardUseCaseImpl.kt`,
  `mate/UiEffectHandler.kt [-]`, удалённая `widget/addlexeme/*` — всё уже закрыто
  business sub-flow до старта UI.
- **`PrimaryLongFabWidget.kt` НЕ удаляется** — `AddLexemeWidget` использует M3
  `Button` напрямую (не `PrimaryLongFabWidget`); `LexemeLongFab` имеет второго
  потребителя (`core/ui/.../text/LexemeEditableText.kt`). Backlog cleanup.
- **DRIFT ui_layout → design_tree**: `LazyColumn × N` сохранён как обычный
  `Column` (внутри `verticalScroll`); `LexemeValueFieldWidget.titleRes` удалён
  (label переехал в chip-Active заголовок SubentityChip внутри LexemeMeaningField).

2 итерации design_tree: F004 (`R.drawable.ic_trash` зафиксирован в #6),
F005 (FAB `enabled` через `alpha(0.38f)` + no-op `onClick` оформлен как явный
DRIFT в #8).

### Артефакт impl (`ui/impl.md`, 14 узлов, 2 итерации, тесты PASS 113/113)

Реализованы все 14 узлов design_tree:

- **`SubentityChip.kt` [+]**: единый виджет с `SubentityChipState { Placeholder, Active }`.
  Placeholder — `SuggestionChip`. Active — `Surface(shape=RoundedCornerShape(999), color=primary)
  + Row(padding start=6/top=2/end=2/bottom=2, spacing=2) + IconBoxed(ic_close, onClick=onDeactivate)`
  (после ит.2 F002 — InputChip заменён на Surface+Row, чтобы тело chip'а не было
  кликабельно, только trailing ✕). Label Active → `MaterialTheme.typography.labelSmall`.
  4 превью.

- **`LexemeMeaningField.kt` [+]**: `Column(spacedBy(4.dp))` обёртка над chip-Active
  (`SubentityChip` state=Active) и `LexemeValueFieldWidget`. `onDeactivate` chip-Active
  маппится на `onCancelEdit` (Msg.CancelTranslationEdit/CancelDefinitionEdit) —
  reducer для свежесозданной субсущности (origin пустой) трактует Cancel как nullify.
  3 превью (view-mode, edit-mode, disabled).

- **`DeleteLexemeButton.kt` [+]**: `TextButton(contentColor=onError, contentPadding=4dp horizontal/0dp vertical)
  + Icon(ic_trash, 16dp) + Spacer(4) + Text(word_card_lexeme_remove, LexemeStyle.BodyS)`.
  1 параметризованный preview (BoolParam enabled).

- **`LexemeValueFieldWidget.kt` [~]**: удалён параметр `titleRes` и Text-заголовок
  (label переехал в chip-Active внутри LexemeMeaningField). View-mode — `InputChip
  + IconBoxed(ic_circle_delete, onRemove)`. Edit-mode — `BasicTextField + IconBoxed(ic_confirm)
  + IconBoxed(ic_close)` с FocusRequester. Ит.2 F001 — `LaunchedEffect(value)`
  сохраняет cursor offset через `selection = TextRange(minOf(oldOffset, value.length))`,
  курсор не прыгает в конец при parent-трансформации ввода.

- **`AddLexemeWidget.kt` [~]**: M3 `Button` (inline label+icon) → M3
  `FloatingActionButton` icon-only. **DRIFT**: `enabled` через `Modifier.alpha(
  if (enabled) 1f else 0.38f)` + `onClick = { if (enabled) onAddLexeme() }`
  (FAB не имеет нативного `enabled`). Реальный disabled FAB — backlog
  (кастомная обёртка в `core/ui`).

- **`LexemeItemWidget.kt` [~]**: переписан в единый `Surface(Card, RoundedCornerShape 12,
  shadow=4, color=whiteColor) → Column(padding=16, spacing=12)`: (1) `state.translation?.let {
  LexemeMeaningField(translation) }`, (2) `state.definition?.let { LexemeMeaningField(definition) }`,
  (3) `if (placeholders) FlowRow { SubentityChip(Placeholder) }`, (4) `DeleteLexemeButton`
  footer. `order: Int` сохранён в сигнатуре с `@Suppress("UNUSED_PARAMETER")`
  (future a11y/debug). Удалены: `LexemeTitleWidget`, `LexemeChipPlaceholderWidget`,
  `HorizontalDivider`, импорты `word_card_bottom_*`. 3 превью.

- **`WordCardScreen.kt` [~]**: добавлен слот `Scaffold.floatingActionButton = {
  if (loaded != null) AddLexemeWidget(enabled = !state.isPendingDbOp && !state.isCreatingLexeme,
  onAddLexeme = { sendMessage(Msg.CreateLexeme) }) }`. Удалены из основной
  `Column`: `Spacer(height=16dp)` + inline `AddLexemeWidget(fillMaxWidth)`.
  Внутренний `Column` со списком лексем сохранён (LazyColumn — DRIFT, backlog).

- **6 удалённых файлов**: `LexemeChipPlaceholderWidget.kt`, `LexemeTitleWidget.kt`,
  `AddTranslationLexemeMenuItem.kt`, `AddDefinitionLexemeMenuItem.kt`,
  `DeleteLexemeMenuItem.kt`. Импорты переключены либо удалены каскадом.

- **strings.xml × 2 [~]**: добавлены ключи `word_card_lexeme_remove` (EN: "Remove",
  RU: "Удалить"). Старые `word_card_bottom_*` ключи оставлены (cleanup в backlog).

**Тесты**: `./gradlew :modules:screen:wordcard:testDebugUnitTest --console=plain` →
**113 PASS / 0 FAIL / 100%**. Тесты не правились (UI-changes не затрагивают
reducer/state контракты). Exit code = 0 → компиляция модуля
`:modules:screen:wordcard` восстановлена.

2 итерации impl: ит.2 закрыты F001 (cursor preservation), F002 (SubentityChip
Active → Surface+Row без ripple), F003 (@file:OptIn убран из обоих файлов —
M3 1.3.2 более не требует). F004 (`@Suppress("UNUSED_PARAMETER") order: Int`)
оставлен как rejected. Регресс-чек: `compileDebugKotlin` + `compileDebugUnitTestKotlin`
зелёные, EXIT=0.

### Артефакт publish_ui (`ui/publish_ui.md`, режим split-to-ui-file)

UI Layout опубликован в публичную спеку:
- `docs/features-spec/wordcard.md` — вставлен раздел `## UI Layout` (ссылка на
  split-файл) между `## UI Messages` и `## IO`. Канонический порядок H2-секций
  спеки сохранён: Бизнес-описание → User Stories → State → UI Messages →
  **UI Layout** (новый) → IO → UseCase → Тестовые сценарии.
- `docs/features-spec/wordcard-ui.md` — создана (489 строк, вынесена из основной
  спеки per режим split-to-ui-file для крупных UI Layout).

**9 корректировок от implement** применены при публикации (ui_layout → wordcard-ui.md):
1. SubentityChip Active: InputChip → Surface+Row+IconBoxed (ит.2 F002).
2. AddLexemeWidget DRIFT FAB enabled через alpha+no-op (design_tree #8).
3. LexemeValueFieldWidget: удалён `titleRes` (ит.1 #4) — label переехал в LexemeMeaningField.
4. LexemeMeaningField: `onDeactivate` маппится на `onCancelEdit` (ит.1 #5).
5. LexemeValueFieldWidget: cursor preservation (ит.2 F001).
6. WordCardScreen: Column вместо LazyColumn (ит.1 #9, DRIFT, backlog).
7. LexemeItemWidget: `order: Int` с `@Suppress("UNUSED_PARAMETER")` (ит.1 #7).
8. LexemeChipPlaceholderWidget удалён (ит.1 #3) — явный bullet в § УДАЛЯЕМ.
9. DeleteLexemeButton: `ic_trash` зафиксирован (ит.1 #6) — без «или эквивалент».

Другие разделы спеки (`## State`, `## UI Messages`, `## IO`, `## UseCase`,
`## Тестовые сценарии`) не трогались per правилу шага publish_ui.

## Ключевые решения

### 7 project_decisions из ui_layout.md

1. **`fab_scaffold_slot`** — AddLexeme размещается в `Scaffold.floatingActionButton`
   как M3 FAB icon-only (Figma `9154:82532`). Эквивалентно абсолютной позиции
   Figma x=340/y=720 с учётом window insets. См. `ui/ui_layout.md` блок `<AddLexeme>`,
   секция МЕНЯЕМ; impl `impl.md` § #8.

2. **`subentity_chip_pill`** — `borderRadius=999` (pill) для ОБОИХ state'ов
   `SubentityChip` (Placeholder + Active). В Figma `9154:82521` active=6,
   placeholder=999 — разные. Причина: UX consistency, один визуальный паттерн.
   См. `ui/ui_layout.md` блок `<SubentityChip>` notes.

3. **`subentity_chip_single_widget`** — один Compose-виджет с param `state`,
   не два разных компонента (в Figma это active componentId=4632:99285 + placeholder
   componentId=28370:397707). Единое UX-явление «значение нет → значение есть».
   См. `ui/ui_layout.md` блок `<SubentityChip>` notes.

4. **`lexeme_meaning_field`** — проектная обёртка-Column над chip-заголовком
   и `LexemeValueField`. Нет аналога в Figma — UX-решение проекта. Один MeaningField =
   одна active суб-сущность. См. `ui/ui_layout.md` блок `<LexemeMeaningField>`;
   design_tree #5.

5. **Исключение «Пример»** — subentity «Пример» (Figma `9154:82523`) исключён
   фичей полностью; placeholder не создаётся. См. `ui/ui_layout.md` ЧТО ДЕЛАЕМ.

6. **Stroke в edit-mode** — выбран `MaterialTheme.colorScheme.outline` /
   `outlineVariant` (либо без рамки) вместо Figma `strokes=fill_QMOUFY (#FFFFFF)`
   (нулевой контраст). См. `ui/ui_layout.md` блок `<LexemeValueField>` notes.

7. **`ic_close` единый close-canon** — для trailing ✕ Active-chip переиспользована
   существующая `ic_close` вместо Figma `ic_close_rounded` (componentId=9163:40871) —
   единый close-canon в проекте. См. `ui/ui_layout.md` блок `<SubentityChip>` slots.

### 2 DRIFT'а из design_tree

- **LazyColumn → Column** (`#9 WordCardScreen.kt`): ui_layout описывал `LazyColumn (× N)`,
  в impl оставлен обычный `Column` внутри `verticalScroll(rememberScrollState())`.
  Переход на LazyColumn потребует убрать parent vertical scroll — backlog cleanup.
  См. `ui/design_tree.md` § log_messages; impl `impl.md` § #9.

- **`LexemeValueFieldWidget.titleRes` удалён** (`#4`): ui_layout перечислял
  параметр `titleRes` и `Text` заголовок над chip-вью. В реализации (ит.1 #4)
  заголовок переехал в chip-Active внутри `LexemeMeaningField` — параметр удалён,
  устранён визуальный дубль title + chip. Локализационные ключи `word_card_bottom_*`
  применяются на уровне `LexemeMeaningField.labelRes`. См. `ui/design_tree.md` #4;
  impl `impl.md` § #4.

### Корректировки impl (ит.2 + DRIFT'ы)

- **F001 (LexemeValueFieldWidget cursor preservation)**: `LaunchedEffect(value)`
  сохраняет старый cursor offset перед обновлением — `selection = TextRange(
  minOf(oldOffset, value.length))`. См. `impl.md` Ит.2.
- **F002 (SubentityChip Active без ripple)**: InputChip заменён на `Surface(shape=
  RoundedCornerShape(999), color=primary, contentColor=onPrimary) + Row + IconBoxed`.
  Тело chip'а не кликабельно — только trailing ✕. См. `impl.md` Ит.2.
- **F003 (`@file:OptIn` cleanup)**: убран из обоих `SubentityChip.kt` и
  `LexemeValueFieldWidget.kt` — `SuggestionChip` / `InputChip` стабильны в
  M3 1.3.2, OptIn не требуется. См. `impl.md` Ит.2.
- **DRIFT FAB enabled**: `Modifier.alpha(0.38f)` + no-op `onClick` (FAB не имеет
  нативного `enabled`). Реальный disabled — backlog (кастомная FAB-обёртка в `core/ui`).
  См. `design_tree.md` #8; `impl.md` § #8.
- **`order: Int` с `@Suppress("UNUSED_PARAMETER")`**: сохранён в сигнатуре
  `LexemeItemWidget` для future a11y/debug per design_tree #7. См. `impl.md`
  Нетривиальные решения #1.
- **`onDeactivate` Active-chip = `onCancelEdit`**: в Msg-наборе (business
  contract v3.2) нет отдельного Msg.Deactivate*; reducer для свежесозданной
  субсущности (origin пустой) трактует Cancel как nullify — UX совпадает.
  См. `impl.md` Нетривиальные решения #2.

### Что вне scope этого sub-flow

- **`mate/*`, `WordCardViewModel.kt`, `WordCardUseCaseImpl.kt`,
  `mate/UiEffectHandler.kt [-]`, `widget/addlexeme/* [-]`** — всё закрыто
  business sub-flow до старта UI (см. `business/summary.md`).
- **TopBar (`9154:82531`) "with btn" вариант** — устоявшийся отход проекта,
  закрытие/комит word edit через close-icon в `LexemeEditableText`. DRIFT,
  не закрывается в IS479.
- **WordField (`9158:72107`) tui-input componentId=4620:57849** — точечная
  подгонка типографики / структуры под Figma вне scope IS479. DRIFT.
- **`PrimaryLongFabWidget.kt` + `LexemeLongFab`** — не мёртвый код в текущем
  виде (есть второй потребитель `LexemeEditableText`), backlog cleanup-задача.
- **Старые ключи `word_card_bottom_title`, `word_card_bottom_title_append`** —
  unused после удаления `AddLexemeBottomWidget`; cleanup в backlog.
- **`word_card_bottom_*` ключи** (`_translation`, `_definition`) — суффикс
  `_bottom` от удалённого bottom-sheet; кандидат на переименование, backlog.
- **`Msg.OpenLexemeMenu`** в reducer/state — unused из UI после удаления
  `LexemeTitleWidget`; cleanup в backlog.

## Артефакты

- `docs/features/IS479_wordcard_lexeme_inline/ui/ui_layout.md` — финальный UI snapshot (501 строка, 5 итераций)
- `docs/features/IS479_wordcard_lexeme_inline/ui/design_tree.md` — DAG 14 узлов (2 итерации)
- `docs/features/IS479_wordcard_lexeme_inline/ui/impl.md` — реализация 14 узлов (2 итерации, тесты 113/113 PASS)
- `docs/features/IS479_wordcard_lexeme_inline/ui/publish_ui.md` — публикация split-to-ui-file (9 корректировок)
- `docs/features/IS479_wordcard_lexeme_inline/ui/log.md` — timeline UI sub-flow
- `docs/features/IS479_wordcard_lexeme_inline/ui/ui_layout_review.md` — findings/verdicts ui_layout
- `docs/features/IS479_wordcard_lexeme_inline/ui/design_tree_review.md` — findings/verdicts design_tree
- `docs/features/IS479_wordcard_lexeme_inline/ui/impl_review.md` — findings/verdicts impl
- `docs/features/IS479_wordcard_lexeme_inline/ui/publish_ui_review.md` — findings/verdicts publish_ui

Архивы предыдущих run'ов: `docs/features/IS479_wordcard_lexeme_inline/ui/.archive_run1/`,
`docs/features/IS479_wordcard_lexeme_inline/ui/.archive_run2/` (ит.2 rerun_run3 — текущий).

### Публичная спека (publish_ui)

- `docs/features-spec/wordcard.md` — обновлена: вставлен раздел `## UI Layout`
  (ссылка на split-файл) между `## UI Messages` и `## IO`.
- `docs/features-spec/wordcard-ui.md` — создана (489 строк, split-to-ui-file).

### Новые виджеты (created)

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/SubentityChip.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeMeaningField.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeButton.kt`

### Изменённые файлы (modified)

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeValueFieldWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeItemWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`
- `core/core-resources/src/main/res/values/strings.xml` (добавлен `word_card_lexeme_remove` EN)
- `core/core-resources/src/main/res/values-ru-rRU/strings.xml` (добавлен `word_card_lexeme_remove` RU)

### Удалённые файлы (deleted)

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeChipPlaceholderWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeTitleWidget.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddTranslationLexemeMenuItem.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddDefinitionLexemeMenuItem.kt`
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeMenuItem.kt`

### Out-of-scope (закрыто business sub-flow до UI)

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/UiEffectHandler.kt` — удалён
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/AddLexemeBottomWidget.kt` — удалён
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/LexemeMeaningWidget.kt` — удалён
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/ActionsWidget.kt` — удалён
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`, `Message.kt`,
  `WordCardReducer.kt`, `DatasourceEffectHandler.kt`, `WordCardViewModel.kt`,
  `app/.../WordCardUseCaseImpl.kt` — изменены business sub-flow (контракты v2.5/v3.2)

_model: claude-opus-4-7[1m]_
