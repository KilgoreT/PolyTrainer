# Форма словаря — Спецификация

Экран создания и редактирования словаря. Одна форма для обоих действий.

---

## Навигация

- Открывается из списка словарей (тап на элемент = редактирование, "Новый словарь" = создание)
- Или напрямую через route DICTIONARY_CREATE (из dropdown AppBar)
- Также через `DICTIONARY_SETUP` route — создание словаря после splash, без AppBar (`showAppBar = false`). Этот режим активируется при любом запуске, когда в БД нет ни одного словаря — как при самом первом запуске, так и после того, как пользователь удалил все словари и закрыл приложение (см. `dictionary-list.md`, user-journey пустого списка). С точки зрения пользователя такой запуск — онбординг создания первого словаря.
- Route с параметром `editId` — optional, если есть = редактирование
- Вся навигация через `NavigationEffect.Back` → `FormNavigationEffectHandler` (наследует `MateNavigationEffectHandler`) → `FormNavigator.back()` → `FormNavigatorImpl` в `app/.../navigator/`. Для SETUP-флоу `FormNavigatorImpl` дёргает `openMainScreen()`; для CREATE — `popBackStack()`

---

## Режим: создание vs редактирование

Определяется `editingDictionaryId`:
- `null` → создание. Кнопка "Создать".
- Не null → редактирование. Кнопка "Сохранить". Форма предзаполнена данными словаря.

---

## Поля формы

### Название словаря

- Обязательное. `isNotBlank()` для активации кнопки.
- Без ограничения длины. Дубликаты разрешены.
- Поле в Material-стиле (`NameFieldWidget`): плавающий uppercase-лейбл «НАЗВАНИЕ СЛОВАРЯ» акцентным цветом, underline 2dp и курсор акцентного цвета.
- Слева от поля — превью выбранного флага (`FlagPreviewWidget`, 58dp с двойным кольцом: кольцо 2dp primary + зазор 2dp фоном) или placeholder с первой буквой названия (live по мере ввода; пустое имя — пустой круг).

### Выбор флага

- Grid всех флагов стран (250) — всегда видим (без toggle). 4 колонки, флаги 56dp.
- Под каждым флагом — название страны на языке девайса (до двух строк с переносом, без ellipsis).
- Выбранный флаг: кольцо 2.5dp акцентного цвета с внутренним зазором + бейдж-галочка 24dp (акцентный круг с обводкой цветом фона, иконка `ic_confirm`) в правом нижнем углу + подпись акцентным bold.
- Над grid — поиск-капсула (`SearchPillWidget`: подложка `searchPillColor`, radius 14, иконка-лупа, кнопка очистки; фильтрует по стране на английском, на языке девайса, и по языку; case-insensitive substring).
- Фильтрация с debounce ~300ms. Реализована в UseCase через Flow.
- Если поле фильтрации пустое — показаны все флаги.
- Непустой запрос без совпадений → по центру зоны grid вторичный текст «Ничего не найдено» (`dictionary_flags_not_found`); условие производное в UI (`flags.isEmpty() && flagFilter.isNotBlank()`), на старте экрана текст не мелькает.
- Тап на флаг → выбран, показан в превью рядом с полем названия.
- Тап на выбранный флаг → снятие выбора.
- Флаг не обязателен — словарь сохраняется с `numericCode = null`.

### Источник флагов

blongho `World.getAllCountries()` + `World.getLanguagesFrom()`. Загружаются один раз (lazy в UseCase). Локализованное имя страны через `Locale("", alpha2).getDisplayCountry(deviceLocale)`.

---

## Бизнес-логика

### Создание

1. Пользователь заполняет название
2. Опционально выбирает флаг
3. "Создать" → INSERT в Room → `setCurrentDictionary(id)` → закрытие формы

### Редактирование

1. Форма предзаполнена (name, flag по numericCode — резолвится в EffectHandler через UseCase)
2. Пользователь меняет поля
3. "Сохранить" → UPDATE в Room → закрытие формы

### Навигация назад

- Данные теряются молча. Без диалога подтверждения.
- Реализуется через `Msg.Back` → `NavigationEffect.Back` → `FormNavigator.back()`

### Фильтрация флагов (отдельный sealed effect)

Чтобы не было конфликта двух handlers на одном sealed-эффекте (см. урок IS471), фильтрация вынесена в отдельный `FlagFilterEffect` / `FlagFilterFlowHandler`:

- `DictionaryFormEffect` — операции с БД (Save, Update, LoadDictionary)
- `FlagFilterEffect` — отдельный sealed для `FilterFlags(query)`
- `FlagFilterFlowHandler` — подписка на отфильтрованный Flow + обработка `FilterFlags` для обновления query

