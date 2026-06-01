---
name: business_contract
output: business_contract.md
input_criteria:
  - scope_analysis.output существует
  - business_walkthrough.output существует
output_criteria:
  - business_contract.md существует
  - содержит раздел State
  - содержит раздел Msg
  - содержит раздел Effect/IO
  - содержит раздел UseCase
---

Шаг Spec business sub-flow. Описать **весь контракт** фичи (State + Msg + Effect/IO + UseCase) в одном артефакте — на основе фактов из `business_walkthrough` и гайдов моделирования через модуль `guides`.

## Что делать

1. Прочитай `business_walkthrough.output` — это фактический фундамент. Все решения контракта обязаны учитывать факты walkthrough (atomicity-методы, sealed-результаты, конвенции data-API и т.п.).
2. Прочитай гайды, подложенные модулем `guides`. При работе над разделами State / Msg / IO / UseCase применяй правила соответствующих гайдов.
3. Пройди по всем `[guide: ...]` пометкам в input — открой упомянутые гайды и применяй правила.
4. Опиши контракт в одном файле — четыре раздела в фиксированном порядке: **State → Msg → Effect/IO → UseCase**. Формат каждого раздела выбирай сам — главное чтобы разделы были связные и взаимно согласованные.
5. На каждое архитектурно нетривиальное решение — короткое обоснование и (если применимо) пометка `[guide: <name>.md → раздел]` со ссылкой на гайд который привёл к этому решению.

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
```

## Что НЕ делать

- Не дублировать промежуточные обоснования — только финальное решение.
- Не описывать удаляемые поля как отдельный раздел: контракт = срез финального состояния.
- Не писать код-реализацию — только сигнатуры и контракт.
- Не задавать вопросов пользователю.

## Feedback iteration (при rerun)

Если `feedback_iteration > 0` — это rerun после `changes_requested` от reviewer-шага. В `plan.dir` будет файл `business_contract_review.md` (или `_iter<N>.md` версии). Прочитай findings оттуда **обязательно** перед работой и устрани каждое замечание.
