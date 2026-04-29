# Форма словаря — Спецификация

Экран создания и редактирования словаря. Одна форма для обоих действий.

---

## Навигация

- Открывается из списка словарей (тап на элемент = редактирование, "Новый словарь" = создание)
- Или напрямую через route DICTIONARY_CREATE (из dropdown AppBar)
- `onBackPress` -> возврат без сохранения
- `onClose` -> после успешного сохранения

---

## Режим: создание vs редактирование

Определяется `editingDictionaryId`:
- `null` -> создание. Кнопка "Создать".
- Не null -> редактирование. Кнопка "Сохранить". Форма предзаполнена данными словаря.

---

## Поля формы

### Название словаря

- Обязательное. `isNotBlank()` для активации кнопки.
- Без ограничения длины. Дубликаты разрешены.
- Рядом с полем -- выбранный флаг (или placeholder если не выбран).

### Выбор флага

- Grid всех флагов стран -- всегда видим (без toggle).
- Над grid -- поле фильтрации (одно поле, фильтрует по стране И по языку, case-insensitive substring).
- Фильтрация ТОЛЬКО через поле фильтрации. Название словаря НЕ влияет на фильтрацию флагов.
- Если поле фильтрации пустое -- показаны все флаги.
- Тап на флаг -> выбран, показан рядом с полем названия.
- Тап на выбранный флаг -> снятие выбора.
- Флаг не обязателен -- словарь сохраняется с `numericCode = null`.

### Источник флагов

blongho `World.getAllCountries()` + `World.getLanguagesFrom()`. Загружаются один раз при открытии формы.

`getAllCountryFlags()` в UseCase обогащает каждый `CountryFlagItem` списком языков из `countryProvider.getLanguagesForCountry(numericCode)`.

---

## Бизнес-логика

### Создание

1. Пользователь заполняет название
2. Опционально выбирает флаг
3. "Создать" -> INSERT в Room -> `setCurrentDictionary(id)` -> закрытие формы

### Редактирование

1. Форма предзаполнена (name, flag по numericCode)
2. Пользователь меняет поля
3. "Сохранить" -> UPDATE в Room -> закрытие формы

### Навигация назад

- Данные теряются молча. Без диалога подтверждения.

---

## UI

### Layout

```
+-----------------------------------------+
|  [Flag/Placeholder]  [ Название       ] |  <- фиксирован сверху
+-----------------------------------------+
|  [ Фильтр флагов...                  ] |  <- фиксирован
+-----------------------------------------+
|  [F] [F] [F] [F] [F]                   |  <- скроллится
|  [F] [F] [F] [F] [F]                   |     занимает всё
|  [F] [F] [F] [F] [F]                   |     оставшееся
|  ...                                    |     пространство
+-----------------------------------------+
|  [ Создать / Сохранить ]                |  <- фиксирован снизу
+-----------------------------------------+
```

Название, фильтр и кнопка -- фиксированы. Grid скроллится между ними (`Modifier.weight(1f)`).

### Компоненты

- Flag/Placeholder: `ImageFlagWidget` (48dp) если выбран, placeholder если нет
- Поле фильтрации: `OutlinedTextField` с иконкой поиска, кнопка очистки. Фильтрация с debounce (~300ms) -- не при каждом символе
- Grid: `LazyVerticalGrid(columns = Fixed(5))`, каждый элемент 48dp, `Modifier.weight(1f)` для заполнения пространства
- Выбранный флаг: `BorderStroke`
- Кнопка: `PrimaryFullButtonWidget`, активна при `name.isNotBlank()`

---

## Доменная модель

### DictionaryUseCase (методы формы)

```kotlin
suspend fun addDictionary(name: String, numericCode: Int?): Long
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
suspend fun setCurrentDictionary(id: Long)
fun getAllCountryFlags(): List<CountryFlagItem>
```

### CountryFlagItem

```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val flagRes: Int,
    val languages: List<String> = listOf(),
)
```

---

## Фильтрация

```kotlin
fun filterFlags(allFlags: List<CountryFlagItem>, query: String): List<CountryFlagItem> {
    if (query.isBlank()) return allFlags
    val q = query.trim().lowercase()
    return allFlags.filter { flag ->
        flag.countryName.lowercase().contains(q) ||
        flag.languages.any { lang -> lang.lowercase().contains(q) }
    }
}

val filteredFlags = filterFlags(allFlags, flagFilter)
```

---

## Что удаляется

- Чекбокс "Привязать к языку" и вся логика привязки
- Bottom sheet выбора языка (список языков, поиск, выбор)
- Двухшаговый flow: выбрал язык → увидел флаги этого языка
- Поле выбранного языка в форме

---

## Ключевые ограничения

1. Привязка к языку убрана -- экран НЕ оперирует понятием "язык" для словаря.
2. Фильтрация флагов -- ТОЛЬКО через поле фильтрации. Название словаря не используется как неявный фильтр.
3. Debounce на фильтрации -- ~300ms, чтобы не фильтровать при каждом нажатии клавиши.
4. Grid флагов занимает всё оставшееся пространство (`Modifier.weight(1f)`). Название, фильтр и кнопка фиксированы.

_model: claude-opus-4-6[1m]_
