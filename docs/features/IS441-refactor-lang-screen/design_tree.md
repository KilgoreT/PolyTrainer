# IS441. Design Tree — переименование lang → dictionary

## Граф

```yaml
# === СЛОЙ 0: Room миграция (нет зависимостей) ===

- id: 0
  file: core/core-db-impl/src/main/java/.../room/migrations/Migration_010_to_011.kt
  action: "+"
  depends: []

# === СЛОЙ 1: Entity (зависят от миграции концептуально, но компилируются независимо) ===

- id: 1
  file: core/core-db-impl/src/main/java/.../entity/LanguageDb.kt → DictionaryDb.kt
  action: "~"
  depends: []

- id: 2
  file: core/core-db-api/src/main/java/.../entity/LanguageApiEntity.kt → DictionaryApiEntity.kt
  action: "~"
  depends: []

- id: 3
  file: core/core-db-api/src/main/java/.../entity/LanguageDump.kt → DictionaryDump.kt
  action: "~"
  depends: []

- id: 4
  file: core/core-db-impl/src/main/java/.../entity/WordDb.kt
  action: "~"
  depends: []

- id: 5
  file: core/core-db-impl/src/main/java/.../entity/WriteQuizDb.kt
  action: "~"
  depends: []

- id: 6
  file: core/core-db-api/src/main/java/.../entity/WordApiEntity.kt
  action: "~"
  depends: []

- id: 7
  file: core/core-db-api/src/main/java/.../entity/WriteQuizComplexEntity.kt
  action: "~"
  depends: []

- id: 8
  file: core/core-db-api/src/main/java/.../entity/Dump.kt
  action: "~"
  depends: [3]

- id: 9
  file: core/core-db-api/src/main/java/.../entity/WordDump.kt
  action: "~"
  depends: []

- id: 10
  file: core/core-db-api/src/main/java/.../entity/WriteQuizDump.kt
  action: "~"
  depends: []

# === СЛОЙ 2: Mapper (зависят от entity) ===

- id: 11
  file: core/core-db-impl/src/main/java/.../mapper/LanguageDumpMapper.kt → DictionaryDumpMapper.kt
  action: "~"
  depends: [1, 3]

- id: 12
  file: core/core-db-impl/src/main/java/.../mapper/WordDumpMapper.kt
  action: "~"
  depends: [4, 9]

- id: 13
  file: core/core-db-impl/src/main/java/.../mapper/WriteQuizDumpMapper.kt
  action: "~"
  depends: [5, 10]

# === СЛОЙ 3: DAO (зависит от entity) ===

- id: 14
  file: core/core-db-impl/src/main/java/.../room/WordDao.kt
  action: "~"
  depends: [1]

- id: 14a
  file: core/core-db-impl/src/main/java/.../RoomPaging.kt
  action: "~"
  depends: [14]

# === СЛОЙ 4: Database (зависит от entity + DAO) ===

- id: 15
  file: core/core-db-impl/src/main/java/.../room/Database.kt
  action: "~"
  depends: [0, 1, 14]

# === СЛОЙ 5: API контракт (зависит от entity) ===

- id: 16
  file: core/core-db-api/src/main/java/.../CoreDbApi.kt
  action: "~"
  depends: [2]

- id: 17
  file: core/core-db-api/src/main/java/.../CoreDbProvider.kt
  action: "~"
  depends: [16]

# === СЛОЙ 6: API реализация (зависит от контракт + DAO + entity) ===

- id: 18
  file: core/core-db-impl/src/main/java/.../CoreDbApiImpl.kt
  action: "~"
  depends: [1, 2, 14, 16]

# === СЛОЙ 7: DI модуль core (зависит от API) ===

- id: 19
  file: core/core-db-impl/src/main/java/.../di/module/ApiModule.kt
  action: "~"
  depends: [16, 18]

# === СЛОЙ 8: Preferences (независимый) ===

- id: 20
  file: modules/datasource/prefs/src/main/java/.../PrefsProvider.kt
  action: "~"
  depends: []

# === СЛОЙ 9: UseCase интерфейсы (зависят от entity + API контракт) ===

- id: 21
  file: modules/screen/createdictionary/src/main/java/.../CreateDictionaryViewModel.kt
  action: "~"
  depends: [38, 39, 40, 41]

- id: 22
  file: modules/screen/dictionaryTab/src/main/java/.../deps/DictionaryTabUseCase.kt
  action: "~"
  depends: []

- id: 23
  file: modules/screen/quiz/chat/src/main/java/.../deps/QuizChatUseCase.kt
  action: "~"
  depends: []

- id: 24
  file: modules/widget/dictionaryappbar/src/main/java/.../deps/DictionaryAppBarUseCase.kt
  action: "~"
  depends: []

- id: 25
  file: modules/screen/splash/src/main/java/.../deps/SplashUseCase.kt
  action: "~"
  depends: []

# === СЛОЙ 10: UseCase реализации (зависят от API + интерфейсы + prefs) ===

- id: 26
  file: app/src/main/java/.../di/module/createDictionary/CreateDictionaryUseCaseImpl.kt
  action: "~"
  depends: [16, 20, 21]

- id: 27
  file: app/src/main/java/.../di/module/dictionarytab/DictionaryTabUseCaseImpl.kt
  action: "~"
  depends: [16, 20, 22]

- id: 28
  file: app/src/main/java/.../di/module/quizchat/QuizChatUseCaseImpl.kt
  action: "~"
  depends: [16, 20, 23]

- id: 29
  file: app/src/main/java/.../di/module/wordCard/WordCardUseCaseImpl.kt
  action: "~"
  depends: [16, 20]

- id: 30
  file: app/src/main/java/.../di/module/widget/DictionaryAppBarUseCaseImpl.kt
  action: "~"
  depends: [16, 20, 24]

- id: 31
  file: app/src/main/java/.../di/module/splash/SplashUseCaseImpl.kt
  action: "~"
  depends: [16, 25]

- id: 32
  file: app/src/main/java/.../di/module/statistictab/StatisticUseCaseImpl.kt
  action: "~"
  depends: [16, 20]

- id: 33
  file: app/src/main/java/.../di/module/quiztab/QuizTabUseCaseImpl.kt
  action: "~"
  depends: [16]

# === СЛОЙ 11: AppComponent (зависит от UseCase) ===

- id: 34
  file: app/src/main/java/.../di/AppComponent.kt
  action: "~"
  depends: [17]

# === СЛОЙ 12: CreateDictionary screen (зависит от UseCase + entity) ===

- id: 35
  file: modules/screen/createdictionary/src/main/java/.../LanguageData.kt → DictionaryData.kt
  action: "~"
  depends: []

- id: 36
  file: modules/screen/createdictionary/src/main/java/.../entity/PresetLangUi.kt → PresetDictionaryUi.kt
  action: "~"
  depends: []

- id: 37
  file: modules/screen/createdictionary/src/main/java/.../entity/LangUpdateUi.kt → DictionaryUpdateUi.kt
  action: "~"
  depends: []

- id: 38
  file: modules/screen/createdictionary/src/main/java/.../logic/State.kt
  action: "~"
  depends: [36]

- id: 39
  file: modules/screen/createdictionary/src/main/java/.../logic/Message.kt
  action: "~"
  depends: [36]

- id: 40
  file: modules/screen/createdictionary/src/main/java/.../logic/CreateDictionaryReducer.kt
  action: "~"
  depends: [38, 39]

- id: 41
  file: modules/screen/createdictionary/src/main/java/.../logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [35, 39]

- id: 42
  file: modules/screen/createdictionary/src/main/java/.../widget/LangPickerWidget.kt → DictionaryPickerWidget.kt
  action: "~"
  depends: [38, 39, 35]

- id: 43
  file: modules/screen/createdictionary/src/main/java/.../widget/LangListWidget.kt → DictionaryListWidget.kt
  action: "~"
  depends: [36, 39]

- id: 44
  file: modules/screen/createdictionary/src/main/java/.../widget/LanguageItemWidget.kt → DictionaryItemWidget.kt
  action: "~"
  depends: []

- id: 45
  file: modules/screen/createdictionary/src/main/java/.../CreateDictionaryScreen.kt
  action: "~"
  depends: [38, 42]

# === СЛОЙ 13: DictionaryTab screen (зависит от UseCase) ===

- id: 46
  file: modules/screen/dictionaryTab/src/main/java/.../entity/TermUiItem.kt
  action: "~"
  depends: []

- id: 47
  file: modules/screen/dictionaryTab/src/main/java/.../logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [22]

# === СЛОЙ 14: Widget — DictionaryPicker ===

- id: 48
  file: modules/widget/dictionarypicker/src/main/java/.../entity/DictUiEntity.kt
  action: "~"
  depends: []

- id: 49
  file: modules/widget/dictionarypicker/src/main/java/.../DictDropDownWidget.kt
  action: "~"
  depends: [48]

# === СЛОЙ 15: Widget — DictionaryAppBar ===

- id: 50
  file: modules/widget/dictionaryappbar/src/main/java/.../mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [24, 48]

# === СЛОЙ 16: QuizChat ===

- id: 51
  file: modules/screen/quiz/chat/src/main/java/.../entity/WriteQuiz.kt
  action: "~"
  depends: []

- id: 52
  file: modules/screen/quiz/chat/src/main/java/.../quiz/QuizGameImpl.kt
  action: "~"
  depends: [23]

# === СЛОЙ 17: Export/Import ===

- id: 53
  file: app/src/main/java/.../di/module/settingstab/SettingsTabUseCaseImpl.kt
  action: "~"
  depends: [16]

# === СЛОЙ 18: Тесты миграции ===

- id: 54
  file: core/core-db-impl/src/androidTest/java/.../room/migrations/MigrationFrom10to11.kt
  action: "+"
  depends: [0]

- id: 55
  file: core/core-db-impl/src/androidTest/java/.../room/schemable/DictionaryV11.kt
  action: "+"
  depends: [0]

- id: 56
  file: core/core-db-impl/src/androidTest/java/.../room/schemable/WordV11.kt
  action: "+"
  depends: [0]

- id: 57
  file: core/core-db-impl/src/androidTest/java/.../room/schemable/WriteQuizV11.kt
  action: "+"
  depends: [0]

- id: 58
  file: core/core-db-impl/src/androidTest/java/.../room/Schema.kt
  action: "~"
  depends: [1]

- id: 59
  file: core/core-db-impl/src/androidTest/java/.../room/dataSource/DataProvider.kt
  action: "~"
  depends: [1]

- id: 60
  file: core/core-db-impl/src/androidTest/java/.../room/AllMigrationTest.kt
  action: "~"
  depends: [54]

# === СЛОЙ 19: Обновление существующих тестов ===

- id: 61
  file: modules/screen/dictionaryTab/src/test/java/.../logic/VocabularyTabReducerKtTest.kt
  action: "~"
  depends: [22, 46]

- id: 62
  file: modules/screen/dictionaryTab/src/test/java/.../logic/DataHelper.kt
  action: "~"
  depends: [46]

- id: 63
  file: modules/screen/dictionaryTab/src/main/java/.../tools/DataHelper.kt
  action: "~"
  depends: [46]

# === СЛОЙ 20: Строковые ресурсы ===

- id: 64
  file: core/core-resources/src/main/res/values/strings.xml
  action: "~"
  depends: []

- id: 65
  file: core/core-resources/src/main/res/values-ru-rRU/strings.xml
  action: "~"
  depends: []
```

