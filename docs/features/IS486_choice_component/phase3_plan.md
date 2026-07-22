# IS486 — Фаза 3: конструктор (ТДД)

Скоуп — [plan.md](plan.md) фаза 3 + отложенная из фазы 2 деградация лексемы в черновик.

## Работы

1. **Data/UseCase контракты** (без UI-решений):
   - `setEnabled(typeId, enabled): EnableOutcome` — рубильник с §7.8 (`LastEnabledCore`);
   - `LastEnabledCore` в delete-пути (softDelete последнего включённого ядра — отказ);
   - Перепривязка: `editComponentType` + цель (ацикличность §8 → `CycleDetected`; живой — сброс значений, degraded — свободно); `MultiForbiddenForChoice` на edit;
   - CRUD опций (К1–К5): add/rename/delete c комбинированным impact;
   - Расширенный `DeletionImpact` (degradedComponents, descendantValueCount) + удаление опций через каскад-модуль (`OptionRemoved`);
   - Создание CHOICE с опциями (валидация: домен разрешает пусто, UI требует ≥1);
   - Builtin-строки в per-dict снапшоте (снятие фильтра `system_key IS NULL` — фаза-2-времянка).
2. **Деградация лексемы в черновик** (из фазы 2): `remaining == 0` → лексема живёт; представление — решение UI (вопрос).
3. **UI конструктора**: builtin-ряды с рубильником; degraded/disabled-метки; пикер цели в Create/Edit; CRUD опций в диалоге CHOICE; снятие дизейбла CHOICE из `DISABLED_TEMPLATES`.
4. Тесты: домен/данные (androidTest + unit), reducer'ы обоих экранов, регрессия, сборка+линт.

## UI-развилки (решаются до имплементации)

- В1: вид пикера цели в Create/Edit-диалоге.
- В2: вид CRUD опций при создании/редактировании CHOICE.
- В3: вид builtin-рядов и degraded/disabled-меток в списке.
- В4: представление лексемы-черновика после деградации (отложено из фазы 2).
