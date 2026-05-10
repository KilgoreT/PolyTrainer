# Crash: PrefsProvider — PrefKey not found

**Дата:** 2026-05-09 14:00:32
**Версия:** 0.1.3 (10003)
**Issue ID:** 49b00709a9ed3843be4f282c96b9e1d0

## Fatal Exception

```
java.lang.IllegalStateException: PrefKey CURRENT_DICTIONARY_ID_LONG not found
    at me.apomazkin.prefs.PrefsProvider$getLongFlow$$inlined$map$1$2.emit(PrefsProvider.java:225)
    at kotlinx.coroutines.flow.internal.SafeCollectorKt$emitFun$1.invoke(SafeCollector.kt:11)
    at kotlinx.coroutines.flow.internal.SafeCollector.emit(SafeCollector.kt:113)
    at androidx.datastore.core.DataStoreImpl$data$1.invokeSuspend(DataStoreImpl.kt:74)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
    at android.os.Handler.handleCallback(Handler.java:938)
    at android.os.Looper.loop(Looper.java:223)
    at android.app.ActivityThread.main(ActivityThread.java:7680)
```

## Описание

`PrefsProvider.getLongFlow()` бросает `IllegalStateException` когда ключ `CURRENT_DICTIONARY_ID_LONG` не найден в DataStore. Происходит при первом запуске или после очистки данных — DataStore пуст, ключ ещё не записан. Flow эмитит данные без этого ключа, и маппинг бросает exception вместо возврата null/default.

Проблема в `PrefsProvider.getLongFlow()` — вероятно `map` блок ожидает что ключ существует и бросает exception если его нет.