## Детали

### #0 Migration_010_to_011.kt [+]

Room-миграция. SQL:
- `ALTER TABLE languages RENAME TO dictionaries`
- `ALTER TABLE words RENAME COLUMN lang_id TO dictionary_id`
- Пересоздание `write_quiz` с `dictionary_id` вместо `lang_id` (SQLite не поддерживает RENAME COLUMN до API 30, нужен CREATE + INSERT + DROP + RENAME)

### #1 LanguageDb.kt → DictionaryDb.kt [~]

**Было:** `data class LanguageDb(numericCode: Int, code: String, name: String?)`
**Стало:** `data class DictionaryDb(numericCode: Int?, name: String, description: String?)` — удалить `code`, `numericCode` nullable, `name` NOT NULL, новое поле `description`

### #2 LanguageApiEntity.kt → DictionaryApiEntity.kt [~]

**Было:** `data class LanguageApiEntity(id: Int, numericCode: Int, code: String, name: String)`
**Стало:** `data class DictionaryApiEntity(id: Long, numericCode: Int?, name: String, description: String?)` — `id` Long, удалить `code`, `numericCode` nullable

### #3 LanguageDump.kt → DictionaryDump.kt [~]

Переименование класса + полей. Добавить `description`.

### #4-10 WordDb, WriteQuizDb, ApiEntity, Dump [~]

