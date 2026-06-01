---
name: business_contract_review
output: business_contract_review.md
reviews: business_contract
input_criteria:
  - business_walkthrough.output существует
  - business_contract.output существует
output_criteria:
  - business_contract_review.md существует
  - содержит секцию Verdict с `verdict: approved | changes_requested`
---

Ревью артефакта `business_contract.output`. Проверить внутреннюю согласованность контракта, учёт фактов walkthrough, применение гайдов.

## Что делать

1. Прочитай `business_walkthrough.output` и `business_contract.output`.
2. Прочитай гайды, подложенные модулем `guides`, — это эталон правил моделирования.
3. Проверь по критериям:
   - **Внутренняя согласованность** — State ↔ Msg ↔ Effect ↔ UseCase. Каждый Msg обрабатывается в reducer'е (или явно через guard). Каждый Effect триггерится из конкретного Msg. Каждый UseCase-метод вызывается из конкретного Effect.
   - **Факты walkthrough учтены** — все упомянутые в walkthrough atomicity-методы / sealed-результаты / nullable-конвенции отражены в контракте корректно. Если в walkthrough есть `addLexemeWithTranslation` и контракт делает два отдельных insert'а — это `changes_requested`.
   - **Гайды применены** — пометки `[guide: ...]` в контракте ссылаются на реально применённое правило; нарушений правил из гайдов нет.
4. Сформируй вердикт.

## Принципы

- **Один блокирующий косяк = changes_requested.** Не копить findings и не пропускать.
- **Не предлагать стилистику** — только содержательные замечания (архитектура, согласованность, факты).
- **Если walkthrough упустил факт критичный для контракта** — это финдинг с предложением «контракту догрепать <что>».

## Формат `business_contract_review.md`

```markdown
# Review: business_contract

## Findings

(список — каждое: о чём, почему проблема, что предложить. Пустой список = approved)

## Verdict

verdict: approved | changes_requested
```

## Семантика вердикта

- `approved` — нет блокирующих замечаний. Runner оставляет `business_contract.status = done`, цепочка продолжается.
- `changes_requested` — есть хотя бы одно блокирующее замечание. Runner ставит `business_contract.status = feedback_required` и инициирует rerun `business_contract` (только этот шаг, downstream не трогается).

## Что НЕ делать

- Не править артефакт `business_contract` — только ревью.
- Не задавать вопросов пользователю.
- Не повторять текст контракта в ревью — только findings.
