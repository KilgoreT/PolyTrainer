# IS515 | Анализ и план: новые встроенные опции «Части речи»

Бриф: [brief.md](brief.md) (Д1–Д4). Код — master `b7b707ce`, схема БД v14.

## 1. Что есть

| Где | Сейчас |
|---|---|
| Домен | `enum PartOfSpeechOption(key)`: NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION, PHRASE (ключи `noun`…`phrase`). |
| Новый словарь | `CoreDbApiImpl.seedBuiltInsForDictionary`: тип `part_of_speech` + строка `component_options` на каждый элемент enum, `position = ordinal`, `label = null`. |
| Старые словари | Опции засеяны один раз миграцией 11→12 (`seedPartOfSpeechPerDictionary`, ключи и позиции 0..5 захардкожены). |
| Полное имя опции | Ресурсы `part_of_speech_option_*`; маппинг `systemKey → ресурс` строками в двух местах: `component_widgets/.../BuiltInDisplay.kt` и `wordcard/.../ComponentLabel.kt`. |
| Сокращение для чипа | Ресурсы `part_of_speech_short_*`; `QuizGameImpl.partOfSpeechBadge` — exhaustive `when` по enum. |
| Миграции | `RoomModule.addMigrations(11→12, 12→13, 13→14)`; тесты `MigrationFromNNtoMM` (androidTest, `MigrationTestHelper`, bundled SQLite); схемы экспортируются в `core/core-db-impl/schemas`. |

## 2. Целевое поведение

1. Встроенных опций 12, порядок: сущ., гл., прил., нареч., местоим., числ.,
   предл., межд., фраз. гл., колл., идиом., фраз.
2. Новый словарь получает все 12 в этом порядке.
3. Существующие словари после обновления получают 6 новых опций и тот
   же порядок; значения лексем не меняются.
4. Чип квиза показывает сокращения новых опций (ru/en).

## 3. План — до → после → зачем

### П1. Домен

**Файл:** `modules/domain/lexeme/.../ComponentOption.kt`.

**До:** 6 элементов enum.

**После:** 12 элементов в порядке Д2:

```kotlin
enum class PartOfSpeechOption(val key: String) {
    NOUN("noun"), VERB("verb"), ADJECTIVE("adjective"), ADVERB("adverb"),
    PRONOUN("pronoun"), NUMERAL("numeral"), PREPOSITION("preposition"),
    INTERJECTION("interjection"), PHRASAL_VERB("phrasal_verb"),
    COLLOCATION("collocation"), IDIOM("idiom"), PHRASE("phrase"),
}
```

**Зачем.** `ordinal` = `position` при засеве нового словаря — порядок
enum и есть порядок в списке. Ключи старых опций не меняются (на них
ссылаются значения и миграция 11→12).

### П2. Ресурсы

**Файлы:** `core/core-resources/src/main/res/values{,-ru-rRU}/strings.xml`.

**После:** `part_of_speech_option_{pronoun,numeral,interjection,phrasal_verb,collocation,idiom}`
(ru: Местоимение, Числительное, Междометие, Фразовый глагол, Коллокация,
Идиома; en: Pronoun, Numeral, Interjection, Phrasal verb, Collocation,
Idiom) и `part_of_speech_short_*` для тех же ключей (ru: местоим., числ., межд.,
фраз. гл., колл., идиом.; en: pron., num., interj., phr. v., coll., idiom).

### П3. Отображение

**Файлы:** `BuiltInDisplay.kt`, `ComponentLabel.kt`, `QuizGameImpl.kt`.

**После:** ветки для 6 новых ключей в обоих строковых маппингах; в
`partOfSpeechBadge` — новые элементы enum (компилятор заставит).

**Зачем.** Иначе новая опция в карточке покажется сырым ключом или
пустой подписью.

### П4. Миграция 14→15

**Файлы:** `room/migrations/Migration_014_to_015.kt` (новый),
`room/Database.kt` (`version = 15`), `di/module/RoomModule.kt`
(`addMigrations(…, Migration_014_to_015)`), схема `15.json` (экспорт Room,
схема не меняется — только данные).

**После:** для каждого типа `system_key = 'part_of_speech'`:

1. Вставить 6 новых опций, если такой `system_key` у этого типа ещё нет
   (идемпотентно, `NOT EXISTS`):
   ```sql
   INSERT INTO component_options (component_type_id, system_key, label, position, created_at, updated_at, removed_at)
   SELECT ct.id, '<key>', NULL, <pos>, <now>, <now>, NULL
   FROM component_types ct
   WHERE ct.system_key = 'part_of_speech'
     AND NOT EXISTS (SELECT 1 FROM component_options co
                     WHERE co.component_type_id = ct.id AND co.system_key = '<key>')
   ```
2. Переставить позиции встроенных опций по Д2:
   `UPDATE … SET position = CASE system_key WHEN 'noun' THEN 0 … WHEN 'phrase' THEN 11 END, updated_at = <now>`
   для опций типа «Часть речи» с ключом из набора.

`<now>` = `System.currentTimeMillis()` (DateTimeConverter хранит
миллисекунды, прецедент 11→12). Своих опций у встроенной «Части речи»
не бывает: add/rename/delete опций встроенного типа возвращают
`BuiltInProtected` (§21.2), поэтому сдвига пользовательских опций нет.

Ключи и позиции в миграции — литералами, НЕ из enum: миграция фиксирует
состояние схемы v15 и не должна меняться вместе с будущим enum
(прецедент 11→12).

**Зачем.** Д1: доставка в существующие словари.

### П5. Тесты

- **androidTest `MigrationFrom14to15`** (прецедент `MigrationFrom13to14`):
  - A изолированный 14→15: словарь с 6 старыми опциями → 12, позиции
    0..11 по Д2, значения лексем (`component_values.option_id`) живы;
  - B идемпотентность: в v14 уже есть опция `collocation` → дубля нет;
  - C словарь без «Части речи» не трогается;
  - D chained 11→15 — боевой путь (опции засеяны 11→12, догружены 14→15).
- **androidTest с числом 6:** `Is486DataLayerTest` (строки ~101, 224, 235)
  и `Phase3ConstructorDataTest.builtinOptions_areProtected` (~480) —
  `assertEquals(6, …)` → `PartOfSpeechOption.entries.size`.
- **`QuizGameImplTest`:** чип для новой встроенной опции (`collocation` →
  «колл.»).
- Ручник (короткий): M1 обновление поверх 0.1.12 — у словаря 12 опций в
  новом порядке, старые значения слов на месте; M2 новый словарь — 12
  опций; M3 слово с «Коллокация» → чип «колл.» в вопросе квиз-чата.

### П6. Доки

- Спека `component-constructor` — состав встроенной «Части речи».
- Спека `quiz-chat` §5 — новые ключи `part_of_speech_short_*`.
- Backlog: пункт IS511 «встроенный атрибут „Тип единицы“» — пометить
  УСТАРЕЛО (решение IS515: типы единиц в «Части речи»).

Ревью плана (один агент, 2026-10-01): блокеров нет; учтено — сдвиг
пользовательских опций убран (их у встроенного типа не бывает), названы
тесты с числом 6, `updated_at` в перестановке, риск забытой регистрации
миграции.

**Дополнение с ручника (2026-10-01, юзер):** в чипе выбранного значения
в карточке слова — сокращение («колл.»), в списке выбора — полное
название. Сокращения общие с чипом квиз-чата: ресурсы переименованы
`chat_quiz_pos_*` → `part_of_speech_short_*`; в wordcard —
`optionShortLabel` (ComponentLabel.kt), используется в
`LexemeComponentsBlock`.

## 4. Риски

1. **Room-валидация схемы:** версия 15 без изменений схемы — `15.json`
   должен совпасть с 14 по структуре; иначе `runMigrationsAndValidate`
   упадёт.
2. **Регистрация миграции:** в `RoomModule` стоит
   `fallbackToDestructiveMigration(dropAllTables = true)` — если поднять
   version до 15 и забыть `addMigrations(…14→15)`, Room молча сотрёт БД;
   `MigrationTestHelper` это не ловит. Поэтому ручник M1 (обновление
   поверх 0.1.12 со словами на месте) обязателен. Заодно поправить
   устаревший KDoc RoomModule («v13»).
3. **Два строковых маппинга `systemKey → ресурс`** (`BuiltInDisplay`,
   `ComponentLabel`) — дублирование, пропустить ключ в одном легко;
   проверить оба. Свести в один — вне скоупа (Backlog).
4. **Бэкапы (IS488, заморожен):** восстановление бэкапа v14 на v15 пройдёт
   через ту же миграцию — отдельных действий не нужно.