`langId` → `dictionaryId` во всех полях и маппинг-функциях.

### #14 WordDao.kt [~]

Переименование методов: `addLanguage` → `addDictionary`, `getLanguages` → `getDictionaries`, и т.д.
SQL-запросы: `lang_id` в WHERE остаётся до миграции, после — `dictionary_id`.

### #16-17 CoreDbApi.kt, CoreDbProvider.kt [~]

`LangApi` → `DictionaryApi`. Все методы и типы.

### #20 PrefsProvider.kt [~]

`CURRENT_LANG_NUMERIC_CODE_INT` → `CURRENT_DICTIONARY_ID_LONG`. Тип: `Int` → `Long`.

### #35 LanguageData.kt → DictionaryData.kt [~]

Объект + enum + extension-функции: переименование.

### #49 DictDropDownWidget.kt [~]

Создаёт `DictUiEntity` напрямую (конструктор). Обращается к `.numericCode` и `.flagRes` — оба станут nullable. Нужно обновить конструктор (добавить `id`), обработать nullable `flagRes` (дефолтная иконка).

### #48 DictUiEntity.kt [~]

**Было:** `val numericCode: Int, val flagRes: Int`
**Стало:** `val id: Long, val numericCode: Int?, val flagRes: Int?`

### #64-65 strings.xml (en + ru) [~]

