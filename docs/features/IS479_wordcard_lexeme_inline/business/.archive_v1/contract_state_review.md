## Итерация 1 (2026-05-19T02:05:00-0600)

### F001 [architect] minor
**Description:** Инвариант 4 написан как `translation?.isEdit == false || edited не пуст` — по тексту описывает противоположное; фактическая формула должна быть `isEdit == true || edited не пуст` либо переформулирована, иначе условие тривиально истинно и не несёт смысла.
**Status:** approved
**Verdict:** Инвариант 4 действительно логически тривиален в текущей формулировке (`isEdit == false || edited не пуст` истинен почти всегда и не описывает заявленный смысл) — нужно переформулировать.

### F002 [architect] minor
**Description:** Раздел «Не удаляется» упоминает extension `addLexeme` и `setLexemeList`, но эти extensions не приведены в контракте State и не зафиксировано, что они остаются — потребители ссылаются на «висящее» имя в рамках артефакта.
**Status:** rejected
**Verdict:** Раздел «Не удаляется» явно перечисляет `addLexeme` и существующие specialized extensions как сохраняемые — это и есть фиксация что они остаются; `setLexemeList` упомянут в инварианте 5 как существующий extension.

### F003 [architect] minor
**Description:** `TextValueState.isEdit` по дефолту `= true`, тогда как семантика создания (создан, но ещё не редактируется) не различима; нужно зафиксировать why-дефолт или сделать `false`.
**Status:** approved
**Verdict:** Дефолт `isEdit = true` в `TextValueState` действительно не задокументирован why, и комбинация «создаётся при нажатии chip сразу в режиме edit» требует явного обоснования в контракте.

### F004 [analyst] critical
**Description:** Инвариант 3 («в lexemeList только лексемы с id из БД») противоречит сценарию ошибки CreateLexeme в open question 3 — не описано промежуточное состояние FAB между нажатием и возвратом RefreshLexeme (loading/disable FAB, защита от двойного нажатия).
**Status:** approved
**Verdict:** State не описывает защиту от двойного нажатия FAB / промежуточного состояния пока `CreateLexeme` в полёте — это область State (нужен флаг/loading), валидный проёб контракта.

### F005 [analyst] critical
**Description:** Не описано состояние пустого lexemeList — UI-сценарий empty state после загрузки слова без лексем не покрыт ни полем, ни инвариантом.
**Status:** approved
**Verdict:** Computed `hasAnyLexeme` намеренно не введён, но описание пустого состояния как явного семантического случая в контракте State отсутствует — поле/инвариант или хотя бы фиксация что `lexemeList.isEmpty()` = empty state нужна.

### F006 [analyst] minor
**Description:** Удалены extensions, но не указаны соответствующие удаляемые Msg/сообщения reducer'а — разрыв между State-контрактом и message-контрактом.
**Status:** rejected
**Verdict:** Msg/Reducer — следующий шаг (contract_ui_msg), out-of-scope для contract_state; артефакт корректно перечислил удаляемые extensions State.

### F007 [analyst] minor
**Description:** Инвариант 4 сформулирован неоднозначно («мягкий, не блокирующий») — либо инвариант, либо политика; в текущем виде не инвариант.
**Status:** approved
**Verdict:** «Мягкий, не блокирующий инвариант» — оксюморон; либо это инвариант (жёсткое утверждение), либо политика, в текущем виде это не инвариант.

### F008 [analyst] minor
**Description:** Не описано поведение при одновременном открытом редактировании translation и definition в одной лексеме.
**Status:** rejected
**Verdict:** Out-of-scope для State — два nullable TextValueState независимы по конструкции (product type), оба могут быть в `isEdit=true` одновременно без нарушения шейпа; политика «один в edit за раз» — это reducer/UI правило.

### F009 [analyst] minor
**Description:** Инвариант про порядок lexemeList не уточняет поведение после удаления лексемы и configuration change.
**Status:** approved
**Verdict:** Инвариант 5 говорит о порядке но не уточняет инвариант после удаления (порядок сохраняется?) и configuration change — границы инварианта размыты.

