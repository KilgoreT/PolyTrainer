# Варианты решения

Все варианты должны покрыть три точки дефекта:
1. `DictionaryAppBarUseCaseImpl.flowCurrentDict()` — throw в combine-лямбде.
2. `DictionaryTabUseCaseImpl.flowCurrentDict()` / `getCurrentDict()` / `addWord` / `getWordList` — throw на null.
3. `QuizChatUseCaseImpl.getCurrentDictionaryId()` — throw IllegalStateException.

Плюс orphaned `CURRENT_DICTIONARY_ID_LONG` после удаления последнего словаря.

---

## A: Nullable Flow — `Flow<DictUiEntity?>` с эмиссией `null` для «нет словаря»

> Самый прямой фикс: меняем контракт `flowCurrentDict()` на nullable, убираем throw, прокидываем nullable до State. Сейчас `state.currentDict` уже nullable — это просто закрывает разрыв между источником и стейтом.

> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null; ЗАПРЕЩЕНО throw на null"

```kotlin
// DictionaryAppBarUseCaseImpl.kt
override fun flowCurrentDict(): Flow<DictUiEntity?> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        val dict = list.find { it.id == id } ?: list.firstOrNull()
        dict?.let { DictUiEntity(/* ... */) }   // null если list пуст — это валидное состояние
    }
}

// DictionaryTabUseCaseImpl.kt
override fun flowCurrentDict(): Flow<DictUiEntity?> = prefsProvider
    .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .map { id ->
        val dict = id?.let { dictionaryApi.getDictionaryById(it) }
            ?: dictionaryApi.getDictionaryList().firstOrNull()
        dict?.let { DictUiEntity(/* ... */) }
    }

override suspend fun getCurrentDict(): DictUiEntity? { /* nullable */ }

// QuizChatUseCaseImpl.kt
override suspend fun getCurrentDictionaryId(): Long? { /* nullable вместо throw */ }
```

Сообщения и обработка в FlowHandler:

```kotlin
// dictionaryappbar/mate/Message.kt
data class CurrentDict(val current: DictUiEntity?) : Msg   // параметр nullable

// dictionaryappbar/mate/DatasourceEffectHandler.kt
launch {
    useCase.flowCurrentDict().collectLatest { dict ->
        send(Msg.CurrentDict(dict))   // null проходит без особой логики
    }
}

// dictionarytab/logic/DatasourceEffectHandler.kt + Reducer + State —
// добавить поле `currentDict: DictUiEntity?` или флаг `hasNoDictionary: Boolean`
// + ветка в LoadTermFlow: если getCurrentDict() == null → не подписываемся на термы
//   (или эмитим пустой список).
```

> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента; State = только отображаемое"

Чистка orphaned pref в `DictionaryUseCaseImpl.deleteDictionary()`:

```kotlin
override suspend fun deleteDictionary(id: Long): Long {
    dictionaryApi.deleteDictionary(id)
    val currentId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    if (currentId == id) {
        val remaining = dictionaryApi.getDictionaryList().firstOrNull()
        if (remaining != null) {
            setCurrentDictionary(remaining.id)
        } else {
            prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null)   // ← новое
        }
    }
    return id
}
```

| | |
|---|---|
| Плюсы | Минимальная поверхность изменений (контракт уже почти готов: `state.currentDict` уже nullable). Соответствует guide-правилу «throw на null запрещён, fallback через `?:`». Закрывает все три точки дефекта одним подходом. Чистка pref как защитный код — снимает orphaned reference. |
| Минусы | Контракт `getCurrentDict(): DictUiEntity` ломается на nullable — затрагивает `LoadTermFlow` в DictionaryTab (надо думать про ветку «нет словаря» в табе). QuizChat: nullable id заставляет UI чата обработать пустой кейс (вероятно redirect или error-state). Тесты на reducer/handler — переписать ассерты на nullable. |
| Сложность | Средняя |
| Файлы | `DictionaryAppBarUseCase.kt` (контракт), `DictionaryAppBarUseCaseImpl.kt`, `DictionaryTabUseCase.kt` (контракт), `DictionaryTabUseCaseImpl.kt`, `QuizChatUseCase.kt` (контракт), `QuizChatUseCaseImpl.kt`, `DictionaryUseCaseImpl.kt` (чистка pref), `dictionaryappbar/mate/Message.kt`, `dictionaryappbar/mate/DatasourceEffectHandler.kt` (минимально), `dictionaryappbar/mate/Reducer.kt` (минимально — null уже валиден в state), `dictionarytab/logic/State.kt` (добавить поле), `dictionarytab/logic/Reducer.kt` (ветка пустого dict), `dictionarytab/logic/DatasourceEffectHandler.kt` (`LoadTermFlow` ветка null), `quiz/chat/...` (обработать nullable id), удалить/устаревший `DictionaryNotFoundException`. Тесты: ~3-5 файлов. |

