# Review: business_design_tree

## Итерация 1 (2026-05-30T19:13:26-0600)

### F001 [architect] critical

**Description:** Node 16 (WordCardUseCaseImpl) ошибочно включает строку `:36` в список замены `.toDomainEntity()` для `LexemeApiEntity`. Реально `:36` это `TermApiEntity.toDomainEntity()` (Term, который НЕ унифицируется в IS482). Реальных LexemeApi call-sites семь — `:49, :68, :78, :101, :111, :153, :212`. DAG говорит «8 точек» — должно быть 7.

**Status:** approved

**Verdict:** `:36` — это `TermApiEntity.toDomainEntity()`, не `LexemeApiEntity`. Если implement по DAG — Term будет ошибочно переименован.

## Итерация 2 (2026-05-30T19:15:36-0600)

### PASS [architect]

F001 закрыт: node 16 теперь говорит «7 точек» с правильным списком `:49, :68, :78, :101, :111, :153, :212`. Явное упоминание что `TermApiEntity.toDomainEntity()` (`:202`) остаётся как есть. Новых проблем нет.

**Решение:** PASS → `raw_findings=[]` → петля закрыта без inquisitor. `review_passed=true`.
