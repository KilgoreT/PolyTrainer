# Логирование

## ЗАПРЕТ

**ЗАПРЕЩЕНО** использовать `android.util.Log` напрямую. Единственное место — `LogcatSink`.
Всё логирование — через `LexemeLogger`. Без исключений.

**Модуль:** `modules/core/logger` (пакет `me.apomazkin.logger`). Чистый Kotlin, без Android зависимостей. Интерфейсы: `LexemeLogger`, `LogLevel`, `LogSink`.

## Архитектура

```
LexemeLogger.log(level, tag, message)
    └── sinks.forEach { if (level >= sink.minLevel) sink.write(...) }
            ├── LogcatSink  → android.util.Log.d/i/w/e
            └── CrashlyticsSink → Crashlytics.log / recordException
```

### Sink-паттерн

`LexemeLogger` не знает куда пишет. Он итерирует `List<LogSink>`, каждый sink:
- Имеет свой `minLevel` — фильтрует сообщения ниже порога
- Реализует `write(level, tag, message)` — пишет в своё направление

Добавить новый sink = создать класс + зарегистрировать в DI (`LoggerModule`). Logger не меняется.

### Существующие sink'и

| Sink | Куда | Маппинг уровней |
|------|------|-----------------|
| `LogcatSink` | Android Logcat | DEBUG→Log.d, INFO→Log.i, WARNING→Log.w, ERROR→Log.e |
| `CrashlyticsSink` | Firebase Crashlytics | WARNING→breadcrumb (`Crashlytics.log`), ERROR→non-fatal (`recordException`); переданное исключение уходит как есть |

### Конфигурация при сборке

Каждый sink имеет свой `minLevel` из `BuildConfig`. Значения по умолчанию (флаг не передан):

| Сборка | LOG_LEVEL (logcat) | REMOTE_LOG_LEVEL |
|--------|--------------------|------------------|
| Магазинная (release из CI) | NONE (отключен) | WARNING |
| Локальный release (проверка R8 и т.п.) | NONE (отключен) | NONE (отключен) |
| Debug | DEBUG | NONE (отключен) |

`NONE` — sink не создаётся.

Переопределение через gradle property:
```bash
./gradlew assembleRelease -PLOG_LEVEL=DEBUG -PREMOTE_LOG_LEVEL=ERROR
```

### Отправка в Firebase

`REMOTE_LOG_LEVEL` — единственный выключатель всей отправки: не `NONE` → включены сбор Crashlytics (включая настоящие краши), сбор Firebase Analytics и `CrashlyticsSink`; `NONE` → с устройства не уходит ничего. Магазинная сборка шлёт всегда, тестовые молчат.

Проверить отправку на тестовой сборке:
```bash
./scripts/cc-build.sh :app:installDebug -PREMOTE_LOG_LEVEL=WARNING
```
При старте в логе: `remote reporting: on | level=WARNING`.

Корзина в Firebase зависит от пакета, не от флага: debug (`co.lexeme.app.dev`) → dev-приложение, любой release (`co.lexeme.app`, включая локальный) → прод. Локальный release с флагом пишет в прод — только осознанно. Подробности — `docs/handbook/specs/logger/spec.md`, «Отправка в Firebase».

### DI

```kotlin
// LoggerModule.kt
@Module
object LoggerModule {
    @Provides
    fun provideSinks(): List<LogSink> = buildList {
        if (BuildConfig.LOG_LEVEL != "NONE") {
            add(LogcatSink(LogLevel.valueOf(BuildConfig.LOG_LEVEL)))
        }
        if (BuildConfig.REMOTE_LOG_LEVEL != "NONE") {
            add(CrashlyticsSink(LogLevel.valueOf(BuildConfig.REMOTE_LOG_LEVEL)))
        }
    }

    @Provides
    fun provideLogger(sinks: List<LogSink>): LexemeLogger = LexemeLoggerImpl(sinks)
}
```

## Уровни

| Уровень | Метод | Когда использовать | Куда уходит |
|---------|-------|--------------------|-------------|
| DEBUG | `d()` | Отладка, трассировка, временные логи | Logcat (debug build) |
| INFO | `i()` | Значимые события (навигация, загрузка данных) | Logcat (debug build) |
| WARNING | `w()` | Проблема, но приложение работает | Crashlytics breadcrumb (когда отправка включена) |
| ERROR | `e()` | Критическая ошибка, неожиданное состояние | Crashlytics non-fatal exception (когда отправка включена) |

Иерархия: DEBUG < INFO < WARNING < ERROR. Sink получает только сообщения `>= minLevel`.

## Теги

### Формат

`###СЛОВО###` — тройные решётки, БОЛЬШИЕ буквы. Слово = область/модуль, не имя класса.

### Константы

Каждый модуль имеет `LogTags.kt`:

```kotlin
package me.apomazkin.stattab

object LogTags {
    const val STAT = "###STAT###"
}
```

**Использовать ТОЛЬКО константы. НЕ хардкодить строки.**

### Таблица тегов

| Модуль | Константа | Значение |
|--------|-----------|----------|
| core/mate | `LogTags.MATE` | `###MATE###` |
| core/ui | `LogTags.UI` | `###UI###` |
| screen/wordstab | `LogTags.VOCAB` | `###VOCAB###` |
| screen/dictionary | `LogTags.DICT` | `###DICT###` |
| screen/quiztab | `LogTags.QUIZ` | `###QUIZ###` |
| screen/quiz/chat | `LogTags.CHAT` | `###CHAT###` |
| screen/stattab | `LogTags.STAT` | `###STAT###` |
| screen/settingstab | `LogTags.SETTINGS`, `LogTags.WEBVIEW` | `###SETTINGS###`, `###WEBVIEW###` |
| screen/wordcard | `LogTags.WORDCARD` | `###WORDCARD###` |
| widget/dictionaryappbar | `LogTags.APPBAR` | `###APPBAR###` |
| app | `LogTags.APP` | `###APP###` |
| core-db-impl | `LogTags.DB` | `###DB###` |

Новый модуль = создай `LogTags.kt` с константой.

## Правила вызова

```kotlin
// Правильно:
logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
logger.w(tag = LogTags.SETTINGS, message = "navigate: privacy_policy")
logger.e(tag = LogTags.APP, message = "unknown pageKey: $pageKey")

// ЗАПРЕЩЕНО:
logger.d(tag = "###MATE###", message = "...")   // хардкод строки
logger.d(message = "...")                        // без тега
android.util.Log.d("TAG", "message")            // прямой Log
if (BuildConfig.DEBUG) { logger.d(...) }         // обёртка (фильтрация в sink)
```

- `tag` обязателен — не полагайся на дефолт `###LEXEME###`
- Сообщение лаконичное с контекстом: `"action: data"` 
- `if (BuildConfig.DEBUG)` — НЕ НУЖЕН, фильтрация на уровне sink'ов
- Прокидывание logger: через конструктор (DI `@Inject` или параметр)

## Расширение

Добавить новый sink (файл, аналитика, remote):
1. Создать класс `class MySink(override val minLevel: LogLevel) : LogSink`
2. Добавить `buildConfigField` в `app/build.gradle.kts`
3. Зарегистрировать в `LoggerModule.provideSinks()`
