# Task

## IS451. Refactor: LexemeLogger — add log levels

Рефакторинг логгера: добавить уровни логирования и sink-паттерн.

## Текущее состояние

- `LexemeLogger` — интерфейс в `core/ui`, один метод `log(tag, message)`
- `LexemeLoggerImpl` — реализация в `app`, всё через `Log.d`
- Прокидывается через Dagger (LoggerModule) и вручную через конструкторы
- Тег по умолчанию `##MATE##` → менять на `##LEXEME##`

## Требования

- Добавить уровни: DEBUG, INFO, WARNING, ERROR
- Иерархия: DEBUG < INFO < WARNING < ERROR — логируется всё от указанного уровня и выше
- **Sink-паттерн:** Logger не знает куда пишет — итерирует список sink'ов
  - `LogSink` — интерфейс с `minLevel` и `write(level, tag, message)`
  - `LogcatSink` — пишет в logcat (Log.d/i/w/e)
  - `CrashlyticsSink` — Firebase Crashlytics (уже подключен):
    - WARNING → `Crashlytics.log(message)` (breadcrumbs, видны в контексте краша)
    - ERROR → `Crashlytics.recordException(e)` (non-fatal, видны всегда в консоли)
    - Если error без exception — формируем: `RuntimeException("$tag: $message")`
  - Sink'и регистрируются в DI, Logger получает `List<LogSink>`
  - Расширение = новый класс + строчка в DI, Logger не меняется
  - Позже: AppMetricaSink (отдельная задача)
- Фильтрация: каждый sink имеет свой minLevel, задаётся при сборке через BuildConfig
  - Дефолты:
    - debug: LOG_LEVEL=DEBUG, REMOTE_LOG_LEVEL=NONE (локально всё, удалённо ничего)
    - release: LOG_LEVEL=NONE, REMOTE_LOG_LEVEL=WARNING (локально ничего, удалённо warning+error)
  - Переопределение: `./gradlew assembleRelease -PLOG_LEVEL=DEBUG -PREMOTE_LOG_LEVEL=ERROR`
- Обратная совместимость: существующие вызовы `log(tag, message)` продолжают работать (default level = DEBUG)
- Миграция существующих вызовов не обязательна в рамках этой задачи

## Архитектура

```
LexemeLogger.log(level, tag, message)
    └── sinks.forEach { if (level >= sink.minLevel) sink.write(...) }
            ├── LogcatSink(minLevel from BuildConfig.LOG_LEVEL)
            ├── CrashlyticsSink(minLevel from BuildConfig.REMOTE_LOG_LEVEL)
            │       ├── WARNING → Crashlytics.log(message)
            │       └── ERROR → Crashlytics.recordException(e)
            └── [будущее: AppMetricaSink, FileSink, ...]
```

## Затронутые файлы

- `modules/core/ui/.../LexemeLogger.kt` — интерфейс (добавить level параметр)
- `modules/core/ui/.../LogLevel.kt` — enum уровней (новый)
- `modules/core/ui/.../LogSink.kt` — интерфейс sink'а (новый)
- `app/.../logger/LexemeLoggerImpl.kt` — реализация (итерация по sink'ам)
- `app/.../logger/LogcatSink.kt` — logcat sink (новый)
- `app/.../logger/CrashlyticsSink.kt` — Firebase Crashlytics sink (новый)
- `app/.../di/module/LoggerModule.kt` — DI конфигурация
- `app/build.gradle.kts` — buildConfigField для LOG_LEVEL, REMOTE_LOG_LEVEL
