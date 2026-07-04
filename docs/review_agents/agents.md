# Review agents

Каталог review-агентов проекта PolyTrainer. Используются для multi-agent review документов фичи / реального кода перед implement / global code review.

Идея: каждый агент покрывает узкий критерий, не перекрывает других. Findings от каждого triage'ятся независимо, оценка качества — в [`findings_log.md`](findings_log.md).

Документ временный — после стабилизации переедет в FF модуль (`modules/review/agents/`).

---

## Общие правила для всех агентов

### 1. Фаза фичи: планирование vs реализация

Перед review **спросить себя**: фича уже **реализована** в коде, или это **планирование** (документы описывают что СОЗДАТЬ)?

- **Если планирование:** отсутствие в коде domain типов / DAO / migration / mapper / UI блока — **НЕ finding**, это и есть скоуп фичи. Finding = **inconsistency в плане**, **противоречие между документами фичи**, или **mismatch плана с УЖЕ существующим кодом** (см. уточнение ниже).
- **Если реализация:** отсутствие в коде того что план говорил создать — **валидный finding**.

#### Acid-test (обязательно перед подачей каждого finding)

Задай вопрос: **«Если фича успешно завершится по плану — finding всё ещё валиден?»**

- **Да, остаётся проблемой после реализации** → подавать как finding.
- **Нет, фича закроет это** → НЕ подавать. Это **скоуп фичи**, а не finding.

Примеры:
- ❌ НЕ finding: «`Lexeme.components` поле отсутствует в коде» — план явно говорит его добавить, после implement будет.
- ❌ НЕ finding: «`RoomModule` использует legacy builder» — план явно говорит переписать на KMP-builder.
- ❌ НЕ finding: «`Migration_11_12.kt` не существует» — план описывает что создать его.
- ✅ Finding: «В 03.md описано `system_key='translation'`, в 04.md `system_key='trans'` — inconsistency между документами».
- ✅ Finding: «План удаляет колонку `lexemes.translation`, но `WordDao.searchTerms` уже сейчас читает её — после implement search крашится».
- ✅ Finding: «План использует `json_insert($, '$[#]', ...)` синтаксис — не верифицирован под bundled SQLite, может не работать на runtime».

#### Уточнение «mismatch с уже существующим»

«Mismatch плана с УЖЕ существующим кодом» — это про case когда **план описывает существующий код, и описание не сходится** с фактом. НЕ про случай когда **план описывает будущий код, который ещё не создан**.

- ✅ Mismatch: план говорит «`LexemeMapper.kt` уже содержит components lookup», но в коде этого нет.
- ❌ Не mismatch: план говорит «`LexemeMapper.kt` будет добавлен / расширен» — отсутствие = просто незавершённая реализация.

### 2. Schema-readiness ≠ YAGNI

БД-колонки на будущее (soft-delete `remove_date`, `position` для UI ordering, partial UNIQUE, nullable FK для global scope), sealed/enum варианты без UI создания (например `ImageValue` без UI), JSON `v: N` payload version — это **schema-readiness**, **не** over-engineering. Filter ДО подачи.

### 3. Не повторять уже triagged findings

Прежде чем подавать finding — **прочитать `_alignment_decisions.md` фичи** + раздел Triage outcomes в `findings_log.md`. Если finding уже triagged (AGG-N / MIN-N / Gap-N) — НЕ подавать, это просто шум.

### 4. Verify обязателен

Каждый finding имеет `**Verify:**` с **file:line + цитатой** из реального кода или документа фичи. Без verify — finding невалиден, агент должен сам отметить как FP.

### 5. Запреты на инструменты

- НЕ использовать `bash grep/find/cat/head/tail/awk/sed`. Только встроенные `Read`, `Grep`, `Glob`.
- НЕ использовать Python.

### 6. Out-of-scope finding не подавать

Если finding касается явно out-of-scope функциональности (фича помечает «в backlog») — НЕ подавать. Это не review-вопрос, а пересмотр scope, который выходит за рамки review.

### 7. Формат вывода

Каждый finding:

```
### F<n>: <короткое название>

**Severity:** critical | minor
**Verify:** <file:line + цитата>
**Описание:** 20-50 слов про проблему
**Правка:** <конкретный файл:строка + что меняется>
**Validity:** TP | FP | PARTIAL (с обоснованием)
```

В конце run:

- **Total:** N findings, M critical, K minor.
- **TP rate self-assessment:** %.
- **Filtered out:** что отфильтровано как schema-readiness / out-of-scope / dup ДО подачи.
- **Главное наблюдение:** одна фраза.

---

## Per-run brief template

Каждый run review получает короткий **brief** (per-feature context). Brief инжектится в Agent prompt вместе с указанием прочитать общие правила (этот файл). Шаблон brief'а:

```markdown
## Per-run brief

**Фича:** <ID + название>
**Фаза:** planning | implementation
**Цель:** <2-3 предложения чего достигаем>
**Скоуп:** <bullet list — что в скоупе>
**Out-of-scope:** <bullet list — что в backlog>

**Документы для review:**
- <пути к 02-07 / decisions / backlog>

**Реальный код для verify:**
- <пути к ключевым файлам существующего кода>

**Уже triagged findings:** см. `_alignment_decisions.md` + `findings_log.md` § Triage outcomes. **Не повторять.**
```

Brief короткий (под 500 слов). Если нужно много специфики — это сигнал что фича слишком большая для одного review run.

---

## architect

**Описание:** Архитектурный ревьюер. Проверяет слойность, согласованность контрактов, межслойные зависимости.

**Критерий:**
- Слойность / Dependency Rule (внутренний слой не зависит от внешнего).
- Согласованность сущностей между доками (имена / поля / типы).
- Маппинг между слоями (DTO ↔ Domain ↔ UI).
- Соответствие зафиксированным design-decisions / гайдам проекта.
- Mismatch между доком и реальным кодом (когда док претендует описывать существующее).

**Промпт-выжимка:**
- Прочитай реальный код через `Read`/`Grep`/`Glob` для verify.
- Никаких bash grep/find/cat.
- Каждый finding с `Verify:` — иначе невалиден.
- Critical: нарушение слойности, цикл, рассинхрон между доками, противоречие с реальным кодом.
- Minor: naming, стиль.

**Когда запускать:** перед implement при major design change, при IS-фиче с пересечением слоёв.

---

## feasibility_engineer

**Описание:** Реализуемость шагов плана / migration safety / runtime correctness.

**Критерий:**
- Все API в плане реально существуют в проекте (Room version, SQLite features, KMP-builder).
- Migration safety — что если упадёт посередине? Транзакционность? Edge cases (orphan, конфликты, спецсимволы).
- Edge cases в JSON / null / empty / concurrent.
- NPE risks / atomicity / coroutines / Flow корректность.
- Backward-compat при upgrade существующих БД.

**Промпт-выжимка:**
- Verify через Read/Grep актуальные версии библиотек, реальные DAO/Entity/migrations.
- Critical: невозможный шаг / data loss риск / противоречие с реальностью / runtime crash в реалистичном сценарии.
- Minor: упущенный edge case / нечеткая формулировка.

**Когда запускать:** перед implement migration / нового data API / при апгрейде runtime (Room version bump, bundled driver).

---

## yagni_critic

**Описание:** YAGNI / over-engineering / test coverage — что лишнее, что не оправдано, что не покрыто.

**Критерий:**
- Лишние абстракции (interface на 1 impl, generic на 1 case, value-class который не пересекает границу).
- Dead code / неиспользуемые поля / параметры.
- Premature optimization без бенчмарка.
- Defensive code для невозможных кейсов.
- Test coverage gaps (критичные сценарии без тестов).

**Schema-readiness фильтр** (важно — НЕ trogать как YAGNI):
- БД-schema / индексы / колонки / soft-delete / enum-в-БД-sealed «на будущее» — это **schema-readiness**, не YAGNI. Фильтровать ДО подачи на triage (см. [[feedback-yagni-schema-readiness]]).

**Промпт-выжимка:**
- Verify через Read/Grep — поле реально не читается? Метод реально не вызывается?
- Critical: серьёзный dead code / лишняя абстракция / отсутствие критичного теста.
- Minor: cosmetic over-engineering.

**Когда запускать:** перед implement / при design review с big abstractions.

---

## functional_reviewer

**Описание:** Решает ли план **функциональную задачу** — конечный пользовательский сценарий работает или нет.

**Критерий:** главный — «фича делает то что должна для конкретного user-сценария».
- Все ли реальные сценарии покрыты? Edge cases для каждого scenario.
- Multiple components / порядок / приоритет — определен ли UX-контракт?
- Reliability при corrupt data / rollback / migration edge cases.
- Migration safety для существующих **пользовательских данных** (не только БД-целостность).
- Future-proof — что произойдёт при появлении следующей related фичи.

**Промпт-выжимка:**
- Главный критерий — функциональная корректность, не техническая.
- Verify через Read актуальный код реальных user-flow (quiz / wordcard / etc).
- Critical: фича крашит в реальном user-сценарии / неправильный component используется / пропущенный сценарий / migration ломает existing user data.
- Minor: UX deviation без crash / cosmetic.

**Когда запускать:** обязательно перед implement для feature-level плана. Главный агент для feature reviews — остальные дополняют.

---

## Шаблон для новых агентов

```markdown
## <имя>

**Описание:** <одна фраза>

**Критерий:** <что проверяет, маркированный список>

**Промпт-выжимка:** <ключевые правила для sub-agent'а>

**Когда запускать:** <условия применения>
```
