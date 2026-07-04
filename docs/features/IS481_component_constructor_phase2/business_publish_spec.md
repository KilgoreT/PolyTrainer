# publish_spec

## Опубликовано

- `docs/features-spec/component-constructor.md` — обновлён (extend phase 1 + phase 2).
- Размер итогового файла: **1470 строк** (phase 1 baseline был ~1359 строк; +~110 строк фактического содержания phase 2 + перетасовка структуры).

Стратегия: один файл `component-constructor.md` объединяет phase 1 и phase 2 (без отдельного `component-constructor-phase2.md` — следуем правилу «не плодить спеки с разными именами для одной фичи»). Сохранены phase 1 разделы (бизнес-описание, user stories, state, UI Messages, UI Layout, IO, UseCase, Тестовые сценарии); phase 2 добавления интегрированы inline в каждый раздел с пометкой «(phase 2)».

## Корректировки от implement

Спека приведена в соответствие с реальной реализацией (фиксации decisions из `business_implement.md`):

1. **`typeId: ComponentTypeId`** в `DatasourceEffect.EditComponent` и в `UseCase.editComponent(...)` (НЕ `Long`). `Long` остаётся только на data API (`LexemeApi.editComponentType`) — parity с `renameComponentType`. Спека отражает эту границу.
2. **`ComponentsManagerScreenState`** — каноническое имя (НЕ `ComponentsManagerState`). Применено как в типе, так и в reducer-таблице.
3. **`epochId` для Edit** живёт внутри `EditDialogState.epochId: Long = 0L` — явно вынесено в data class (parity с Create/Rename/Delete epoch correlation).
4. **`EditOutcome.Failure` Reducer закрывает editDialog** — асимметрия с `RenameOutcome.Failure`, зафиксирована в reducer-таблице (`editDialog=null` при Failure). Test-driven решение из implement-стадии.
5. **`CreateScopeChange(Scope.Global)` очищает `selectedDictionaryIds`** — добавлено в reducer-таблицу как отдельная строка (`createDialog?.copy(scope=Global, selectedDictionaryIds=emptySet())`). Test-driven решение, не было в design_tree.
6. **`LexemeApi.editComponentType`** принимает все 4 параметра (`typeId: Long, name, template, isMulti`) — template и isMulti оба передаются на API. Immutability check выполняется на UseCaseImpl как gate ПЕРЕД вызовом API (не отдаётся вниз).
7. **`snackbarState`** — каноническое имя поля (НЕ `snackbar`). Применено везде в reducer-таблице и State.
8. **`:modules:screen:components_manager` build.gradle** получает dep на `:core:core-db-api` — упомянуто в Domain-shared types / State секции (`availableDictionaries: List<DictionaryApiEntity>` напрямую импортируется из `core-db-api`).
9. **`Removed` ветка добавлена в `RenameComponentOutcome` / `SoftDeleteComponentOutcome`** API-entity — отражено в подразделе "Data API".
10. **Data API доп. ветки** (`TemplateImmutable` в `EditComponentOutcome`) — оставлены как «defensive parity»; основная проверка на UseCase.

## PUML

- PUML-схем в проекте не найдено (нет `.puml` / `.plantuml` файлов в `docs/features/IS481_component_constructor_phase2/`).
- Шаг пропущен.

## Что НЕ попало в спеку

- Удаляемые phase 1 поля (`snackbar` → переименовано в `snackbarState`, оригинал убран — спека показывает финальное имя).
- Развёрнутый ADT-аудит EditOutcome веток (детали — в `business_contract_spec.md` черновике).
- Детали реализации: классы `ComponentsManagerUseCaseImpl`, packages вроде `app/.../componentsmanager/`, decisions data impl (cardinality downgrade approximation). Это всё в `business_implement.md`.
- Iter 1 заметки и build/test ход — в `business_implement.md`.

## log_messages

- Прочитан черновик `business_contract_spec.md` (629 строк), `business_implement.md` (71 строк), существующая `docs/features-spec/component-constructor.md` (1359 строк).
- Проверено отсутствие PUML файлов в feature dir.
- Сформирован финальный spec: phase 1 sections preserved + phase 2 sections inlined в каждый раздел (Бизнес-описание / User Stories / State / UI Messages / UI Layout / IO / UseCase / Тестовые сценарии).
- 10 наименований/контрактных корректировок применены из implement-стадии (см. § Корректировки).
- Перезаписан `docs/features-spec/component-constructor.md` (1470 строк итого).
- Отчёт `business_publish_spec.md` записан.

_model: claude-opus-4-7[1m]_
