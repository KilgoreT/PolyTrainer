# IS502 | Крэши Crashlytics: nav-гонка и краш локальной release-сборки

Issue: https://github.com/KilgoreT/PolyTrainer/issues/502
Стектрейсы: `crash_A_nav_dictionary_create_stacktrace.txt`,
`crash_B_migration_strict_stacktrace.txt`; консоль —
`crashlytics_console.png`. Разбор: [analysis.md](analysis.md).

## Крэш A — навигация (прод, живой пользователь)

- **Где/когда:** co.lexeme.app **0.1.7 (10007)**, 24.09.2026; Crashlytics
  issue `26ac01fe`; незнакомый девайс — реальный юзер.
- **Что:** `IllegalStateException: State must be at least 'CREATED' to
  be moved to 'DESTROYED'` на `route=DICTIONARY_CREATE?editId={editId}`.
- **Класс:** гонка двойной навигации — второй `navigate` до того, как
  entry первого прогрелся из INITIALIZED.
- **Фикс (сделан):** идемпотентный гейт + узкий `catch
  IllegalStateException` в `AppNavigationExecutor` — на все экраны.

## Крэш B — миграция на локальной release-сборке (не баг прода)

- **Где/когда:** версия `Debug (1)` = локальная release-сборка без
  версии из CI (проверка R8 25.09) под прод-пакетом `co.lexeme.app`;
  Crashlytics issue `3f48f5cd`.
- **Что:** `Migration didn't properly handle: component_types` на
  `CheckInit` → `MateFailPolicy.Strict` → краш.
- **Причина:** база на девайсе осталась от непрод-сборки с
  промежуточной схемой при версии 11. У юзеров из магазина так быть не
  может: каждый релиз поднимает версию ровно одной миграцией (0.1.4/0.1.5
  — v11 без `component_types`, 0.1.6/0.1.7 — v13; проверено по тегам).
- **Решение:** миграции не трогаем. Системная причина ложной тревоги —
  локальная release-сборка слала отчёты в прод-корзину Firebase. Фикс
  (сделан): единый выключатель отправки по `REMOTE_LOG_LEVEL`.

## Итоговый объём

1. **Ф1. Nav-гейт** в `AppNavigationExecutor` (крэш A). Смоук двойных
   тапов пройден: дубль `Push(WordCard)` отброшен `push skipped`.
2. **Выключатель отправки в Firebase** (крэш B):
   - отправка ⇔ `REMOTE_LOG_LEVEL` не `NONE` — сбор Crashlytics
     (включая фатальные краши), Firebase Analytics и `CrashlyticsSink`;
   - по умолчанию: магазинная сборка — `WARNING`, локальный release и
     debug — `NONE`; включить для тестовой — `-PREMOTE_LOG_LEVEL=WARNING`;
   - автостарт обоих SDK выключен в манифесте;
   - спека `logger` и гайд `logging.md` обновлены.

## Не входит

- Release-политика `MateFailPolicy` (лог + non-fatal вместо краша) —
  остаётся в Backlog: общая защита прода, к этим крашам не привязана.
- Ретро-фикс 0.1.7 — фикс Ф1 доедет со следующим релизом.
- Проблема `per_dict_components` (restoreState) — отдельная запись
  Backlog.
