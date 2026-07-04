# Checklist

- [ ] Юзер открывает Settings tab → видит пункт «Управление компонентами» → drill-in → видит aggregated view всех user-defined компонентов из всех словарей с counter «используется в N словарей» [spec]
  - [ ] Лог: ###ComponentConstructor### OpenComponentsManager from SettingsTab [spec]
  - [ ] Лог: ###ComponentConstructor### Aggregated view loaded: N user-defined types [spec]
- [ ] Юзер открывает любой из табов (Vocabulary/Quiz/Statistic) с выбранным словарём → видит icon-button «молоток» в DictionaryAppBar (видим если currentDict != null) → drill-in → видит per-dictionary view только этого словаря [spec]
  - [ ] Лог: ###ComponentConstructor### OpenPerDictionaryComponents dictId=<id> tab=<vocab|quiz|stat> [spec]
  - [ ] Лог: ###ComponentConstructor### Per-dict view loaded: dictId=<id>, N user-defined types [spec]
- [ ] Юзер создаёт новый user-defined компонент (имя + шаблон + scope «На все»/«На конкретные» + cardinality is_multi checkbox) → компонент появляется в списке [spec]
  - [ ] Лог: ###ComponentConstructor### CreateComponent name=<name> template=<key> scope=<global|per-dict:[ids]> isMulti=<true|false> [spec]
  - [ ] Лог: ###ComponentConstructor### CreateComponent success typeId=<id> (или ошибка коллизии имени) [spec]
- [ ] Юзер переименовывает user-defined компонент → новое имя везде (cascade обновление quiz_configs.component_refs если есть) [spec]
  - [ ] Лог: ###ComponentConstructor### RenameComponent typeId=<id> oldName=<old> newName=<new> [spec]
  - [ ] Лог: ###ComponentConstructor### RenameCascade quiz_configs updated: N rows [spec]
- [ ] Юзер удаляет user-defined компонент → soft-delete с warning сколько values станет скрыто + сколько quiz_configs затронуто + сколько prefs сбросится [spec]
  - [ ] Лог: ###ComponentConstructor### PreviewDeletionImpact typeId=<id> values=N dicts=[...] quizConfigsAffected=N affectedPrefs=N [spec]
  - [ ] Лог: ###ComponentConstructor### SoftDeleteComponent typeId=<id> removedAt=<ts> [spec]
  - [ ] Лог: ###ComponentConstructor### DeleteCascade quiz_configs cleaned: N refs removed, prefs reset: N [spec]
- [ ] Юзер пытается понизить cardinality is_multi=true→false при наличии лексем с count>1 → операция блокируется с показом списка проблемных лексем [spec]
  - [ ] Лог: ###ComponentConstructor### DowngradeCheck typeId=<id> maxCount=<N> blocking=true [spec]
  - [ ] Лог: ###ComponentConstructor### DowngradeBlocked typeId=<id> conflictingLexemes=[...] [spec]
- [ ] Cold start после soft-delete: компонент не появляется в активных queries (фильтр removed_at IS NULL во всех DAO) [spec]
  - [ ] Лог: ###ComponentConstructor### ColdStart active types loaded: N (removed: M filtered out) [spec]
- [ ] Quiz session использует только не-removed компоненты (фильтр на parent.removed_at IS NULL при JOIN component_values↔component_types) [spec]
  - [ ] Лог: ###ComponentConstructor### QuizSession effectiveRefs filtered: kept=N removed=M [spec]

## Ручное тестирование

