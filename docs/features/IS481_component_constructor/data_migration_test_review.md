# Review: data_migration_test.md

## Итерация 1 (2026-06-17T22:30:00-06:00)

### Architect (3 minor)

- **F175** [minor]: idempotency claim mismatch — WHERE filter не exercised реально через rollback.
- **F176** [minor]: special chars test отсутствует (overlap F182).
- **F177** [minor]: KSP index name dependency (Open Q #6).

### QA engineer (10: 3 critical + 7 minor)

- **F178** [critical]: Case H malformed JSON может бросать SQLITE_ERROR на bundled SQLite.
- **F179** [critical]: idempotency не проверяет RENAME rollback.
- **F180** [critical]: Case D mixed dataset (long_text + text в той же таблице).
- **F181** [minor]: Case G timeline confusion.
- **F182** [minor]: special chars JSON rewrite (dup F176).
- **F183** [minor]: updated_at >= created_at invariant.
- **F184** [minor]: COUNT translation=1 invariant.
- **F185** [minor]: FK CASCADE chain regression test.
- **F186** [minor]: failAfterStep=1 RENAME — most risky DDL.
- **F187** [minor]: 13.json TDD expected exception type.

### Inquisitor verdicts

| ID | Severity | Status |
|---|---|---|
| F175 | minor | approved |
| F176 | minor | approved (merged with F182) |
| F177 | minor | approved |
| F178 | critical | approved |
| F179 | critical | approved |
| F180 | critical | approved |
| F181 | minor | approved |
| F182 | minor | rejected (dup F176) |
| F183 | minor | approved |
| F184 | minor | approved |
| F185 | minor | approved |
| F186 | minor | approved |
| F187 | minor | approved |

## Итоги итерации 1

- **Approved:** 3 critical (F178, F179, F180) + 9 minor (F175, F176, F177, F181, F183, F184, F185, F186, F187). 1 rejected (F182 dup).
- **Решение:** есть approved critical → reset streak, repeat iter 2.

## Conductor STOP requested by user

После завершения atomic review:after iter 1 (выше) conductor останавливается перед запуском iter 2 execute. Пользователь решает continuation.

---

## Итерация 2 (2026-06-21T10:30:00-06:00)

### Architect (4: 2 critical + 2 minor)

- **F188** [critical]: Built-in `translation` row отсутствует после `helper.createDatabase(12)` — Case A line 66 `v12.lookupBuiltInTypeId("translation")` вернёт NULL/0. Нужен либо explicit helper `insertBuiltInComponentType` для v12 setup, либо переключиться на `createDatabase(11) → runMigrationsAndValidate(12, M11→M12)` (M11→M12 уже seedит translation).

- **F189** [critical]: M3 idempotency phase 1 setup аналогично декларирует "1 built-in translation" без explicit insert — та же проблема (зеркало F188).

- **F190** [minor]: `insertLexeme` helper (lines 384-388) не передаёт `word_class` колонку (nullable в v12; SQLite заполнит NULL — молча). Добавить параметр `word_class: String? = null` либо комментарий explicit.

- **F191** [minor]: Case L (line 299) «Открыть connection повторно с `PRAGMA foreign_keys=ON`» не уточняет API — `MigrationTestHelper` не возвращает connection после `runMigrationsAndValidate`. Нужен pattern `BundledSQLiteDriver().open(dbFile.absolutePath)` + `execSQL("PRAGMA foreign_keys=ON")` чтобы implementer не споткнулся.

### QA (1 minor)

- **F192** [minor]: Case K helper `insertComponentValueText` (lines 405-413) использует обычный Kotlin escape для newline в text — в SQL literal это станет real newline byte внутри JSON value column. SQLite SQL литерал допускает real newline (multi-line strings), но JSON-парсеры требуют `\n` escape — round-trip через `JSONObject(value).getString("text")` зависит от того, как Android JSONObject обрабатывает invalid embedded newline. Нужен explicit contract в helper про JSON-escape `\n/\r/\t` ДО подстановки в SQL.

### Inquisitor verdicts (conductor override)

Sub-agent inquisitor выдал rejected для всех 5 с reasoning «helpers нет в документе» — это **bogus**. Verify через Bash grep подтвердил: helpers `insertLexeme` (line 384), `lookupBuiltInTypeId` (line 403), `insertComponentValueText` (line 405), `PRAGMA foreign_keys` (lines 36, 299) все реально объявлены в текущем артефакте. Conductor override: все 5 approved.

| ID | Severity | Status |
|---|---|---|
| F188 | critical | approved (conductor override) |
| F189 | critical | approved (conductor override) |
| F190 | minor | approved (conductor override) |
| F191 | minor | approved (conductor override) |
| F192 | minor | approved (conductor override) |

## Итоги итерации 2

- **Approved:** 2 critical (F188, F189) + 3 minor (F190, F191, F192). 0 rejected.
- **Решение:** есть approved critical → reset streak, repeat iter 3.

---

## Итерация 3 (2026-06-21T11:00:00-06:00)

### Architect PASS

### QA (7: 4 critical + 3 minor)

| ID | Severity | Description |
|---|---|---|
| F193 | critical | 4-arg `runMigrationsAndValidate(name, version, true, mig)` не существует; real API = 2-arg, returns SQLiteConnection |
| F194 | critical | False claim «не возвращает connection» (line 322) — построен whole F191 fix pattern |
| F195 | critical | `getDatabasePath("test.db")` free-standing — это `Context` method |
| F196 | critical | «reopen via raw driver to seed» pattern избыточен и ломает helper lifecycle |
| F197 | minor | escape order comment misleading |
| F198 | minor | EXPLAIN substring fragile |
| F199 | minor | columnExists/scalarText/scalarLong/countWhere helpers body placeholder |

### Inquisitor: все 7 approved (verify через Read подтвердил compile blockers через сравнение с MigrationFrom11to12.kt:50,55 + MigrationFrom11to12IdempotencyTest.kt:37).

## Итоги итерации 3

- **Approved:** 4 critical (F193, F194, F195, F196) + 3 minor (F197, F198, F199). 0 rejected.
- **Решение:** есть approved critical → reset streak, repeat iter 4.

**Systemic note:** cluster F193-F196 — это очередной кейс IS481cc-F8 (sub-agent не verify через source). Iter 2 sub-agent написал bogus API signature без проверки real Room API в existing test file. Lesson: для library API утверждений обязателен Read existing usage в проекте.

---

## Итерация 4 (2026-06-21T11:30:00-06:00)

### PASS [architect + qa combined]

Verify-цепочка: real Room 2.8.4 API alignment подтверждён (createDatabase(N) 1-arg, runMigrationsAndValidate(N, listOf(M)) 2-arg returns SQLiteConnection); Case L использует helper-returned connection напрямую; M3 idempotency raw driver path использует `instrumentation.targetContext.getDatabasePath`; EXPLAIN substring drop, sqlite_master only; helper bodies inlined (columnExists, scalarLong, scalarText, countWhere).

## Итоги итерации 4

- **Approved:** 0. **Rejected:** 0. raw_findings = ∅ → review_passed = true.
- `changes_made = true` (iter 4 правил содержание) → repeat iter 5 (clean check).
