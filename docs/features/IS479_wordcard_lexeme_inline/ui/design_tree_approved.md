# design_tree — approved findings (ит.1, для ит.2)

## F004 [minor]

**Description:** иконка DeleteLexemeButton оставлена «выбор в impl» (ic_circle_delete | ic_close), хотя ui_layout указывает `ic_trash (или эквивалент)` — лучше зафиксировать конкретный drawable на этапе design_tree.

**Verdict:** design_tree должен фиксировать конкретный drawable, не оставлять выбор в impl.

**Закрыть:** В деталях узла `DeleteLexemeButton.kt [+]` зафиксировать `R.drawable.ic_trash` явно (есть в `core/core-resources/.../drawable/ic_trash.xml`), убрать упоминание ic_circle_delete | ic_close как опций.

## F005 [minor]

**Description:** AddLexemeWidget.enabled реализуется через alpha+no-op (M3 FAB не имеет native enabled) — поведенческое отклонение от ui_layout (`enabled: Boolean` подразумевает стандартный disabled), стоит зафиксировать как drift/decision.

**Verdict:** M3 FloatingActionButton не имеет нативного enabled, alpha+no-op onClick — поведенческое отклонение, должно быть зафиксировано в design_tree.

**Закрыть:** В деталях узла `AddLexemeWidget.kt [~]` добавить явный drift: "M3 FloatingActionButton не имеет нативного param `enabled`. Реализация через `alpha(if (enabled) 1f else 0.38f)` + `onClick = if (enabled) onAddLexeme else {}` (no-op при disabled). Тривиальная семантическая эквивалентность." Также добавить ссылку на это в backlog UI (если хочешь добавить кастомный FAB-обёртку в core/ui).
