# Dictionary Management — Спецификация

Управление словарями: создание, переключение, (будущее: редактирование, удаление).

---

## Как сейчас

### Доменная модель

Сущность называется "язык" (`Language`). Один язык = один словарь.
Идентификация — по `numericCode` (ISO 3166-1 numeric).

```
┌─────────────────────────┐
│ languages               │
├─────────────────────────┤
│ id        INTEGER PK    │  autoGenerate
│ numericCode INTEGER UNQ │  ISO 3166-1, обязательный, UNIQUE
│ code      TEXT          │  не используется нигде
│ name      TEXT?         │  nullable, локализованная строка (зависит от локали)
│ addDate   INTEGER       │  timestamp создания
│ changeDate INTEGER?     │  timestamp изменения
└─────────────────────────┘
     ↑ FK
┌─────────────────────────┐
│ words                   │
├─────────────────────────┤
│ lang_id   INTEGER FK    │  → languages.id, CASCADE
│ ...                     │
└─────────────────────────┘
     ↑ FK
┌─────────────────────────┐
│ write_quiz              │
├─────────────────────────┤
│ lang_id   INTEGER       │  дублирует word→language, для прямой фильтрации
│ lexeme_id INTEGER FK    │  → lexemes.id, CASCADE
│ ...                     │
└─────────────────────────┘
```

### API контракт

```kotlin
interface LangApi {
    suspend fun addLang(numericCode: Int, name: String): Long
    suspend fun getLang(numericCode: Int): LanguageApiEntity?
    suspend fun getLangList(): List<LanguageApiEntity>
    fun flowLangList(): Flow<List<LanguageApiEntity>>
}
```

Поиск словаря — по `numericCode`, не по `id`.

### Entity

```kotlin
// Room
data class LanguageDb(
    val id: Long?,
    val numericCode: Int,       // обязательный
    val code: String,           // не используется
    val name: String?,          // nullable
    val addDate: Date,
    val changeDate: Date?,
)

// API
data class LanguageApiEntity(
    val id: Int,                // Int, не Long — несогласованность
    val numericCode: Int,
    val code: String,
    val name: String,
    val addDate: Date,
    val changeDate: Date?,
)

// UI (dropdown словарей)
data class DictUiEntity(
    val numericCode: Int,       // идентификатор
    val title: String,
    val flagRes: Int,           // обязательный
)

// UI (экран создания)
data class PresetLangUi(
    val flagRes: Int,
    val countryNumericCode: Int,
    val langNameRes: Int,       // @StringRes
)
```

### Preference

```
Ключ:   CURRENT_LANG_NUMERIC_CODE_INT
Тип:    Int (numericCode)
```

Двойной lookup при каждом чтении:
```
prefs → numericCode → getLang(numericCode) → id → использовать
```

### Переключение словарей

```
User тапает словарь в dropdown
  → setInt(CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
  → Flow-подписчики:
    ├── DictionaryAppBar → обновляет заголовок
    ├── VocabularyTab → перезагружает список слов
    └── Quiz/Stats → при открытии читают из prefs
```

### Создание словаря

```
Экран: 6 захардкоженных языков (LanguageData)
User выбирает → SaveLang(numericCode, langName)
  → addLang(numericCode, langName) — INSERT в Room
  → saveCurrentLang(numericCode) — запись в prefs
  → закрытие экрана
```

- Выбрать можно только один язык
- Нет проверки на дубли (UNIQUE constraint → краш)
- `langName` резолвится из string resource в composable
- Нет кнопки "назад" (кроме первого запуска это неудобно)

### Удаление / редактирование словаря

Не реализовано. Нет UI, нет API.

---

## Как должно быть

### Доменная модель

Сущность — "словарь" (`Dictionary`). Не привязан к конкретному языку.
Идентификация — по `id`.

```
┌──────────────────────────┐
│ dictionaries             │
├──────────────────────────┤
│ id          INTEGER PK   │  autoGenerate
│ numericCode INTEGER?     │  nullable, опциональный (словарь без языка)
│ name        TEXT NOT NULL │  user-defined название
│ description TEXT?        │  опциональный комментарий
│ addDate     INTEGER      │  timestamp создания
│ changeDate  INTEGER?     │  timestamp изменения
└──────────────────────────┘
     ↑ FK
┌──────────────────────────┐
│ words                    │
├──────────────────────────┤
│ dictionary_id INTEGER FK │  → dictionaries.id, CASCADE
│ ...                      │
└──────────────────────────┘
     ↑ FK
┌──────────────────────────┐
│ write_quiz               │
├──────────────────────────┤
│ dictionary_id INTEGER    │  для прямой фильтрации
│ lexeme_id   INTEGER FK   │  → lexemes.id, CASCADE
│ ...                      │
└──────────────────────────┘
```

