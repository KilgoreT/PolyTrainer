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
- Рядом с полем — выбранный флаг (ImageFlagWidget 48dp) или placeholder (серый круг с первой буквой названия).

### Выбор флага

- Grid всех флагов стран (250) — всегда видим (без toggle).
- Под каждым флагом — название страны на языке девайса (одна строка, ellipsis).
- Над grid — поле фильтрации (одно поле, фильтрует по стране на английском, на языке девайса, и по языку; case-insensitive substring).
- Фильтрация с debounce ~300ms. Реализована в UseCase через Flow.
- Если поле фильтрации пустое — показаны все флаги.
- Тап на флаг → выбран, показан рядом с полем названия.
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

### Layout

```
+-----------------------------------------+
|  [Flag/Placeholder]  [ Название       ] |  ← фиксирован сверху
+-----------------------------------------+
|  [ Фильтр флагов...                  ] |  ← фиксирован
+-----------------------------------------+
|  [F]    [F]    [F]    [F]    [F]        |  ← скроллится
|  name   name   name   name   name       |     подписи стран
|  [F]    [F]    [F]    [F]    [F]        |     занимает всё
|  ...                                    |     пространство
+-----------------------------------------+
|  [ Создать / Сохранить ]                |  ← фиксирован снизу
+-----------------------------------------+
```

Название, фильтр и кнопка — фиксированы. Grid скроллится между ними (`Modifier.weight(1f)`). `imePadding()` — кнопка видна при открытой клавиатуре.

### Компоненты

- Flag/Placeholder: `ImageFlagWidget` (48dp) если выбран, серый круг с первой буквой названия если нет
- Поле фильтрации: `OutlinedTextField` с иконкой поиска, кнопка очистки
- Grid: `LazyVerticalGrid(columns = Fixed(5))`, каждый элемент — флаг 48dp + подпись страны (1 строка, ellipsis)
- Выбранный флаг: `BorderStroke`
- Кнопка: `PrimaryFullButtonWidget`, активна при `name.isNotBlank()`, отступ 16dp снизу

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
