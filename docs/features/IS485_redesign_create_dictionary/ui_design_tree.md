# IS485 — Design tree: редизайн экрана «Создание словаря» (UI)

Источник: `ui_layout.md` (финальный snapshot), `02_scope.md`, факты кода из `ui_walkthrough.md`. Mate-слой не затрагивается (State/Msg/Reducer/эффекты — без изменений). Все новые файлы — локальные composables в `modules/screen/dictionary/.../form/widget/`.

## Часть 1: Граф

```yaml
- id: 0
  file: modules/core/theme/src/main/java/me/apomazkin/theme/Color.kt
  action: "~"
  depends: []

- id: 1
  file: core/core-resources/src/main/res/values/strings.xml
  action: "~"
  depends: []

- id: 2
  file: core/core-resources/src/main/res/values-ru-rRU/strings.xml
  action: "~"
  depends: []

- id: 3
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FormHeaderWidget.kt
  action: "+"
  depends: [0, 1, 2]

- id: 4
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagPreviewWidget.kt
  action: "+"
  depends: [0]

- id: 5
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/NameFieldWidget.kt
  action: "+"
  depends: [0]

- id: 6
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/SearchPillWidget.kt
  action: "+"
  depends: [0, 1, 2]

- id: 7
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/SubmitButtonWidget.kt
  action: "+"
  depends: [0]

- id: 8
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/widget/DictionaryAppBar.kt
  action: "~"
  depends: []

- id: 9
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagGridWidget.kt
  action: "~"
  depends: [0, 1, 2]

- id: 10
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/DictionaryFormWidget.kt
  action: "~"
  depends: [3, 4, 5, 6, 7, 9]

- id: 11
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormScreen.kt
  action: "~"
  depends: [0, 8, 10]
```

Параллельность: #0, #1, #2, #8 — независимы (стартовая волна); #3–#7, #9 — параллельно после токенов/строк; #10 собирает виджеты; #11 — вершина.

Удалений файлов нет: миграции из `## ❌ УДАЛЯЕМ` ui_layout — это замены ВЫЗОВОВ внутри #10, не удаление файлов (общие core:ui-виджеты остаются, project_decision №4).

## Часть 2: Детали изменений

### #0 Color.kt [~]

**Было:** плоский список `val` + `LexemeColor` (`Color.kt:48-92`); цветов макета нет.

**Стало:** добавить 5 `val` (стиль файла — одиночные значения, как существующие `grayTextColor` и пр.):

```kotlin
val formBackground = Color(0xFFFCFCFA)
val formTextSecondary = Color(0xFF8A8A90)
val formTextTertiary = Color(0xFF55555C)
val formTextHint = Color(0xFF9A9AA2)
val searchPillColor = Color(0xFFF1F0EC)
```

Тень кнопки — производная `LexemeColor.primary.copy(alpha = 0.5f)` локально в #7, отдельный val не нужен. `LexemeColor.primary` НЕ трогать (decision №3).

### #1 strings.xml (EN) [~]

**Стало:** +3 ключа, 1 правка:

```xml
<string name="dictionary_form_subtitle">Pick a language and name your dictionary</string>
<string name="dictionary_flags_not_found">Nothing found</string>
<string name="dictionary_edit_title">Edit dictionary</string>
<!-- правка существующего (рассинхрон с RU): -->
<string name="dictionary_filter_flags_hint">Search by country or language…</string>
```

### #2 strings.xml (RU) [~]

**Стало:** те же 3 ключа:

```xml
<string name="dictionary_form_subtitle">Выберите язык и назовите словарь</string>
<string name="dictionary_flags_not_found">Ничего не найдено</string>
<string name="dictionary_edit_title">Редактирование словаря</string>
```

(`dictionary_filter_flags_hint` RU уже корректен; единственный потребитель ключа — форма, правка EN безопасна. ⚠ `dictionary_new` шарится с кнопками «Новый словарь» в `DictionaryListScreen.kt:110,132` — текст менять нельзя, использование как заголовок допустимо, семантика совпадает.)

### #3 FormHeaderWidget.kt [+]

Заголовочная зона формы (ui_layout `<𝗙𝗼𝗿𝗺𝗛𝗲𝗮𝗱𝗲𝗿>`).

```kotlin
@Composable
internal fun FormHeaderWidget(
    showTitle: Boolean,          // = !showAppBar (онбординг)
    modifier: Modifier = Modifier,
)
// Column(spacing=4): if(showTitle) Text(dictionary_new, LexemeStyle.H4, LexemeColor.secondary)
//                    Text(dictionary_form_subtitle, LexemeStyle.BodyM, formTextSecondary)
// + @PreviewWidget: оба варианта (showTitle true/false)
```

### #4 FlagPreviewWidget.kt [+]

Превью выбранного флага 58dp с двойным кольцом (ui_layout `<𝗙𝗹𝗮𝗴𝗣𝗿𝗲𝘃𝗶𝗲𝘄>`).

