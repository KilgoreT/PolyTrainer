# Концепция удаления компонентов

> **В этом документе смешаны два состояния:**
> - § «Текущее состояние удалений по таблицам» — описывает **M12 state** (как удаление устроено сейчас). Колонка soft-delete называется `remove_date`, используется только в `component_types`.
> - § «Рекомендации soft-delete» и далее — описывают **target state после M13**. Колонка переименована в `removed_at`, добавлена в `component_values`.
>
> Не путать. В рекомендациях/решениях используется новое имя (`removed_at`), в аудите — старое (`remove_date`).

## Принцип

**Soft-delete без recovery в этой фиче.** Удалённый компонент помечается `removed_at = now()` и скрывается из активных queries. Лежит в БД бесконечно.

UI восстановления (корзина / архив) и фоновый TTL hard-delete — **отдельная следующая фича** (см. `Backlog.md`). В IS481 component_constructor — только сам soft-delete механизм (без юзер-видимого пути назад).

## Текущее состояние удалений по таблицам

Аудит существующих DAO / UseCase / UI триггеров и FK CASCADE.

### `dictionaries`
- **DAO:** `WordDao.deleteDictionary(id)` — hard `DELETE FROM dictionaries WHERE id = :id`.
- **FK CASCADE:** удаление каскадно сносит `words` → далее `lexemes` → `component_values` + `write_quiz`. Также каскадно сносит `component_types` (per-dict, built-in с `dictionary_id=NULL` не задеты) и `quiz_configs`.
- **UseCase:** `DictionaryUseCaseImpl.deleteDictionary(id)` (+ сброс/переключение `CURRENT_DICTIONARY_ID_LONG` pref).
- **UI:** `DictionaryListScreen` → confirm-dialog → `ConfirmDeleteDictionaryWidget`.

### `words`
- **DAO:** `WordDao.removeWordSuspend(id): Int` — hard delete.
- **FK CASCADE:** на `lexemes` → `component_values` + `write_quiz`. `samples`/`hints` FK НЕ объявлен (orphan).
- **UseCase:** `WordApiImpl.deleteWordSuspend` (legacy manual cascade: samples + lexemes до DELETE word). Вызывают `DictionaryTabUseCaseImpl.deleteWord` и `WordCardUseCaseImpl.deleteWord`.
- **UI:** `dictionaryTab` confirm-delete-word widget + WordCard «удалить слово» с короткоживущим Undo snackbar.

### `lexemes`
- **DAO:** `WordDao.deleteLexemeById(id)`, `deleteDefinitionSuspend(vararg id)`, `deleteDefinitionsSuspend(vararg)` — все hard.
- **FK CASCADE:** на `component_values` + `write_quiz`. `samples`/`hints` без FK — orphan.
- **UseCase:** `LexemeApiImpl.deleteLexeme(id)` ← `WordCardUseCaseImpl.deleteLexeme`. **Auto-cascade lexeme** при удалении последнего component (ветка `remaining == 0` в `deleteLexemeComponentBy`).
- **UI:** WordCard — кнопка/жест удаления лексемы; implicit cascade при удалении последнего translation/definition компонента.

### `component_types`
- **DAO:** `ComponentTypeDao.softDelete(id, now)` — `UPDATE ... SET remove_date = :now WHERE id = :id AND system_key IS NULL` (soft, защита от удаления built-in).
- **FK CASCADE:** при hard delete row (вне `softDelete`) `component_values` каскадно сносится. На уровне soft-delete каскада нет — values остаются, read-queries фильтруют type по `remove_date IS NULL`.
- **UseCase:** **нет** — `softDelete` не дёргается. Тип read-only, конструктор UI ещё не реализован.
- **UI:** **нет триггера**.

### `component_values`
- **DAO:** `ComponentValueDao.delete(id)`, `deleteByLexemeAndType(lexemeId, typeId)` — оба hard.
- **FK CASCADE:** leaf, ничего не каскадно. Сам каскадно удаляется при delete `lexeme` или `component_type` (hard).
- **UseCase:** `LexemeApiImpl.deleteComponentValue(id)` → возвращает count оставшихся; `WordCardUseCaseImpl.deleteComponentValue` / `deleteDefinitionComponent` / `deleteLexemeTranslation`. При `remaining == 0` дёргается cascade `deleteLexeme`.
- **UI:** WordCard — удаление translation/definition «chip»; `RemoveComponentResult.LexemeCascadeRemoved` indicator.

