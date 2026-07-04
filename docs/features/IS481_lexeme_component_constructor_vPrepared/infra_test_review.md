# Review: infra_test

## Итерация 1 (2026-06-03T21:51:00Z)

Conductor arbitration (автономный inquisitor по запросу пользователя «фандинги сам разрешай»).

### F001 [architect] minor — test5 strict vs test3 semantic inconsistent

**Status:** approved (merge с F003).

**Verdict:** одна и та же проблема — test3 assertion strictness. Объединить с F003/F004.

### F002 [architect] minor — test3 «semantic» остаётся whitespace-sensitive

**Status:** approved (merge с F003).

**Verdict:** часть той же проблемы — test3 contains check whitespace-sensitive внутри key:value.

### F003 [qa_engineer] medium — test3 `result.contains("\"x\":1")` false-positive на `"x":11/12/100`

**Status:** approved (consolidated).

**Verdict:** реальный bug в assertion. `"x":11` содержит подстроку `"x":1` → false-positive pass. Test 5 (`json_remove`) использует strict equality в той же ситуации — несогласованность.

**Fix (tech debt):** В iter2 имплементации поменять test 3 на strict:
```kotlin
assertEquals("""[{"x":1}]""", result.replace(" ", ""))
```
`.replace(" ", "")` нейтрализует whitespace для случая bundled SQLite с пробелами в JSON output. Аналогично применить к test 5 для консистентности.

### F004 [qa_engineer] low — test3 структура неполна (пройдёт `[{...},{...}]`)

**Status:** approved (consolidated с F003).

**Verdict:** strict equality из F003 закрывает заодно — `assertEquals("""[{"x":1}]""", ...)` отвергает любую другую структуру массива.

### F005 [qa_engineer] low — F009 closure: test process ≠ app process

**Status:** approved.

**Verdict:** F009 в `02_scope.md` — runtime verify в production app, не в test process. Тест из (а) — feature smoke, не закрытие F009.

**Fix (tech debt):** В iter2 имплементации добавить в manual smoke check шаг 5 (release APK): один `Log.i(TAG, "sqlite version: ${db.query(\"SELECT sqlite_version()\").first()}")` в app boot (например в `Application.onCreate` либо в первом query через DI-полученный Database). Сверить значение в logcat с тем что показывает test 6 — должно совпасть (одинаковый bundled artifact в обоих процессах). Если не совпадают → bundled не активен в продакшене → fail.

### F006 [qa_engineer] info — step4 manual check duplicates Pre-requisites

**Status:** rejected.

**Verdict:** Pre-requisites = условие-setup перед запуском чек-листа; step 4 = верификация что после release build `isMinifyEnabled` не сбросился (потенциальный side-effect AGP / IDE auto-changes). Оба valid, не дубликат.

### F007 [qa_engineer] info — regression "3×" без baseline

**Status:** approved.

**Verdict:** «время прогона не выросло >3×» неизмеримо без baseline.

**Fix (tech debt):** В iter2 имплементации добавить шаг: «перед изменением `BaseMigration.kt` — прогнать `AllMigrationTest` с текущим legacy ctor, записать total time как baseline в commit message либо в `infra_implement.md`. После правки — assert новое время ≤ 3× baseline. Если регрессия — отдельный finding F011 (compat layer перформанс), не блокер acceptance 6.1».

---

## Решение по итерации

3 approved minor (no approved critical).

**По строгому review module:** `minor_only_streak = 1` → repeat для попытки fix → iter2.

**Conductor decision (autonomy mode):** accept с tech debt. Обоснование:
- Все 3 approved — improvements (assertion strictness clarity, F009 verify scope, regression measurability).
- Не блокируют acceptance criteria 6.1/6.2/6.3.
- Fix-инструкции зафиксированы выше — имплементатор применит при написании `BundledSqliteFeatureTest.kt` и manual smoke check'а.
- Iter2 sub-agent overhead vs ценность правки 3 minor — не оправдан в данном контексте.

`infra_test` помечен done. Tech debt 3 пунктов передан имплементатору через эту review-секцию.