### Сценарий 1. Открытие aggregated view из Settings tab
1. Установить debug build с минимум двумя словарями, в каждом — по 1-2 user-defined компонента (можно создать через сценарий 3 перед запуском).
2. Открыть приложение → нижняя навигация → Settings tab.
3. Ожидание: в списке настроек виден пункт «Управление компонентами» (рядом с «Управление языками», `LangManageWidget`).
4. Тапнуть пункт.
5. Logcat: `###ComponentConstructor### OpenComponentsManager from SettingsTab` + следом `###ComponentConstructor### Aggregated view loaded: N user-defined types`.
6. Ожидание: открывается full-screen `ComponentsManagerScreen` со списком всех user-defined компонентов из всех словарей.
7. Ожидание: каждая строка содержит — имя, template (TEXT и т.п.), cardinality badge (single/multi), counter «используется в N словарей» с раскрытием списка словарей, optional counter values.
8. Ожидание: built-in компоненты (translation) в списке отсутствуют.
9. Ожидание (empty state): если 0 user-defined → текст «У вас нет своих компонентов. Translation работает автоматически в каждом словаре. Создать свой компонент → выбрать словарь X».

### Сценарий 2. Открытие per-dictionary view из DictionaryAppBar (3 таба)
1. Открыть приложение → Vocabulary tab.
2. Через picker (DictDropDownWidget) выбрать любой словарь (currentDict != null).
3. Ожидание: в `DictionaryAppBar` появляется icon-button «молоток» **перед** picker'ом (если currentDict не выбран — иконка скрыта; проверить snapshot до выбора).
4. Тапнуть icon-button «молоток».
5. Logcat: `###ComponentConstructor### OpenPerDictionaryComponents dictId=<id> tab=vocab` + следом `###ComponentConstructor### Per-dict view loaded: dictId=<id>, N user-defined types`.
6. Ожидание: открывается full-screen `PerDictionaryComponentsScreen`, в списке только user-defined компоненты этого словаря (built-in скрыты).
7. Назад → перейти в Quiz tab → повторить шаги 3-6 (ожидание: log с `tab=quiz`).
8. Назад → перейти в Statistic tab → повторить шаги 3-6 (ожидание: log с `tab=stat`).
9. Ожидание: icon-button присутствует на всех 3 табах (общий `DictionaryAppBar`), screen открывается одинаково с правильным `dictId`.

### Сценарий 3. Создание нового user-defined компонента
1. Любой из двух entry-point'ов (aggregated либо per-dict).
2. Тапнуть CTA «Создать новый компонент».
3. Ожидание: открывается create form с полями — Имя (input), Шаблон (selector с UI-превью каждого template, по умолчанию TEXT), Несколько значений (`is_multi` checkbox, off по умолчанию), Scope (radio «На все словари» / «На конкретные» с multi-select словарей).
4. Заполнить: Имя = «Definition», Шаблон = TEXT, is_multi = off, Scope = «На все словари».
5. Тапнуть «Создать».
6. Logcat: `###ComponentConstructor### CreateComponent name=Definition template=text scope=global isMulti=false` + следом `###ComponentConstructor### CreateComponent success typeId=<N>`.
7. Ожидание: форма закрывается, компонент появляется в списке с counter «используется в 0 словарей» (только что создан, values ещё нет).
8. Повторить шаги 2-6 с тем же именем «Definition» и тем же scope (global) либо с per-dict scope в словаре, где global «Definition» активен.
9. Ожидание: form показывает ошибку «Имя уже занято» (uniqueness invariant из aspect `userdefined_identity_invariant` — same-scope коллизия + cross-scope global vs per-dict коллизия).
10. Logcat: `###ComponentConstructor### CreateComponent name=Definition ... success=false reason=name_collision`.
11. Повторить с per-dict scope в другом словаре и уникальным именем — успех (две независимых записи в разных словарях).

### Сценарий 4. Переименование user-defined компонента
1. Aggregated либо per-dict view → выбрать компонент с known именем (например «Definition» создан в Сценарии 3).
2. Тапнуть action «Переименовать» → ввести новое имя «Определение».
3. Подтвердить.
4. Logcat: `###ComponentConstructor### RenameComponent typeId=<id> oldName=Definition newName=Определение` + следом `###ComponentConstructor### RenameCascade quiz_configs updated: N rows` (где N = число `quiz_configs` строк, у которых `component_refs` содержал `user:Definition`).
5. Ожидание: в списке новое имя «Определение», старое нигде не отображается.
6. Открыть Quiz tab выбранного словаря → меню → picker → проверить radio: ранее checked `UserDefined("Definition")` теперь отображается как `UserDefined("Определение")` (cascade в `quiz_configs.component_refs` отработал; pref `quiz_picker_dict_<id>` может либо остаться `user:Definition` (stale, fail-soft) либо мигрировать — точное поведение фиксируется на business_contract).
7. Запустить quiz session — компонент работает под новым именем (или fallback на translation если pref-ref stale).

