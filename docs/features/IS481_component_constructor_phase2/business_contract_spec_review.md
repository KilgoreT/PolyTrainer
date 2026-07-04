
## Итерация 1 (2026-06-22T19:19:53-06:00)

### F001 [architect] critical — accepted (user-shortcut)

**Description:** Рассинхрон спека↔контракт по типу `typeId`. В contract.md `OpenEditDialog(val typeId: Long)`, `EditDialogState.typeId: Long`, `useCase.editComponent(typeId: Long)`, `DatasourceEffect.EditComponent(val typeId: Long)`. В спеке те же сигнатуры — с `ComponentTypeId`. Спека consistent с существующей codebase (base уже использует `ComponentTypeId` typed inline class) — контракт надо привести к `ComponentTypeId`.

**Action:** non-blocking для downstream — спека отражает реальный codebase convention (ComponentTypeId), implement-шаг будет использовать его. Контракт остаётся с Long как historical inaccuracy. Закрытие — implement шаг приведёт к финальному код = ComponentTypeId.

### F002 [architect] critical — accepted (user-shortcut)

**Description:** Рассинхрон имени state-класса. Контракт `ComponentsManagerState`; спека `ComponentsManagerScreenState`. Base codebase использует `ComponentsManagerScreenState` (`mate/State.kt:25`) — спека правильно отражает финал.

**Action:** non-blocking — implement-шаг использует existing `ComponentsManagerScreenState`. Контракт inaccurate, не блокирующий.

### F003 [architect] critical — accepted (user-shortcut)

**Description:** В спеке отсутствует `epochId: Long` в `CreateDialogState` и `EditDialogState`. Контракт фиксирует явно для F124/F136 correlation; base codebase тоже несёт epochId (`mate/State.kt:61, 71, 80`).

**Action:** non-blocking — implement-шаг сохранит существующий `epochId` baseline + добавит в новый EditDialogState (явно учитывая F138 invariant). Спека — drift от финала; следует добавить, но не блокирует.

### F004 [architect] minor — accepted

**Description:** Спека `snackbar: SnackbarState?`, base — `snackbarState: SnackbarState?`. Naming inconsistent с финалом.

**Action:** non-blocking — implement шаг использует `snackbarState`.

### F005 [architect] minor — accepted

**Description:** `failureLabel(cause)` упомянут в спеке без определения.

**Action:** non-blocking — minor formulation.

### F006 [architect] minor — accepted

**Description:** Спека упоминает existing Msg `DismissSnackbar/OnRetryClick/RequestBack/Empty` через комментарий «existing families», не явно в срезе финала.

**Action:** non-blocking — комментарий читается однозначно.

---

(All findings accepted as non-blocking per user-shortcut «хватит уже ревью»; trigger_step_rerun пропущен; downstream business_design_tree использует business_contract.md + business_contract_spec.md как-есть с known drift к финальному codebase.)
