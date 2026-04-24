# IS441. Ресерчи

---

## R1. Влияние концепции "словарь вместо языка" на доменную область

### Контекст

Сейчас приложение построено вокруг концепции "язык": пользователь создаёт язык, добавляет слова к языку, квиз фильтруется по языку. Вся цепочка данных завязана на `langId`.

Предлагается расширить до "словарь": произвольное название ("Биология на английском"), опциональный флаг, опциональный язык. Словарь = контейнер для пар "термин — определение", не обязательно привязанный к иностранному языку.

### Масштаб изменений

**Затронуто: ~68 файлов** по всем слоям приложения.

Это не рефакторинг одного экрана — это **изменение ключевой доменной сущности**, от которой зависит всё остальное.

---

### Слой 1: База данных (Room)

#### Таблица `languages`

Текущая схема:
```sql
CREATE TABLE languages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numericCode INTEGER UNIQUE,   -- ISO 3166-1 numeric
    code TEXT,
    name TEXT,
    addDate INTEGER,
    changeDate INTEGER
)
```

Что нужно:
- `numericCode` перестаёт быть обязательным (словарь без языка → без numericCode).
- `name` становится user-defined строкой (уже сейчас так, но генерируется автоматически).
- Нужны новые поля: `description` (опциональный комментарий), может быть `flagNumericCode` отдельно от `numericCode`.
- Таблицу логично переименовать в `dictionaries`.

#### FK-каскад

```
languages.id ← words.lang_id ← lexemes.word_id ← write_quiz.lexeme_id
                                                   write_quiz.lang_id
```

**Проблема:** `write_quiz` хранит `lang_id` **отдельно** от `lexeme_id`.
Это дублирование — `lang_id` квиза можно вычислить через `lexeme → word → lang_id`.
Но оно используется для прямой фильтрации в SQL (без JOIN) — это оптимизация.

При переименовании `lang_id` → `dictionary_id`:
- Нужна Room-миграция (11-я по счёту).
- Все SQL-запросы с `WHERE lang_id = :langId` нужно обновить.
- FK constraints нужно пересоздать.

#### SQL-запросы в WordDao (20+ штук)

Все запросы с фильтрацией по `lang_id`:

| Запрос | Что делает |
|--------|-----------|
| `getTermList(langId)` | Все слова по языку |
| `searchTerms(langId, pattern)` | Поиск слов с пагинацией |
| `searchTermsPaging(langId, pattern)` | Paging3 source по языку |
| `getRandomWriteQuizList(grade, limit, langId)` | Случайные квизы по грейду |
| `getEarliest(limit, langId)` | Давно не повторявшиеся |
| `getFrequentMistakes(limit, langId)` | Частые ошибки |
| `addWordSuspend(langId, value)` | Добавление слова |
| Статистика: `flowWordCount`, `flowLexemeCount`, `flowQuizCount` | Счётчики по языку |

Все эти запросы — **фильтр по контейнеру**. Если контейнер = словарь, логика не меняется. Меняется только имя параметра.

---

### Слой 2: API контракты (core-db-api)

#### CoreDbApi.LangApi → DictionaryApi

```kotlin
// Сейчас:
interface LangApi {
    suspend fun addLang(numericCode: Int, name: String): Long
    suspend fun getLang(numericCode: Int): LanguageApiEntity?
    suspend fun getLangList(): List<LanguageApiEntity>
    fun flowLangList(): Flow<List<LanguageApiEntity>>
}

// Станет:
interface DictionaryApi {
    suspend fun addDictionary(name: String, numericCode: Int? = null): Long
    suspend fun getDictionary(id: Long): DictionaryApiEntity?
    suspend fun getDictionaryList(): List<DictionaryApiEntity>
    fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
}
```

**Ключевое изменение:** `numericCode` перестаёт быть primary идентификатором словаря. Идентификатор — `id`. `numericCode` становится опциональным атрибутом.

#### Entity модели

Переименование:
- `LanguageApiEntity` → `DictionaryApiEntity`
- `LanguageDump` → `DictionaryDump`
- `WordApiEntity.langId` → `WordApiEntity.dictionaryId`
- `WriteQuizApiEntity.langId` → `WriteQuizApiEntity.dictionaryId`
- `WriteQuizUpsertApiEntity.langId` → `WriteQuizUpsertApiEntity.dictionaryId`

---

### Слой 3: UseCase реализации (app модуль)

Затронуты **все 9 UseCaseImpl**:

| UseCase | Что меняется |
|---------|-------------|
| `CreateDictionaryUseCaseImpl` | `addLang()` → `addDictionary()`, `saveCurrentLang()` → `saveCurrentDictionary()` |
| `DictionaryTabUseCaseImpl` | `getLangId()` → `getDictionaryId()`, все вызовы `langApi` → `dictionaryApi`, `getCurrentDict()` логика |
| `DictionaryAppBarUseCaseImpl` | `flowCurrentDict()` — уже использует `DictUiEntity`, но внутри `langApi`. Пересадка на `dictionaryApi` |
| `QuizChatUseCaseImpl` | `getCurrentLangId()` → `getCurrentDictionaryId()`. Все вызовы `quizApi` с `langId` → `dictionaryId` |
| `QuizTabUseCaseImpl` | Минимальные изменения — зависит от `langApi` |
| `StatisticUseCaseImpl` | Все три flow-метода используют `langId` для фильтрации |
| `WordCardUseCaseImpl` | Использует `langApi` для добавления квизов — `langId` при создании `WriteQuiz` |
| `SplashUseCaseImpl` | `langApi.flowLangList()` → `dictionaryApi.flowDictionaryList()` — проверяет есть ли хоть один словарь |
| `SettingsTabUseCaseImpl` | Export/Import — работает с `LanguageDump` |

---

### Слой 4: Preferences

```kotlin
// Сейчас:
CURRENT_LANG_NUMERIC_CODE_INT("INT_currentLangNumericCode")

// Проблема:
```

Сейчас текущий словарь хранится как `numericCode: Int`.
Для кастомных словарей (без numericCode) это не работает.

**Нужно:** хранить `dictionaryId: Long` вместо `numericCode: Int`.

Новый PrefKey: `CURRENT_DICTIONARY_ID_LONG`.

**Миграция prefsов не нужна.** В `QuizChatUseCaseImpl.getCurrentLangId()` и `DictionaryTabUseCaseImpl.getCurrentDict()` уже есть fallback: если ключ отсутствует или словарь не найден → берётся первый из списка → записывается в prefs. Достаточно просто использовать новый ключ — при первом обращении сработает fallback.

Все readers/writers обновить на новый ключ и тип `Long`.

**Кто читает CURRENT_LANG_NUMERIC_CODE_INT:**
- `DictionaryAppBarUseCaseImpl.flowCurrentDict()` — AppBar
- `DictionaryTabUseCaseImpl.flowCurrentDict()` — VocabularyTab
- `DictionaryTabUseCaseImpl.getCurrentDict()` — sync read
- `QuizChatUseCaseImpl.getCurrentLangId()` — квиз
- `StatisticUseCaseImpl` — все три метода статистики
- `WordCardUseCaseImpl` — добавление квизов

**Кто пишет:**
- `DictionaryAppBarUseCaseImpl.changeDict()` — переключение в dropdown
- `DictionaryTabUseCaseImpl.changeDict()` — то же
- `CreateDictionaryUseCaseImpl.saveCurrentLang()` — при создании

---

### Слой 5: Механизм переключения словарей

Текущий flow:

```
Юзер тапает словарь в dropdown
    ↓
DictDropDownWidget → Msg.ChangeDict(dict)
    ↓
DictionaryAppBarReducer → Effect: ChangeDict
    ↓
EffectHandler → useCase.changeDict(numericCode)
    ↓
PrefsProvider.setInt(CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    ↓
DataStore обновлён
    ↓
Flow-подписчики получают новое значение:
├── DictionaryAppBar → обновляет текущий словарь в UI
├── VocabularyTab → перезагружает список слов
└── (Quiz, Stats — при открытии читают из prefs)
```

**Что меняется:** `numericCode` → `dictionaryId: Long`. Вся реактивная цепочка остаётся, но тип и ключ preference другой.

---

### Слой 6: Квиз-система

**Квиз жёстко привязан к языку/словарю.**

Цепочка:
```
QuizGame.loadData()
    → QuizChatUseCase.getCurrentLangId()       // из prefs
    → QuizChatUseCase.getRandomWriteQuizList(langId)
        → QuizApi.getRandomWriteQuizList(langId)
            → WordDao: WHERE lang_id = :langId
```

Три SQL-запроса (random, earliest, frequentMistakes) — все фильтруются по `lang_id`.

**Для словарей без языка (биология, механика):** квиз работает точно так же. `lang_id` по сути — это `dictionary_id`, фильтр по контейнеру. Семантика "язык" не важна для логики квиза — важен только ID контейнера.

**Вывод:** квиз-система не требует изменения логики. Только переименование `langId` → `dictionaryId` в параметрах и SQL.

---

### Слой 7: Статистика