### `samples`
- **DAO:** `WordDao.removeSampleSuspend(vararg SampleDb)` — `@Delete` hard.
- **FK CASCADE:** **FK НЕ объявлен** на `lexemeId` — orphan-style при удалении лексемы.
- **UseCase:** только внутри `WordApiImpl.deleteWordSuspend` (manual cleanup перед DELETE word). User-facing CRUD нет.
- **UI:** триггера нет. `removeDate` в схеме (TODO «удалить?»), но никем не пишется и не читается. **Фактически dead branch.**

### `hints`
- **DAO:** **нет.** Entity зарегистрирована в Database, но без `@Insert` / `@Delete` / `@Query`.
- **FK CASCADE:** FK НЕ объявлен — orphan.
- **UseCase:** **нет.** `HintMapper` / `HintDumpMapper` существуют, но никем не вызываются.
- **UI:** **нет триггера.** Таблица **полностью dead** — только migrations её держат.

### `quiz_configs`
- **DAO:** **нет** методов удаления (только insert/update/get).
- **FK CASCADE:** удаление dictionary каскадно сносит configs.
- **UseCase:** нет прямого вызова — cleanup только через cascade dictionary-delete.
- **UI:** нет триггера; config создаётся атомарно при `addDictionary` (default `[BuiltIn(TRANSLATION)]`).

### `write_quiz`
- **DAO:** `WordDao.removeWriteQuiz(lexemeId)` — hard.
- **FK CASCADE:** на `lexeme_id`; сам — leaf.
- **UseCase:** **orphan API** — `removeWriteQuiz` не дёргается ни одним UseCase. Очистка через cascade от lexeme/word/dictionary.
- **UI:** нет триггера; квиз-запись живёт пока живёт лексема.

### Существенные находки

- **Soft-delete (`removeDate`) объявлен в `ComponentTypeDb`, `SampleDb`, `HintDb`,** но активно используется **только** в `component_types` (`softDelete` + фильтры `WHERE remove_date IS NULL`). В `samples`/`hints` `removeDate` никем не пишется/читается.
- **Orphan API:** `ComponentTypeDao.softDelete` и `WordDao.removeWriteQuiz` существуют, но никто не вызывает.
- **Dead branches:** `samples` (без UI CRUD, удаляется только при delete word), `hints` (никаких DAO-методов, никем не используется — только в schema).
- `WordApiImpl.deleteWordSuspend` делает **legacy manual cascade** (samples + lexemes до DELETE words) — paranoia, FK CASCADE уже работает на lexemes; sample-удаление здесь единственный реальный delete-call для `samples`.

## Рекомендации soft-delete per таблица

Аргументы за soft-delete (с TTL recovery) — где целесообразно.

### Нужен soft-delete

- **`dictionaries`** — сейчас hard. Recovery критичен: словарь содержит недели/месяцы накопленных данных (слова, лексемы, компоненты). Случайное удаление = огромная боль. TTL 30 дней даёт окно одуматься.
- **`words`** — сейчас hard + короткоживущий Undo snackbar (~4-5 сек). TTL даёт **второй уровень** защиты — если юзер закрыл snackbar и через час понял что зря.
- **`lexemes`** — сейчас hard + auto-cascade при удалении последнего компонента. **Особо опасный сценарий** — юзер удалил один компонент и неявно потерял всю лексему. Soft-delete + recovery защищает от непреднамеренного auto-cascade.
- **`component_types`** — soft-delete **уже есть** в DAO. В M13 — rename `remove_date` → `removed_at`, привязать UseCase (конструктор).
- **`component_values`** — нужен soft-delete: при auto-cascade lexeme'ы (см. выше) values каскадно теряются, recovery должен возвращать и их вместе с lexeme.
- **`samples`** — нужен soft-delete **если** появится UI CRUD для samples (создавать/редактировать/удалять примеры). Сейчас dead branch — пока решение «оставить как есть до фичи».
- **`hints`** — то же. Полностью dead branch. До появления реальной фичи hint'ов — решение не нужно.

### Hard-delete достаточно

- **`quiz_configs`** — derived/конфиг. При случайном удалении словаря cascade убирает configs тоже — но **default config регенерируется** автоматически на следующее `addDictionary` (и на recovery dictionary можно вызвать seed config). Recovery не нужен на уровне БД.
- **`write_quiz`** — append-only log результатов квиза. Юзер не управляет вручную. Очистка только через cascade. Восстановление не имеет смысла (свежие квизы — это новые попытки).

### Финальное распределение scope

