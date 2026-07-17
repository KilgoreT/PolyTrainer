# UI Walkthrough: IS485 redesign_create_dictionary

Факты о текущем UI-коде, релевантные редизайну экрана «Создание словаря». Только то, что есть в коде сейчас; без дизайн-решений.

## 1. Целевой экран

### DictionaryFormScreen.kt
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormScreen.kt`

- Публичная сигнатура: `factory`, `navigator`, `editingDictionaryId: Long? = null`, `showAppBar: Boolean = true` (`DictionaryFormScreen.kt:26-34`).
- `SystemBarsWidget(color = whiteColor)` — захардкожен белый на оба system bar (`DictionaryFormScreen.kt:51-53`).
- `Scaffold` с `topBar = { if (showAppBar) DictionaryAppBar(onBackPress = …) }` (`DictionaryFormScreen.kt:54-59`).
- Контент: `Box` с `.padding(paddings).fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().background(color = whiteColor)` (`DictionaryFormScreen.kt:61-69`).
- `BackHandler { sendMsg(DictionaryFormMsg.Back) }` (`DictionaryFormScreen.kt:49`).

### ⚠️ Факт-рассинхрон со scope: два разных `DictionaryAppBar`

Экран импортирует **`me.apomazkin.dictionary.widget.DictionaryAppBar`** (`DictionaryFormScreen.kt:19`) — это **локальный** виджет модуля dictionary:

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/widget/DictionaryAppBar.kt:16-18` — сигнатура: `DictionaryAppBar(onBackPress: () -> Unit)`. **Параметра `titleResId` НЕТ.**
- Тайтл захардкожен: `stringResource(id = R.string.dictionary_selection_title)`, стиль `LexemeStyle.H5` (`DictionaryAppBar.kt:29-34`). То есть в create/edit сейчас в шапке всегда «Dictionaries / Словари».
- Кнопка «назад»: `IconBoxed(iconRes = R.drawable.ic_back, size = 44, colorEnabled = enableIconColor)` (`DictionaryAppBar.kt:20-27`).

`DictionaryAppBar` из `modules/widget/dictionaryappbar` — **другой** виджет: принимает `@StringRes titleResId: Int` + `factory`/`navigator`, содержит dropdown словарей и прогресс, back-кнопки не имеет (`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBar.kt:29-42`). Формой НЕ используется. Схема scope «смена `titleResId` для DictionaryAppBar» в текущем коде не реализуема без добавления параметра в локальный AppBar — у него тайтл захардкожен.

### DictionaryFormWidget.kt
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/DictionaryFormWidget.kt`

Текущая композиция — `Column` с `padding(horizontal = 16.dp)` (`:40-43`), сверху вниз:

1. `Row` превью+имя (`:45-68`): `ImageFlagWidget`/`FlagPlaceholderWidget` размером `48.dp` (`:50-59`), `Spacer(8.dp)`, `LexemeTextFieldWidget(weight(1f), placeHolder = R.string.dictionary_name_hint)` (`:61-67`).
2. Поле фильтра — **сырой Material3 `OutlinedTextField`** (не общий виджет): `singleLine = true`, `textStyle = LexemeStyle.BodyM`, placeholder `R.string.dictionary_filter_flags_hint` цветом `grayTextColor`, `leadingIcon = Icons.Default.Search`, trailingIcon `Icons.Default.Close` при непустом фильтре (`:72-102`). Форма дефолтная (M3 OutlinedTextField, скругление не задано).
3. `FlagGridWidget(weight(1f))` (`:106-111`).
4. Кнопка: `buttonTextRes = if (editingDictionaryId != null) R.string.dictionary_save else R.string.dictionary_create` (`:115-119`), `PrimaryFullButtonWidget(titleRes, enabled = saveButtonEnabled)` (`:121-125`).

Спейсеры между блоками — `8.dp`, внизу `16.dp` (`:70,104,113,127`).

### FlagGridWidget.kt
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagGridWidget.kt`

- `LazyVerticalGrid(columns = GridCells.Fixed(5), spacedBy(8.dp)×2)` (`:36-40`).
- Маркер выбора — `Surface(shape = CircleShape, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary))` при `isSelected`, иначе `null` (`:47-53`); сравнение по `numericCode` (`:43`).
- Флаг `ImageFlagWidget(size(48.dp))` внутри Surface c `padding(4.dp)` (`:55-60`).
- Подпись: `LexemeStyle.BodyS`, `grayTextColor`, `maxLines = 1`, `TextOverflow.Ellipsis`, `width(56.dp)` (`:62-70`). Bold-варианта для выбранного нет.
- Бейджа-галочки нет — только кольцо-бордер.

