# Design Tree: IS451 — Logger Levels

## Graph

```yaml
- id: 0
  file: modules/core/ui/src/main/java/me/apomazkin/ui/logger/LogLevel.kt
  action: "+"
  depends: []

- id: 1
  file: modules/core/ui/src/main/java/me/apomazkin/ui/logger/LogSink.kt
  action: "+"
  depends: [0]

- id: 2
  file: modules/core/ui/src/main/java/me/apomazkin/ui/logger/LexemeLogger.kt
  action: "~"
  depends: [0]

- id: 3
  file: app/src/main/java/me/apomazkin/polytrainer/logger/LogcatSink.kt
  action: "+"
  depends: [0, 1]

- id: 4
  file: app/src/main/java/me/apomazkin/polytrainer/logger/CrashlyticsSink.kt
  action: "+"
  depends: [0, 1]

- id: 5
  file: app/src/main/java/me/apomazkin/polytrainer/logger/LexemeLoggerImpl.kt
  action: "~"
  depends: [0, 1, 2]

- id: 6
  file: app/build.gradle.kts
  action: "~"
  depends: []

- id: 7
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/LoggerModule.kt
  action: "~"
  depends: [3, 4, 5, 6]

- id: 8
  file: modules/screen/stattab/src/main/java/me/apomazkin/stattab/mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [2]

- id: 9
  file: modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [2]

- id: 10
  file: modules/screen/quiztab/src/main/java/me/apomazkin/quiztab/logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [2]

- id: 11
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [2]

- id: 12
  file: modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [2]

- id: 13
  file: modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [2]

- id: 14
  file: modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt
  action: "~"
  depends: [2]

- id: 15
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt
  action: "~"
  depends: [2]

- id: 16
  file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
  action: "~"
  depends: [2]

- id: 17
  file: core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/migrations/MigrationFrom07to08.kt
  action: "~"
  depends: []

- id: 18
  file: core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/utils/CommonExtensions.kt
  action: "~"
  depends: [2]
```

## Details

### #0 LogLevel.kt [+]

Enum levels for log severity. Implements `Comparable` naturally (enum ordinal).

```kotlin
enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
```

### #1 LogSink.kt [+]

Interface for log destinations. Depends on LogLevel.

```kotlin
interface LogSink {
    val minLevel: LogLevel
    fun write(level: LogLevel, tag: String, message: String)
}
```

### #2 LexemeLogger.kt [~]

Extend interface with level parameter and shortcut methods. 📎 guide: code-style (logging conventions).

**Was:**
```kotlin
interface LexemeLogger {
    fun log(tag: String = "##MATE##", message: String)
}
```

**Becomes:**
```kotlin
interface LexemeLogger {
    fun log(level: LogLevel = LogLevel.DEBUG, tag: String = "##LEXEME##", message: String)

    fun d(tag: String = "##LEXEME##", message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String = "##LEXEME##", message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String = "##LEXEME##", message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String = "##LEXEME##", message: String) = log(LogLevel.ERROR, tag, message)
}
```