**В этой фиче (IS481 component_constructor):**
- `component_types` — rename `remove_date` → `removed_at` (M13) + привязать UseCase из конструктора к существующему `softDelete`.
- `component_values` — добавить колонку `removed_at` (M13) + soft-delete операции (cascade при soft-delete родительского `component_type` — через JOIN-фильтр на `parent.removed_at IS NULL`, без UPDATE values).
- **DAO convention:** все active-data queries для `component_types` / `component_values` обязаны иметь `WHERE removed_at IS NULL` (либо JOIN с тем же фильтром на родителя). Audit на этапе implementation: пройти по всем DAO methods на этих таблицах.

**Recovery UI и background TTL hard-delete** — вне scope этой фичи (отдельная следующая фича, см. `Backlog.md`).

**В backlog (отдельные фичи позже):**
- `dictionaries` — soft-delete + recovery (огромная боль при случайном удалении).
- `words` — soft-delete + recovery (TTL даёт второй шанс после короткого Undo snackbar).
- `lexemes` — soft-delete + recovery (особо: защита от непреднамеренного auto-cascade при удалении последнего component).

**Не трогаем сейчас:**
- `samples` / `hints` — фактически dead branches; отложено до **миграции в template компоненты** (decommission отдельных таблиц, заменить на user-defined component templates).
- `quiz_configs` — hard-delete достаточно навсегда (derived, defaults регенерируются).
- `write_quiz` — hard-delete достаточно навсегда (append-only log результатов квиза).

## Что удалять

При удалении user-defined `component_type` затрагивает:
- Сам `component_type` (per-dictionary).
- Все `component_value` относящиеся к этому типу (values в лексемах).
- Ссылки в `quiz_configs.component_refs` (нужно убрать ref на удалённый тип — иначе квиз попытается тянуть удалённый компонент).

При **soft-delete**:
- `component_type.remove_date = now()` — тип скрыт из активных запросов.
- Связанные `component_value` — либо тоже soft-delete (cascade), либо помечаются неявно (через JOIN на `component_type.remove_date IS NULL`).
- `quiz_configs.component_refs` — убрать ref сразу (иначе при recovery возникнут вопросы что с конфигом). Либо хранить отдельный `removed_refs` если хотим точное восстановление.

**Recovery в этой фиче не реализуется** — отдельная следующая фича (см. `Backlog.md`). При появлении: очистить `removed_at`, связанные values становятся видны автоматически через JOIN-фильтр, `quiz_configs.component_refs` — пользователь сам решит включать ли назад в квиз.

## TTL

**В этой фиче TTL hard-delete НЕ реализуется.** Удалённые компоненты хранятся в БД бесконечно (с пометкой `removed_at`, скрыты из активных queries). Корзины и manual «Удалить навсегда» в UI этой фичи нет. Полноценный recovery + background TTL — отдельная следующая фича (см. `Backlog.md`).

Фоновый TTL job — **отдельная следующая фича** (см. `Backlog.md`). Тогда и решим TTL value (30/14/60 дней, конфигурируемость).

## Где раздел recovery

**Не в этой фиче.** Корзина / архив с восстановлением — отдельная следующая фича (см. `Backlog.md` — объединена с background TTL hard-delete).

Когда будет реализовано — отдельный экран «Удалённые компоненты», возможные места размещения: внутри конструктора (общий view, кнопка «Удалённые»), либо в settings tab. Карточка восстановления: имя, словарь, сколько values, сколько дней до hard-delete (если TTL), кнопка «Восстановить».

## Открытые вопросы

1. **TTL value.** Решено: в этой фиче TTL hard-delete не реализуется, soft-deleted записи хранятся бесконечно. Background cleanup job — отдельная фича позже (см. `Backlog.md`); TTL value будет выбран там.
2. **Visibility в quiz session.** Закрыто как невозможный сценарий: чтобы юзер удалил компонент, он должен закрыть chat (где идёт активная квиз-сессия) и перейти в конструктор — то есть сессия физически закрывается раньше, чем юзер дотягивается до delete. Multi-window / sync-from-other-device не реализованы. Edge case не существует в текущем UI.
3. **Conflict при recovery.** Закрыто как вне scope этой фичи: recovery откладывается в отдельную следующую фичу (см. `Backlog.md`). При её реализации — диалог с переименованием (юзер вводит новое имя для восстановленного при конфликте; default `name (2)` подсказка).
4. **Hard-delete по запросу из корзины.** Закрыто как вне scope этой фичи: корзины нет, manual hard-delete тоже отложен в фичу recovery + TTL.
5. **Безопасность от случайной массовой потери.** Закрыто: only per-row delete, никаких bulk «удалить все» в UI. Это применяется как convention к конструктору в этой фиче.
