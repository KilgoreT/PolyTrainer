## Задача

При удалении всех словарей происходит краш.

## Контекст

GitHub Issue: https://github.com/KilgoreT/PolyTrainer/issues/476

Стектрейс краша:

```
--------- beginning of crash
AndroidRuntime  E  FATAL EXCEPTION: main
                   Process: co.lexeme.app, PID: 12337
                   me.apomazkin.dictionarytab.deps.DictionaryNotFoundException: No Dictionaries found
                       at me.apomazkin.polytrainer.di.module.widget.DictionaryAppBarUseCaseImpl$flowCurrentDict$1.invokeSuspend(DictionaryAppBarUseCaseImpl.kt:46)
                       at me.apomazkin.polytrainer.di.module.widget.DictionaryAppBarUseCaseImpl$flowCurrentDict$1.invoke(Unknown Source:13)
                       at me.apomazkin.polytrainer.di.module.widget.DictionaryAppBarUseCaseImpl$flowCurrentDict$1.invoke(Unknown Source:6)
                       at kotlinx.coroutines.flow.FlowKt__ZipKt$combine$1$1.invokeSuspend(Zip.kt:29)
                       ...
                       at me.apomazkin.core_db_impl.CoreDbApiImpl$DictionaryApiImpl$flowDictionaryList$$inlined$map$1$2.emit(Emitters.kt:50)
                       ...
                       Suppressed: kotlinx.coroutines.internal.DiagnosticCoroutineContextException: [StandaloneCoroutine{Cancelling}@6042874, Dispatchers.Main.immediate]
```

Точка падения: `DictionaryAppBarUseCaseImpl.flowCurrentDict` (`app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt:46`) — выбрасывается `DictionaryNotFoundException` когда список словарей пуст.

## Пользовательское наблюдение (правило бизнес-логики)

После удаления всех словарей:
- по кнопке "назад" должен идти выход из приложения (`activity.finish()`);
- следующий запуск приложения — это по сути онбординг создания словаря (`DICTIONARY_SETUP`).

Это связывает два уже существующих механизма (`ListNavigationEffect.ExitApp` при пустом списке + `SplashScreen → openDictionarySetup()` при первом запуске) в единый user-journey, который должен быть явно зафиксирован в спеке `dictionary-list.md` (с кросс-ссылкой на `dictionary-create.md`).

_model: claude-opus-4-7[1m]_