```kotlin
@Composable
internal fun FlagPreviewWidget(
    selectedFlag: CountryFlagItem?,
    name: String,
    modifier: Modifier = Modifier,
)
// Box(58, CircleShape): кольцо 2.dp LexemeColor.primary (внешний край) → зазор 2.dp formBackground →
//   контент 50.dp (Figma: shadow 0-2px фон + 2-4px primary → толщина кольца 2, не 4):
//   selectedFlag != null → ImageFlagWidget(flagRes, Modifier.size(50.dp))
//   selectedFlag == null → FlagPlaceholderWidget(letter = name.firstOrNull()?.toString() ?: "", size(50.dp))
// Общие виджеты НЕ меняются — размер внешним Modifier (паттерн DictionaryFormWidget.kt:50-58).
// + @PreviewWidget: флаг / буква / пустой
```

### #5 NameFieldWidget.kt [+]

Material-поле имени (ui_layout `<𝗡𝗮𝗺𝗲𝗙𝗶𝗲𝗹𝗱>`).

```kotlin
@Composable
internal fun NameFieldWidget(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,     // dictionary_name_hint
    modifier: Modifier = Modifier,
)
// Column(spacing=6):
//   Text(labelRes.uppercase(), LexemeStyle.BodySBold + letterSpacing 0.3sp, LexemeColor.primary)
//   BasicTextField(value, onValueChange, singleLine, textStyle = LexemeStyle.H6 + LexemeColor.secondary,
//     cursorBrush = SolidColor(LexemeColor.primary))
//   HorizontalDivider(thickness = 2.dp, color = LexemeColor.primary)
// + @PreviewWidget: пустое / заполненное
```

### #6 SearchPillWidget.kt [+]

Капсула поиска (ui_layout `<𝗦𝗲𝗮𝗿𝗰𝗵𝗣𝗶𝗹𝗹>`).

```kotlin
@Composable
internal fun SearchPillWidget(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
)
// Surface(shape = RoundedCornerShape(14.dp), color = searchPillColor, height 45.dp):
//   Row(vAlign=center, padding h=16, spacing=12):
//     Icon(Icons.Default.Search, tint = formTextHint, size 19.dp)
//     BasicTextField(weight(1f), singleLine, textStyle BodyM/LexemeColor.secondary,
//       decorationBox: if value.isEmpty() → Text(dictionary_filter_flags_hint, BodyM, formTextHint))
//     if (value.isNotEmpty()) IconButton(Icons.Default.Close, tint formTextHint) { onValueChange("") }
// + @PreviewWidget: пустое / с текстом
```

### #7 SubmitButtonWidget.kt [+]

Кнопка с цветной тенью (ui_layout `<𝗦𝘂𝗯𝗺𝗶𝘁𝗕𝘂𝘁𝘁𝗼𝗻>`).

```kotlin
@Composable
internal fun SubmitButtonWidget(
    @StringRes titleRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
// Button(height 56, fillMaxWidth, shape RoundedCornerShape(16.dp),
//   colors: enabled → LexemeColor.primary/whiteColor; disabled → colorScheme.secondary/onSecondary
//     (как LexemeButton disabled, LexemeButton.kt:41-44),
//   modifier.shadow(только при enabled, shape тот же,
//     ambientColor/spotColor = LexemeColor.primary.copy(alpha = 0.5f),
//     elevation: старт 10.dp, подобрать на девайсе — Compose elevation ≠ Figma blur 22)
//   Text(titleRes, LexemeStyle.BodyLBold)
// + @PreviewWidget: enabled / disabled
```

### #8 DictionaryAppBar.kt (локальный) [~]

**Было** (`DictionaryAppBar.kt:16-34`):
```kotlin
@Composable
fun DictionaryAppBar(onBackPress: () -> Unit)
// title захардкожен: stringResource(R.string.dictionary_selection_title), LexemeStyle.H5
```

**Стало:**
```kotlin
@Composable
fun DictionaryAppBar(
    onBackPress: () -> Unit,
    @StringRes titleResId: Int = R.string.dictionary_selection_title,  // default = текущее поведение
)
// title: stringResource(titleResId); остальное без изменений (ic_back 44, LexemeStyle.H5)
```

⚠ Default обязателен: второй потребитель — `DictionaryListScreen.kt:64` («Все словари») — не меняется.

### #9 FlagGridWidget.kt [~]

**Было** (`FlagGridWidget.kt:36-71`): `GridCells.Fixed(5)`, флаг 48dp, выбор — только `BorderStroke(2.dp, primary)`, подпись `BodyS`/`grayTextColor`/`maxLines=1`/`Ellipsis`, empty-state нет.