Три flow-метода:
```kotlin
flowWordCount(langId: Long): Flow<Int>
flowLexemeCount(langId: Long): Flow<Int>
flowQuizStat(langId: Long): Flow<StatisticApiEntity>
```

Все фильтруются по `langId`. Та же история — это фильтр по контейнеру. Переименование, без изменения логики.

---

### Слой 8: Export/Import

```kotlin
data class Dump(
    val languages: List<LanguageDump>,
    val words: List<WordDump>,
    val lexemes: List<LexemeDump>,
    // ...
)
```

`LanguageDump` содержит `numericCode`, `code`, `name`.

**Проблема совместимости:** если пользователь экспортирует на старой версии и импортирует на новой — нужен маппинг `LanguageDump` → `DictionaryDump`. И наоборот.

Нужно:
- Версионирование формата дампа.
- Обратная совместимость при импорте.

---

### Слой 9: UI компоненты

#### DictionaryAppBar (modules/widget/dictionaryappbar)

State:
```kotlin
data class DictionaryAppBarState(
    val currentDict: DictUiEntity? = null,
    val availableDictList: List<DictUiEntity> = emptyList(),
    // ...
)
```

`DictUiEntity` уже абстрагирован от "языка":
```kotlin
data class DictUiEntity(
    val numericCode: Int,  // → станет опциональным
    val title: String,
    val flagRes: Int,      // → станет опциональным (словарь без флага)
)
```

Нужно: добавить дефолтный флаг/иконку для словарей без привязки к стране.

#### DictionaryPicker (modules/widget/dictionarypicker)

Dropdown со списком словарей. Использует `DictUiEntity`. Минимальные изменения — добавить поддержку `flagRes = null`.

#### VocabularyTab

Уже использует `dictionaryId` неявно (через `getCurrentDict()`). Переименование внутри UseCase.

#### CreateDictionary Screen

Полностью переделывается в рамках IS441.

---

### Слой 10: Splash Screen

```kotlin
class SplashUseCaseImpl(private val langApi: CoreDbApi.LangApi) {
    fun langListFlow() = langApi.flowLangList()
}
```

SplashScreen проверяет: есть ли хоть один язык? Если нет → перенаправляет на CreateDictionary.

**Изменение:** `langApi.flowLangList()` → `dictionaryApi.flowDictionaryList()`. Логика та же.

---

### Сравнение entity: было → станет

#### Room: таблица `languages` → `dictionaries`

```
БЫЛО:                                СТАНЕТ:
┌─────────────────────────┐          ┌─────────────────────────┐
│ languages               │          │ dictionaries            │
├─────────────────────────┤          ├─────────────────────────┤
│ id        INTEGER PK    │    →     │ id        INTEGER PK    │
│ numericCode INTEGER UNQ │    →     │ numericCode INTEGER?    │  nullable, не UNQ
│ code      TEXT          │    ✕     │ (удалить — не используется)
│ name      TEXT?         │    →     │ name      TEXT          │  not null, user-defined
│ addDate   INTEGER       │    →     │ addDate   INTEGER       │
│ changeDate INTEGER?     │    →     │ changeDate INTEGER?     │
│                         │          │ description TEXT?       │  НОВОЕ: комментарий
└─────────────────────────┘          └─────────────────────────┘
```

**Ключевое изменение:** идентификатор словаря — `id`, не `numericCode`.

- `numericCode` был UNIQUE и обязательный → становится nullable и опциональный (словарь "Биология" не имеет numericCode)
- `code` — не используется нигде, удалить
- `name` — был nullable, становится not null (user-defined название)
- `description` — новое поле, опциональный комментарий

#### Room: таблица `words`

```
БЫЛО:                                СТАНЕТ:
┌─────────────────────────┐          ┌─────────────────────────┐
│ words                   │          │ words                   │
├─────────────────────────┤          ├─────────────────────────┤
│ id        INTEGER PK    │    →     │ id        INTEGER PK    │
│ lang_id   INTEGER FK    │    →     │ dictionary_id INTEGER FK│  переименование
│ value     TEXT          │    →     │ value     TEXT          │
│ add_date  INTEGER       │    →     │ add_date  INTEGER       │
│ change_date INTEGER?    │    →     │ change_date INTEGER?    │
└─────────────────────────┘          └─────────────────────────┘

FK: lang_id → languages(id)         FK: dictionary_id → dictionaries(id)
```

Только переименование колонки + FK.

#### Room: таблица `write_quiz`

