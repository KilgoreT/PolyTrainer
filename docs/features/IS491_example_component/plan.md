# IS491 — план реализации по слоям (TDD)

Основа: [brief.md](brief.md) (решения Д1–Д7) +
[analysis_spec_alignment.md](analysis_spec_alignment.md) (П1–П5, С1–С7) +
[use_cases.md](use_cases.md) (UC-сценарии).
Порядок фаз — снизу вверх: домен → данные/миграция → карточка → конструктор/квизы/display → хвосты.

**TDD-порядок в каждой фазе: сначала тесты (red), потом код (green).** Тесты пишутся
по контракту из брифа/юзеркейсов ДО реализации; правка тестов после — только если
неверно спланирован сам тест, не потому что код не сошёлся.

**Лог-контракт** (см. `use_cases.md § Лог-контракт`): фичевый тег
`###CAPTIONED_TEXT###`; события seed / backfill / suggestions / value add-update-remove /
quizPicker. Логи — часть реализации соответствующих секций (помечены ниже),
по ним идут ручные тесты юзеркейсов. Логи — per-event, через `LexemeLogger`.

---

## Фаза 1. Домен + данные + миграция

### 1.1 Домен (`modules/domain/lexeme`)

Новые типы — контракт для тестов нижних слоёв (сам по себе декларативный, без логики):

- [ ] `ComponentTemplate`: новый элемент `CAPTIONED_TEXT("captioned_text")`;
  `fields` → `listOf(Field("text", TEXT), Field("caption", TEXT))`.
- [ ] `TemplateValues`: новый подтип
  `data class CaptionedTextValues(val text: Primitive.Text, val caption: Primitive.Text?)`.
  Опциональность caption выражена типом (`Field.required` не вводим — решение аналитики §4).
- [ ] `BuiltInComponent`: новый элемент `EXAMPLE("example")`.
- [ ] **Компилируемость в том же коммите** (ревью: enum ломает exhaustive-when
  в чужих модулях — иначе CI/Build APK красный между фазами): ветки для
  `CAPTIONED_TEXT` в `ComponentTemplateLabel.labelRes()` (+ ресурс
  `components_template_captioned` ru/en) и `ComponentByTemplate` (заглушка —
  путь мёртвый, П5); ветка `EXAMPLE` в `ComponentChoiceItem`. Display-сайты
  с `else`-ветками (`BuiltInDisplay`, `ComponentLabel`) не ломаются — остаются
  в фазе 3.

### 1.2 Сериализация envelope (`core/core-db-impl/mapper/TemplateValuesJson.kt`)

- [ ] **ТЕСТЫ СНАЧАЛА** — unit round-trip по контракту (UC2, UC5, С4):
  captioned с caption / без caption (ключ отсутствует в JSON) / битый caption →
  значение живёт с caption=null + лог / битый text → null + лог / schema mismatch →
  null; регресс: TEXT/IMAGE/CHOICE ветки не изменились.
- [ ] Код: `toJson()` — ветка `CaptionedTextValues`, caption==null → ключ опускается;
  `parseTemplateValues()` — рефактор (чтение `fields.getJSONObject("value")` сейчас
  стоит ДО ветвления и уронит captioned; перенести чтение полей внутрь веток) +
  captioned-ветка с defensive-caption.

### 1.3 Seed + backfill (П1)

- [ ] **ТЕСТЫ СНАЧАЛА** — androidTest миграции 12→13 (UC13, UC14): builtin «Пример»
  появился во всех существующих словарях (position=2, dependsOn=Lexeme, core=false,
  isMultiple=true, enabled=true); существующие данные не тронуты; guard не даёт
  дубля при пересечении с seed-путём нового словаря; новый словарь после апдейта
  получает «Пример» через seed. Нюанс инфраструктуры (ревью): `createDatabase(12)`
  создаёт ПУСТЫЕ таблицы — словари и их component_types вставлять руками
  (`insertDictionary`/`execSQL`) до `runMigrationsAndValidate(13)`; файл `13.json`
  генерится сборкой с version=13.
- [ ] Код: `seedBuiltInsForDictionary` + строка `example`; `Database` version 12→13
  (schema-json 13 = 12 по DDL); `Migration_012_to_013` — data-backfill
  `INSERT … SELECT FROM dictionaries` по образцу `seedPartOfSpeechPerDictionary`,
  guard по `(system_key, dictionary_id)`; регистрация в `RoomModule`.
- [ ] Лог-контракт: `###CAPTIONED_TEXT### seed: example dictId=<id>` (в seed) и
  `###CAPTIONED_TEXT### backfill 12->13: example seeded to <n> dictionaries`
  (в миграции, счётчик по факту INSERT).

### 1.4 Запрос подсказок caption (С6)

- [ ] **ТЕСТЫ СНАЧАЛА** — androidTest DAO (UC3, UC10–UC12): частотная сортировка,
  tie-break при равных частотах — алфавит; скоуп строго по typeId (два компонента
  шаблона не смешиваются); soft-deleted исключены; пустые caption исключены;
  **регистр значим** — «ielts» и «IELTS» отдельные пункты (решение ревью
  2026-08-02, дедупа нет).
