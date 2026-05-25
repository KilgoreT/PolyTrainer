---
name: contract_io
output: contract_io.md
output_criteria:
  - contract_io.md существует
  - contract_io.md содержит секцию Effects
  - contract_io.md содержит секцию Subscribers
  - contract_io.md содержит секцию Проверка реактивности
  - contract_io.md содержит секцию Бизнес-инварианты
  - contract_io.md содержит секцию Расхождения spec ↔ code
  - Effects содержит три категории (Datasource / Navigation / Ui)
  - каждый Effect — целостный блок с Source / Handler / Action / Return Msg / Reducer-логика / Edge cases
  - Проверка реактивности с явными Y/N + обоснованием для каждого ответа
---

Шаг 3 контрактного блока. **Канон** Effects + Datasource Msg + Subscribers + Edge cases. Объединённый шаг outer world layer.

Forward-refs шага 2 — гипотезы. Проверь, дополни, перепиши. Шаг 3 = источник истины Msg ↔ Effect.

См. полные правила в `docs/features/FORGEFLOW_contract_design.md` § 3 `contract_io` и § «Формат проектной спеки → Раздел 6. IO».

## Проверка input

1. **`contract_ui_msg.output` содержит header «черновик»** — `> ⚠ Этот артефакт — черновик`. Без него непонятно что Msg-список черновой — **error**: «contract_ui_msg.md без header-маркера, обработка прервана».
2. **`contract_ui_msg.output` содержит sealed interface Msg** в code-block. Без него нечего связывать с effects — **error**: «UI Msg структура отсутствует».
3. **`contract_state.output` содержит State структуру и `## Режим работы`**. Без них недоступна reducer-логика return Msg — **error**.
4. **`parent.scope_analysis.output` доступен** — для бизнес-сценариев и инвариантов (особенно subscribers).

## Источник

Берётся из режима (определён на шаге 1):

- **Режим 1:** UI Msg (forward-refs на effects из шага 2) + бизнес-сценарии задачи + **бизнес-инварианты** («что должно автоматически обновиться когда X меняется снаружи»)
- **Режим 2:** спека `## IO` раздел
- **Режим 3:** `EffectHandler.kt` (sealed Effect интерфейсы + onEffect + subscribe блоки) + `Reducer.kt` (reducer-логика для return Msg)

## Часть A — Effects (целостные блоки)

Описать все Effects по 3 категориям. **Каждый Effect — один целостный блок** (Source / Handler / Action / Return Msg / Reducer-логика / Edge cases). Не разрывать цепочку Effect → Msg → state change по разделам.

### Datasource Effect

`sealed interface DatasourceEffect : Effect` + варианты (LoadX, SaveX, DeleteX, …).

Per effect:
- **Source** — какие Msg порождают
- **Action** — что делает (вызов какого UseCase метода)
- **Return Msg** — какой Msg возвращает (если fire-and-forget — `Msg.Empty`)
- **Reducer-логика для Return Msg** — семантически (не имена extension-функций)
- **Edge cases** — failure / timeout / not found / concurrent calls
- **Почему** — только если выбор нетривиален. Триггеры: (a) выбран не-default паттерн (Msg.Empty для return); (b) есть >1 альтернатива (inline в reducer vs effect); (c) изменение существующего контракта.

### Navigation Effect

`sealed interface FeatureNavigationEffect : NavigationEffect` + варианты (OpenX, ExitApp для root).

Per effect:
- **Source** — какие Msg порождают
- **Navigator method** — какой метод Navigator'а вызывается
- **Почему** — только нетривиальное. Триггеры: (a) per-screen sealed вместо базового NavigationEffect (root-only действия); (b) cross-graph навигация (tab → root); (c) изменение существующего.

### UI Effect

`sealed interface UiEffect : Effect` + варианты (ShowSnackbar, …).

Per effect:
- **Source** — какие Msg порождают
- **UI Action** — конкретное UI-действие (snackbar / toast / vibration)
- **Pattern** — через State (`UiMsg.Snackbar → snackbarState`) или side-channel (Channel/SharedFlow)
- **Почему** — только нетривиальное. Триггеры: (a) выбор side-channel vs State не очевиден (borderline между long-lived и one-shot); (b) borderline между UiEffect и Datasource (например логирование).

## Часть B — Subscribers (отдельный раздел `## Subscribers`)

Subscribers выводятся **из бизнес-инвариантов**, не из UI Msg:
- «Когда пользователь сменил словарь в другом экране → текущий должен реактивно обновиться»
- «Когда настройки изменены в Settings → экран Quiz должен учесть новый режим»

Per subscriber:
- **Source** — на что подписан (UseCase.Flow / Room / DataStore / другой StateFlow)
- **Emit Msg** — какой Msg эмитит при каждом обновлении (если тот же что Return Msg от Effect — ссылка «см. Effect.X», не дублировать)
- **Operators** — `collectLatest` / `collect` / `debounce` / `filter`
- **First emit special** — если первый emit инициирует зависимую загрузку (Ready/Updated паттерн)
- **Lifecycle** — стандартный (subscribe до dispose, работа Mate)
- **Reducer-логика для emit Msg** — inline (если не уже описана в Effect)
- **Почему** — только нетривиальное. Триггеры: (a) обоснование бизнес-инварианта; (b) выбор `collectLatest` vs `collect`; (c) разделение Ready/Updated; (d) изменение существующего.