```
БЫЛО:                                СТАНЕТ:
┌─────────────────────────┐          ┌──────────────────────────┐
│ write_quiz              │          │ write_quiz               │
├─────────────────────────┤          ├──────────────────────────┤
│ id        INTEGER PK    │    →     │ id        INTEGER PK     │
│ lang_id   INTEGER       │    →     │ dictionary_id INTEGER    │  переименование
│ lexeme_id INTEGER FK    │    →     │ lexeme_id INTEGER FK     │
│ grade     INTEGER       │    →     │ grade     INTEGER        │
│ score     INTEGER       │    →     │ score     INTEGER        │
│ error_count INTEGER     │    →     │ error_count INTEGER      │
│ add_date  INTEGER       │    →     │ add_date  INTEGER        │
│ last_select_date INT?   │    →     │ last_select_date INT?    │
└─────────────────────────┘          └──────────────────────────┘
```

Только переименование `lang_id` → `dictionary_id`.

#### Kotlin entity: LanguageDb → DictionaryDb

```kotlin
// БЫЛО:
@Entity(tableName = "languages")
data class LanguageDb(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val numericCode: Int,           // UNIQUE, обязательный
    val code: String,               // не используется
    val name: String? = null,       // nullable
    val addDate: Date,
    val changeDate: Date? = null,
)

// СТАНЕТ:
@Entity(tableName = "dictionaries")
data class DictionaryDb(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val numericCode: Int? = null,   // nullable, опциональный
    val name: String,               // not null, user-defined
    val description: String? = null,// НОВОЕ
    val addDate: Date,
    val changeDate: Date? = null,
)
```

#### Kotlin entity: WordDb

```kotlin
// БЫЛО:
@ColumnInfo(name = "lang_id") val langId: Long

// СТАНЕТ:
@ColumnInfo(name = "dictionary_id") val dictionaryId: Long
```

#### Kotlin entity: WriteQuizDb

```kotlin
// БЫЛО:
@ColumnInfo(name = "lang_id") val langId: Long = 0

// СТАНЕТ:
@ColumnInfo(name = "dictionary_id") val dictionaryId: Long = 0
```

#### API entity: LanguageApiEntity → DictionaryApiEntity

```kotlin
// БЫЛО:
data class LanguageApiEntity(
    val id: Int,
    val numericCode: Int,
    val code: String,
    val name: String,
    // ...
)

// СТАНЕТ:
data class DictionaryApiEntity(
    val id: Long,                   // Long вместо Int (консистентность)
    val numericCode: Int? = null,   // nullable
    val name: String,
    val description: String? = null,// НОВОЕ
    // ...
)
```

#### Preference: ключ текущего словаря

```
БЫЛО:
  Ключ:  CURRENT_LANG_NUMERIC_CODE_INT
  Тип:   Int (numericCode страны, например 826 для Англии)
  Чтение: prefsProvider.getInt(...)
  Поиск: langApi.getLang(numericCode) → получить id

СТАНЕТ:
  Ключ:  CURRENT_DICTIONARY_ID_LONG
  Тип:   Long (id записи в таблице dictionaries)
  Чтение: prefsProvider.getLong(...)
  Поиск: НЕ НУЖЕН — id уже прямой идентификатор
```

**Убирается лишний шаг.** Сейчас везде двойная операция: прочитать numericCode из prefs → найти language по numericCode → взять id. Станет: прочитать id из prefs → использовать.

Это упрощает 6 мест чтения — убирается промежуточный lookup `getLang(numericCode)`.

#### UI entity: DictUiEntity

```kotlin
// БЫЛО:
data class DictUiEntity(
    val numericCode: Int,       // обязательный
    val title: String,
    val flagRes: Int,           // обязательный
)

// СТАНЕТ:
data class DictUiEntity(
    val id: Long,               // НОВОЕ: прямой идентификатор
    val numericCode: Int? = null,// nullable
    val title: String,
    val flagRes: Int? = null,   // nullable (словарь без флага)
)
```

Переключение словарей теперь по `id`, не по `numericCode`.

#### UI entity: PresetLangUi (экран создания)

```kotlin
// БЫЛО:
data class PresetLangUi(
    @DrawableRes val flagRes: Int,
    val countryNumericCode: Int,
    @StringRes val langNameRes: Int,
)

// СТАНЕТ: две сущности
// 1. Для пресетов (выбор языка):
data class PresetLangUi(
    @DrawableRes val flagRes: Int,
    val countryNumericCode: Int,
    @StringRes val langNameRes: Int,
)
// 2. Для кастомного словаря (будущее):
// отдельный UI-flow с текстовым вводом
```

PresetLangUi пока не меняется — пресеты языков остаются как есть.

#### Каскад изменений по слоям

