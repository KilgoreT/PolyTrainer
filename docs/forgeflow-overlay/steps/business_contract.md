---
name: business_contract
output: business_contract.md
input_criteria:
  - 02_scope.md существует
  - business_walkthrough.md существует
output_criteria:
  - business_contract.md существует
  - business_contract.md содержит секцию State
  - business_contract.md содержит секцию Msg
  - business_contract.md содержит секцию Effect/IO
  - business_contract.md содержит секцию UseCase
  - business_contract.md содержит секцию Соответствие scope.aspects
---

Шаг Spec business sub-flow. Описать **весь контракт** фичи (State + Msg + Effect/IO + UseCase) в одном артефакте — на основе фактов из `business_walkthrough` и гайдов моделирования через модуль `guides`.

## Что делать

1. Прочитай `business_walkthrough.output` — это фактический фундамент. Все решения контракта обязаны учитывать факты walkthrough (atomicity-методы, sealed-результаты, конвенции data-API и т.п.).
2. Прочитай `scope_analysis.output` целиком, **особенно секцию `## Аспекты`** — каждый aspect должен быть либо отражён в контракте, либо явно зафиксирован как «не покрываем». См. § 6 ниже.
3. Прочитай гайды, подложенные модулем `guides`. При работе над разделами State / Msg / IO / UseCase применяй правила соответствующих гайдов.
4. Пройди по всем `[guide: ...]` пометкам в input — открой упомянутые гайды и применяй правила.
5. Опиши контракт в одном файле — четыре раздела в фиксированном порядке: **State → Msg → Effect/IO → UseCase**. Формат каждого раздела выбирай сам — главное чтобы разделы были связные и взаимно согласованные.
6. **Заверши секцией `## Соответствие scope.aspects`** — обязательной (см. § Соответствие scope.aspects ниже). Финализация шага запрещена без неё.
7. На каждое архитектурно нетривиальное решение — короткое обоснование и (если применимо) пометка `[guide: <name>.md → раздел]` со ссылкой на гайд который привёл к этому решению.

## Принципы

- **Контракт связный.** State диктует Msg, Msg диктует Effect, Effect диктует UseCase. Решения в одном разделе влияют на остальные — пересмотри если меняется один.
- **Факты walkthrough — не нарушай.** Если walkthrough зафиксировал atomicity-метод X — UseCase обязан его использовать (а не два отдельных insert'а).
- **Применяй гайды.** Не изобретай паттерны — правила моделирования уже описаны в гайдах.
- **Если факта не хватает — догрепай.** Walkthrough мог упустить деталь; не моделируй наугад.

## Формат `business_contract.md`

```markdown
# Business contract: <feature>

## State

<data class + nested + computed properties + инварианты; формат свободный>

## Msg

<sealed interface Msg + per Msg описание реакции reducer'а>

## Effect/IO

<Effect-сущности + Subscribers + связь Msg → Effect>

## UseCase

<interface UseCase + методы + сигнатуры>

## Соответствие scope.aspects

<обязательная таблица — см. раздел ниже>
```

## Соответствие scope.aspects (обязательный раздел)

**Цель:** защита от process gap «aspect молча выпал между scope и contract» (закрывает FlowBacklog finding IS481cc-F6).

`scope_analysis.output` содержит секцию `## Аспекты` — список архитектурных швов / инвариантов / открытых вопросов, которые фича обязана учесть. До настоящего шага aspects могли потеряться: business_contract писатель видел scope, но не делал явный cross-check «каждый aspect → отражён где-то в контракте либо явно отложен».

**Правило:** в финальной секции `## Соответствие scope.aspects` пройди по **каждому** aspect из `scope_analysis.output § Аспекты` и классифицируй один из трёх статусов:

- **`покрыт`** — aspect отражён в контракте конкретным элементом. Указать **где именно** (Msg-вариант / UseCase-метод / State-поле / Effect / sealed-outcome). Например: «`cardinality_safety` — покрыт: `UseCase.createComponent` использует DAO-метод `insertSingleSafe @Transaction`; reducer ветка `Msg.CreateResult(CardinalityViolation)` маппит outcome».
- **`не покрыт (out-of-scope MVP)`** — aspect признан полезным, но реализация откладывается. Обязательно указать **reason** (почему отложен) и **destination** (либо ID в `docs/Backlog.md`, либо отдельная фича/тикет). Без destination статус не валиден.
- **`не применимо`** — aspect присутствует в scope, но фактически не релевантен текущему контракту (например aspect про forward-compat unknown template не влияет на UseCase signatures). Обязательно короткое обоснование почему.

**Формат таблицы:**

```markdown
## Соответствие scope.aspects

| Aspect | Статус | Где / куда |
|---|---|---|
| db_migration | покрыт | data layer (вне business contract scope); business UseCase методы создают rows совместимые с M13 схемой |
| cardinality_safety | покрыт | `UseCase.createComponent` → DAO `insertSingleSafe`; `CreateOutcome.CardinalityViolation` ветка reducer |
| multi_to_single_downgrade | **не покрыт (out-of-scope MVP)** | reason: edit-flow для существующих компонентов требует отдельного UseCase + UI dialog; **destination: `docs/Backlog.md → IS481 phase 2`** |
| forward_compat_unknown | не применимо | живёт на JSON parser layer (data); UseCase API не знает про unknown templates |
| ... | ... | ... |
```

**Дисциплина в конце шага:** sub-agent проверяет что в таблице **каждый** aspect из `scope_analysis.output § Аспекты` присутствует ровно один раз. Если хотя бы один aspect пропущен — таблица неполная, финализация шага запрещена. Если aspect помечен «не покрыт» без destination — финализация запрещена.

Это делает gap явным **до** перехода на `business_design_tree` / `business_test`, а не на test-фазе когда design-шаги уже закрыты.

## Что НЕ делать

- Не дублировать промежуточные обоснования — только финальное решение.
- Не описывать удаляемые поля как отдельный раздел: контракт = срез финального состояния.
- Не писать код-реализацию — только сигнатуры и контракт.
- Не задавать вопросов пользователю.

## Feedback iteration (при rerun)

Если `feedback_iteration > 0` — это rerun после `changes_requested` от reviewer-шага. В `plan.dir` будет файл `business_contract_review.md` (или `_iter<N>.md` версии). Прочитай findings оттуда **обязательно** перед работой и устрани каждое замечание.
