# Checklist

- [ ] Вызов `logger.log(level, tag, message)` → сообщение доставляется во все sink'и с `minLevel <= level` [spec]
  - [ ] Лог: ###LEXEME### dispatch message to N sinks [spec]
- [ ] Вызов `logger.d/i/w/e(tag, message)` → маппится на соответствующий LogLevel и проходит через sink'и [spec]
  - [ ] Лог: ###LEXEME### shortcut method delegates to log() [spec]
- [ ] LogcatSink пишет в Logcat с правильным уровнем (d→Log.d, i→Log.i, w→Log.w, e→Log.e) [spec]
  - [ ] Лог: ###LEXEME### LogcatSink.write level=X [spec]
- [ ] CrashlyticsSink: WARNING → `Crashlytics.log()`, ERROR → `Crashlytics.recordException()` [spec]
  - [ ] Лог: ###LEXEME### CrashlyticsSink.write level=X [spec]
- [ ] Debug build: LogcatSink active (minLevel=DEBUG), CrashlyticsSink disabled (NONE) [spec]
  - [ ] Лог: ###LEXEME### sink configuration: logcat=DEBUG, remote=NONE [spec]
- [ ] Release build: LogcatSink disabled (NONE), CrashlyticsSink active (minLevel=WARNING) [spec]
  - [ ] Лог: ###LEXEME### sink configuration: logcat=NONE, remote=WARNING [spec]
- [ ] Миграция: все существующие `log(tag, message)` переведены на шорткаты d/i/w/e [spec]
- [ ] Запрет: прямых вызовов `android.util.Log` нет нигде кроме LogcatSink [spec]

## Ручное тестирование

### Логирование в debug-сборке
1. Собрать debug APK (`./gradlew assembleDebug`)
2. Запустить приложение
3. Выполнить любое действие, вызывающее логирование (напр. открыть словарь)
4. Ожидание: в Logcat видны сообщения с тегом `##LEXEME##` уровня DEBUG
5. Логи:
   - `###LEXEME### LogcatSink.write level=DEBUG`

### Уровни в Logcat
1. Добавить временный вызов `logger.e("TEST", "error message")` в любой экран
2. Запустить debug-сборку
3. Фильтровать Logcat по тегу `TEST`
4. Ожидание: сообщение отображается как ERROR (красный) в Logcat
5. Логи:
   - `###LEXEME### LogcatSink.write level=ERROR`

### CrashlyticsSink в release
1. Собрать release APK с `REMOTE_LOG_LEVEL=WARNING`
2. Вызвать `logger.w("TEST", "warning breadcrumb")`
3. Вызвать `logger.e("TEST", "error exception")`
4. Ожидание: в Firebase Console — breadcrumb для warning, non-fatal exception для error
5. Логи:
   - `###LEXEME### CrashlyticsSink.write level=WARNING`
   - `###LEXEME### CrashlyticsSink.write level=ERROR`

### Миграция и запрет Log
1. Проверить что нет `import android.util.Log` нигде кроме LogcatSink
2. Проверить что все вызовы `logger.log(tag, message)` заменены на `logger.d/i/w/e`
3. Собрать debug APK — компиляция ✅
