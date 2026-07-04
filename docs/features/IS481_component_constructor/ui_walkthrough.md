# UI walkthrough — IS481 component_constructor

Discovery шаг ui sub-flow. Только факты из реального кода с `file:line`. Решения и layout — на `ui_layout`.

> **Контекст.** Infra-фаза IS481 уже создала каркасы двух screen-модулей с placeholder-Composable (`Text("TODO: UI in ui_implement")`) — UI этой фичи будет писаться поверх существующего scaffold, а не с нуля. Существующие infra-артефакты тоже зафиксированы — они описывают где именно «жить» новому UI.

## § Existing scaffolded модулей (infra IS481)

Каркасы для UI уже стоят, осталось наполнить их разметкой.

- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt:17-32` — текущая реализация: `Box { Text("TODO: UI in ui_implement") }`. Сигнатура `ComponentsManagerScreen(factory, navigator)` через `viewModelFactory { factory.create(navigator) }`.
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt:17-32` — placeholder. Сигнатура `PerDictionaryComponentsScreen(dictionaryId: Long, factory, navigator)` — `dictionaryId` пробрасывается в ViewModel через assisted-injection (`factory.create(dictionaryId, navigator)`).
- State уже определены и ждут UI:
  - `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/State.kt:25-46` — `ComponentsManagerScreenState` с `userDefinedTypes: List<UserDefinedRow>?`, флагами `isLoading/isCreating/isRenaming/isDeleting`, диалогами `createDialog/renameDialog/deleteConfirm`, `snackbarState: SnackbarState?`. UI-комментарий на :88-91: "Composable отрисовывает SnackbarHost reading state".
  - `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/State.kt:30-55` — `PerDictionaryComponentsScreenState` с `dictionaryId/dictionaryName`, `items: List<PerDictRow>?`, теми же диалогами и `snackbarState`.
  - Row-типы: `UserDefinedRow(typeId, name, template, isMulti, scope, usageCount, dictionaryNames)` (components_manager State.kt:48-57), `PerDictRow(typeId, name, template, isMulti, isGlobal, valueCount)` (per_dictionary_components State.kt:57-66).
- Snackbar-комментарии в `UiEffectHandler.kt` (`components_manager/mate/UiEffectHandler.kt:10`, `per_dictionary_components/mate/UiEffectHandler.kt:9`) явно указывают: «reducer записывает text в `state.snackbarState`. UI отрисует через SnackbarHost reading state» — рендеринг snackbar'а — задача `ui_layout`.

## § Settings entries — drill-in pattern

### Файлы

- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsTabScreen.kt:75-122` — `Scaffold(topBar = SettingsAppBar())` + `LazyColumn` с `contentPadding = horizontal 16.dp`, `verticalArrangement = spacedBy(8.dp)`. Группировка через `item { SettingsSectionWidget { … } }`.
- Существующие drill-in entries в первой секции (:91-99): `LangManageWidget(onClick = { sendMessage(Msg.OpenLangManagement) })`, `ExportDataWidget`, `ImportDataWidget`. Вторая секция (:101-107): `PrivacyPolicyWidget`, `AboutAppWidget`.
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/SettingsSectionWidget.kt:21-41` — section wrapper: `Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, dividerColor))` + `Column(padding = 8.dp, verticalArrangement = spacedBy(4.dp))`.
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/items/base/SettingsItemWidget.kt:35-91` — единый row-pattern: `Surface(shape = RoundedCornerShape(12.dp))` + `Row` с `padding(horizontal=8.dp, vertical=12.dp)`, `spacedBy(12.dp)`. Внутри: `IconBoxed(size=24)` слева + `Text(style=LexemeStyle.BodyL)` weight 1f + опциональный `IconBoxed(R.drawable.ic_next, color=unselectedGreyColor)` справа. Принимает `iconRes: Int`, `titleRes: Int`, `showNextIcon: Boolean`, `onClick: (() -> Unit?)?`.
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/items/LangManageWidget.kt:8-18` — пример конкретного entry: `SettingsItemWidget(iconRes = R.drawable.ic_lang, titleRes = R.string.settings_section_lang_management, showNextIcon = true, onClick)`. 11 строк кода. Прямой precedent для `ComponentsManageWidget`.