## Обязательный раздел `## Проверка реактивности`

В output обязательно:

```markdown
## Проверка реактивности

1. Может ли state экрана устареть, если другой экран изменит данные в БД? — Y/N + обоснование (одна строка)
2. Есть ли настройки/пользовательские preferences, изменение которых должно отразиться? — Y/N + обоснование
3. Активный экран должен реагировать на push-нотификации / system events / lifecycle? — Y/N + обоснование
4. Есть ли DAO-flow / DataStore-flow / другие реактивные источники эмитящие изменения данных этого экрана? — Y/N + обоснование
```

**Каждый ответ N обязательно с обоснованием** (одна строка — почему именно N). Штамповка `N×4` без обоснований — недопустима. Без раздела или с ответами без обоснования шаг не считается завершённым.

**Хотя бы один Y → обязателен соответствующий subscriber.** Если Y, но subscriber отсутствует — рассогласование, шаг возвращается на доработку.

## Обязательный раздел `## Бизнес-инварианты`

Список инвариантов которые обосновали наличие subscribers (для ревью «не упустили ли»).

## Edge cases (опционально, в конце)

Cross-effect сценарии (race на двойной тап, concurrent loads и т.п.). Если уникальные для одного effect — описаны в его блоке.

## В режиме 2 — обязательная сверка spec ↔ code

- Grep `sealed interface .*Effect\|^\s*data (object|class) .* : .*Effect` по `EffectHandler.kt` → выписать все Effects
- Grep `flow.*\(\|\.flow.*\|collectLatest\|\.collect\s*{` по `EffectHandler.kt` → выписать подписки Subscribers
- Сравнить со спекой `## IO`. Расхождения → `## Расхождения spec ↔ code` (обязателен даже если их нет).
- При расхождении в технических декларациях — приоритет коду.

## Правила моделирования

1. **Effect = sum type.** Каждый Effect = одна команда наружу. Не плодить Effect-варианты для результатов — результат это return Msg.
2. **Payload Effect** — только параметры команды. Не класть state-копию в Effect.
3. **Edge cases — обязательная инвентаризация** для каждого Datasource Effect. Не видны в макете — здесь обнаруживаются.
4. **Navigation = Effect, не State.** Sealed `XxxNavigationEffect` с per-screen вариантами. `ExitApp` только в per-screen sealed root-экранов.
5. **UiEffect — выбор паттерна:**
   - Через State (текущий стандарт) — для retain через config change
   - Side-channel — для one-shot fire-and-forget (toast, vibration)
6. **Subscriber lifecycle** — стандартный (subscribe до dispose, работа Mate).
7. **Один Msg, два источника** — нормально (Effect.LoadList и Subscriber.flowList оба могут эмитить `Msg.Loaded`).

## Что НЕ делать

- Не патчить артефакт шага 2 backwards. Все детали Msg ↔ Effect — здесь.
- Не плодить «Алгебра: sum» / «Алгебра: product» для каждого Effect. Тривиальные — без «Почему».
- Не описывать UseCase методы (это шаг 4)

### Запрет: не менять shape State и формы UI Msg

`contract_io` **канонизирует** Effects, Datasource Msg, Subscribers, Edge cases. **НЕ** канонизирует shape State и формы UI Msg — они зафиксированы в `contract_state` и `contract_ui_msg`.

**Запрещено:**
- Вводить новые поля в `WordCardState` (или nested) — это shape State, работа `contract_state`.
- Менять сигнатуру UI Msg (`data object` ↔ `data class`, добавлять/убирать payload-поля) — работа `contract_ui_msg`.
- Переопределять reducer-логику UI Msg (например менять deferred-policy на immediate-nullify) — работа `contract_ui_msg`.

**Если в процессе ты обнаружил что для покрытия edge case нужно изменить shape State / форму UI Msg / reducer-логику UI Msg — это сигнал feedback в предыдущий шаг.** В output `contract_io.md` зафиксируй в отдельной секции «Требуется feedback в предыдущие шаги» с конкретным указанием:
- Какой шаг (`contract_state` / `contract_ui_msg`).
- Какое изменение.
- Почему оно не может быть решено в текущих рамках.

Conductor решит — либо принять feedback и откатить предыдущий шаг, либо обосновать почему текущий контракт остаётся как есть и tech debt уходит в backlog.

**Что можно:**
- Канонизировать Datasource Msg (формы и payload).
- Описывать reducer-логику для Datasource Msg (это новая reducer-логика, она не «переопределяет» — она дополняет UI Msg reducer).
- Описывать edge cases — concurrent / race / failure.
- Использовать **существующие** state-поля и существующие UI Msg.
