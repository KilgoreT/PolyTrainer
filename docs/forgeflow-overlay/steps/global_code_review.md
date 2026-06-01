---
name: global_code_review
output: REVIEW.md
input_criteria:
  - scope_analysis.output существует
  - выполнен check (lint / test / build pass)
output_criteria:
  - REVIEW.md существует
  - содержит секции Architecture / Bugs / YAGNI
  - каждое findings имеет поле Verify со ссылкой `file:line` через встроенные Claude tools
  - содержит секцию Triage с решением по каждому finding (`→ закрыть в фиче` / `→ backlog` / `→ rejected`)
---

Финальный архитектурный обзор всей фичи перед закрытием flow. Прогоняется автоматически после `check` (lint / test / build pass). Цель — поймать проблемы которые проскользнули мимо per-step ревью: архитектурные сдвиги между слоями, реальные баги в коде, over-engineering.

## Что делать

1. Прочитай артефакты завершённых sub-flow (`<layer>_summary.output?` — суффикс `?` означает что input nullable: если sub-flow не запускался по `<layer>_touched = false`, input будет `null` — пропускай этот слой в обзоре, не упоминай).
2. Прочитай `scope_analysis.output` для понимания скоупа фичи.
3. **Запусти 3 параллельных subagent'а** через Task tool — по одному на каждое направление обзора (запускать в одном сообщении, чтобы шли параллельно).
4. Собери их findings в единый артефакт `REVIEW.md`.
5. Перейди в фазу triage (см. ниже).

## Три направления обзора

Каждый subagent работает в своей перспективе. Findings комплементарны — direction не повторяют друг друга.

### Направление 1 — Architecture

Промпт subagent'а: «Прогони архитектурный обзор всего изменённого кода фичи. Найди: нарушения слойности (UI лезет в business, business в data); неправильные паттерны (mate / TEA нарушения); размытые границы между sub-flow артефактами; рассинхрон между контрактом и реализацией; computed property пропущена / derived в composable; dependency rule нарушен».

Subagent **обязан** использовать встроенные Claude tools (`Grep` / `Glob` / `Read`) и для каждого finding указать `Verify: <tool> <args> → <output>` со ссылкой `file:line`.

### Направление 2 — Bugs

Промпт subagent'а: «Прогони bug-hunting обзор изменённого кода. Найди: реальные баги (race conditions, leaks, cancellation issues); пропущенные edge cases (null, empty, concurrent); некорректное error handling (молчаливое глотание ошибок, неправильные fallback'и); потенциальные NPE / краши; неправильное использование Coroutines / Flow».

Subagent **обязан** использовать встроенные Claude tools и `Verify: <tool> <args> → <output>` для каждого finding.

### Направление 3 — YAGNI

Промпт subagent'а: «Прогони YAGNI / over-engineering обзор. Найди: лишние абстракции (interface на один impl, generic-параметр на один use case); dead code (неиспользуемые параметры, методы, флаги); преждевременные оптимизации без бенчмарка; defensive code для невозможных кейсов; over-validation на внутренних границах; backward-compat шимы которые можно удалить».

Subagent **обязан** использовать встроенные Claude tools и `Verify: <tool> <args> → <output>` для каждого finding.

## Формат `REVIEW.md`

```markdown
# Code review: <feature> (<ticket>)

Commit: <hash> | Date: <date>

## Architecture

### [critical/minor] <название>
**Где:** <file:line>
**Что не так:** ...
**Почему важно:** ...
**Предложение:** ...
**Verify:** <tool> <args> → <output>
**Triage:** <заполнится на паузе>

## Bugs

(аналогично)

## YAGNI

(аналогично)

## Triage Summary

(заполняется conductor'ом на паузе вместе с пользователем)

- Закрыть в фиче: <N> findings
- В backlog: <N> findings
- Rejected: <N> findings
```

## Triage (на паузе)

После того как все 3 subagent'а отработали и `REVIEW.md` собран — шаг становится `pause: true`. Conductor вместе с пользователем проходит по каждому finding и проставляет в поле `Triage:` одно из решений:

- **`→ закрыть в фиче`** — finding критичный, чиним прямо сейчас (заводим коммит, или перезапускаем `<layer>_implement` шаг с feedback'ом).
- **`→ backlog`** — finding валидный, но out-of-scope текущей фичи. Conductor добавляет запись в `docs/Backlog.md` (раздел подбирается по типу — Архитектура / Tech Debt / etc.).
- **`→ rejected`** — finding невалидный (галлюцинация / стилистика / out-of-scope). Краткое обоснование в поле Triage.

В конце triage — сводка в секции `## Triage Summary`. После этого шаг закрывается как done.

## Что НЕ делать

- Не запускать ревью если `check` не прошёл (input_criteria блокирует).
- Не пропускать `Verify:` ни в одном finding — claim о коде без verification = auto-reject.
- Не решать самостоятельно на triage — это диалог с пользователем.
- Не править код по findings внутри этого шага — правки идут через перезапуск `<layer>_implement` либо отдельный коммит.