### Navigation chain для нового entry (уже подведён в infra)

- `Msg.OpenComponentsManager : Msg` — `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/Message.kt:10`.
- `SettingsNavigationEffect.OpenComponentsManager : SettingsNavigationEffect` — `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffect.kt:9`.
- Reducer обработка — `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/SettingsTabReducer.kt:34-36`.
- `SettingsNavigator.openComponentsManager()` — `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigator.kt:9`.
- `SettingsNavigationEffectHandler.kt:19` — handler.
- Регистрация маршрута в навиграфе — `modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt:11` (`private const val COMPONENTS_MANAGER_ROUTE = "components_manager"`), :24 (передача callback), :49-53 (`composable(route = COMPONENTS_MANAGER_ROUTE) { compositionRoot.ComponentsManagerScreenDep(onBackPress = { navController.backPress() }) }`), :69-73 (`goToComponentsManager()` extension).
- `CompositionRoot.ComponentsManagerScreenDep(onBackPress)` — interface `modules/screen/main/src/main/java/me/apomazkin/main/CompositionRoot.kt:59-61`. Импл `app/src/main/java/me/apomazkin/polytrainer/uiDeps/CompositionRootImpl.kt:196-207`.

UI-задача: добавить **один** новый widget (по образцу `LangManageWidget`) и вставить вызов в `SettingsTabScreen.kt` LazyColumn (первая секция, после `LangManageWidget` либо отдельная секция).

## § DictionaryAppBar — current layout + молоток

### Файлы

- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBar.kt:43-77` — `TopAppBar(title = AppBarTitleWidget(titleResId), actions = { if (isLoading) CircularProgressIndicator else DictDropDownWidget(...) })`. Один action-slot, по факту — только dropdown (или прогресс во время загрузки). Никаких icon-button'ов сейчас нет.
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/State.kt:10-15` — `DictionaryAppBarState(isLoading, currentDict: DictUiEntity?, availableDictList, isDropDownMenuOpen)`. `currentDict` уже nullable → флаг «icon-button visibility = `currentDict != null`» доступен напрямую.
- `modules/widget/dictionarypicker/src/main/java/me/apomazkin/dictionarypicker/DictDropDownWidget.kt:25-57` — текущий dropdown: `IconDropdownWidget` с иконкой-флагом (`ImageFlagWidget`/`FlagPlaceholderWidget`) + список словарей в menu + `DividerMenuItem` + `AddDictMenuWidget`.
- Title-widget — `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/widget/AppBarTitleWidget.kt:13-22` — `Text(style = LexemeStyle.H5, color = colorScheme.secondary)`.

### Navigation chain «молоток» (готов в infra)

