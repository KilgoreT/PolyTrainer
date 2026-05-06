# Spec: IS451 — LexemeLogger: уровни логирования и sink-паттерн

## Проблема

Текущий логгер (`LexemeLogger`) имеет единственный метод `log(tag, message)`, который всегда пишет в `Log.d`. Нет:
- Уровней логирования (всё = debug)
- Фильтрации по severity
- Возможности направлять логи в разные destination (logcat, Crashlytics, AppMetrica)
- Гибкой конфигурации per-build-type

## Что остаётся без изменений

- Пакет интерфейса: `me.apomazkin.ui.logger` в `modules/core/ui`
- Пакет реализации: `me.apomazkin.polytrainer.logger` в `app`
- DI-модуль: `LoggerModule` в `app/.../di/module/`
- Точки вызова логгера по коду — мигрируются на шорткаты d/i/w/e

## Что удаляется

- Дефолтный тег `##MATE##` → заменяется на `##LEXEME##`
- Прямой вызов `Log.d` в `LexemeLoggerImpl` → заменяется на итерацию по sink'ам

## Что добавляется / меняется

### 1. LogLevel — enum уровней

Файл: `modules/core/ui/.../logger/LogLevel.kt`

```kotlin
enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
```

Иерархия: DEBUG < INFO < WARNING < ERROR. Sink логирует сообщение, если `messageLevel >= sink.minLevel`.

### 2. LogSink — интерфейс sink'а

Файл: `modules/core/ui/.../logger/LogSink.kt`

```kotlin
interface LogSink {
    val minLevel: LogLevel
    fun write(level: LogLevel, tag: String, message: String)
}
```

### 3. LexemeLogger — расширение интерфейса

Файл: `modules/core/ui/.../logger/LexemeLogger.kt`

**Было:**
```kotlin
interface LexemeLogger {
    fun log(tag: String = "##MATE##", message: String)
}
```

**Стало:**
```kotlin
interface LexemeLogger {
    fun log(level: LogLevel = LogLevel.DEBUG, tag: String = "##LEXEME##", message: String)
    
    fun d(tag: String = "##LEXEME##", message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String = "##LEXEME##", message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String = "##LEXEME##", message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String = "##LEXEME##", message: String) = log(LogLevel.ERROR, tag, message)
}
```

⚠️ НЕ backward compatible: вызовы `log("TAG", "msg")` сломаются — первый позиционный параметр теперь `LogLevel`. Миграция всех call-site обязательна.

### 4. LexemeLoggerImpl — итерация по sink'ам

Файл: `app/.../logger/LexemeLoggerImpl.kt`

**Было:** прямой `Log.d`
**Стало:** получает `List<LogSink>` через конструктор, итерирует:

```kotlin
class LexemeLoggerImpl @Inject constructor(
    private val sinks: List<@JvmSuppressWildcards LogSink>
) : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String) {
        sinks.forEach { sink ->
            if (level >= sink.minLevel) {
                sink.write(level, tag, message)
            }
        }
    }
}
```

### 5. LogcatSink

Файл: `app/.../logger/LogcatSink.kt`

Пишет в Android Logcat, маппинг уровней:
- DEBUG → `Log.d`
- INFO → `Log.i`
- WARNING → `Log.w`
- ERROR → `Log.e`

`minLevel` из `BuildConfig.LOG_LEVEL`.

### 6. CrashlyticsSink

Файл: `app/.../logger/CrashlyticsSink.kt`

Пишет в Firebase Crashlytics:
- WARNING → `FirebaseCrashlytics.getInstance().log(message)` (breadcrumbs)
- ERROR → `FirebaseCrashlytics.getInstance().recordException(RuntimeException("$tag: $message"))`

`minLevel` из `BuildConfig.REMOTE_LOG_LEVEL`.

### 7. BuildConfig — уровни фильтрации

Файл: `app/build.gradle.kts`

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "LOG_LEVEL", "\"DEBUG\"")
        buildConfigField("String", "REMOTE_LOG_LEVEL", "\"NONE\"")
    }
    release {
        buildConfigField("String", "LOG_LEVEL", "\"NONE\"")
        buildConfigField("String", "REMOTE_LOG_LEVEL", "\"WARNING\"")
    }
}
```

Переопределение через gradle properties:
```
./gradlew assembleRelease -PLOG_LEVEL=DEBUG -PREMOTE_LOG_LEVEL=ERROR
```

NONE = sink не добавляется в DI (или minLevel выше любого уровня).

### 8. LoggerModule — DI конфигурация

Файл: `app/.../di/module/LoggerModule.kt`

- Binds `LexemeLoggerImpl` → `LexemeLogger`
- Provides `List<LogSink>` (LogcatSink + CrashlyticsSink, фильтруя NONE)
- LogcatSink: `minLevel = LogLevel.valueOf(BuildConfig.LOG_LEVEL)` (если не NONE)
- CrashlyticsSink: `minLevel = LogLevel.valueOf(BuildConfig.REMOTE_LOG_LEVEL)` (если не NONE)

### 9. Тег по умолчанию

`##MATE##` → `##LEXEME##` в дефолтном параметре интерфейса.

## Файлы

| Файл | Действие |
|------|----------|
| `modules/core/ui/src/main/java/me/apomazkin/ui/logger/LogLevel.kt` | Создать |
| `modules/core/ui/src/main/java/me/apomazkin/ui/logger/LogSink.kt` | Создать |
| `modules/core/ui/src/main/java/me/apomazkin/ui/logger/LexemeLogger.kt` | Изменить |
| `app/src/main/java/me/apomazkin/polytrainer/logger/LexemeLoggerImpl.kt` | Изменить |
| `app/src/main/java/me/apomazkin/polytrainer/logger/LogcatSink.kt` | Создать |
| `app/src/main/java/me/apomazkin/polytrainer/logger/CrashlyticsSink.kt` | Создать |
| `app/src/main/java/me/apomazkin/polytrainer/di/module/LoggerModule.kt` | Изменить |
| `app/build.gradle.kts` | Изменить |

## Дополнительно

- Миграция существующих вызовов `log(tag, message)` к использованию шорткатов `d()`, `i()`, `w()`, `e()` — IN SCOPE
- Запретить прямые вызовы `android.util.Log` в коде (lint rule или convention)
- Убрать `import android.util.Log` из всех файлов кроме `LogcatSink`

## Ограничения

- AppMetricaSink — отдельная задача
- FileSink — отдельная задача

_model: claude-opus-4-6_