### Сценарий 5. Soft-delete user-defined компонента с warning
1. Aggregated либо per-dict view → выбрать компонент с накопленными values (например создать 2-3 lexeme'ы с component_value этого типа предварительно).
2. Тапнуть action «Удалить».
3. Logcat: `###ComponentConstructor### PreviewDeletionImpact typeId=<id> values=3 dicts=[1,2] quizConfigsAffected=2 affectedPrefs=1`.
4. Ожидание: confirm-dialog с warning: «Будет скрыто N values в M словарях. Затронуто X конфигов квиза. Сбросится выбор компонента в Y словарях (в picker'е квиза)».
5. Подтвердить удаление.
6. Logcat: `###ComponentConstructor### SoftDeleteComponent typeId=<id> removedAt=<ts>` + `###ComponentConstructor### DeleteCascade quiz_configs cleaned: 2 refs removed, prefs reset: 1`.
7. Ожидание: компонент исчезает из обоих views (aggregated и per-dict).
8. Открыть Quiz tab затронутого словаря → меню → picker → ожидание: ref удалённого компонента отсутствует в `availableTypes`; если был выбран — fallback на `availableTypes.first()` (translation).
9. Запустить quiz session: items этого component_type не появляются (`WHERE removed_at IS NULL` фильтр в DAO + JOIN-фильтр на parent.removed_at).
10. Открыть WordCard любой lexeme'ы, у которой был value этого компонента — value скрыт (фильтр в `LexemeDbEntity.componentValueListDb` mapping per F031).

### Сценарий 6. Cardinality downgrade блокировка
1. Создать user-defined компонент с `is_multi = true` (например «Sample», TEXT, per-dict).
2. Через WordCard добавить 2+ values этого компонента к одной lexeme'е (например две цитаты на одно слово).
3. Открыть per-dict либо aggregated view → выбрать «Sample» → edit → переключить `is_multi` чекбокс с true на false.
4. Подтвердить.
5. Logcat: `###ComponentConstructor### DowngradeCheck typeId=<id> maxCount=2 blocking=true` + `###ComponentConstructor### DowngradeBlocked typeId=<id> conflictingLexemes=[<lexemeId>]`.
6. Ожидание: операция блокируется, dialog показывает список лексем с count>1 (юзер видит конкретные слова, из которых нужно удалить лишние values).
7. Удалить через WordCard лишние values так, чтобы у всех лексем count == 1.
8. Повторить шаги 3-4.
9. Logcat: `###ComponentConstructor### DowngradeCheck typeId=<id> maxCount=1 blocking=false`.
10. Ожидание: downgrade проходит, `is_multi = false` сохраняется, badge меняется на «single».

### Сценарий 7. Cold start фильтр на removed_at
1. Выполнить Сценарий 5 (soft-delete компонента).
2. **Cold start**: kill процесс (swipe из recents) → перезапустить приложение.
3. Открыть Settings → ComponentsManager.
4. Logcat: `###ComponentConstructor### ColdStart active types loaded: N (removed: M filtered out)` (где M ≥ 1 — soft-deleted из шага 1).
5. Ожидание: удалённый компонент не в списке (фильтр `WHERE removed_at IS NULL` на чтении).
6. Открыть любой dict-tab → молоток → per-dict view.
7. Ожидание: удалённый компонент не в списке (тот же фильтр в per-dict DAO).
8. Открыть WordCard любой lexeme'ы со скрытыми values удалённого типа.
9. Ожидание: values этого типа не отображаются (`LexemeDbEntity` post-load filter либо JOIN-фильтр работает после cold start).
10. Дополнительно (verify schema): подключить ADB → `adb shell` → `sqlite3 <db>` → `SELECT id, name, removed_at FROM component_types WHERE removed_at IS NOT NULL;` — soft-deleted row физически в БД, не hard-deleted.

### Сценарий 8. Quiz session использует только не-removed компоненты
1. Словарь с translation + user-defined «Definition» (active). Quiz config содержит обоих в `component_refs`.
2. Запустить quiz session, открыть picker, выбрать `UserDefined("Definition")`, идёт квиз с definition items.
3. Завершить session.
4. Через ComponentsManager soft-delete «Definition» (Сценарий 5).
5. Logcat (после delete): `###ComponentConstructor### DeleteCascade quiz_configs cleaned: 1 refs removed` (ref на «Definition» удалён из `component_refs`).
6. Открыть quiz chat снова → picker → ожидание: только `BuiltIn(TRANSLATION)` (один пункт, disabled+checked); `UserDefined("Definition")` отсутствует.
7. Запустить новую quiz session.
8. Logcat: `###ComponentConstructor### QuizSession effectiveRefs filtered: kept=1 removed=0` (текущий `component_refs` уже не содержит удалённый ref после cascade; runtime фильтр на parent.removed_at тоже отработал).
9. Ожидание: items только translation; definition items не появляются.
10. Edge case (cascade race): если cascade `quiz_configs.component_refs` не отработал атомарно с soft-delete (Open question on `data_design_tree`) — runtime JOIN-фильтр на `parent.removed_at IS NULL` всё равно гарантирует, что quiz не отдаст items удалённого типа (defence in depth).

## Примечание о логах

Тэг `###ComponentConstructor###` — **новый**, на момент создания checklist в кодовой базе **отсутствует**. Будут добавлены вызовы `logger.d(tag = "###ComponentConstructor###", ...)` в:
- UseCase методы (`createUserDefinedComponent`, `renameComponent`, `softDeleteComponent`, `previewDeletionImpact`, downgrade check) — точки входа и outcomes;
- Reducer/EffectHandler двух новых экранов — навигационные effects;
- Migration M12→M13 (`Migration_012_to_013.kt`) — счётчики rewrite'нутых rows, дроп индексов, backfill timestamps;
- DAO cascade-методы (`quiz_configs.component_refs` cleanup, prefs cleanup) — счётчики затронутых rows.

Параллельно используются существующие теги:
- `###MATE### RunEffect: <Effect>` / `Reduce ---message---: <Msg>` — из `MateTypedEffectHandler` / Reducer (см. checklist IS481_quiz_component_picker).
- Crashlytics-логи для fail-soft парсинга (unknown template / unknown primitive / type mismatch — per aspect `parser_fail_soft`).

Детализация payload каждого лог-сообщения и точное место вызова — фиксируются на шагах `business_contract`/`business_design_tree`/`data_design_tree`.

## log_messages
- checklist.md создан со списком 8 корневых бизнес-сценариев из concept (ui_placement.md + deletion_concept.md) + раздел «Ручное тестирование» с 8 сценариями
- лог-точки для каждого корневого пункта помечены как `[spec]` подпункты под тегом `###ComponentConstructor###` (новый тег, в codebase отсутствует — добавляется на implementation шагах)
- системные события (load, error, schema migration steps, fail-soft парсинг) не вынесены как корневые — будут добавлены подпунктами на следующих шагах (infra/business/ui/data design+test+implement)
- сценарий 6 (cardinality downgrade) уточнён: блокировка на UseCase-уровне с показом списка проблемных лексем (aspect `multi_to_single_downgrade`)
- сценарий 8 (quiz session) включает defence in depth — runtime JOIN-фильтр на parent.removed_at работает даже если cascade `quiz_configs.component_refs` не отработал атомарно

_model: claude-opus-4-7[1m]_
