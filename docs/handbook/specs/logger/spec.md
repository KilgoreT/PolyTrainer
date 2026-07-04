# Logger

## Обзор

`LexemeLogger` — единый интерфейс логирования приложения. Поддерживает уровни severity и sink-паттерн для гибкого направления логов в различные destination.

## Уровни логирования

```kotlin
enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
```

Иерархия: DEBUG < INFO < WARNING < ERROR. Сообщение попадает в sink, если его уровень >= minLevel sink'а.

## Интерфейс

```kotlin
// modules/core/ui
interface LexemeLogger {
    fun log(level: LogLevel = LogLevel.DEBUG, tag: String = "##LEXEME##", message: String)
}
```

## Sink-паттерн

```kotlin
// modules/core/ui
interface LogSink {
    val minLevel: LogLevel
    fun write(level: LogLevel, tag: String, message: String)
}
```

`LexemeLoggerImpl` получает `List<LogSink>` через DI и итерирует при каждом вызове `log()`.

### LogcatSink

Пишет в Android Logcat:
- DEBUG → `Log.d`
- INFO → `Log.i`
- WARNING → `Log.w`
- ERROR → `Log.e`

### CrashlyticsSink

Пишет в Firebase Crashlytics:
- WARNING → `Crashlytics.log(message)` (breadcrumbs, видны в контексте краша)
- ERROR → `Crashlytics.recordException(RuntimeException("$tag: $message"))` (non-fatal)

## Конфигурация уровней

Build-type конфигурация через `BuildConfig`:

| Build type | LOG_LEVEL (logcat) | REMOTE_LOG_LEVEL (Crashlytics) |
|------------|-------------------|-------------------------------|
| debug | DEBUG | NONE |
| release | NONE | WARNING |

`NONE` = sink не регистрируется.

Переопределение: `./gradlew assembleRelease -PLOG_LEVEL=DEBUG -PREMOTE_LOG_LEVEL=ERROR`

## Расширяемость

Новый sink = новый класс `XxxSink : LogSink` + строчка в DI. Logger не меняется.

## DI

`LoggerModule` (Dagger):
- Binds: `LexemeLoggerImpl` → `LexemeLogger`
- Provides: `List<LogSink>` — собирает sink'и с учётом BuildConfig (NONE = не добавлять)

_model: claude-opus-4-6_
