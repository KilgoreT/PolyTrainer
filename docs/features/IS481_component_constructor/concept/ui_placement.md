# UI placement — конструктор компонентов

> **Этот документ описывает target state ПОСЛЕ миграции M13.** Упоминания `is_multi`, `removed_at`, `TemplateValues`, scope (`dictionaryId=null` для user-defined) — это post-M13 модель. В M12 этих полей в БД и типов в domain нет (соответствия M12 → M13 см. в `template_model.md` § Миграция M12→M13).

## Решение по entry-point'ам

Два независимых пути в конструктор, не пересекаются между собой:

### Глобальный конструктор (aggregated view)

- **Где:** `SettingsTabScreen` → новый пункт `ComponentsManageWidget` рядом с `LangManageWidget` / `ExportDataWidget` / `ImportDataWidget`.
- **Открытие:** drill-in через `SettingsNavigator` (full-screen `ComponentsManagerScreen`).
- **Почему туда:** прямой прецедент `LangManageWidget` (управление языками — тоже глобальный справочник). Готовый wiring через `SettingsNavigator` + `CompositionRootImpl`. Семантически components — глобальная пользовательская «настройка структуры словарей», стоят в одном ряду с языками.

### Per-dictionary конструктор (scoped view)

- **Где:** `DictionaryAppBar` widget — отдельная **icon button (молоток)** **перед** `DictDropDownWidget` (picker'ом словаря).
- **Видимость:** только когда `currentDict != null`. Если словарь не выбран — иконка скрыта.
- **Появляется автоматически на всех трёх табах** где используется этот appbar: `dictionaryTab` / `quizTab` / `statTab`.
- **Открытие:** иконка-молоток → drill-in `PerDictionaryComponentsScreen` (передаёт `currentDict.id`).
- **Почему так:** maximum reach (3 таба сразу), контекст 100% явный (current dict из state appbar'а), discoverability выше overflow menu / long-press, не загромождает dropdown переключения словарей. Иконка-молоток имеет понятную семантику «конструктор / инструменты».

### Cross-flow

Два пути **независимые**, друг в друга не переходят. Если юзер в Settings и хочет работать с компонентами конкретного словаря — выходит из Settings, идёт в любой dict-tab, через picker выбирает словарь, жмёт молоток. Глобальный view не делает drill-in в per-dictionary (избыточная сложность для MVP).

## Что на каждом view

Два независимых view. Сначала фиксируем содержание, потом решаем где разместить.

### Общий view (aggregated)

Показывает только **user-defined** компоненты из всех словарей. Built-in (translation, future) **не отображаются** — юзер на них повлиять не может, отображение без действий = шум; built-in видны там, где применяются (quiz picker, создание lexeme, карточка слова).

Per строка списка:
- Имя компонента.
- Template (TEXT, …).
- **Cardinality badge** — `single` либо `multi` (соответствует `is_multi` в БД). Визуально различимо, чтобы юзер сразу видел тип компонента.
- В скольких словарях используется (счётчик).
- В каких именно словарях (раскрыть/перейти).
- Сколько values уже накоплено (опционально для контекста).

Операции:
- Переименовать.
- Удалить (всегда можно; **обязательно** показать сколько values станет скрыто, в каких словарях, перед подтверждением). Механика удаления — soft-delete (помечает `removed_at`, скрывает из активных queries), см. `deletion_concept.md`. Recovery вне scope этой фичи — удалённый компонент пути назад в UI не имеет.
- **Создать новый** user-defined компонент. Обязательные поля:
  - **Имя** — произвольная строка (uniqueness в рамках scope, см. ниже).
  - **Шаблон** — выбор из `ComponentTemplate` (с UI-превью каждого) — см. `template_model.md`.
  - **Несколько значений на лексему** — чекбокс (`is_multi`). По умолчанию off. Когда on — у лексемы допустимо N значений этого компонента (например 2-3 цитаты). Когда off — только одно (например «часть речи»).
  - **Scope** — переключатель «На все словари» (global, `dictionaryId=null`) / «На конкретные» (выбор одного или нескольких dict; в БД = N независимых записей с `dictionaryId=X`).

Empty state (0 user-defined компонентов): «У вас нет своих компонентов. Translation работает автоматически в каждом словаре. Создать свой компонент → выбрать словарь X».

### Per-dictionary view (scoped)

Показывает только **user-defined** компоненты конкретного словаря. Built-in не перечисляем по той же причине — юзер видит их в quiz picker / редакторе лексемы.

Per строка списка:
- Имя компонента.
- Template (TEXT, …).
- **Cardinality badge** — `single` / `multi` (`is_multi` в БД).
- Сколько values уже накоплено в этом словаре.

Операции:
- Создать новый user-defined компонент в этом словаре (имя, template, scope, is_multi).
- Переименовать user-defined.
- Удалить user-defined — **обязательный** warning сколько values станет скрыто в этом словаре. Механика — soft-delete, см. `deletion_concept.md`. Recovery вне scope этой фичи.

Empty state (0 user-defined в словаре): «В этом словаре только translation. Создать дополнительный компонент → CTA».

### Домен (подтверждено)

Источник: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt:8-15`.

- `systemKey` null → user-defined; non-null → built-in.
- `dictionaryId` null → global (применяется ко всем словарям); non-null → per-dictionary.

Значит:
- User-defined может быть **глобальным** (`dictionaryId=null`) — одна запись применяется ко всем словарям.
- Либо **локальным** (`dictionaryId=X`) — привязан к конкретному.
- `definition` в словаре A и `definition` в словаре B (оба локальные) = **две независимые записи**, переименование одного не трогает второй.
- Global `definition` (одна запись с `dictionaryId=null`) применяется во всех словарях; переименование трогает везде.

Built-in (translation) — `systemKey=TRANSLATION`, `dictionaryId=null` (global by definition).

#### Шаблон ↔ значение

Архитектура значений компонента (примитивы, typed views per template, парсер JSON) — см. отдельные документы:
- [`template_model.md`](template_model.md) — `Primitive`, `Field`, `ComponentTemplate` enum со schema через `fields: List<Field>`.
- [`typed_views.md`](typed_views.md) — `TemplateValues` sealed + конкретные `*Values` data classes per template, расположение по модулям.

Для UI: каждый template имеет свой Compose composable (1:1), который рендерит свои поля. При создании компонента UI показывает превью каждого template'а с дефолтным контентом. **Template после создания не меняется** (см. `template_model.md` Open Question #10 — template immutable после релиза).

## Открытые вопросы (решить ДО выбора места)

1. Нужен ли общий view в MVP — или хватит per-dictionary? **Решено: оба сразу.** Per-dictionary — для drill-in работы в контексте словаря. Общий (aggregated) — для просмотра всех user-defined из всех словарей, создания global компонентов (scope=all), управления глобально доступными.
2. Built-in компоненты read-only везде? Или можно «отключить» built-in для конкретного словаря? **Решено: read-only глобально на MVP.** Built-in присутствует в каждом словаре по умолчанию, юзер не может его убрать в конструкторе. Per-dictionary disable built-in (для случая «словарь без translation») — отдельная фича позже (см. `Backlog.md`), реализуется без breaking changes (добавится фильтр в DAO + UI переключатель).
3. User создаёт user-defined в словаре A — это копируется / доступно в словаре B автоматически? **Закрыто (решено ранее, см. раздел «Создать новый» + «Домен (подтверждено)»):** автоматического копирования нет. Юзер сам выбирает scope при создании — global (`dictionaryId=null`, применяется ко всем) либо per-dictionary (одна или несколько конкретных, в БД = N независимых rows). В A и B будут видны разные записи в зависимости от scope.
