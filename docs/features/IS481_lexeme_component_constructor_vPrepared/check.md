# Check — IS481 vPrepared

Выполнен после `infra_implement` (5/6 узлов DAG имплементированы).

## Команды (из `forgeflow.yml`)

| Check | Команда | EXIT | Лог |
|---|---|---|---|
| lint | `./gradlew :app:lintDebug --console=plain` | 0 | `/tmp/ff_lint.log` |
| test | `./gradlew testDebugUnitTest --console=plain` | 0 | `/tmp/ff_test.log` |
| build | `./gradlew assembleDebug --console=plain` | 0 | `/tmp/ff_build.log` |

## Результат

Все 3 проверки прошли с EXIT:0. Изменения в `RoomModule.kt` (KMP builder + `setDriver(BundledSQLiteDriver())` + `Dispatchers.IO`), `core-db-impl/build.gradle.kts` (sqlite-bundled deps), `deps/datastore.versions.toml` (version pin 2.6.2), `app/proguard-rules.pro` (keep-rules), новый `BundledSqliteFeatureTest.kt` — не сломали unit test слой и собираются успешно.

## Не проверено (требует device/emulator)

- `BundledSqliteFeatureTest` (acceptance 6.2) — `connectedDebugAndroidTest` пропущен (нет device/emulator). Отложено на manual прогон / CI с emulator.
- `AllMigrationTest` под bundled driver (acceptance 6.1) — **эскалирован** в IS481 main (D1 из `infra_implement.md`, finding IS481-F5 в FlowBacklog).
- Manual smoke check 6.3 — release APK + emulator API 23+34. Отложено на manual прогон.

## Tech debt применённый

- F003 — strict equality в test 3/5 ✓.
- F005 — runtime `sqlite_version` log в `RoomModule.logBundledSqliteVersion` ✓.
- F007 — N/A (D1: BaseMigration не менялся).