⚠️ НЕ backward compatible: существующие вызовы `log("TAG", "msg")` сломаются — первый позиционный параметр теперь `LogLevel`. Миграция (#8-#16) обязательна для компиляции.

### #3 LogcatSink.kt [+]

Sink writing to Android Logcat. Maps LogLevel to `Log.d/i/w/e`. Only file allowed to use `android.util.Log`. 📎 guide: code-style (logging ban).

```kotlin
class LogcatSink(override val minLevel: LogLevel) : LogSink {
    override fun write(level: LogLevel, tag: String, message: String)
    // DEBUG -> Log.d, INFO -> Log.i, WARNING -> Log.w, ERROR -> Log.e
}
```

### #4 CrashlyticsSink.kt [+]

Sink writing to Firebase Crashlytics. WARNING -> breadcrumb (`Crashlytics.log`), ERROR -> non-fatal (`Crashlytics.recordException`).

```kotlin
class CrashlyticsSink(override val minLevel: LogLevel) : LogSink {
    override fun write(level: LogLevel, tag: String, message: String)
    // WARNING -> FirebaseCrashlytics.getInstance().log("$tag: $message")
    // ERROR -> FirebaseCrashlytics.getInstance().recordException(RuntimeException("$tag: $message"))
}
```

### #5 LexemeLoggerImpl.kt [~]

Replace direct `Log.d` with sink iteration.

**Was:**
```kotlin
class LexemeLoggerImpl @Inject constructor() : LexemeLogger {
    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }
}
```

**Becomes:**
```kotlin
class LexemeLoggerImpl @Inject constructor(
    private val sinks: List<@JvmSuppressWildcards LogSink>
) : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String) {
        sinks.forEach { sink ->
            if (level >= sink.minLevel) sink.write(level, tag, message)
        }
    }
}
```

Remove `import android.util.Log`.

### #6 app/build.gradle.kts [~]

Add `buildConfigField` for log levels in `buildTypes`.

**Was:** no log-level fields in buildTypes.

**Becomes:**
```kotlin
buildTypes {
    getByName("debug") {
        // ... existing config ...
        buildConfigField("String", "LOG_LEVEL", "\"DEBUG\"")
        buildConfigField("String", "REMOTE_LOG_LEVEL", "\"NONE\"")
    }
    getByName("release") {
        // ... existing config ...
        buildConfigField("String", "LOG_LEVEL", "\"NONE\"")
        buildConfigField("String", "REMOTE_LOG_LEVEL", "\"WARNING\"")
    }
}
```

Support gradle property override: read from `project.findProperty("LOG_LEVEL")` with fallback to hardcoded default.

### #7 LoggerModule.kt [~]

Rewrite from `@Binds` interface to `@Module` object with `@Provides` methods.

**Was:**
```kotlin
@Module
interface LoggerModule {
    @Binds
    fun bindLoggerImpl(impl: LexemeLoggerImpl): LexemeLogger
}
```

**Becomes:**
```kotlin
@Module
object LoggerModule {
    @Provides
    fun provideSinks(): List<@JvmSuppressWildcards LogSink> = buildList {
        if (BuildConfig.LOG_LEVEL != "NONE") {
            add(LogcatSink(LogLevel.valueOf(BuildConfig.LOG_LEVEL)))
        }
        if (BuildConfig.REMOTE_LOG_LEVEL != "NONE") {
            add(CrashlyticsSink(LogLevel.valueOf(BuildConfig.REMOTE_LOG_LEVEL)))
        }
    }

    @Provides
    fun provideLogger(sinks: List<@JvmSuppressWildcards LogSink>): LexemeLogger {
        return LexemeLoggerImpl(sinks)
    }
}
```

### #8 stattab/DatasourceEffectHandler.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #9 dictionarytab/DatasourceEffectHandler.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #10 quiztab/DatasourceEffectHandler.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #11 quiz/chat/DatasourceEffectHandler.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #12 settingstab/DatasourceEffectHandler.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #13 dictionaryappbar/DatasourceEffectHandler.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #14 main/Settings.kt [~]

Удалить `import android.util.Log` и вызов `Log.e(...)`. Лог дублируется в MainUiDepsProvider (`unknown pageKey`). Замена на LexemeLogger не нужна.

### #15 quiz/chat/QuizGameImpl.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

### #16 core-db-impl/CoreDbApiImpl.kt [~]

Remove `import android.util.Log` and replace direct `Log.*` calls with `LexemeLogger` calls using `d()` shortcut.

## Parallelism

- Layer 0 (no deps): #0, #6
- Layer 1 (deps on #0): #1, #2
- Layer 2 (deps on #0, #1, #2): #3, #4, #5
- Layer 3 (deps on sinks + impl): #7
- Migration (deps on #2 only, parallel to each other): #8, #9, #10, #11, #12, #13, #14, #15, #16

## Notes

- Nodes #8-#16 are migration nodes. Each removes direct `android.util.Log` usage and replaces with `LexemeLogger`. Some of these files may not have `LexemeLogger` injected yet — will need constructor injection or parameter passing.

### #17 MigrationFrom07to08.kt [~]

Удалить мусорный `Log.d("###", ...)` и `import android.util.Log`. Лог не несёт ценности.

### #18 CommonExtensions.kt [~]

Добавить опциональный параметр `logger: LexemeLogger? = null`. Заменить `Log.d(...)` на `logger?.d(...)`. Удалить `import android.util.Log`.
