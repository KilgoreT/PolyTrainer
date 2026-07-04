# check.md — IS481 component_constructor

## Lint

```
./scripts/cc-build.sh :app:lintDebug
===== BUILD SUCCESSFUL (EXIT=0) =====
```

## Unit tests

```
./scripts/cc-build.sh testDebugUnitTest
===== BUILD SUCCESSFUL (EXIT=0) =====
```

Все unit tests PASS (включая IS481 retrofit tests business + ui + data layers).

## Build APK

```
./scripts/cc-build.sh assembleDebug
===== BUILD SUCCESSFUL (EXIT=0) =====
```

## Итог

Все три check (lint / test / build) PASS. Apk собран, lint-warnings отсутствуют как блокеры, unit tests все green.

**Не запускалось** (out of scope этого шага):
- Instrumented tests (`connectedAndroidTest`) — требуют эмулятор.
- Migration tests (`MigrationFrom12to13` + `IdempotencyTest`) — instrumented, на user device.
- Manual smoke testing — checklist scenarios 1-8.