- [ ] Код: `ComponentValueDao` — one-shot `suspend`-запрос
  `json_extract(value,'$.fields.caption.value') + COUNT GROUP BY ORDER BY cnt DESC,
  cap ASC`; `CoreDbApi.LexemeApi.getCaptionSuggestions(typeId)` (wordcard ходит
  через `lexemeApi`). Дедупа в impl нет.
- [ ] Лог-контракт: `###CAPTIONED_TEXT### suggestions: typeId=<id> count=<n>`
  (n = число пунктов списка).

## Фаза 2. Карточка слова — template-aware пайплайн + UI

### 2.1 Template-aware edit-пайплайн (С1+С2+С3 одним рефактором)

- [ ] **ТЕСТЫ СНАЧАЛА** — reducer-тесты wordcard по контракту (UC2, UC5–UC9,
  UC25–UC27): commitDecision-матрица captioned: text пуст → не сохраняется/удаление;
  только caption без text → невозможно (Д7); text есть, caption пуст → валидный
  Update; caption из одних пробелов → trim → null (UC26); оба поля изменены →
  один Update с обоими (UC27); отмена редактирования → изменения не применяются,
  эффектов нет (UC25); ничего не менялось → NoOp; origin lossy fix: сохранённое
  captioned значение, открытое в edit и закоммиченное пустым →
  `PessimisticRemove` (не `LocalRemove`); caption-input Msg меняет state;
  anchor черновика — по text.
- [ ] Код: `ComponentValueState` — `origin: String` → typed (`TemplateValues?`),
  + `editedCaption: String?`; `commitDecision()` — «пусто» = пустой text,
  `Update` несёт `TemplateValues`; `textValuesOf()` → фабрика по шаблону;
  `Msg.UpdateComponentCaptionInput` + ветка редьюсера;
  `commitRealLexeme`/`commitDraftLexeme`.
- [ ] `WordCardReducer.kt` — необъявленная ревью-поверхность ретайпа origin
  (обязательно): merge в `reduceRefreshLexemeComponents` (`asText().orEmpty()` →
  typed), предикат «пустой origin = локальный мусор» в remove-ветке
  (`origin.isEmpty() && template != CHOICE`), call-sites `textValuesOf`.

### 2.2 Подсказки в state/эффектах

- [ ] **ТЕСТЫ СНАЧАЛА** — reducer: `CaptionSuggestionsLoaded` кладёт список в state
  по typeId; фокус caption-поля эмитит `LoadCaptionSuggestions` один раз.
- [ ] Код: `DatasourceEffect.LoadCaptionSuggestions(typeId)` → one-shot →
  `Msg.CaptionSuggestionsLoaded`; фильтрация по вводу — локально в composable.
- [ ] Лог-контракт (в DatasourceEffectHandler, per-event):
  `###CAPTIONED_TEXT### value add: typeId=<id> valueId=<id> text.len=<n> caption='<c>'`,
  `value update: valueId=<id> text.len=<n> caption='<c>'`, `value remove: valueId=<id>`
  (caption='null' если пуст).

### 2.3 UI (`wordcard/widget/lexeme` + `modules/core/ui`) — вне TDD-контура (composable)

- [ ] `ComponentValueField`: третья ветка `CAPTIONED_TEXT` (рядом с ранним return
  CHOICE) — НЕ `ComponentByTemplate` (мёртвый путь, П5): многострочное text-поле
  (`LexemeEditableText`, singleLine=false уже есть) + caption-combobox.
- [ ] Новый виджет combobox (С7): `TextField` (singleLine) + `DropdownMenu`;
  одно поле = ввод нового значения И фильтр списка; выбор пункта подставляет
  значение; стиль — существующие dropdown-виджеты, шрифт из темы.
- [ ] Рендер сохранённого значения: text + caption (второстепенен визуально);
  пустой caption не рендерится.

## Фаза 3. Конструктор, квизы, display

- [ ] **ТЕСТЫ СНАЧАЛА** — `QuizChatUseCaseImplTest` (UC19): captioned-компоненты
  (builtin и кастомные) не попадают в доступные типы пикера; TEXT попадает;
  CHOICE по-прежнему нет.
- [ ] Код квизов (П2): фильтр — чёрный список → **белый**
  (`it.template == ComponentTemplate.TEXT`).
- [ ] Лог-контракт: `###CAPTIONED_TEXT### quizPicker: available=[<display-имена>]`
  (после фильтрации).
- [ ] Конструктор: radio-группа подхватит `CAPTIONED_TEXT` автоматически
  (`labelRes` + ресурс уже добавлены в фазе 1.1 ради компилируемости).
  Single/multiple чекбокс generic — работает (Д1). Builtin «Пример» со
  свитчем — автоматом (UC16, UC17).