Изменения:
- Таблица `languages` → `dictionaries`
- `numericCode` — nullable, не UNIQUE (несколько словарей могут ссылаться на один язык)
- `code` — удалён (не использовался)
- `name` — NOT NULL, user-defined
- `description` — новое поле
- `lang_id` → `dictionary_id` в `words` и `write_quiz`

### API контракт

```kotlin
interface DictionaryApi {
    suspend fun addDictionary(name: String, numericCode: Int? = null, description: String? = null): Long
    suspend fun getDictionary(id: Long): DictionaryApiEntity?
    suspend fun getDictionaryList(): List<DictionaryApiEntity>
    fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
    suspend fun updateDictionary(id: Long, name: String, description: String?)   // будущее
    suspend fun deleteDictionary(id: Long)                                        // будущее
}
```

Поиск — по `id`, не по `numericCode`.

### Entity

```kotlin
// Room
data class DictionaryDb(
    val id: Long?,
    val numericCode: Int?,          // nullable
    val name: String,               // NOT NULL
    val description: String?,       // новое
    val addDate: Date,
    val changeDate: Date?,
)

// API
data class DictionaryApiEntity(
    val id: Long,                   // Long (было Int — несогласованность)
    val numericCode: Int?,          // nullable
    val name: String,
    val description: String?,       // новое
    val addDate: Date,
    val changeDate: Date?,
)

// UI (dropdown словарей)
data class DictUiEntity(
    val id: Long,                   // новое — идентификатор
    val numericCode: Int?,          // nullable
    val title: String,
    val flagRes: Int?,              // nullable (словарь без флага)
)

// UI (экран создания — пресеты)
data class PresetLangUi(
    val flagRes: Int,
    val countryNumericCode: Int,
    val langNameRes: Int,
)
// PresetLangUi не меняется — пресеты языков остаются
```

### Preference

```
Ключ:   CURRENT_DICTIONARY_ID_LONG
Тип:    Long (id записи в таблице dictionaries)
```

Прямой доступ, без промежуточного lookup:
```
prefs → id → использовать
```

Fallback: если ключ пуст → взять первый словарь из списка → записать id.

### Переключение словарей

```
User тапает словарь в dropdown
  → setLong(CURRENT_DICTIONARY_ID_LONG, dictionary.id)
  → Flow-подписчики:
    ├── DictionaryAppBar → обновляет заголовок
    ├── VocabularyTab → перезагружает список слов
    └── Quiz/Stats → при открытии читают из prefs
```

Логика та же, тип ключа другой (Long вместо Int, id вместо numericCode).

### Создание словаря (текущий этап — IS441)

Пока остаётся экран с 6 пресетами. Изменения:
- При создании: `addDictionary(name, numericCode)` → получаем `id`
- Сохранение текущего: `setLong(CURRENT_DICTIONARY_ID_LONG, id)`
- Добавить AppBar с кнопкой "назад" (кроме первого запуска)
- Добавить фильтрацию уже созданных словарей

### Создание словаря (будущее — кастомный)

```
Экран:
  [Название словаря]        — текстовое поле, обязательно
  [Описание]                — текстовое поле, опционально
  Язык (опционально):       — выбор из списка
    ○ English / Spanish / ...
    ○ Без языка
  Флаг (опционально):       — выбор из подмножества стран по языку
    🇬🇧 🇺🇸 🇦🇺 🇨🇦 ...
  [ Создать ]
```

### Редактирование словаря (будущее)

- Изменение `name`, `description`
- Изменение флага (numericCode)
- UI: экран аналогичный созданию, но с предзаполненными полями

### Удаление словаря (будущее)

- CASCADE: удаление словаря → удаление всех words → lexemes → write_quiz
- Подтверждение через диалог
- Если удаляется текущий словарь → переключиться на первый из оставшихся
- Если словарей не осталось → перенаправить на экран создания

### UI — дефолтная иконка для словарей без флага

Когда `numericCode == null` (словарь без языка) → `flagRes == null` → показать дефолтную иконку (например, ic_dictionary или ic_book). Нужно добавить в core-resources.

---

## Export/Import

### Сейчас

```kotlin
data class Dump(
    val languages: List<LanguageDump>,
    val words: List<WordDump>,        // содержит langId
    val lexemes: List<LexemeDump>,
    val quizzes: List<WriteQuizDump>, // содержит langId
)
```

### Как должно быть

```kotlin
data class Dump(
    val version: Int = 2,                       // версионирование
    val dictionaries: List<DictionaryDump>,      // было languages
    val words: List<WordDump>,                   // dictionaryId вместо langId
    val lexemes: List<LexemeDump>,
    val quizzes: List<WriteQuizDump>,            // dictionaryId вместо langId
)
```

Обратная совместимость при импорте:
- `version == 1` (или отсутствует): маппить `LanguageDump` → `DictionaryDump`, `langId` → `dictionaryId`
- `version == 2`: читать напрямую