### F010 [analyst] minor
**Description:** TextValueState.isEdit дефолт true, но translation/definition nullable со значением null — комбинация «поле существует и сразу в режиме edit» противоречит whitelist-логике «создаётся только при нажатии chip».
**Status:** approved
**Verdict:** Действительно комбинация «translation: null + дефолт isEdit=true при создании» создаёт семантическое противоречие с whitelist-логикой «создаётся только при нажатии chip» — артефакт сам этого не разрешает.

## Итерация 2 (2026-05-19T02:20:00-0600)

### F011 [architect] minor
**Description:** Обоснование дефолта `TextValueState.isEdit = true` («никогда не существует до момента нажатия chip») противоречит сохраняемому `Lexeme.toLexemeState()`, который создаёт `TextValueState` из БД с явным `isEdit=false` — рационал в контракте ложен.
**Status:** approved
**Verdict:** Рационал «никогда не существует до chip» прямо опровергается путём загрузки из БД через `Lexeme.toLexemeState()` с `isEdit=false`.

### F012 [architect] minor
**Description:** Инвариант 9 («никакой другой код не имеет права писать в `isCreatingLexeme`, сбрасывается только при success/failure `CreateLexeme`») противоречит политике «`isCreatingLexeme` сбрасывается также при NavigateBack» — либо инвариант шире, либо политика нарушает его.
**Status:** approved
**Verdict:** Инвариант 9 явно запрещает другому коду писать в `isCreatingLexeme`, а политика NavigateBack делает именно это — прямое противоречие внутри артефакта.

### F013 [architect] minor
**Description:** Инварианты 7 и 9 смешивают слои: invariant 7 — UI-поведение FAB, invariant 9 содержит code-discipline rule «никакой другой код не имеет права писать» — это политики/контракт reducer, не state-инварианты.
**Status:** rejected
**Verdict:** Invariant 7 явно помечен как UI-инвариант, проецируемый из State, а invariant 9 описывает допустимые переходы поля State — это легитимный state-инвариант, не code-discipline.

### F014 [analyst] minor
**Description:** Инвариант 1 `id == NOT_IN_DB ⇒ isLoading || value.isEmpty()` — дизъюнкция позволяет `isLoading == false ∧ value.isEmpty() ∧ id == NOT_IN_DB` как «легитимное» состояние, противоречит семантике инварианта; стоит ужесточить до `id == NOT_IN_DB ⇔ isLoading`.
**Status:** approved
**Verdict:** Формула `id == NOT_IN_DB ⇒ isLoading || value.isEmpty()` действительно допускает `id==NOT_IN_DB ∧ !isLoading ∧ value==""`, что противоречит заявленному смыслу.

### F015 [analyst] minor
**Description:** Политика «`isCreatingLexeme` сбрасывается при NavigateBack» висит в воздухе — если эффект `CreateLexeme` вернётся уже после ухода со страницы, инвариант 9 конфликтует с политикой; нужно явно прописать что NavigateBack отменяет эффект либо игнорирует поздний `RefreshLexeme`.
**Status:** approved
**Verdict:** Поздний `RefreshLexeme` после NavigateBack-сброса нарушит invariant 9 (двойная запись в `isCreatingLexeme`), артефакт обязан явно закрыть этот сценарий.

### F016 [analyst] minor
**Description:** Инвариант 8 покрывает только `translation`/`definition` внутри `LexemeState`, но `WordState` имеет аналогичную пару `value`/`edited` с `isEditMode` — для неё симметричный инвариант (`edited == value` при коммите) семантически не отражён.
**Status:** rejected
**Verdict:** `WordState.value`/`edited` уже покрыт invariant 2 (`isEditMode == false ⇒ edited == ""`) — отдельный «симметричный» инвариант не требуется.

---

## Итог review-петли

- **Итерация 1:** 10 findings → 7 approved (2 critical + 5 minor), 3 rejected → repeat
- **Итерация 2:** 6 findings → 4 approved (все minor), 2 rejected → **approved_critical = []** → `review_passed=true` → выход из repeat
- **4 minor findings итерации 2 — открытый technical debt** артефакта (F011, F012, F014, F015). Не блокируют шаг, но желательно адресовать при последующих правках или в `contract_io` (F012/F015 про lifecycle isCreatingLexeme).
