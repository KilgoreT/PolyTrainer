# Approved findings — contract_io, итерация 2

20 approved (6 critical + 14 minor). Из них **3 critical** — рассинхрон с другими артефактами (`contract_state` / `contract_ui_msg`). Решение: **откатить** соответствующие изменения в `contract_io`, не выводя их обратно в другие шаги.

## Главное (откат) — что убрать из contract_io

### Откат F019 — поле `isRemovingWord` убрать

`contract_io` v2 ввёл поле `isRemovingWord: Boolean` в State для блокировки double-RemoveWord. Это **изменение shape State** — вне scope `contract_io`. Откатить.

**Альтернатива:**
- `NavigationEffect.Back` идемпотентна на уровне `Navigator.back()` (вторая попытка popBackStack на уже unmounted screen — no-op).
- Cancellation `viewModelScope` после первого Back прерывает второй handler.
- Tech debt принять: «возможно второй RemoveWord effect улетит в БД — DELETE idempotent».

В sealed list Datasource Msg `RemoveWordFailed` оставить (нужен для notification path). Reducer для него: `state.copy(snackbarState = SnackbarState(title=..., show=true))`, без сброса несуществующего флага.

### Откат F024 — `RemoveWord` payload вернуть

В contract_io v2 ты предложил `data object RemoveWord` (убрать payload). Это **изменение формы UI Msg** — вне scope. Откатить.

`RemoveWord(wordId: Long)` как в `contract_ui_msg.md` v2 остаётся. Handler берёт `wordId` из payload.

### Откат F012-driven immediate nullify — вернуть deferred

В ит.2 ты сделал `CommitTranslationEdit` ветка 1 immediate nullify (`translation = null` в reducer). Это **переопределяет reducer-логику UI Msg** — вне scope. Откатить.

Вернуть deferred (как в `contract_ui_msg.md` v2):
- `CommitTranslationEdit` ветка 1 (`edited.isEmpty()`): reducer ставит `isEdit = false`, **не трогает translation field**, шлёт effect `RemoveTranslation(lexemeId)`. После прихода `RefreshTranslation(lexemeId, translation=null)` от data — `translation = null`.

То же для `CommitDefinitionEdit`, `RemoveTranslation`, `RemoveDefinition`.

**F012 view-mode семантика** (что показывается в transient):
- В transient (`isEdit=false`, ждём Refresh) `translation` ещё не null — поле остаётся с **старым origin**.
- UI в view-mode показывает `origin` (старое значение translation) до прихода Refresh.
- Это **легитимное transient-окно** — фиксируем явно в Edge case. Пользователь увидит «удаление с задержкой».
- Альтернатива (мгновенное визуальное удаление) — задача UI sub-flow (например, спиннер поверх chip), не reducer-логика.

**savedOrigin для failure rollback** — больше не нужен (поскольку nullify deferred). Если effect упал — `RemoveTranslationFailed` reducer просто оставляет `translation` как было (origin не менялся). Notify через snackbar.

### F035 UpdateWordFailed без rollback — оставить как есть, но обосновать

Асимметрия с F012 (translation/definition: savedOrigin → rollback) была: для translation reducer оптимистично менял `origin = edited` сразу. **С откатом F012** этого больше нет — translation reducer не меняет `origin` сразу. Значит **симметрия восстанавливается**: ни word, ни translation/definition не нуждаются в savedOrigin/rollback — все обновляют `origin` только при приходе Refresh.

**Уточнение для UpdateWord:** reducer `CommitWordChanges` устанавливает `wordState.value = wordState.edited`, `isEditMode = false`, `edited = ""`. Это оптимистическая запись. **Если effect упал** — value показывает новое значение, БД хранит старое. На следующий вход в экран `LoadWord` вернёт старое value из БД — silent revert. Это tech debt, можно зафиксировать в `## Бизнес-инварианты` как «word commit оптимистичный, БД-revert silent».

Если хотим симметрию с translation — поменять reducer `CommitWordChanges` чтобы он **не** менял value сразу, ждать `WordUpdated(text)` от handler'а. Но это **изменение reducer-логики UI Msg** — вне scope contract_io. Если решим так — feedback в `contract_ui_msg`. Пока — оставить оптимистично + tech debt.

### F032 «Mate сериализует» — убрать неверное утверждение