**Стало:**
```kotlin
@Composable
internal fun FlagGridWidget(
    flags: List<CountryFlagItem>,
    selectedFlag: CountryFlagItem?,
    isFilterActive: Boolean,        // НОВЫЙ параметр: flagFilter.isNotBlank() — для empty-state
    onFlagClick: (CountryFlagItem) -> Unit,
    modifier: Modifier = Modifier,
)
// if (flags.isEmpty() && isFilterActive) → Box(fillMaxSize) { Text(dictionary_flags_not_found,
//     BodyM, formTextSecondary, align Center) }
// else LazyVerticalGrid(GridCells.Fixed(4), spacedBy 8×8):
//   item = Column(hAlign=center, spacing=8):
//     Box(56, CircleShape):
//       ImageFlagWidget(size 56)
//       if isSelected: border 2.5dp primary (с внутренним зазором от flag)
//       if isSelected: Box(24, CircleShape, primary, border 2.dp formBackground, align BottomEnd)
//         { Icon(ic_confirm, tint whiteColor, size 11.dp) }
//     Text(localizedName, maxLines=2, textAlign=center,
//       style = if isSelected BodySBold else BodyS,
//       color = if isSelected LexemeColor.primary else formTextTertiary)
// Toggle-поведение onFlagClick — без изменений (reducer как есть).
// + @PreviewWidget: сетка с выбранным / empty-state
```

ℹ️ Derived-условие empty-state вычисляется в #10 и передаётся параметром `isFilterActive` — сам виджет не знает о `flagFilter` (флаг в стейт НЕ заводится, решение брифа).

### #10 DictionaryFormWidget.kt [~]

**Было** (`DictionaryFormWidget.kt:40-128`): Column(h=16): Row(ImageFlagWidget/FlagPlaceholderWidget 48 + LexemeTextFieldWidget) → OutlinedTextField фильтра → FlagGridWidget → PrimaryFullButtonWidget; спейсеры 8.

**Стало:**
```kotlin
@Composable
internal fun DictionaryFormWidget(
    formState: DictionaryFormScreenState,
    showTitle: Boolean,                      // НОВЫЙ параметр (= !showAppBar, прокид с экрана)
    sendMsg: (DictionaryFormMsg) -> Unit,
)
// Column(padding h=24):
//   FormHeaderWidget(showTitle)
//   Spacer(16)
//   Row(vAlign=center, spacing=14):
//     FlagPreviewWidget(formState.selectedFlag, formState.name)
//     NameFieldWidget(formState.name, { sendMsg(NameChanged(it)) }, R.string.dictionary_name_hint, weight(1f))
//   Spacer(20)
//   SearchPillWidget(formState.flagFilter, { sendMsg(FlagFilterChanged(it)) })
//   Spacer(8)
//   FlagGridWidget(formState.flags, formState.selectedFlag,
//     isFilterActive = formState.flagFilter.isNotBlank(), { sendMsg(SelectFlag(it)) }, weight(1f))
//   SubmitButtonWidget(
//     titleRes = if (formState.editingDictionaryId != null) R.string.dictionary_save else R.string.dictionary_create,
//     enabled = formState.saveButtonEnabled, onClick = { sendMsg(Save) })
//   Spacer(16)
// 🚨 Отступ кнопки: Figma h=22, контент h=24 → единый h=24 (одно значение отступа на весь
//    контент; числовое отклонение 2dp, фиксируется как проектная унификация).
// Все Msg — существующие; превью-функции переписать под новую композицию.
```

### #11 DictionaryFormScreen.kt [~]

**Было** (`DictionaryFormScreen.kt:26-69`): `SystemBarsWidget(color = whiteColor)`; `Scaffold(topBar = { if (showAppBar) DictionaryAppBar(onBackPress) })`; контент `.background(whiteColor)`.

**Стало:**
```kotlin
// SystemBarsWidget(color = formBackground)
// Scaffold(containerColor = formBackground,
//   topBar = { if (showAppBar) DictionaryAppBar(
//     onBackPress = { sendMsg(Back) },
//     titleResId = if (editingDictionaryId != null) R.string.dictionary_edit_title
//                  else R.string.dictionary_new) })
// контент: .background(formBackground); DictionaryFormWidget(formState, showTitle = !showAppBar, sendMsg)
// BackHandler и паддинги (statusBars/navigationBars/ime) — без изменений (:49, :61-69).
// Превью-функцию дополнить вариантом showAppBar=false.
```

## Правила соблюдены

- Полные пути; сквозная нумерация; DAG без циклов (волны: {0,1,2,8} → {3,4,5,6,7,9} → {10} → {11}).
- Существующие файлы (#0,1,2,8,9,10,11) проверены по ui_walkthrough (file:line) — пути реальные.
- Псевдокод, не реализация; Mate-файлы и общие core:ui-виджеты в графе отсутствуют намеренно.

_model: claude-fable-5 (артефакт составлен conductor'ом без суб-агентов и без ревью — по указанию пользователя)_