Правило: один sealed эффект — один handler. Иначе `MateTypedEffectHandler` обработает один эффект дважды.

---

## UI

### Заголовок экрана (схема по режимам, IS485)

- **Онбординг** (`showAppBar = false`): AppBar отсутствует; в контенте — заголовок «Новый словарь» (`dictionary_new`, H4) + подзаголовок «Выберите язык и назовите словарь» (`dictionary_form_subtitle`).
- **Create/Edit** (AppBar есть): заголовок живёт в локальном `DictionaryAppBar` (`titleResId`: create → `dictionary_new`, edit → `dictionary_edit_title`); внутренний дубль заголовка не рисуется; подзаголовок остаётся в контенте.
- Фон экрана и system bars — `formBackground` (#FCFCFA).

### Layout

```
+-----------------------------------------+
|  [AppBar: ← Заголовок]  (create/edit)   |
+-----------------------------------------+
|  Новый словарь          (онбординг)     |  ← FormHeaderWidget
|  Выберите язык и назовите словарь       |     (подзаголовок — всегда)
+-----------------------------------------+
|  [FlagPreview]  [ НАЗВАНИЕ СЛОВАРЯ    ] |  ← фиксирован
|      58dp       [ имя + underline     ] |
+-----------------------------------------+
|  ( 🔍 Поиск по стране или языку…      ) |  ← капсула, фиксирована
+-----------------------------------------+
|  [F]     [F]     [F]     [F]            |  ← 4 колонки, скроллится
|  name    name    name    name           |     подписи до 2 строк
|  ...     («Ничего не найдено» если      |
|           фильтр без совпадений)        |
+-----------------------------------------+
|  [ Создать / Сохранить ]  r16 + тень    |  ← фиксирован снизу
+-----------------------------------------+
```

Заголовочная зона, превью+имя, поиск и кнопка — фиксированы. Grid скроллится между ними (`Modifier.weight(1f)`). `imePadding()` — кнопка видна при открытой клавиатуре. Горизонтальный отступ контента — 24dp.

### Компоненты

- Заголовочная зона: `FormHeaderWidget` (title только при `showAppBar=false`, subtitle всегда)
- Превью флага: `FlagPreviewWidget` — 58dp, двойное кольцо (2dp primary + зазор 2dp); внутри `ImageFlagWidget` (50dp) или `FlagPlaceholderWidget` с первой буквой
- Поле названия: `NameFieldWidget` — BasicTextField + uppercase-лейбл + underline 2dp + акцентный курсор
- Поиск: `SearchPillWidget` — Surface r14 на `searchPillColor`, `Icons.Default.Search`, кнопка очистки при непустом вводе
- Grid: `LazyVerticalGrid(columns = Fixed(4))`, элемент — флаг 56dp + подпись (2 строки, перенос); выбранный — кольцо 2.5dp + бейдж-галочка + bold-подпись; empty-state «Ничего не найдено»
- Кнопка: `SubmitButtonWidget` — высота 56, radius 16, цветная тень от primary при enabled (elevation ~10dp, тюнинг на девайсе), disabled приглушённая без тени; активна при `name.isNotBlank()`, отступ 16dp снизу
- Все новые виджеты — локальные в `form/widget/` (общие `core:ui`-виджеты не менялись); цвета — `formBackground`, `formTextSecondary`, `formTextTertiary`, `formTextHint`, `searchPillColor` в `theme/Color.kt`

---

## Доменная модель

### DictionaryUseCase (методы формы)

```kotlin
suspend fun addDictionary(name: String, numericCode: Int?): Long
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
suspend fun setCurrentDictionary(id: Long)
fun updateFilter(query: String)
fun flagsFlow(): Flow<List<CountryFlagItem>>
suspend fun getDictionary(id: Long): DictionaryItem
fun findFlag(numericCode: Int): CountryFlagItem?
```

### CountryFlagItem

```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val localizedName: String = "",
    val flagRes: Int,
    val languages: List<String> = listOf(),
)
```

---

## Фильтрация

Реализована в UseCase. Ищет по трём полям: countryName (en), localizedName (device locale), languages.

```kotlin
fun filterFlags(allFlags: List<CountryFlagItem>, query: String): List<CountryFlagItem> {
    if (query.isBlank()) return allFlags
    val q = query.trim().lowercase()
    return allFlags.filter { flag ->
        flag.countryName.lowercase().contains(q) ||
        flag.localizedName.lowercase().contains(q) ||
        flag.languages.any { lang -> lang.lowercase().contains(q) }
    }
}
```