> Edge cases:
> - Гонка: prefs.setLong(null) и Room-delete могут эмитнуть в разном порядке — combine выдержит, потому что результат всё равно `null`.
> - В DictionaryTab пустой State — это новое поведение, и до этого фикса дефекта не было видно только потому, что подписка на prefs не триггерилась. После фикса pref обнуляется → эмиссия `null` → таб должен корректно показать пустой/редирект state. Нужно решить: показать «нет словаря» в таба, либо вернуть пользователя на DICTIONARY_SETUP. Спека `dictionary-list.md` намекает на ExitApp + следующий запуск = онбординг, но для in-flight удаления решать отдельно.

---

## B: Sealed state — `CurrentDictState { Loading; Selected(DictUiEntity); Empty }`

> Вместо nullable вводим явный sealed-класс для трёх случаев: загрузка, выбран словарь, пусто. Контракт Flow эмитит sealed-значение — каждый case явно.

> 📎 guide: docs/guides/effect-handlers.md — "разделение первого emit (Ready vs Updated): два разных Message вместо boolean-флага"

```kotlin
sealed interface CurrentDictState {
    data object Loading : CurrentDictState
    data class Selected(val dict: DictUiEntity) : CurrentDictState
    data object Empty : CurrentDictState
}

override fun flowCurrentDict(): Flow<CurrentDictState> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        val dict = list.find { it.id == id } ?: list.firstOrNull()
        when {
            dict != null -> CurrentDictState.Selected(DictUiEntity(/* ... */))
            list.isEmpty() -> CurrentDictState.Empty
            else -> CurrentDictState.Empty   // фактически тот же кейс
        }
    }
}

// dictionaryappbar/mate/Message.kt
sealed interface Msg {
    data class CurrentDictSelected(val current: DictUiEntity) : Msg
    data object CurrentDictAbsent : Msg
    // ...
}
```

В State — отдельное поле под sealed:

```kotlin
data class State(
    val currentDict: CurrentDictState = CurrentDictState.Loading,
    // ...
)
```

| | |
|---|---|
| Плюсы | Self-documenting контракт: «нет словаря» — это явное доменное состояние, не специальное значение nullable. Соответствует effect-handlers guide «два разных Message вместо boolean-флага». Loading-state бесплатно появляется (сейчас на этапе до первой эмиссии State показывает старое значение / default). |
| Минусы | Большая поверхность изменений — sealed надо протянуть через все три модуля (AppBar, DictionaryTab, QuizChat) + UI потребует when-веток вместо `?:`. Для AppBar дополнительная Loading-сущность фактически не нужна (виджет либо рендерится либо нет). Гарантированно ломает все downstream-callsite'ы (Reducer'ы, тесты, UI). Высокая стоимость для бага, который правится nullable. |
| Сложность | Высокая |
| Файлы | Всё что в варианте A + новые файлы под `CurrentDictState` (где? в одном из shared-модулей под core-ui или в `dictionaryappbar/deps`? — добавляет вопрос модульной локализации), UI каждого таба (rendering зависит от sealed-when), тесты — массово переписать матчеры. ~15-20 файлов. |

> Edge cases:
> - Sealed придётся импортировать туда же, куда импортируется `DictUiEntity`. Если хочется state localized для каждого модуля (свой sealed в каждом UseCase) — получаем 3 копии плюс маппинг между ними.
> - Loading-стейт в AppBar смешивается с первой эмиссией combine: combine эмитит только когда оба upstream дали значение → между подпиской и первым эмитом виджет на дефолтном Loading. Это поведенческое изменение, требует ревизии UI (placeholder для AppBar в Loading).