### Стейт (данные для макета)
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormState.kt:7-14` — `editingDictionaryId`, `name`, `flagFilter`, `flags: List<CountryFlagItem>`, `selectedFlag`, `saveButtonEnabled`. Всё, что упоминает scope, на месте; производная «ничего не найдено» (`flags.isEmpty() && flagFilter.isNotBlank()`) в UI сейчас не рендерится нигде.

### Проводка режимов (RootRouter — не менять)
`/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/route/RootRouter.kt`

- Онбординг `DICTIONARY_SETUP`: `showAppBar = false` (`RootRouter.kt:60-67`).
- `DICTIONARY_CREATE?editId={editId}`: `showAppBar` не передаётся → дефолт `true`; `editingDictionaryId` из аргумента (`RootRouter.kt:68-86`).

## 2. UI-примитивы `modules/core/ui` (используемые формой)

### LexemeTextFieldWidget
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/input/base/LexemeTextFieldWidget.kt:37-47`

Параметры: `modifier`, `imeAction`, `autoCorrect`, `erasable`, `placeHolder: Int?`, `isInputEnabled`, `value`, `onValueChange`, `onKeyboardActions`. Внешний вид зашит: `RoundedCornerShape(0.dp)` (`:91`), рамки прозрачные (`:97-100`), `singleLine = false` (`:89`), `textStyle = LexemeStyle.BodyL` (`:90`), placeholder `BodyM` + `grayTextColor` (`:81-88`). Параметров стиля (shape/цвета/размер текста) нет.

### PrimaryFullButtonWidget → LexemeButton
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/btn/PrimaryFullButtonWidget.kt:17-33` — обёртка: `fillMaxWidth()`, `height = 56.dp`, цвета `colorScheme.primary`/`onPrimary`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/btn/base/LexemeButton.kt:30-58` — база: `DEFAULT_ROUNDED_CORNER = 12` зашит константой в `RoundedCornerShape(DEFAULT_ROUNDED_CORNER.dp)` (`:56`), параметра формы/тени нет; `titleTextStyle: TextStyle = LexemeStyle.BodyM` — параметризуем (`:45`); disabled-цвета `colorScheme.onSecondary`/`secondary` (`:41,44`). Никакой `.shadow()` не применяется.

### FlagPlaceholderWidget
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/FlagPlaceholderWidget.kt:17-33` — `letter: String`, `modifier` (дефолт `size(24.dp)`); круг `grayTextColor.copy(alpha = 0.2f)`, буква `LexemeStyle.BodyL` + `grayTextColor`. Стиль текста/цвета не параметризованы.

### ImageFlagWidget
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/ImageFlagWidget.kt:18-31` — `flagRes`, `modifier`, `contentDescription`; внутри цепочка `modifier.size(24.dp).clip(CircleShape)`, `ContentScale.FillHeight`. Вызывающие переопределяют размер внешним `Modifier.size(48.dp)`.

### SystemBarsWidget
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/SystemBarsWidget.kt:10-15, :34-40` — две перегрузки: единый `color` или раздельные `statusBarColor`/`navigationBarColor`; плюс флаги тёмных иконок. Принимает любой `Color` — препятствий для фона макета нет.

### IconBoxed (используется back-кнопкой AppBar)
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/IconBoxed.kt:26-36` — `iconRes`, `enabled`, `size: Int = 56`, `colorEnabled/colorDisabled`, `clipShape = CircleShape`, `onClick`.