В F021 (line 132) у тебя написано «Mate сериализует effects через `viewModelScope.launch`» — это **неверно**, `launch` не сериализует. Убрать утверждение. Заменить на:
- «Mate запускает каждый effect в независимой корутине через `viewModelScope.launch{...}`. Порядок завершения effects не гарантирован. Race между UPDATE/DELETE возможен на уровне БД — handler не имеет mutex'а. Acceptable для текущей фичи (БД-level idempotency для DELETE, last-write-wins для UPDATE). Если в будущем потребуется строгий порядок — нужен mutex / sequential dispatch (задача framework-level, не feature)».

### F033/F034 — после отката F019 closure findings

Concurrent CreateLexeme + RemoveWord race (orphan-lexeme): без `isRemovingWord` поля единственный guard — `wordState.id != NOT_IN_DB`. Если `RemoveWord` уже отправлен и вернётся через `NavigationEffect.Back`, в state ничего не сигнализирует «word удаляется». Concurrent CreateLexeme может улететь.

**Альтернатива (без shape state change):** при `RemoveWord` Msg reducer выставляет `isLoading = true` (используем существующее поле!). Это блокирует все Msg с guard'ом `!isLoading`. Семантика: «word в процессе удаления = loading-режим, UI заблокирован». Реиспользуем существующее поле — это reducer-policy для UI Msg `RemoveWord`, что находится в **scope contract_ui_msg**.

Это значит — F033/F034 fix требует feedback в `contract_ui_msg`: «reducer `RemoveWord` дополнительно выставляет `isLoading = true`». Записать как feedback-секцию в новом артефакте.

## Minor findings (14) — все простые fix'ы или wontfix

Подробнее в `contract_io_review.md` итерация 2.

- **F026** — guards-таблицы расходятся: после отката isRemovingWord синхронизуются автоматически.
- **F027** — forward-ref в `contract_ui_msg` не содержит новых failure-Msg: можно либо добавить feedback в ui_msg, либо принять что forward-ref в ui_msg остаётся минимальным (полный канон — в io). Wontfix, упомянуть в feedback-секции опционально.
- **F028** — избыточный `?.` в pseudocode: точечный fix.
- **F029** — NavigateBack reducer нота: после отката `isRemovingWord` не нужно.
- **F030** — null vs throw не различаются: backlog, оставить.
- **F036** — NavigateBack во время Remove: после отката `isRemovingWord` — упоминается как «handler cancellation = БД может не завершить DELETE; acceptable, idempotent retry на следующем входе».
- **F037** — OpenLexemeMenu не в guards: общая сноска guard покрывает.
- **F038** — EC2 Failure при втором edit: добавить явный сценарий в Edge cases.
- **F039** — `TAG` в Log.w: указать `private const val TAG = "DatasourceEffectHandler"`.
- **F040** — RemoveTranslationFailed isMenuOpen: явная нота «failure не трогает menu (он уже закрыт reducer'ом RemoveTranslation Msg)».
- **F041** — DismissNotification batching: добавить ноту.
- **F042** — двойной Commit-empty: добавить EC.

## Раздел в новом артефакте — feedback в предыдущие шаги

Создай раздел `## Feedback в предыдущие шаги` с пунктами:

1. **`contract_ui_msg`:** Рассмотреть добавление в reducer `RemoveWord` Msg выставления `isLoading = true` — для блокировки concurrent lexeme/translation ops пока word удаляется. (F033/F034)
2. **`contract_state`:** Tech debt — `CommitWordChanges` оптимистическая запись `value = edited` без savedValue; silent revert при failure. Решение в backlog. (F035)
3. **`contract_ui_msg`:** Forward-ref таблица может быть расширена failure-Msg семейством, либо явно зафиксировано что forward-ref минимальный. Wontfix. (F027)

Conductor решит — принимать feedback (откатить нужный шаг и провести правки) или фиксировать в backlog.

---

## Задача итерации 3

Перепиши `contract_io.md` с откатами:
1. Убрать поле `isRemovingWord` из state-описания.
2. Вернуть `RemoveWord(wordId: Long)` (data class).
3. Вернуть deferred-подход для Commit-empty и Remove-translation/definition.
4. Убрать `savedOrigin` payload из `RemoveTranslation/Definition` effect.
5. Заменить F021 утверждение про Mate-сериализацию.
6. Добавить раздел `## Feedback в предыдущие шаги`.
7. Закрыть minor findings точечно (см. выше).

Header, секции, `_model_` — сохрани.
