# Approved findings (business_design_tree, iter 1)

## F001 [architect] critical

**Description:** Node 16 (WordCardUseCaseImpl) ошибочно включил `:36` в список замены `.toDomainEntity()`. `:36` — это `TermApiEntity.toDomainEntity()` (Term), не `LexemeApiEntity`. Реальных LexemeApi call-sites — 7: `:49, :68, :78, :101, :111, :153, :212`.

**Что нужно:** В `business_design_tree.md` node 16:
- Поправить заголовочное число «8 точек» → «7 точек».
- Список line-numbers: убрать `:36`, оставить `:49, :68, :78, :101, :111, :153, :212`.
- Явно зафиксировать: `TermApiEntity.toDomainEntity()` остаётся как есть (Term не унифицируется в IS482).

**TermApiEntity.toDomainEntity** определён в `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt:202`, не путать с `LexemeApiEntity.toDomainEntity` на `:216`.
