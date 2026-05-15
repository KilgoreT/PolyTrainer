# Research — IS476 / delete_all_dictionaries_crash

## 1. Природа проблемы

### С точки зрения пользователя

Пользователь зашёл в Настройки → "Управление словарями" (раздел `DICTIONARY_LIST`).
Видит список своих словарей. Тапает иконку удаления, подтверждает диалог.
Удаляет один за другим, пока в списке не останется единственный словарь.
Подтверждает удаление последнего словаря.

Ожидание: словарь исчезает из списка, экран показывает пустое состояние
(спека `dictionary-list.md`, §"Пустое состояние": "Если словарей нет — 'Нет
словарей / Создайте первый!' по центру + кнопка 'Новый словарь'. AppBar не
показывается.").

Получает: приложение падает. Процесс убит с `DictionaryNotFoundException`.

### С точки зрения системы

`DictionaryAppBarUseCaseImpl.flowCurrentDict()` подписан на комбинацию prefs
+ Room. После CASCADE-delete в Room таблица `dictionaries` становится пустой,
Room эмитит пустой `List<DictionaryEntity>`. Внутри `combine`-лямбды:

```kotlin
(list.find { it.id == id } ?: list.firstOrNull())
    ?.let { dict -> DictUiEntity(...) }
    ?: throw DictionaryNotFoundException()
```

`list.firstOrNull()` возвращает `null`, контракт типа `Flow<DictUiEntity>`
non-null — лямбда кидает исключение. `combine` не глотает исключения внутри
трансформера: исключение распространяется до `collectLatest` в
`DatasourceEffectHandler.subscribe()`, не ловится, и роняет всю корутину
`viewModelScope` → необработанный exception крашит процесс через
`CoroutineExceptionHandler` по умолчанию.

> 📎 guide: docs/guides/effect-handlers.md — "Flow останавливается в FlowHandler — дальше только Messages; UI не знает про Flow — видит только State"
> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО — throw на null: it[longPreferencesKey(prefKey.value)] ?: throw IllegalStateException(...)  // ← краш при первом запуске"

### Почему это проблема

1. **Краш как штатное последствие штатного действия.** Удаление последнего
   словаря — валидный пользовательский сценарий: спека `dictionary-list.md`
   явно описывает пустое состояние. Поведение «удали всё → краш» нарушает
   user trust и противоречит собственной спеке.
2. **Безвозвратность.** После рестарта приложение попадает на
   `RootPoint.SPLASH` → `SplashScreen` → решает куда вести. Пустой список
   словарей корректно обрабатывается через `DICTIONARY_SETUP`, но
   пользователь только что прошёл краш на ровном месте.
3. **Дефект архитектурный, не локальный.** Тот же антипаттерн (`throw
   DictionaryNotFoundException`) повторён в трёх местах:
   `DictionaryAppBarUseCaseImpl.flowCurrentDict()`,
   `DictionaryTabUseCaseImpl.flowCurrentDict()`,
   `DictionaryTabUseCaseImpl.getCurrentDict()` — плюс
   `IllegalStateException("Dictionary not found")` в
   `DictionaryTabUseCaseImpl.addWord/getWordList/getCurrentDictionaryId
   (QuizChatUseCaseImpl)`. Любая из них может выстрелить при пустом списке.
4. **Скрытое состояние подписок.** Виджет AppBar не виден на
   `DictionaryListScreen` (другой экран в RootRouter), но ViewModel'и табов
   (`VocabularyTab`, `QuizTab`, `StatisticTab`) и их встроенные
   `DictionaryAppBarViewModel` живы в памяти — `MainScreen` остаётся в back
   stack под `DICTIONARY_LIST`. Их подписки на `flowCurrentDict()` продолжают
   получать Room-эмиссии. Крах прилетает «из бэкграунда» с экрана, который
   пользователь даже не видит — отсюда стектрейс из AppBar при действиях на
   списке.

## 2. Воспроизведение

### Входное состояние

- В приложении ≥ 1 словарь.
- Пользователь хотя бы раз дошёл до главного экрана (`RootPoint.MAIN_ROUTER`
  → `MainScreen`). Это разворачивает все четыре таб-ViewModel и поднимает
  подписки `DictionaryAppBar.DatasourceEffectHandler.subscribe()` для трёх
  табов (Vocabulary, Quiz, Stats).

### Шаги

1. Запустить приложение → попасть на главный экран
   (`MAIN_ROUTER/MAIN/VOCABULARY` — стартовая destination).
2. Внизу bottom-bar → таб "Настройки" (`TabPoint.SETTINGS`).
3. В Настройках → "Управление словарями" → `openDictionaryList()` →
   `navController.navigate(RootPoint.DICTIONARY_LIST.route)` — переход
   вне навграфа `MAIN_ROUTER` (на уровне `RootRouter`), `MAIN_ROUTER` остаётся
   в back stack.
4. На экране списка удалить словари по одному. На последнем
   `Msg.ConfirmDelete` → `DictionaryListEffect.DeleteDictionary(id)` →
   `dictionaryUseCase.deleteDictionary(id)`.
5. Падение происходит синхронно с CASCADE-delete: Room эмитит пустой список
   подписчикам, и любой активный `flowCurrentDict()` бросает исключение.

### Условия / окружение

- Любая версия Android. Поведение детерминированное — зависит только от
  факта, что обе подписки активны и Room эмитит пустой список.
- Не требует ротации экрана, swipe-back, многозадачности — простая
  последовательность тапов.

## 3. Корневая причина

### Главная

`DictionaryAppBarUseCaseImpl.flowCurrentDict()` —
`app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt:32-48`:

```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        (list.find { it.id == id } ?: list.firstOrNull())
            ?.let { dict -> DictUiEntity(...) }
            ?: throw DictionaryNotFoundException()    // строка 46
    }
}
```

Возвращаемый тип `Flow<DictUiEntity>` non-null. Пустой `list` — валидное
состояние домена, но контракт его не пропускает. Эмиссия моделируется
исключением — антипаттерн в reactive-stream'е.

> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null. ПРАВИЛЬНО — fallback при null; ЗАПРЕЩЕНО — force unwrap / throw на null"
> 📎 guide: docs/guides/effect-handlers.md — "Flow останавливается в FlowHandler — дальше только Messages"

### Близнец-источник проблем

`DictionaryTabUseCaseImpl.flowCurrentDict()` —
`app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt:56-70`:

```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity> = prefsProvider
    .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .map { id: Long? ->
        val dict = (id?.let { dictionaryApi.getDictionaryById(it) }
            ?: dictionaryApi.getDictionaryList().firstOrNull())
            ?.let { dict -> DictUiEntity(...) }
        dict ?: throw DictionaryNotFoundException()    // строка 69
    }
```

Тот же дефект. Подписан `DictionaryTab.DatasourceEffectHandler.subscribe()`
(`modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/DatasourceEffectHandler.kt:42-46`).

> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null"
ViewModel `DictionaryTabViewModel` жив в составе `MainScreen` через
`VocabularyTabDep`. Подписка активна с момента первого захода на
`TabPoint.VOCABULARY` (стартовая destination MainScreen) и до уничтожения
ViewModelStoreOwner вкладочного NavBackStackEntry. Эта подписка ловит
эмиссию пустого списка из Room **одновременно** с AppBar — какая упадёт
первой, зависит от планировщика корутин.

> Важно: `DictionaryTabUseCaseImpl.flowCurrentDict()` подписан только на
> `getLongFlow(CURRENT_DICTIONARY_ID_LONG)`, а не на `flowDictionaryList()`.
> Room-delete сам по себе ещё не триггерит её эмиссию. Но
> `DictionaryUseCaseImpl.deleteDictionary()` (см. строки 60-67) при удалении
> текущего id делает `prefsProvider.setLong(...)` для нового id или —
> **критично** — **не делает ничего**, если оставшегося словаря нет. То
> есть в кейсе «удалили все» pref остаётся со старым id, и
> `DictionaryTabUseCaseImpl.flowCurrentDict()` НЕ эмитит. А вот AppBar
> `combine` реагирует на Room-эмиссию пустого списка немедленно — отсюда
> стектрейс именно из AppBar (как и зафиксировано в триаже).

### Природа

`flowCurrentDict()` спроектирован под молчаливое предположение «хотя бы один
словарь всегда есть». Это предположение исторически держится первой
инициализацией через `DICTIONARY_SETUP`, но пользователь может его сломать,
удалив все словари — и спека `dictionary-list.md` явно описывает это как
валидное состояние.

> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента ... вычисление происходит в редьюсере, а не в composable"
> 📎 guide: docs/guides/prefs-datastore.md — "Ключ может не существовать при: Первом запуске приложения, Очистке данных приложения, Race condition"

## 4. Data flow

### Триггер → краш

```
[User] taps "Удалить" в ConfirmDeleteDictionaryWidget
  ↓
DictionaryListMsg.ConfirmDelete
  ↓ DictionaryListReducer (modules/screen/dictionary/.../DictionaryListReducer.kt:30-33)
state.hideDeleteDialog() + DictionaryListEffect.DeleteDictionary(id)
  ↓
DictionaryListEffectHandler.onEffect (modules/screen/dictionary/.../DictionaryListEffectHandler.kt:24-29)
  withContext(IO) { dictionaryUseCase.deleteDictionary(id) }
  ↓
DictionaryUseCaseImpl.deleteDictionary (app/.../di/module/dictionary/DictionaryUseCaseImpl.kt:60-67)
  dictionaryApi.deleteDictionary(id)   ← Room: CASCADE delete (см. WordDao)
  if (currentId == id) {
      val remaining = dictionaryApi.getDictionaryList().firstOrNull()  ← null
      remaining?.let { setCurrentDictionary(it.id) }                   ← no-op
  }
  ↓
Room уведомляет всех подписчиков на `wordDao.flowDictionaries()` —
эмитит `emptyList<DictionaryEntity>`
  ↓
[Подписчик 1: AppBar — ЖИВОЙ для всех активных табов в MainScreen]
DictionaryAppBarUseCaseImpl.flowCurrentDict() lambda внутри combine:
  list.find { it.id == id } → null
  list.firstOrNull()        → null
  throw DictionaryNotFoundException()      ← КРАШ
  ↓
Исключение проходит:
  combine{} → DatasourceEffectHandler (widget/dictionaryappbar/.../DatasourceEffectHandler.kt:34-37)
    launch { useCase.flowCurrentDict().collectLatest { send(Msg.CurrentDict(...)) } }
  ↓
Корутина `viewModelScope` (DictionaryAppBarViewModel) падает.
Никакого try/catch на пути. Корневой scope с дефолтным
CoroutineExceptionHandler → uncaught exception → процесс убит.

> 📎 guide: docs/guides/effect-handlers.md — "Без сложной логики в хендлерах — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере"

[Подписчик 2: DictionaryTab — тоже живой]
DictionaryTabUseCaseImpl.flowCurrentDict() подписан на prefs.getLongFlow,
а не на flowDictionaryList → эмиссия не приходит (pref не меняется,
см. §3). Краш произойдёт лишь при следующем заходе на vocabulary-таб
после рестарта, если pref всё ещё ссылается на удалённый id — но к этому
моменту splash перерулит на DICTIONARY_SETUP, так что в практике не
стреляет.

[Подписчик 3: DictionaryListFlowHandler]
modules/screen/dictionary/.../DictionaryListFlowHandler.kt — корректно
обрабатывает пустой список: эмитит Msg.DictionariesLoaded(emptyList).
Reducer переводит state.dictionaries в emptyList. UI готов показать
"Пустое состояние" — но процесс уже убит подписчиком №1.
```

### Подписки: когда живы

- `DictionaryAppBarViewModel` инстанциируется при первом рендере любого
  таба, который кладёт AppBar (`VocabularyTabDep`, `QuizTabScreenDep`,
  `StatisticTabScreenDep` — см. `CompositionRootImpl`). NavBackStackEntry
  таба сохраняется в `MainScreen.navController` благодаря `saveState =
  true` в `BottomBarWidget.openTab` (строка 78). Значит, **все три AppBar
  ViewModel'и могут быть живы одновременно** — по одному на NavBackStackEntry
  каждого таба, который пользователь хоть раз посетил.
- При навигации в `DICTIONARY_LIST` (это уровень `RootRouter`, не
  `MainRouter`) `MAIN_ROUTER` остаётся в back stack целиком — все его
  NavBackStackEntry с ViewModelStoreOwner живы, подписки активны.
- Подписки запускаются единожды в `Mate.subscribeToLongRunningFlows()` при
  инициализации, и живут до `viewModel.onCleared()`. `unsubscribe()` есть
  (`MateFlowHandler.unsubscribe()`), но никто его не вызывает явно — пока
  `viewModelScope` жив, подписка жива.

## 5. Затронутые компоненты

| Файл | Роль |
|------|------|
| `app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt` | **Источник краха.** `flowCurrentDict()` строка 46 — `throw DictionaryNotFoundException()`. Подписан на `flowDictionaryList()`, реагирует на пустой список немедленно. |
| `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt` | Тот же антипаттерн `throw` в `flowCurrentDict()` (стр. 69), `getCurrentDict()` (стр. 53), `addWord/getWordList` (стр. 79-80, 124-125). Сейчас не стреляет благодаря тому, что подписан только на prefs, но это везение, не дизайн. |
| `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt` | `getCurrentDictionaryId()` (стр. 42) тоже бросает `IllegalStateException("Dictionary not found")`. Вызывается из `QuizGameImpl.kt:174` — потенциально стрельнёт, если пользователь успеет открыть чат-квиз без словарей. |
| `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/DatasourceEffectHandler.kt` | Подписчик, через который исключение прорывается наружу. `collectLatest` не оборачивает в try/catch. |
| `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/deps/DictionaryAppBarUseCase.kt` | Контракт `fun flowCurrentDict(): Flow<DictUiEntity>` — non-null возврат, заставляет реализации моделировать «нет словаря» исключением. |
| `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/State.kt` | `currentDict: DictUiEntity?` — поле уже nullable. UI готов к null (`DictDropDownWidget` принимает nullable). Структурно State уже учитывает пустой случай — расходится с контрактом Flow. |

> 📎 guide: docs/guides/state-and-extensions.md — "Иммутабельные data class'ы ... Дефолтные значения для всех полей — безопасное создание через конструктор без аргументов"
> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента ... каждый элемент UI — поле в стейте, каждое поле — элемент в UI"
| `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt` | `Msg.CurrentDict(current: DictUiEntity)` — параметр non-null. Чтобы пропустить null до State, надо менять и контракт сообщения, и/или ввести `Msg.CurrentDictAbsent`. |

> 📎 guide: docs/guides/messages.md — "Sealed interface для Msg ... data class (с данными) ... data object (без данных)"
> 📎 guide: docs/guides/effect-handlers.md — "Паттерн: разделение первого emit (Ready vs Updated) ... Вместо boolean-флага в State — два разных Message"
| `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt` | Контракт: `suspend fun getCurrentDict(): DictUiEntity` (non-null!) + `fun flowCurrentDict(): Flow<DictUiEntity>`. Plus содержит сам класс `DictionaryNotFoundException`. |
| `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/DatasourceEffectHandler.kt` | Подписчик flowCurrentDict для vocabulary-таба. Использует `dictionaryTabUseCase.getCurrentDict()` в `LoadTermFlow` (стр. 56) — non-null контракт упирается в реальность. |
| `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/State.kt` | Сейчас State **не содержит** ссылки на текущий словарь — DictionaryTab оперирует им только локально внутри effects. То есть если фикс потребует «нет словаря — пустое состояние таба», поле придётся добавлять. |
| `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/DictionaryListEffectHandler.kt` | Триггерит CASCADE через `dictionaryUseCase.deleteDictionary(id)`. Сам не имеет дефектов, но запускает цепочку. |
| `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImpl.kt` | `deleteDictionary()` строки 60-67: переключает текущий pref только если есть `remaining`. В пустом случае pref оставляет указывать на удалённый id — формально orphaned reference. |

> 📎 guide: docs/guides/prefs-datastore.md — "Запись перед чтением ... Запись происходит на SplashScreen при первом запуске ... Nullable Flow — страховка на race condition"
| `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt` | `getLongFlow()` корректно эмитит `null`, когда ключа нет. В нашем кейсе ключ есть (хранит id удалённого словаря) — pref-Flow не эмитит ничего, только Room. |

> 📎 guide: docs/guides/prefs-datastore.md — "getLongFlow(prefKey: PrefKey): Flow<Long?> — nullable — ключ может не существовать"
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt` (стр. 109-115) | `deleteDictionary` + `flowDictionaryList` — низкоуровневые контракты Room, корректны. |

> 📎 guide: docs/guides/data-layer.md — "Foreign keys с CASCADE delete на всех связях"
| `app/src/main/java/me/apomazkin/polytrainer/route/RootRouter.kt` + `MainRouter.kt` + `modules/screen/main/.../MainScreen.kt` | Контекст: `DICTIONARY_LIST` не popUp-ает `MAIN_ROUTER`, табы остаются на back stack → их ViewModel'и живы → подписки активны → краш прилетает с экрана, которого пользователь не видит. |
| `docs/features-spec/dictionary-list.md` | Спека описывает удаление и пустое состояние, но не описывает реактивный контракт `flowCurrentDict()` при пустом списке. |
| `docs/features-spec/dictionary-appbar.md` | Спека описывает виджет, но молчит про пустой список. State `currentDict` уже nullable — расхождение со спекой и с контрактом Flow. |

## 6. Ограничения

### Чего нельзя ломать при будущем фиксе

1. **Контракт MainScreen → AppBar.** AppBar — общий виджет для трёх табов
   (Vocabulary/Quiz/Stats). Любая правка `DictionaryAppBarUseCase` /
   `Msg.CurrentDict` распространяется на все три. State `currentDict`
   уже nullable, поэтому downstream-код виджета должен корректно
   обрабатывать null (см. `DictDropDownWidget` — принимает `currentDict:
   DictUiEntity?`). Проверить: AppBar.kt:64-72 — `currentDict =
   state.currentDict` без `!!`, безопасно.
2. **Контракт `DictionaryTabUseCase.getCurrentDict(): DictUiEntity`.**
   Используется в эффектах (`DatasourceEffect.LoadTermFlow → val
   dictionaryId = dictionaryTabUseCase.getCurrentDict().id.toInt()`). Если
   менять на nullable, надо думать про поведение vocabulary-таба при
   отсутствии словарей: показывать пустое состояние / редирект.
   В State пока нет поля под "нет словаря" — структурное расширение.

> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента ... вычисление происходит в редьюсере"
> 📎 guide: docs/guides/effect-handlers.md — "Без сложной логики в хендлерах — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере"
3. **CASCADE delete в Room.** Удаление словаря триггерит каскад: words →
   lexemes → write_quiz. Это завязка на схеме БД (`WordDao.deleteDictionary`).
   Менять нельзя — это бизнес-инвариант.

> 📎 guide: docs/guides/data-layer.md — "Foreign keys с CASCADE delete на всех связях"
4. **`SplashScreen` redirect logic.** Если на старте словарей нет → splash
   ведёт в `DICTIONARY_SETUP`. Это страховка, на которую фикс не должен
   полагаться (она срабатывает только при рестарте, а удаление —
   in-flight операция, рестарта нет).
5. **Прозрачность для пользователя.** Спека `dictionary-list.md` уже
   обещает пустое состояние («Нет словарей / Создайте первый!»). Фикс
   должен сохранить эту мечту — то есть после удаления последнего
   словаря пользователь должен остаться на `DictionaryListScreen` в
   пустом состоянии, без крашей и без принудительной навигации.
6. **Reactive principle (effect-handlers guide).** "Flow останавливается в
   FlowHandler — дальше только Messages; ошибки реактивного источника
   обязан обрабатывать UseCase/FlowHandler, не пропуская исключения
   наружу". Фикс должен оставаться в этой парадигме — исключения не
   должны быть событиями реактивного потока.

> 📎 guide: docs/guides/effect-handlers.md — "Flow останавливается в FlowHandler — дальше только Messages; UI не знает про Flow — видит только State"
> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО — throw на null ... ПРАВИЛЬНО — fallback при null"
7. **Обратная совместимость pref'а CURRENT_DICTIONARY_ID_LONG.**
   `DictionaryUseCaseImpl.deleteDictionary()` оставляет pref со
   старым id, если оставшихся нет — это orphaned reference. Если фикс
   будет чистить pref (`setLong(null)` / `remove()`), надо проверить
   все читатели: `DictionaryAppBarUseCaseImpl`, `DictionaryTabUseCaseImpl`,
   `QuizChatUseCaseImpl`, `SplashUseCaseImpl`. Все они уже обрабатывают
   `id == null` через fallback на `getDictionaryList().firstOrNull()`,
   так что чистка pref — безопасное direction, но не lazy fix.

> 📎 guide: docs/guides/prefs-datastore.md — "Кто пишет? кто читает? ... Если Flow — обработать null для Long/Int"
8. **`DictionaryNotFoundException` живёт в
   `dictionarytab.deps.DictionaryNotFoundException`** —
   импортируется как из `DictionaryAppBarUseCaseImpl`, так и из
   `DictionaryTabUseCaseImpl`. Удаление этого класса (если решим, что
   исключения тут не нужны вовсе) затронет три файла; перенос —
   ломает один импорт.

_model: claude-opus-4-7[1m]_