- `Msg.OpenPerDictionaryComponents(val dictionaryId: Long) : Msg` — `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt:37`. Payload явно несёт `dictionaryId` (комментарий :36: «reducer does not read state»).
- `DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(val dictionaryId: Long)` — `DictionaryAppBarNavigationEffect.kt:7`.
- Reducer — `mate/DictionaryAppBarReducer.kt:39-41`.
- `DictionaryAppBarNavigator.openPerDictionaryComponents(dictionaryId)` — `DictionaryAppBarNavigator.kt:7`.
- `DictionaryAppBarNavigationEffectHandler.kt:17-18` — handler.
- Маршрут per-dict экрана зарегистрирован в `modules/screen/main/src/main/java/me/apomazkin/main/Vocabulary.kt:11` (`private const val PER_DICT_COMPONENTS_ROUTE = "per_dict_components"`), :40-52 (`composable(...)` с `navArgument(PER_DICT_COMPONENTS_DICT_ID_ARG, NavType.LongType)`), :62-66 (`goToPerDictionaryComponents`). **Маршрут живёт только в `Vocabulary.kt`** — Quiz.kt / Statistic.kt не регистрируют отдельные copy (нужно проверить если требуется отдельный регистр для других tab-хостов).
- `CompositionRoot.PerDictionaryComponentsScreenDep(dictionaryId, onBackPress)` — `CompositionRoot.kt:63-67`. Импл `CompositionRootImpl.kt:209-222`. Wiring lambda `openPerDictionaryComponents` подведён **во всех 3 хостах** — `VocabularyTabDep` (CompositionRootImpl.kt:60-86), `QuizTabScreenDep` (:102-128), `StatisticTabScreenDep` (:142-165). Все три инжектят `DictionaryAppBarNavigatorImpl(onOpenDictionaryCreate, onOpenPerDictionaryComponents)`.

UI-задача: вставить **icon-button** перед `DictDropDownWidget` в `actions` slot, отправляющий `Msg.OpenPerDictionaryComponents(currentDict.id)`, видимый только при `state.currentDict != null`.

### Иконка «молоток»

- Существующих drawable c семантикой «молоток / конструктор / инструменты» **нет** — проверены `core/core-resources/src/main/res/drawable/`. Доступные icons: `ic_add`, `ic_back`, `ic_close`, `ic_delete`, `ic_edit`, `ic_lang`, `ic_more`, `ic_more_horizonral`, `ic_next`, `ic_send`, `ic_share`, `ic_trash`, `ic_tab_settings`, `ic_tab_vocabulary`, `ic_tab_training`, `ic_tab_stats`, `ic_tab_dashboard_selected`, `ic_about`, `ic_circle_delete`, `ic_add_value`, `ic_add_circled`, `ic_confirm`, `ic_quiz_write`, `ic_download`, `ic_upload`, `ic_privacy_policy`, `ic_feedback`, `ic_rate`, `ic_logo`, `ic_logo_splash`, `ic_logo_easter_egg`, `ic_selected`, `ic_chat_bg`, `ic_move`, `ic_clear`. Нужно добавить новый vector drawable (например `ic_components.xml` / `ic_hammer.xml`).

## § List / Row patterns

### LazyColumn-based screens

- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsTabScreen.kt:83-89` — `LazyColumn(contentPadding = PaddingValues(horizontal=16.dp), verticalArrangement = Arrangement.spacedBy(8.dp))` + `item { SettingsSectionWidget { … } }`. Не плоский список, а группа секций — каждая секция Surface c rounded border.
- `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/DictionaryListScreen.kt:117-129` — плоский `LazyColumn { items(dictionaries) { item -> DictionaryListItemWidget(item, onItemClick, onDeleteClick) } }`. Без contentPadding, divider'ов нет.
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/VocabularyTabScreen.kt:156-167` — `WordListWidget` отдельная composable, использует `collectAsLazyPagingItems()` (paging) — для components/dictionary не нужно, но pattern на знание.

### Row item — examples

- `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/widget/DictionaryListItemWidget.kt:26-60` — образец row: `Row(modifier = fillMaxWidth().clickable { onItemClick }.padding(horizontal=16.dp, vertical=8.dp), verticalAlignment = CenterVertically)` → flag (`ImageFlagWidget`/`FlagPlaceholderWidget`) + `Spacer(width=8.dp)` + `Text(modifier=weight(1f), style=LexemeStyle.BodyL, color=colorScheme.secondary)` + `IconBoxed(R.drawable.ic_trash, size=44, onClick=onDeleteClick)`. Click на всю строку → edit, click на иконку справа → delete. **Дополнительно — `IconBoxed` сам имеет clickable** (см. § Theme tokens).
- `modules/screen/settingstab/.../widgets/settings/items/base/SettingsItemWidget.kt:35-91` — variant: `Surface(onClick)` оборачивает весь row, без отдельного trailing-action icon. Используется когда action на row — drill-in.