11 строк с `lang_` префиксом: `lang_selection_title`, `lang_selection_subtitle`, `lang_english`...`lang_portuguese`, `lang_selection_button`, `lang_selection_error`. Переименовать ID: `lang_` → `dict_` или `dictionary_`. Обновить ссылки в #35, #42, #43.

### #54-57 Тестовые Schemable [+]

Новые версионированные схемы для миграции 10→11: `DictionaryV11`, `WordV11`, `WriteQuizV11`.

## Параллельность

Из графа выводится:

**Параллельно (нет общих зависимостей):**
- #0 (миграция) | #1-10 (entity) | #20 (prefs) | #35-37 (screen entity)
- #54-57 (тесты миграции) — после #0
- #26-33 (UseCase impl) — после #16, #20
- #42-45 (screen widgets) — после #38, #39
- #46-52 (другие модули) — после своих UseCase интерфейсов

**Строго последовательно:**
- Entity (#1-2) → Mapper (#11) → DAO (#14) → Database (#15) → API impl (#18)
- API контракт (#16) → UseCase impl (#26-33) → AppComponent (#34)

## Статистика

| Действие | Кол-во |
|----------|--------|
| [+] создание | 5 |
| [~] изменение | 63 |
| [-] удаление | 0 |
| **Всего** | **68** |
