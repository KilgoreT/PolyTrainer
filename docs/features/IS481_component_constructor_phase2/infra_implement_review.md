
## Итерация 1 (2026-06-22T18:37:15-06:00)

### PASS [architect]

### F033 [senior] minor

**Description:** Log-message format inconsistency vs design tree. Design tree пропишет `"step 1 renameRemoveDate: ok"` (короткие алиасы), а реализация пишет `"M12→M13 step 1 renameComponentTypesRemoveDate: ok"` (full function name + версионный префикс). Семантически не bug — лог даже полезнее (есть префикс migration version + полный method name для grep). Стоит зафиксировать в § Нетривиальные решения.

**Status:** accepted (user-shortcut, не блокер)

### F034 [senior] minor

**Description:** Shared `me.apomazkin.logger.LogTags` коллидирует по simple name с per-module `me.apomazkin.components_manager.LogTags` и `me.apomazkin.per_dictionary_components.LogTags`. Сейчас не bug; при будущем добавлении feature-tag в screen-модулях потребуется alias-import. Альтернатива: переименовать shared в `FeatureLogTags` или `SmokeTags`. Не блокер для phase 2.

**Status:** accepted (user-shortcut, не блокер; зафиксировать в Backlog для будущего рефактора naming)

(review_passed=true; user-accepted after iter 1 — «хватит уже ревью»)