- [ ] Display «Пример» (С5): ресурс `builtin_component_example` (рус «Пример» /
  англ "Example") + ветки в `BuiltInDisplay.componentDisplayName`,
  `ComponentLabel.labelOfRef`, `componentLabelOf` (UC20); `ComponentChoiceItem`
  уже покрыт в фазе 1.1.

## Фаза 4. Хвосты

- [ ] Спека `component-constructor/spec.md`: §3 таблица шаблонов + captioned_text
  (мульти разрешён); §4 builtin-набор + «Пример»; §5 типы (`CaptionedTextValues`,
  fields); §10 формат multi-field envelope + правило «null caption = опущенный ключ»
  + владелец валидации пустого значения (UI-гейт); правка канонических примеров
  «Пример → Перевод» в **§1/§3/§5** (П4); §18 фактический рендер-путь
  `ComponentValueField` (П5).
- [x] Бриф: две формулировки поправлены 2026-08-02 (миграция 12→13 + backfill;
  белый список вместо «как IMAGE»); Д4 переписан (регистр значим).
- [ ] Backlog: закрыть пункт «IS481 wordcard_components: origin lossy» (закрыт
  фазой 2.1); пункт «seed на destructive-fallback» — актуализировать (устарел).
- [ ] Решить судьбу мёртвых `ComponentByTemplate`/`TextWidget`/`ComponentBlock`
  (`ui/unused/CheckedTextWidget.kt`): снести или оставить с пометкой.
- [ ] Ручной девайс-чек-лист (по образцу IS486, из [use_cases.md](use_cases.md)):
  создание кастомного captioned, «Пример» в старом словаре после апдейта,
  combobox (новое/выбор/фильтр/регистр/кириллица), multiple, пустой caption,
  disable, «Пример» как цель зависимости, квиз-пикер; плюс UC15 (доступность
  как у части речи), UC17 (builtin-защита), UC20 (локализация ru/en — новые
  ресурсы), UC25–UC27 (отмена, trim caption, оба поля).

---

## Статус реализации (2026-08-02)

Фазы 1–3 и доковая часть фазы 4 — **РЕАЛИЗОВАНЫ** (ветка `IS491_example_component`):

- Фаза 1: домен (+ ветки компилируемости), сериализатор (8 новых unit-тестов),
  seed «Пример»; DAO подсказок (+ androidTest `CaptionSuggestionsDaoTest`),
  лог-контракт. Unit зелёные.
  **UPD 2026-08-02 (решение юзера):** отдельная миграция 12→13 отменена — v12
  нигде не релизилась (прод 0.1.5 = v11, dev-сборка v12 удаляется). Seed «Пример»
  **схлопнут в `Migration_011_to_012`** (шаг 6b), версия БД остаётся 12,
  `Migration_012_to_013`/`MigrationFrom12to13`/`13.json` удалены; покрытие —
  `MigrationFrom11to12.is491_exampleSeededToAllDictionaries_finalShape`.
- Фаза 2: template-aware пайплайн (19 новых reducer-тестов `CaptionedValueTest`,
  все wordcard-тесты зелёные), `CaptionField` combobox, `CaptionedValueField`.
  Отступление от плана: вместо ретайпа `origin: String → TemplateValues?` — дом-паттерн
  CHOICE (`originCaption`/`editedCaption` отдельными полями + template-aware
  `asText`/`commitDecision`/remove-ветка) — closes С1+С2+С3 меньшей поверхностью;
  origin-lossy закрыт и для будущих не-текстовых шаблонов.
  Нюанс UX: text-поле captioned НЕ коммитит по потере фокуса (фокус легально
  уходит в caption) — коммит по focusLost caption-поля или общим flush.
- Фаза 3: белый список TEXT в квиз-пикере (+тест), display-сайты, ресурсы.
- Фаза 4 (доки): спека §1/§3/§4/§5/§10/§11/§18 обновлена; Backlog: «origin lossy»
  закрыт, «seed destructive-fallback» помечен устаревшим; `manual_test_checklist.md`
  создан. Мёртвые `ComponentByTemplate`-виджеты помечены (снос — отдельное решение).

**ОСТАЛОСЬ:** прогон androidTest на девайсе (`MigrationFrom12to13`,
`CaptionSuggestionsDaoTest`, регресс `Is486DataLayerTest` с 3-м builtin) →
ручной чек-лист → правки по фидбеку.

## Порядок и контроль

- **TDD:** в фазах 1–3 каждая секция начинается с тестов; код пишется под падающие
  тесты. Composable-обвязка (2.3) — вне юнит-контура, проверяется девайс-чек-листом.
- Сборка/линт/тесты — только `./scripts/cc-build.sh <task>`; тесты по одному модулю
  последовательно; `domain:lexeme` — task `test` (JVM-модуль).
- Ветка `IS491_example_component` от master.
- Фаза 1 полностью тестируема без UI — коммитабельная точка (при условии
  веток компилируемости из 1.1: без них CI/Build APK красный).
- Фаза 2 — самая объёмная (рефактор пайплайна); reducer-тесты до UI-обвязки.
