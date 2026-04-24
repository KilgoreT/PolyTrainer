# IS441. Задачи

---

## 1. Переименование домена: lang → dictionary

Механическое переименование ключевой доменной сущности по всем слоям.
Без изменения логики — только имена. Новая функциональность (кастомные словари, опциональный флаг) — позже.

### Критерии приёмки

**Сборка и тесты:**
- [ ] `./gradlew assembleDebug` проходит
- [ ] `./gradlew testDebugUnitTest` проходит
- [ ] `./gradlew :core:core-db-impl:connectedDebugAndroidTest` проходит (миграционные тесты)

**Кодовая база — ноль упоминаний старых имён:**
- [ ] Grep `LangApi` по .kt файлам — 0 результатов (кроме старых миграций 4→10)
- [ ] Grep `LanguageDb` по .kt файлам — 0 результатов (кроме старых миграций и schemable)
- [ ] Grep `LanguageApiEntity` по .kt файлам — 0 результатов
- [ ] Grep `LanguageDump` по .kt файлам — 0 результатов
- [ ] Grep `CURRENT_LANG_NUMERIC_CODE_INT` по .kt файлам — 0 результатов
- [ ] Grep `\.langId` по .kt файлам — 0 результатов (кроме старых schemable и миграций)

**Room миграция:**
- [ ] Таблица `dictionaries` существует после миграции (была `languages`)
- [ ] Колонка `dictionary_id` в `words` (была `lang_id`)
- [ ] Колонка `dictionary_id` в `write_quiz` (была `lang_id`)
- [ ] FK constraints работают (CASCADE delete)
- [ ] Данные сохранены после миграции (количество строк, значения полей)
- [ ] Тест `MigrationFrom10to11` зелёный

**Preferences:**
- [ ] Новый ключ `CURRENT_DICTIONARY_ID_LONG` используется
- [ ] Fallback работает: при пустом ключе берётся первый словарь
- [ ] Переключение словарей в dropdown работает

**Функциональность не сломана:**
- [ ] Splash → если нет словарей → экран создания
- [ ] Создание словаря → сохраняется в Room → становится текущим
- [ ] Переключение словарей в AppBar → слова перезагружаются
- [ ] Квиз загружает вопросы по текущему словарю
- [ ] Статистика отображается по текущему словарю
- [ ] Export/Import работает (новый формат + обратная совместимость со старым)

**Строковые ресурсы:**
- [ ] Нет строк с `lang_` префиксом (кроме `lang_english`, `lang_german` и т.д. — названия языков, не UI)

### 1.1 Room-миграция #11

- Переименовать таблицу `languages` → `dictionaries`
- Переименовать колонку `lang_id` → `dictionary_id` в таблицах `words` и `write_quiz`
- Пересоздать FK constraints
- Написать миграционный тест

### 1.2 Entity и маппер слой

- `LanguageDb` → `DictionaryDb`
- `LanguageApiEntity` → `DictionaryApiEntity`
- `LanguageDump` → `DictionaryDump`
- `WordDb.langId` → `WordDb.dictionaryId`
- `WriteQuizDb.langId` → `WriteQuizDb.dictionaryId`
- `WordApiEntity.langId` → `WordApiEntity.dictionaryId`
- `WriteQuizApiEntity.langId` → `WriteQuizApiEntity.dictionaryId`
- `WriteQuizUpsertApiEntity.langId` → `WriteQuizUpsertApiEntity.dictionaryId`
- Обновить все маппер-функции (`toDumpEntity()`, `toApiEntity()`, `toDbEntity()`)

### 1.3 API контракты (core-db-api)

- `CoreDbApi.LangApi` → `CoreDbApi.DictionaryApi`
- `CoreDbProvider.getLangApi()` → `CoreDbProvider.getDictionaryApi()`
- Обновить сигнатуры: `addLang()` → `addDictionary()`, `getLang()` → `getDictionary()`, и т.д.
- Параметры `langId` → `dictionaryId` во всех методах `TermApi`, `WordApi`, `QuizApi`, `StatisticApi`

### 1.4 API реализация (core-db-impl)

- `LangApiImpl` → `DictionaryApiImpl`
- Обновить все SQL-запросы в `WordDao`: `WHERE lang_id` → `WHERE dictionary_id`
- Обновить все реализации в `CoreDbApiImpl`

### 1.5 Preferences

- Новый ключ: `CURRENT_DICTIONARY_ID_LONG` (хранит `id: Long` вместо `numericCode: Int`)
- Миграция prefsов **не нужна**: fallback в `getCurrentLangId()` и `getCurrentDict()` уже берёт первый словарь из списка и записывает в prefs, если ключ отсутствует. Достаточно просто использовать новый ключ — при первом обращении сработает fallback.
- Обновить всех readers/writers (6 мест чтения, 3 места записи)

### 1.6 UseCase реализации (app модуль)

- Все 9 UseCaseImpl: заменить `langApi` → `dictionaryApi`, `langId` → `dictionaryId`
- `getCurrentLangId()` → `getCurrentDictionaryId()`

### 1.7 Screen/Widget модули

- `DictUiEntity.numericCode` — пока оставить, но учитывать что станет опциональным
- Параметры `langId` в UseCase интерфейсах фичей → `dictionaryId`
- `TermUiItem.langId` → `TermUiItem.dictionaryId`

### 1.8 Export/Import совместимость

- Импорт: поддержать старый формат `LanguageDump` → маппить в `DictionaryDump`
- Экспорт: использовать новый формат `DictionaryDump`
- Добавить версию формата дампа

### 1.9 Тесты

- Обновить миграционные тесты в `core-db-impl`
- Обновить тесты `VocabularyTabReducer` (ссылки на `langId`)
- Обновить тесты `WordCard` (если есть ссылки)
