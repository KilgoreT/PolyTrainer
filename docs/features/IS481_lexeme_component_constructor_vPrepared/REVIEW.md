# Code review: IS481 vPrepared (Bundled SQLite driver prereq)

3 параллельных reviewer: Architecture, Bugs, YAGNI. Conductor triage inline.

## Architecture

### [critical] A1. `android.util.Log` обход проектного `LexemeLogger`
**Где:** `RoomModule.kt:4, 83, 88`
**Что не так:** В data-DI используется напрямую `android.util.Log` вместо инжектированного `LexemeLogger`, нарушая абстракцию data-слоя (см. `CoreDbApiImpl.kt:35` — паттерн уже применён).
**Verify:** Read `RoomComponent.kt:22` → `@BindsInstance logger: LexemeLogger` (уже в графе).
**Triage:** → закрыть в фиче (auto-closed после A2 fix — удаляем `logBundledSqliteVersion` целиком, Log более не нужен).

### [critical] A2. Orphan `CoroutineScope(Dispatchers.IO)` в `@Provides` методе
**Где:** `RoomModule.kt:78`
**Что не так:** `CoroutineScope(Dispatchers.IO).launch { ... }` создаёт scope без `SupervisorJob`, без application-lifecycle ownership, в `@Provides` методе (side-effect внутри DI provider — анти-паттерн). DUP с Bugs B1 + YAGNI Y1.
**Verify:** Read `RoomModule.kt:78` + Grep `applicationScope|SupervisorJob` в `core-db-impl/src/main/` → no results.
**Triage:** → закрыть в фиче. Решение: удалить `logBundledSqliteVersion` целиком. F009 закрывается через `BundledSqliteFeatureTest.sqliteVersion_isAtLeast3_45` + manual smoke 6.3 (release APK upcrash при первом query если bundled не активен).

### [minor] A3. Side-effect в `@Provides` ломает single responsibility
**Где:** `RoomModule.kt:62`
**Triage:** → rejected (DUP A2, auto-closed).

### [minor] A4. Дубликат `DATABASE_NAME` prod vs androidTest
**Где:** `RoomModule.kt:95` (`"name"`) vs `Schema.kt:16` (`"TestDatabaseName"`). TODO с 2021.
**Triage:** → backlog (5-летний tech debt про имя БД, не относится к scope prereq).

### [minor] A5. `useReaderConnection` overkill для read-only sqlite_version
**Triage:** → rejected (auto-closed после A2 fix).

### [minor] A6. `androidTestImplementation(sqliteBundled)` потенциально дубликат `implementation`
**Где:** `core-db-impl/build.gradle.kts:57`
**Что не так:** В современных AGP `implementation` dependencies автоматически видны в androidTest source set.
**Triage:** → закрыть в фиче. Verify через `./gradlew :core:core-db-impl:compileDebugAndroidTestKotlin` без строки 57.

## Bugs

### [critical] B1. Orphan CoroutineScope (DUP A2/Y1)
**Triage:** → rejected (DUP A2, auto-closed).

### [minor] B2. Double `Dispatchers.IO` (setQueryCoroutineContext + launch dispatcher)
**Triage:** → rejected (auto-closed после A2 fix).

### [minor] B3. `tearDown` без `::conn.isInitialized` check — маскирует ошибку setUp
**Где:** `BundledSqliteFeatureTest.kt:29-37`
**Что не так:** Если `BundledSQLiteDriver().open(":memory:")` упадёт (например `UnsatisfiedLinkError`), `conn.close()` в `@After` бросит `UninitializedPropertyAccessException`, маскируя реальную причину.
**Triage:** → закрыть в фиче. Defensive fix: `if (::conn.isInitialized) conn.close()`.

### [minor] B4. `.use {}` coverage на prepare() — verification (всё OK)
**Triage:** → rejected (verification confirmation, не finding).

### [minor] B5. Race init-order между setDriver и logBundledSqliteVersion
**Triage:** → rejected (auto-closed после A2 fix).

### [minor] B6. ProGuard `-keep class androidx.sqlite.** { native <methods>; }` узкое
**Что не так:** Канонический keep для native methods — `-keepclasseswithmembernames class * { native <methods>; }`. Текущее правило keep'ит native только внутри уже-сохранённых классов в `androidx.sqlite.**`. Если в `androidx.sqlite.db.**` появятся native — под угрозой.
**Triage:** → backlog (improvement; рекомендация Android docs, не блокер acceptance).

## YAGNI

### [critical] Y1. `logBundledSqliteVersion` fire-and-forget в production
**Где:** `RoomModule.kt:62, 77-91`
**Что не так:** 14 строк production-кода + 5 импортов + orphan scope ради одной строки `Log.i` без `BuildConfig.DEBUG` gate. Задача F009 уже решена `BundledSqliteFeatureTest.sqliteVersion_isAtLeast3_45` (тот же bundled artifact в test process = в app process).
**Triage:** → закрыть в фиче. **Главное действие — удалить `logBundledSqliteVersion` целиком.** Закрывает Y1, A1, A2, A3, A5, B1, B2, B5 одновременно.

### [minor] Y2. `Log.i` в `sqliteVersion_isAtLeast3_45` дублирует assertion message
**Где:** `BundledSqliteFeatureTest.kt:111` + TAG + `import android.util.Log`.
**Triage:** → закрыть в фиче. Удалить Log.i + TAG + import.

### [minor] Y3. Manual smoke 6.3 тяжеловат для prereq
**Triage:** → rejected (procedural, не код).

### [minor] Y4. `.replace(" ", "")` в test3/test5 асимметрия с test2 (strict)
**Где:** `BundledSqliteFeatureTest.kt:78, 100`
**Что не так:** Test 2 (`jsonObject_isSupported`) strict без replace; tests 3/5 (`jsonInsertAppend`/`jsonRemove`) с `.replace(" ", "")`. Если SQLite внезапно начнёт форматировать с пробелами — test 2 сломается consistently, а 3/5 пропустят регрессию.
**Triage:** → закрыть в фиче. Убрать `.replace` из tests 3 и 5.

## Triage Summary

| Категория | Действие | Findings |
|---|---|---|
| → закрыть в фиче | apply fixes сейчас | A1 (auto), A2, A6, B3, Y1, Y2, Y4 |
| → backlog | сохранить для будущего | A4 (DATABASE_NAME), B6 (ProGuard general keep) |
| → rejected | дубликат или невалид | A3, A5, B1, B2, B4, B5, Y3 |

**Главное действие:** удалить `logBundledSqliteVersion` целиком из `RoomModule.kt` → закрывает 8 findings одним shot'ом.

## Запись в Backlog.md

- **[ProGuard general keep для native methods].** Заменить `-keep class androidx.sqlite.** { native <methods>; }` на канонический `-keepclasseswithmembernames class * { native <methods>; }` чтобы покрыть native methods вне already-kept классов. Стандартная рекомендация Android docs.
- **[DATABASE_NAME prod vs test sync, expose как internal const].** TODO с 2021 на `RoomModule.kt:39`. Вынести `DATABASE_NAME = "name"` в `Database` companion object как `internal`, тогда androidTest сможет ссылаться вместо хардкода `"TestDatabaseName"`.