### Dividers

- `dividerColor = Color(0xffE4E5E7)` — `modules/core/theme/src/main/java/me/apomazkin/theme/Color.kt:31`.
- Используется в `SettingsSectionWidget` как `BorderStroke(1.dp, dividerColor)` для контура секции; отдельный `Divider`-composable пока в проекте не выявлен (rows внутри section'а разделены `Arrangement.spacedBy(4.dp)`).

## § Dialog patterns

- `modules/core/ui/src/main/java/me/apomazkin/ui/dialog/base/LexemeDialog.kt:24-54` — базовый wrapper: `Dialog(DialogProperties(dismissOnBackPress, dismissOnClickOutside, ...))` + `Surface(shape = RoundedCornerShape(16.dp))` + `Column(padding=24.dp) { content }`. Параметры: `onDismissRequest, dismissOnBackPress, dismissOnClickOutside, usePlatformDefaultWidth, decorFitsSystemWindows, content: ColumnScope.() -> Unit`.
- `modules/core/ui/src/main/java/me/apomazkin/ui/dialog/AlarmDialogWidget.kt:26-54` — confirm-delete pattern: `LexemeDialog { content; Row(padding(top=16.dp), spacedBy(12.dp)) { CancelButtonWidget(weight 1f); AlarmButtonWidget(weight 1f, alarmButtonText) } }`. Параметры: `alarmButtonText: Int`, `onAlarmClick`, `onDismissRequest`, `content`.
- Реальное использование:
  - `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/widget/ConfirmDeleteDictionaryWidget.kt:18-41` — confirm-delete для словаря. `AlarmDialogWidget { Text(H6); Spacer(8.dp); Text(BodyL, color=grayTextColor) }` — пример title + subtitle. Прямой precedent для delete-with-impact-warning в IS481 (вместо subtitle подставить counts из `DeletionImpact`).
  - `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/widget/ConfirmDeleteWordWidget.kt:16-39` — confirm-delete для слова. Аналогично.
- `ModalBottomSheet` — используется в `dictionaryTab` для `AddWordBottomSheetWidget` (`VocabularyTabScreen.kt:171-176`), но не в settings/dictionary list scenarios. Для form-create dialog'а в IS481 — оба варианта (Dialog vs BottomSheet) — UX-решение на `ui_layout`.

## § Snackbar patterns

- `Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) })` — точка монтирования.
- Образцы установки:
  - `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt:65-77, 108` — `val snackbarHostState = remember { SnackbarHostState() }` снаружи + проброс в hostable composable + `snackbarHost = { SnackbarHost(...) }`. Управление через `UiHostImpl(snackbarHostState, context)` (`wordcard/widget/internal/UiHostImpl.kt:11-16`) — обёртка, которая в effect handler'е дёргает `snackbarHostState.showSnackbar(context.getString(messageRes))`.
  - `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/VocabularyTabScreen.kt:77-88, 114-116` — pattern «state-driven snackbar»: `state.snackbarState.show` triggers `snackbarHostState.showSnackbar(state.snackbarState.title)` в `LaunchedEffect`. После показа отсылает `UiMsg.ShowNotification(show=false)`.
- В State'ах двух IS481 экранов уже определён `SnackbarState(text: String)` (`components_manager/mate/State.kt:93`, `per_dictionary_components/mate/State.kt:105`). State-driven snackbar — pattern из `VocabularyTabScreen` ближе.

## § Theme / Resources / Primitives

### Theme tokens

- `modules/core/theme/src/main/java/me/apomazkin/theme/Color.kt`:
  - `dividerColor = Color(0xffE4E5E7)` (:31)
  - `enableIconColor = Color(0xff252628)` (:32)
  - `unselectedGreyColor = Color(0xff95989D)` (:30)
  - `grayTextColor = Color(0xFF7B7E85)` (:34) — body subtitle / hint
  - `whiteColor`, `blackColor`, `bgAlfa` и другие.
  - `LexemeColor` object (:48-92) — semantic palette (primary, secondary, tertiary, error и т.д.), маппится на `MaterialTheme.colorScheme.*`.
- `modules/core/theme/src/main/java/me/apomazkin/theme/LexemeStyle.kt:116-132` — типографика: `H1..H6` + `BodyXL/L/M/S` (с Bold/Regular вариантами). `H6 = headlineSmall (20.sp, Medium)`, `BodyL = bodyLarge (17.sp, Regular)`, `BodyLBold = titleSmall (17.sp, Bold)`. Используется через `LexemeStyle.H6` / `LexemeStyle.BodyL`.
- `modules/core/theme/src/main/java/me/apomazkin/theme/Theme.kt` — `AppTheme` Composable wrapper (импортируется в Preview-функциях).

### Tier 1 primitives (`modules/core/ui/`)

- `modules/core/ui/src/main/java/me/apomazkin/ui/IconBoxed.kt:26-59` — клик-able icon с tint/disabled-state: `IconBoxed(iconRes, enabled=true, contentDescriptionRes, size: Int = 56, colorEnabled, colorDisabled, clipShape=CircleShape, onClick)`. Это generic icon-button — прямой кандидат для «молотка».
- `modules/core/ui/src/main/java/me/apomazkin/ui/input/PrimaryTextFieldWidget.kt:27-74` — основной text input + send button. Принимает `value: String`, `onValueChange`, `onSendAction`, `placeHolder: Int?`, `isSendEnabled`, `isInputEnabled`. Использует `LexemeTextFieldWidget` под капотом — `OutlinedTextField`-based (см. `input/base/LexemeTextFieldWidget.kt`).
- `modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeRadioMenuItem.kt:21-39` — radio menu item (IS481 Tier 1 primitive, comment на :9-15 явно адресует IS481). `LexemeRadioMenuItem(isSelected, title, enabled, onSelect)`. Для scope-radio (global vs per-dict).
- `modules/core/ui/src/main/java/me/apomazkin/ui/btn/`:
  - `PrimaryFullButtonWidget` — full-width primary.
  - `PrimaryFabWidget`, `PrimaryLongFabWidget`, `SecondaryFabWidget` — FAB.
  - `CancelButtonWidget`, `AlarmButtonWidget`, `ErrorButtonWidget`, `SecondaryButtonWidget`, `PrimaryTextButtonWidget` — кнопки для диалогов / inline.
- **Checkbox / Switch** — нет проектного wrapper'а. `material3.Checkbox` есть только в `core/ui/src/main/java/me/apomazkin/ui/unused/CheckedTextWidget.kt:34` (помечен `unused/`). Для `is_multi` чекбокса в create-form — либо завести проектный wrapper, либо использовать стандартный `material3.Checkbox/Switch`.

### Strings / drawables (`core/core-resources/`)

- `core/core-resources/src/main/res/values/strings.xml` — общий пул строк (`item_title_settings`, `settings_section_lang_management`, `button_delete`, etc.). RU-локализация в `values-ru-rRU/strings.xml`.
- `core/core-resources/src/main/res/drawable/ic_*.xml` — vector icons (см. список в § DictionaryAppBar выше). Молоток / wrench / tools отсутствует.
- `core/core-resources/src/main/res/font/poppins_regular.ttf`, `poppins_medium.ttf` — fonts.
- `core/core-resources/src/main/res/values/colors.xml` — могут жить XML-цвета (не сверял; для Compose всё в `theme/Color.kt`).
- IS481-specific строки нужно добавить либо в `core-resources` (если переиспользуемы), либо в `modules/screen/components_manager/src/main/res/values/strings.xml` / `per_dictionary_components/src/main/res/values/strings.xml` (если scope-локальные). Convention из других screen-модулей.

## § Similar screens — references

Прямые аналоги по shape экрана IS481 («drill-in CRUD list поверх tab»):

1. **LangManagement / DictionaryList** — `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/DictionaryListScreen.kt`. Имеет: `Scaffold + topBar + LazyColumn items + per-row delete-action + Confirm-dialog + Primary FAB-button "create new"`. **Семантически ближайший** к `ComponentsManagerScreen` / `PerDictionaryComponentsScreen` — тоже global aggregated list пользовательских сущностей с CRUD.
2. **DictionaryTab / Vocabulary** — `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/VocabularyTabScreen.kt`. Имеет: state-driven snackbar pattern, FAB для open-create-bottom-sheet, BackHandler, confirm-delete dialog. Полезен для snackbar + FAB подхода.
3. **WordCard** — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`. Pattern: TopBar c overflow-menu (kebab) + delete-confirm dialog + snackbar через `UiHostImpl`. Альтернативная snackbar-стратегия (внешний host, не state-driven).
4. **SettingsTab** — `modules/screen/settingstab/.../SettingsTabScreen.kt`. Используется для расположения нового entry-point. Не CRUD, но конкретный визуальный контракт для `ComponentsManageWidget`.

Полностью аналогичного экрана «list + create-form-dialog + rename + delete-with-impact-warning» в проекте **нет**. `DictionaryListScreen` — ближайший, но без rename и без impact-preview.

## § Вердикт

**Аналог: найден частично.**

Полностью изоморфный экран в кодовой базе отсутствует, но все необходимые building blocks существуют:

- Tier 1 primitives (`IconBoxed`, `LexemeTextFieldWidget`/`PrimaryTextFieldWidget`, `LexemeRadioMenuItem`, кнопки) — готовы к использованию.
- Tier 1 dialog scaffolding (`LexemeDialog` + `AlarmDialogWidget`) — готовы; для create-form-dialog'а с input полями нужно собрать самостоятельно поверх `LexemeDialog` (precedent inline-собранных диалогов в `ConfirmDeleteDictionaryWidget` / `ConfirmDeleteWordWidget`).
- Settings entry pattern (`SettingsItemWidget` + drill-in via `Msg → NavigationEffect → Navigator`) — готов, scaffolding для нового entry уже подведён (Msg/Effect/Navigator/route).
- DictionaryAppBar action-slot, navigation pipeline для icon-button «молоток» — готовы; не хватает только самой icon-button композиции в `DictionaryAppBar.kt:51-75` и vector drawable.
- LazyColumn + row-pattern, confirm-delete dialog с title+subtitle, state-driven snackbar — есть прямые precedents (`DictionaryListScreen`, `ConfirmDeleteDictionaryWidget`, `VocabularyTabScreen`).
- `ComponentsManagerScreen` / `PerDictionaryComponentsScreen` уже существуют как placeholder-композайблы — UI пишется поверх готового scaffold; State уже моделирует диалоги и snackbar.

Что **отсутствует** и должно появиться в рамках `ui_implement`:
- Vector drawable «молоток» (`ic_*.xml` в `core-resources/drawable/`).
- Проектный wrapper для `Checkbox`/`Switch` (для `is_multi` поля) — либо использовать `material3.Checkbox`.
- Composable per-template preview (Tier 2 widget модуль `component_widgets/` — судя по scope, его создание ещё впереди; в текущем дереве модулей он отсутствует — проверено через `find modules/widget`).
- IS481-specific strings.xml в screen-модулях либо в `core-resources`.
- Сам UI-layout (list rows с `name + template + cardinality-badge + usage-counter`, create-form с name/template/is-multi/scope, delete-confirm с impact-preview).

_model: claude-opus-4-7[1m]_
