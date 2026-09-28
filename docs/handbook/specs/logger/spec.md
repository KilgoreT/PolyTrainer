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
// modules/core/logger
interface LexemeLogger {
    fun log(
        level: LogLevel = LogLevel.DEBUG,
        tag: String = "###LEXEME###",
        message: String,
        throwable: Throwable? = null,
    )
}
```

## Sink-паттерн

```kotlin
// modules/core/logger
interface LogSink {
    val minLevel: LogLevel
    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?)
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
- WARNING → `Crashlytics.log("$tag: $message")` (breadcrumbs, видны в контексте краша); если передано исключение — дополнительно `recordException(throwable)`.
- ERROR → `Crashlytics.log(...)` + `recordException(throwable ?: RuntimeException("$tag: $message"))` (non-fatal): переданное исключение уходит как есть, синтетическое — только при его отсутствии.

## Конфигурация уровней

Значения по умолчанию (флаг при сборке не передан) — `BuildConfig`, `app/build.gradle.kts`:

| Сборка | Пакет | LOG_LEVEL (logcat) | REMOTE_LOG_LEVEL |
|--------|-------|--------------------|------------------|
| Магазинная (release из CI, `BuildSource.CI_PROD`) | `co.lexeme.app` | NONE | WARNING |
| Локальный release (проверка R8 и т.п., `BuildSource.LOCAL`) | `co.lexeme.app` | NONE | NONE |
| Debug (локально и в CI) | `co.lexeme.app.dev` | DEBUG | NONE |

`NONE` = sink не регистрируется.

Переопределение: `./gradlew assembleRelease -PLOG_LEVEL=DEBUG -PREMOTE_LOG_LEVEL=ERROR`

## Отправка в Firebase

**Правило: отправка включена ⇔ `REMOTE_LOG_LEVEL` не `NONE`.** Одно значение управляет тремя вещами сразу:

- сбором Crashlytics (`setCrashlyticsCollectionEnabled`) — в том числе настоящих крашей, которые SDK ловит сам, мимо логгера;
- сбором Firebase Analytics (`setAnalyticsCollectionEnabled`) — автоматические события (открытия, экраны);
- регистрацией `CrashlyticsSink` (см. DI).

Автостарт обоих SDK выключен в манифесте (`firebase_crashlytics_collection_enabled`, `firebase_analytics_collection_enabled` = `false`); решение принимает `App` при старте и пишет в лог строку `remote reporting: on|off | level=…`.

Итог по значениям по умолчанию: магазинная сборка шлёт всегда, тестовые (debug, локальный release) молчат.

**Включить отправку для тестовой сборки** — намеренно, флагом при сборке:

```bash
./scripts/cc-build.sh :app:installDebug -PREMOTE_LOG_LEVEL=WARNING
```

**Куда уходят данные.** Firebase-проект один (`lexeme-app`), в нём два приложения; корзину выбирает пакет сборки, а не флаг:

- debug (`co.lexeme.app.dev`) → dev-приложение;
- любая release-сборка (`co.lexeme.app`), включая локальную, → прод-приложение. Поэтому локальный release с флагом пишет в прод-корзину — включать осознанно.

## Расширяемость

Новый sink = новый класс `XxxSink : LogSink` + строчка в DI. Logger не меняется.

## DI

`LoggerModule` (Dagger):
- Binds: `LexemeLoggerImpl` → `LexemeLogger`
- Provides: `List<LogSink>` — собирает sink'и с учётом BuildConfig (NONE = не добавлять)

Сбор Crashlytics и Analytics включает `App.onCreate` по тому же `REMOTE_LOG_LEVEL` (см. «Отправка в Firebase»).

_model: claude-opus-4-6_
