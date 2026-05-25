## Итерация 1 (2026-05-19T05:48:00-0600)

### PASS [architect]

### PASS [analyst]

Артефакт v2 принят с 5 структурными инвариантами.

---

## Итерация 2 (2026-05-19T18:00:00-0600)

Артефакт переработан: WordState → sealed (Loaded vs NotLoaded), список инвариантов расширен с 5 до 9 (включая нелокальный single-edit-mode). Re-review.

### F001 [architect] critical

**Description:** Инвариант 9 синтаксически некорректен — `? : +` парсится как `... ? 1 : (0 + ...)`. Нужна явная скобка для каждого тернарного слагаемого.

**Status:** approved

### F002 [architect] critical

**Description:** Раздел «Прочие nested без изменений» перечисляет `WordState` как «структурно остаётся» — прямое противоречие новому sealed-разделу выше.

**Status:** approved

### F003 [architect] critical

**Description:** Раздел «Что закрыто типом» ссылается на «Прежние инварианты 1, 6 и cross-field часть 7» — нумерация старой версии, в новом документе таких номеров нет. Читатель не сможет соотнести.

**Status:** approved

### F004 [architect] critical

**Description:** Per-field описание `WordState.Loaded.edited` ссылается на «инвариант 1», но не отражает что conjunct `wordState is Loaded` есть в самом инварианте — читатель может счесть инвариант обобщённым на весь `WordState`.

**Status:** approved

### F005 [architect] critical

**Description:** Инвариант 5 переформулирован односторонне (`lexemeList.isNotEmpty ⇒ Loaded`), но в тексте «Зачем» не сказано что это однонаправленный guard — `Loaded ∧ lexemeList.isEmpty` валидно.

**Status:** rejected

### F006 [architect] minor

**Description:** «3 структурных инварианта… закрываются компилятором» — счёт без референса к старым номерам. Переформулировать «корреляция трёх полей» или назвать.

**Status:** approved

### F007 [architect] minor

**Description:** `@Stable` на `sealed interface WordState` и `data object NotLoaded` избыточен. Object всегда стабилен, для sealed interface стабильность наследуется реализациями.

**Status:** approved

### F008 [architect] minor

**Description:** Per-field фраза «Дефолт `WordCardState.wordState`» оборвана — нет «= NotLoaded».

**Status:** approved

### F009 [analyst] critical

**Description:** Инвариант 1 не покрывает обратное направление — `Loaded(isEditMode=true, edited="")` валидно по форме, но семантически противоречит «буфер ввода активен». Нет инварианта связи `EnterEditMode ⇒ edited == value`.

**Status:** rejected

### F010 [analyst] critical

**Description:** Инвариант 9 + Create*-сценарий: новая `TextValueState(isEdit=true)` создаётся параллельно с возможным существующим edit-mode на другой лексеме. Текст «любой Create* обязан закрыть прочие» — reducer-rule, а не структурный snapshot-факт.

**Status:** rejected

### F011 [analyst] critical

**Description:** Пропущен инвариант `isCreatingLexeme × edit-mode`. Сценарий FAB при `wordState.isEditMode=true` — возможна комбинация `isCreatingLexeme=true ∧ wordState.isEditMode=true ∧ новая лексема с isEdit=true` — нарушение инв. 9 не как transient, а как структурная дыра.

**Status:** rejected

### F012 [analyst] critical

**Description:** Дефолт `TextValueState.isEdit = true` противоречит сценарию маппинга `Lexeme.toLexemeState()` после `RefreshLexeme`. Если маппер забудет переопределить дефолт — лексема из БД сразу в edit-mode. State-контракт не ловит.

**Status:** approved

### F013 [analyst] minor

**Description:** Инвариант 5 не различает «Loaded + isLoading=true» — комбинация валидна по форме, но семантически спорна (первичная загрузка завершена ⇒ Loaded ⇒ isLoading=false?).

**Status:** approved

### F014 [analyst] minor

**Description:** `WordState.Loaded.value: String` декларируется как «non-empty по контракту Loaded», но это утверждение **не зафиксировано инвариантом**. Sealed закрывает только наличие поля, не его непустоту.

**Status:** approved

### F015 [analyst] minor

**Description:** Пропущен сценарий «удаление лексемы во время её редактирования» — снэпшот после удаления валиден, но политика reducer'а не определена.

**Status:** rejected

### F016 [analyst] minor

**Description:** `WordState.Loaded.showWarningDialog × isEditMode` не определено — допустимо ли одновременно редактировать заголовок и показывать диалог удаления?

**Status:** rejected

---

## Итерация 3 (2026-05-19T18:30:00-0600)

### F017 [architect] minor

**Description:** Заголовок «Кандидаты-инварианты» говорит «именно эти 9 инвариантов» — должно быть «11» после добавления #10, #11.

**Status:** approved

### F018 [analyst] minor

**Description:** Инвариант 7 (`isLoading ⇒ empty`) производен из #11 + контрапозиции #5. Либо удалить, либо пометить как производный.

**Status:** approved

### F019 [analyst] minor

**Description:** Инвариант 6 содержит избыточный conjunct `!isLoading` — следует из #11 (`Loaded ⇒ !isLoading`).

**Status:** approved

---

## Итерация 4 (2026-05-19T18:45:00-0600)

### F020 [analyst] minor

**Description:** Инвариант 11 формально излишен — пересекается с инв.7 в обратную сторону, контрапозиция инв.5 уже даёт нужную часть; либо инв.7 либо инв.11 — дублирование.

**Status:** approved

### F021 [analyst] minor

**Description:** Описание `Loaded.value` использует слово «предполагается» вместо ссылки на инв.10 — ослабляет утверждение.

**Status:** approved

### F022 [analyst] minor

**Description:** Обоснование инв.6 не раскрывает контрапозицию инв.11 — читатель может не увидеть как `!isLoading` производно.

**Status:** approved

### F023 [architect] minor

**Description:** Раздел «Что закрыто типом» утверждает что `(valid id, "" value)` физически невозможна — это неверно: sealed закрывает только наличие полей, `Loaded(value="")` валидно по типу, закрывается инв.10.

**Status:** approved

### F024 [architect] minor

**Description:** Обоснование инв.9 апеллирует к истории reducer'а («Reducer случайно может оставить два isEdit=true») — формальное нарушение собственного self-check «нет упоминаний reducer-веток».

**Status:** approved

### F025 [architect] minor

**Description:** Инв.6 в обосновании ссылается на инв.11 без явной развёртки контрапозиции.

**Status:** approved

---

## Итог review-петли

- **Итерация 1:** PASS — артефакт v2 с 5 инвариантами.
- **Итерация 2:** Re-review после переделки на sealed WordState → 16 findings → 10 approved (5 critical + 5 minor), 6 rejected.
- **Итерация 3:** После применения → 3 minor approved.
- **Итерация 4:** → 6 minor approved.

**Streak only-minor: 2 итерации подряд (3, 4) → выход по новому правилу модуля review.**

9 minor approved (F017–F025) остаются как **tech debt** в этом review-файле — точечные стилистические/документационные правки, не блокируют шаг. Список можно проработать в `implement` или в отдельной правке артефакта.

`contract_state.md` v2.3 финализирован.