### Полный инвентарь `me.apomazkin.ui` (для справки)
- корень: `ImageTitledWidget`, `ImageBgGradWidget`, `ImageFlagWidget`, `IconBoxed`, `FlagPlaceholderWidget`, `ErrorStateWidget`, `ImageRoundedWidget`, `SystemBarsWidget`, `LexemeBottomSheet`
- `btn/`: `PrimaryFullButtonWidget`, `PrimaryFabWidget`, `PrimaryLongFabWidget`, `PrimaryTextButtonWidget`, `SecondaryButtonWidget`, `SecondaryFabWidget`, `AlarmButtonWidget`, `CancelButtonWidget`, `ErrorButtonWidget`; `btn/base/`: `LexemeButton`, `LexemeFab`, `LexemeLongFab`, `LexemeTextButton`, `GradientButtonWidget`, `GradientFabWidget`, `OutlineButtonWidget`
- `input/`: `PrimaryTextFieldWidget`; `input/base/`: `LexemeTextFieldWidget`
- `dialog/`: `AlarmDialogWidget`; `dialog/base/`: `LexemeDialog`
- `dropdown/`: `LexemeRadioRow`, `LexemeRadioMenuItem`, `LexemeSubmenuMenuItem`
- `text/base/`: `LexemeEditableText`; `unused/`: `CheckedTextWidget`, `WordEditorWidget`

Отдельного примитива «поиск-пилюля», «бейдж», «заголовок экрана» в core:ui нет.

## 3. Тема (`modules/core/theme`)

### Color.kt
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/theme/src/main/java/me/apomazkin/theme/Color.kt`

- Файл — плоский список одиночных `val` + объект `LexemeColor` (`:48-92`).
- `LexemeColor.primary = Color(0xFF4A49BC)` (`:50`), `secondary = Color(0xFF19191B)` (`:56`), `background = whiteColor` (`:74`).
- Имеющиеся серые: `grayTextColor = 0xFF7B7E85` (`:34`), `unselectedGreyColor = 0xff95989D` (`:30`), `dividerColor = 0xffE4E5E7` (`:31`), `enableIconColor = 0xff252628` (`:32`), `disableButtonTitleColor = 0xffB5AEE1` (`:33`).
- **Цветов макета нет**: `#FCFCFA`, `#8A8A90`, `#55555C`, `#9A9AA2`, `#F1F0EC` в файле отсутствуют; токена тени тоже нет.

### LexemeStyle.kt (типографика)
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/theme/src/main/java/me/apomazkin/theme/LexemeStyle.kt`

- `LexemeStyle`: `H1..H6` + `BodyXL(Bold)/BodyL(Bold)/BodyM(Bold)/BodyS(Bold)/labelSmall` (`:116-132`).
- Крупные стили: H3 = ExtraBold 36sp (`:34-39`), H4 = SemiBold 28sp (`:40-45`), H5 = Medium 24sp (`:47-52`), H6 = Medium 20sp (`:54-59`).
- Факт: `PoppinsRegular`/`PoppinsMedium` объявлены (`:11-17`), но вся `Typography` собрана на `defaultFontFamily = FontFamily.Default` (`:19`) — Poppins в стилях не используется.

### Theme.kt
`/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/theme/src/main/java/me/apomazkin/theme/Theme.kt:56-64` — `AppTheme` всегда применяет одну схему (`appColorScheme` на базе `darkColorScheme(...)` из LexemeColor), `shapes = Shapes()` (дефолт M3), т.е. кастомных shape-токенов нет.

## 4. Строковые ресурсы (`core/core-resources`)

`values/strings.xml` / `values-ru-rRU/strings.xml`, префикс `dictionary_` (номера строк EN | RU):

| key | EN (`values`) | RU (`values-ru-rRU`) |
|---|---|---|
| `dictionary_selection_title` (53\|43) | Dictionaries | Словари |
| `dictionary_name_hint` (63\|53) | Dictionary name | Название словаря |
| `dictionary_new` (65\|55) | New dictionary | Новый словарь |
| `dictionary_save` (71\|61) | Save | Сохранить |
| `dictionary_create` (72\|62) | Create | Создать |
| `dictionary_filter_flags_hint` (73\|63) | **Filter flags…** | **Поиск по стране или языку…** |

- Рассинхрон EN/RU хинта поиска подтверждён (зафиксирован и в scope).
- Строк «заголовок-подзаголовок формы» и «ничего не найдено» не существует — добавлять. `dictionary_new` («New dictionary / Новый словарь») уже есть — используется где-то ещё, но по смыслу совпадает с заголовком макета.
- Прочие dictionary_-ключи: `selection_subtitle`, `selection_button`, `selection_error`, `bind_language`, `continue`, `delete_title`, `delete_message`, `empty_title`, `empty_subtitle`.

## 5. Drawable-иконки

- Галочки: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/drawable/ic_selected.xml` — вектор-галочка 24dp, fill `#252628`; `ic_confirm.xml` — галочка 24dp, fill `#ffffff` (рукописный штрих). Обе — просто path, тонируемы через `Icon(tint=…)`.
- Прочее: `ic_back.xml`, `ic_close.xml`, `ic_clear.xml` в том же каталоге.
- **Отдельной иконки поиска (`ic_search`) в ресурсах нет** — экран использует `Icons.Default.Search` из material-icons (`DictionaryFormWidget.kt:86`), закрытие — `Icons.Default.Close` (`:96`).