---

## C: Чистка pref + nullable Flow (минимальный фикс корня без рефакторинга контрактов API)

> Чистый минимум: AppBar и DictionaryTab `flowCurrentDict()` меняем на nullable (`Flow<DictUiEntity?>`), плюс `DictionaryUseCaseImpl.deleteDictionary()` чистит orphaned pref. QuizChat временно оставляем как есть, но добавляем gating на навигации (нельзя открыть quiz-chat без словаря). Близок к варианту A, но точечнее: не трогаем контракт `getCurrentDict()` (suspend, non-null) и `getCurrentDictionaryId()` — только реактивные стримы.

> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null; fallback через `?:` или filterNotNull"

```kotlin
// AppBar и DictionaryTab flowCurrentDict() — nullable (как в A)
override fun flowCurrentDict(): Flow<DictUiEntity?> = /* без throw */

// suspend getCurrentDict() / getCurrentDictionaryId() — оставляем non-null
// но защищаем gating: пустой список → не даём перейти на vocabulary-tab / quiz-chat
// (на уровне MainRouter / навигации)

// DictionaryUseCaseImpl.deleteDictionary — чистит pref (как в A)
```

Gating на навигации:

```kotlin
// app/.../route/MainRouter.kt или BottomBarWidget
// Если dictionaries.isEmpty() → bottom-bar выключен, переход на VocabularyTab
// невозможен.  Реалистично: ExitApp при пустом списке уже работает (см. spec) —
// добавить guard в SplashScreen на старте + guard на навигацию в Quiz-Chat.
```

> 📎 guide: docs/guides/reducer-patterns.md — "Conditional навигация (ExitApp vs Back): выбор делается в reducer на основе state"

| | |
|---|---|
| Плюсы | Минимальная сурфейс. Корень дефекта (throw в Flow) закрыт. Контракты `getCurrentDict()` / `getCurrentDictionaryId()` не меняются — нет переработки downstream. Orphaned pref чистится. Решение опирается на существующий механизм `ExitApp` (пустой список → exit → следующий запуск онбординг). |
| Минусы | Дефект в `getCurrentDict()` остаётся скрытой миной: если кто-то вызовет suspend-метод при пустом списке (например, через QuizChat или race-сценарий), снова получим краш. Gating на навигации — отдельный кусок работы, легко забыть случай (например, deep-link). Решение «скрыть проблему» а не «убрать». |
| Сложность | Низкая (реактивные стримы) + Средняя (gating navigation aware). |
| Файлы | `DictionaryAppBarUseCase.kt`, `DictionaryAppBarUseCaseImpl.kt`, `DictionaryTabUseCase.kt`, `DictionaryTabUseCaseImpl.kt` (только flowCurrentDict), `DictionaryUseCaseImpl.kt`, `dictionaryappbar/mate/Message.kt`, `dictionaryappbar/mate/Reducer.kt`, `dictionarytab/logic/...` (поле под пустой стейт). Gating: `MainRouter.kt` / `BottomBarWidget.kt` / `SplashScreen` (проверка пустого списка). Тесты: ~2-3 файла. |

> Edge cases:
> - Race: пользователь удаляет последний словарь и **одновременно** не успевает уйти с vocabulary-таба (если что-то типа autoclose) → `getCurrentDict()` suspend-вызов может ещё стрельнуть. Закрыть либо try/catch в FlowHandler (антипаттерн), либо всё-таки nullable suspend (=A).
> - Gating quiz-chat — проверить точку входа (`QuizChatScreen` запускается из тестов? deep-link?), чтобы не получилось window с пустым списком + открытым чатом.

---

## D: Stop-on-empty в FlowHandler — подписка фильтрует пустой случай через `filterNotNull` / останов

> Источник (`flowCurrentDict()`) остаётся **non-null**, но FlowHandler (или сам useCase через map) защищает себя: при пустом списке `flowCurrentDict()` эмитит дефолтный sentinel (например, `DictUiEntity.NONE`) или подписка отключается через `takeWhile { list.isNotEmpty() }`. Reducer/State получают сигнал «нет словаря» отдельным сообщением.

