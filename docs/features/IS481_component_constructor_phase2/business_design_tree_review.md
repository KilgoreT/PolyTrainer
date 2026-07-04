
## Итерация 1 (2026-06-22T19:30:21-06:00)

### F001 [architect] critical — closed inline

**Description:** Узел #11 (`DatasourceEffectHandler.kt`) инжектирует `dictionariesFlowHandler: DictionariesFlowHandler` (узел #12), но в `deps: [5, 10]` отсутствует ребро на `#12`. DAG не отражает реальный constructor edge.

**Status:** approved → closed inline (user-shortcut)

**Action:** `business_design_tree.md:93` обновлён → `deps: [5, 10, 12]` с комментарием закрытия finding.

### F002 [architect] minor — accepted

**Description:** Узлы #8 (Manager Msg) и #20 описывают "Edit family (8 cases)", но перечислено 7 (`OpenEditDialog`, `CloseEditDialog`, `EditNameChange`, `EditTemplateChange`, `EditMultiToggle`, `SubmitEdit`, `EditResult`). Также наследуется из contract.md ("8 case'ов" labeling).

**Status:** accepted (non-blocking labeling)

**Action:** counting label несоответствие — implement шаг создаст конкретные 7 Msg cases (+ CreateDictionaryToggle / DictionariesLoaded отдельно = 9 total Manager-specific). Labeling "8" — historical inaccuracy наследована из контракта; не блокирует имплементацию.

---

(F001 closed inline + F002 accepted; trigger_step_rerun пропущен per user shortcut «хватит ревью».)
