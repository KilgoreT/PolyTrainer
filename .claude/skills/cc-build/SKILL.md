---
invoke: auto
---

# cc-build — gradle wrapper for PolyTrainer

**ВСЕГДА использовать `./scripts/cc-build.sh` вместо прямого `./gradlew` в этом репо.** Любая gradle-команда: assemble, compile, test, lint, install, dependencies, clean, etc.

## Когда использовать

- Любой запуск gradle через Bash: build, тесты, lint, install, depinfo и т.д.
- Не имеет значения один таск или несколько.

## Как

```
./scripts/cc-build.sh <task1> [<task2> ...]
```

Примеры:
- `./scripts/cc-build.sh :app:assembleDebug`
- `./scripts/cc-build.sh :app:lintDebug :app:assembleDebug`
- `./scripts/cc-build.sh :modules:screen:quiz:chat:testDebugUnitTest`
- `./scripts/cc-build.sh :app:installDebug`

Output: фильтр на `e:` / `w:` / `BUILD SUCCESSFUL|FAILED` / `Tests run:` / failed tasks / `Installed on`. Полный лог в `<repo>/tmp/cc-build.out` (`tmp/` в `.gitignore`; читать через Read если нужны детали).

В конце скрипт явно печатает `===== BUILD SUCCESSFUL (EXIT=0) =====` или `===== BUILD FAILED (EXIT=N) =====`. **Верить с первого раза.** EXIT code = source of truth.

## Что НЕ делать

- **Не вызывать `./gradlew` напрямую** через Bash tool. Output теряется (gradle daemon пишет частично мимо stdout на UP-TO-DATE → 0 строк в pipe).
- **Не оборачивать в `| tail` / `| head` / `--quiet` / `--info`** — обрезает важные строки, скрипт уже сам управляет выводом.
- **Не пересобирать «на всякий случай»** если скрипт сказал `BUILD SUCCESSFUL` — это потеря времени.
- **Не использовать `--rerun-tasks`** без явной причины (кэш на UP-TO-DATE = норма, не баг).

## Корневая причина зачем skill существует

Прямой `./gradlew ... | tail -10` через Bash tool в этой среде на UP-TO-DATE задачах выдаёт **пустой** или **обрезанный** вывод. Несколько прогонов «на угадай» = потеря времени пользователя. Скрипт детерминированный: progress на stderr, фильтр на stdout, явный verdict.