> 📎 guide: docs/guides/effect-handlers.md — "Flow останавливается в FlowHandler — дальше только Messages; Debounce/filter — в UseCase или в FlowHandler"

```kotlin
// Вариант D.1 — sentinel:
companion object {
    val EMPTY_DICT = DictUiEntity(id = -1, name = "", /* ... */)
}
override fun flowCurrentDict(): Flow<DictUiEntity> = combine(...) { id, list ->
    val dict = list.find { it.id == id } ?: list.firstOrNull()
    dict?.let { DictUiEntity(/* ... */) } ?: EMPTY_DICT
}

// Вариант D.2 — два потока:
override fun flowCurrentDict(): Flow<DictUiEntity?> = /* как в A */
override fun flowHasAnyDict(): Flow<Boolean> = dictionaryApi.flowDictionaryList()
    .map { it.isNotEmpty() }
// FlowHandler подписывается на оба и эмитит разные Msg.
```

| | |
|---|---|
| Плюсы | Сохраняет non-null контракт `Flow<DictUiEntity>` (если кто-то держит за это). D.2 разводит «текущий словарь» и «есть ли вообще словари» — концептуально чище для UI, который и так показывает разные ветки. |
| Минусы | D.1 (sentinel) — антипаттерн «magic value», тестировать сложнее, легко забыть проверку. D.2 — два Flow в одном UseCase, дополнительная сложность подписок, два source of truth. По сути это вариант A в одёжке non-null. Не закрывает `getCurrentDict()` suspend и `getCurrentDictionaryId()` — те всё равно надо менять (либо как A, либо gating). |
| Сложность | Средняя |
| Файлы | Те же что в A, плюс sentinel/второй Flow в UseCase + Message + Reducer (`Msg.DictionariesExist(Boolean)` или подобное). ~10-12 файлов. |

> Edge cases:
> - D.1: sentinel может незаметно протечь в UI (id=-1 в DictUiEntity → флаг страны "" → пустой FlagPlaceholder и т.д.). Защита — компайл-тайм проверки невозможны, только дисциплина.
> - D.2: гонка между двумя Flow — combine их обратно, либо принять рассогласование первого emit'а.

---

## E: try/catch в FlowHandler — перехват исключения в подписке (АНТИПАТТЕРН, для полноты)

> На уровне `DatasourceEffectHandler.subscribe()` оборачиваем `collectLatest` в `try { } catch (DictionaryNotFoundException) { send(Msg.CurrentDictAbsent) }` или `.catch { ... }` оператор Flow. Источник не меняется.

> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО throw на null: краш при первом запуске"

```kotlin
launch {
    useCase.flowCurrentDict()
        .catch { e ->
            if (e is DictionaryNotFoundException) emit(/* ??? */)
            else throw e
        }
        .collectLatest { dict -> send(Msg.CurrentDict(dict)) }
}
```

| | |
|---|---|
| Плюсы | Самое маленькое изменение — один файл. |
| Минусы | Исключение моделирует валидное состояние — прямое нарушение `docs/guides/effect-handlers.md` («ошибки реактивного источника обязан обрабатывать UseCase/FlowHandler, не пропуская исключения наружу» — формально подходит, но guide подразумевает обработку ошибок, а не использование исключения как события). После `.catch` Flow прекращается — пользователь не получает следующих эмиссий (новый словарь создан → нет реакции, пока подписка не пересоздана). Не закрывает `getCurrentDict()` suspend и `getCurrentDictionaryId()`. Не решает orphaned pref. |
| Сложность | Низкая |
| Файлы | `dictionaryappbar/mate/DatasourceEffectHandler.kt`, `dictionarytab/logic/DatasourceEffectHandler.kt`. |

> Edge cases:
> - `.catch { }` глотает один раз и завершает upstream — после первого «пусто» подписка мертва. Чтобы её перезапустить, нужна retry-логика. Для UI это значит: AppBar после удаления всех словарей не реагирует на новый словарь, пока не пересоздадим ViewModel.
> - Этот вариант явно упоминается как тупиковый в задаче — оставлен для полноты сравнения.

---

_model: claude-opus-4-7[1m]_