```
Room (DictionaryDb)
  │
  ├─ id стал основным идентификатором
  ├─ numericCode стал nullable
  │
  ▼
API (DictionaryApiEntity)
  │
  ├─ Все методы: langId → dictionaryId
  ├─ getLang(numericCode) → getDictionary(id) — убирается lookup
  │
  ▼
UseCase (app модуль)
  │
  ├─ getCurrentLangId() → getCurrentDictionaryId()
  ├─ Убрать двойной lookup (prefs → numericCode → getLang → id)
  ├─ Теперь: prefs → id → готово
  │
  ▼
Preferences
  │
  ├─ CURRENT_DICTIONARY_ID_LONG хранит id напрямую
  ├─ Fallback: если ключ пустой → первый словарь → записать id
  │
  ▼
Widget (DictionaryAppBar, DictionaryPicker)
  │
  ├─ DictUiEntity.id — переключение по id
  ├─ flagRes nullable → дефолтная иконка для словарей без флага
  │
  ▼
Screen (CreateDictionary)
  │
  ├─ После создания: saveCurrentDictionary(id) вместо saveCurrentLang(numericCode)
  ├─ Будущее: кастомный ввод названия + опциональный выбор флага
```

#### Экран словарей: как будет выглядеть

```
СЕЙЧАС:                              ПОТОМ (после полного рефакторинга):
┌──────────────────────┐             ┌──────────────────────┐
│  Создание словаря    │             │  ← Создание словаря  │  ← кнопка назад
│  Выберите язык       │             │                      │
├──────────────────────┤             │  [Название словаря]  │  текстовое поле
│ 🇬🇧 English      ✓  │             │  [Описание]          │  опционально
│ 🇪🇸 Spanish         │             │                      │
│ 🇫🇷 French          │             │  Язык (опционально): │
│ 🇩🇪 German          │             │  ○ English           │
│ 🇮🇹 Italian         │             │  ○ Spanish           │
│ 🇵🇹 Portuguese      │             │  ○ Без языка         │
│                      │             │                      │
│                      │             │  Флаг (опционально): │
│                      │             │  🇬🇧 🇺🇸 🇦🇺 🇨🇦 ...   │
│                      │             │                      │
│   [ Создать ]        │             │   [ Создать ]        │
└──────────────────────┘             └──────────────────────┘
```

Первый этап (IS441): только переименование домена + рефакторинг по конвенциям.
UI экрана пока остаётся как есть — кастомный ввод будет отдельной задачей.

---

### Итоговая оценка

#### Что НЕ меняется (логика остаётся)

- Квиз-система: фильтр по контейнеру, не по "языку"
- Статистика: фильтр по контейнеру
- Переключение словарей: реактивная цепочка та же
- Splash: проверка "есть ли хоть один словарь"
- Пагинация слов: фильтр по контейнеру

#### Что меняется (переименование, ~68 файлов)

| Что | Откуда | Куда |
|-----|--------|------|
| Таблица | `languages` | `dictionaries` |
| Колонка | `lang_id` | `dictionary_id` |
| Entity | `LanguageDb/ApiEntity/Dump` | `DictionaryDb/ApiEntity/Dump` |
| API | `LangApi` | `DictionaryApi` |
| Preference | `CURRENT_LANG_NUMERIC_CODE_INT` | `CURRENT_DICTIONARY_ID_LONG` |
| Параметры | `langId: Long` | `dictionaryId: Long` |

#### Что меняется (новая логика)

| Что | Описание |
|-----|----------|
| `numericCode` становится опциональным | Словарь без привязки к стране/языку |
| `DictUiEntity.flagRes` становится опциональным | Нужна дефолтная иконка |
| Preference хранит `id: Long` вместо `numericCode: Int` | Fallback сделает сам, миграция не нужна |
| Room-миграция #11 | Переименование таблицы + колонок + FK |
| Export/Import | Версионирование формата, обратная совместимость |
| CreateDictionary screen | Новый UX (название, описание, опциональный флаг/язык) |

#### Рекомендация

Переименование `lang → dictionary` — это **механическая работа на ~68 файлов**, но сама по себе не сложная. Основная сложность в:

1. **Room-миграция** — FK constraints, переименование колонок, тестирование миграции.
2. **Preferences** — новый ключ `CURRENT_DICTIONARY_ID_LONG`, fallback подхватит автоматически.
3. **Export/Import совместимость** — старые дампы с `LanguageDump` должны импортироваться.
4. **UI для словарей без флага** — дефолтная иконка, отсутствие `numericCode`.

Рекомендую делать в **отдельной задаче**, не в IS441. IS441 — рефакторинг экрана. Переименование домена — отдельный мерж.