## 6. Паттерны нового дизайна — есть ли аналоги в проекте

- **Крупный in-content заголовок экрана**: H3/H4 в контенте экранов не используются. Максимум — `LexemeStyle.H5` (24sp) в empty-state (`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/widget/EmptyDictionaryWidget.kt:30`) и в AppBar-тайтлах. Аналога «большой заголовок в теле экрана» нет.
- **Поиск-пилюля** (капсульное поле): нет. Единственные скругления полей — `RoundedCornerShape(0.dp)` в `LexemeTextFieldWidget.kt:91` и дефолтный M3 shape у `OutlinedTextField` формы. Кастомных `BasicTextField`-поисковиков в modules/ нет.
- **Бейдж-галочка поверх круга** (selected marker): нет. Единственный selected-маркер флага — `BorderStroke` (`FlagGridWidget.kt:49-53`). `Icons.Default.Check` в modules/ не используется.
- **Цветная тень** (`.shadow(ambientColor/spotColor)`): в modules/ не используется вообще.
- **Не-белый фон экрана**: есть два прецедента передачи цвета в `SystemBarsWidget` — Splash (`modules/screen/splash/.../SplashScreen.kt`, `colorBackground`) и VocabularyTab в action-mode (`modules/screen/dictionaryTab/.../ui/VocabularyTabScreen.kt`, `colorScheme.secondary`). Остальные экраны — `whiteColor`.
- **Сетки**: `GridCells` в живом коде один — `GridCells.Fixed(5)` целевого `FlagGridWidget.kt:37` (в `modules/widget/chipPicker/.../ChipPickerWidget.kt` — закомментирован `Fixed(4)`).

## 7. Потребители общих виджетов (risk-лист для рестайла)

- `LexemeTextFieldWidget(` — форма + `modules/screen/wordcard/.../widget/WordFieldWidget.kt` + `modules/widget/component_widgets/.../dialogs/CreateComponentDialog.kt`, `EditComponentDialog.kt`, `RenameComponentDialog.kt` и др. (10+ вызовов), плюс обёртка `input/PrimaryTextFieldWidget.kt` в core:ui.
- `PrimaryFullButtonWidget(` — форма + `modules/core/ui/.../ErrorStateWidget.kt` + component_widgets/диалоги.
- `FlagPlaceholderWidget(` — форма + `modules/screen/dictionary/.../list/widget/DictionaryListItemWidget.kt`.
- `ImageFlagWidget(` — форма, `FlagGridWidget`, `DictionaryListItemWidget`.

Подтверждает аспект scope: у всех общих виджетов есть внешние потребители, глобальный рестайл затронет другие экраны.

## Вердикт

Аналог: **не найден**.

Ни один из ключевых паттернов макета не существует в проекте: нет крупного in-content заголовка (максимум H5 в empty-state), нет капсульного поля поиска (поля — 0.dp или дефолт M3), нет бейджа-галочки на круге (выбор флага — только кольцо-бордер), нет цветных теней (`.shadow` не используется), нет тёплого фона (везде `whiteColor`, кроме Splash/VocabularyTab-actionmode). Цвета макета в `Color.kt` отсутствуют. Все элементы нового дизайна придётся строить заново; из готового переиспользуемы: `SystemBarsWidget` (принимает любой цвет), `ImageFlagWidget`/`FlagPlaceholderWidget` (размер через modifier), галочки `ic_selected`/`ic_confirm`, стили `LexemeStyle.H3–H6`. Дополнительный факт для `ui_layout`: локальный `DictionaryAppBar` формы не принимает `titleResId` — тайтл захардкожен (`dictionary_selection_title`), схема заголовка из scope потребует добавления параметра в этот локальный виджет.

_model: claude-fable-5_
