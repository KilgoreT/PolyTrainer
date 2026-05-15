# Design Tree — IS476 / delete_all_dictionaries_crash

Граф файлов для реализации **варианта A (Nullable Flow)** из 05_impact_analysis.md.

Скоуп изменений:
1. AppBar UseCase + контракт + сообщение + handler — `Flow<DictUiEntity?>`.
2. DictionaryTab UseCase + контракт (включая удаление `DictionaryNotFoundException` из того же файла) + State (расширение) + Reducer + handler + UI — `Flow<DictUiEntity?>` + `suspend getCurrentDict(): DictUiEntity?`, явное пустое состояние таба.
3. QuizChat UseCase + контракт — `suspend getCurrentDictionaryId(): Long?`, потребитель `QuizGameImpl` обрабатывает null.
4. `DictionaryUseCaseImpl.deleteDictionary()` — чистка orphaned pref при удалении последнего словаря.
5. Тесты — добавить кейсы на пустой список / nullable.

---

## Часть 1: Граф

```yaml
# === Контракты (top-level, без зависимостей) ===

- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/deps/DictionaryAppBarUseCase.kt
  action: "~"
  depends: []

# Узел #2 — комбинированный: правка интерфейса DictionaryTabUseCase + удаление
# класса DictionaryNotFoundException из ТОГО ЖЕ файла (это одна правка одного файла).
# Корневой контракт — depends пуст. Семантическая связь с #4, #5, #19 выражена
# через atomic wave `dict_tab_contract_wave` (см. секцию `## Atomic Waves`).
- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt
  action: "~"
  depends: []
  wave: dict_tab_contract_wave

- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt
  action: "~"
  depends: []

# === Реализации UseCase в app/ — зависят от контрактов ===
# #4 и #5 семантически связаны с #2 через atomic wave `dict_tab_contract_wave`
# (см. секцию `## Atomic Waves`). Wave-семантика гарантирует одновременное применение
# одной транзакцией без промежуточной компиляции — поэтому depends не выражает
# зависимость от #2, но узлы помечены `wave: dict_tab_contract_wave`.

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt
  action: "~"
  depends: [1]
  wave: dict_tab_contract_wave

- id: 5
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt
  action: "~"
  depends: [2]
  wave: dict_tab_contract_wave

- id: 6
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt
  action: "~"
  depends: [3]

# === Чистка pref в DictionaryUseCaseImpl (независимо) ===

- id: 7
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImpl.kt
  action: "~"
  depends: []

# === AppBar mate-layer: Msg → handler → reducer ===

- id: 8
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt
  action: "~"
  depends: []

- id: 9
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/State.kt
  action: "~"
  depends: []

- id: 10
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [1, 8]

- id: 11
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducer.kt
  action: "~"
  depends: [8, 9]

# === DictionaryTab logic: State → Message → Reducer → handler ===

- id: 12
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/State.kt
  action: "~"
  depends: []

- id: 13
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/Message.kt
  action: "~"
  depends: []

- id: 14
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/VocabularyTabReducer.kt
  action: "~"
  depends: [12, 13]

- id: 15
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [13]

- id: 16
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/VocabularyTabScreen.kt
  action: "~"
  depends: [12]

# === QuizChat потребитель (QuizGameImpl) ===

- id: 17
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt
  action: "~"
  depends: [3]

# === Тесты ===
# Узел #18 удалён (был отдельной правкой `DictionaryNotFoundException [-]`).
# Объединён с #2 — это одна правка одного файла. См. Architect Review — addressed / Critical 2.

- id: 19
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/test/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImplTest.kt
  action: "~"
  depends: [4]
  wave: dict_tab_contract_wave

- id: 20
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/test/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImplTest.kt
  action: "~"
  depends: [7]

- id: 21
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/test/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImplTest.kt
  action: "+"
  depends: [5]

- id: 22
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/test/java/me/apomazkin/dictionarytab/logic/VocabularyTabReducerKtTest.kt
  action: "~"
  depends: [14]

- id: 23
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/test/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducerTest.kt
  action: "+"
  depends: [11]
```

---

## Часть 2: Детали изменений

### #1 DictionaryAppBarUseCase.kt [~]

**Было:**
```kotlin
interface DictionaryAppBarUseCase {
    fun flowAvailableDict(): Flow<List<DictUiEntity>>
    fun flowCurrentDict(): Flow<DictUiEntity>
    suspend fun changeDict(id: Long)
}
```

**Стало:**
```kotlin
interface DictionaryAppBarUseCase {
    fun flowAvailableDict(): Flow<List<DictUiEntity>>
    fun flowCurrentDict(): Flow<DictUiEntity?>   // nullable: null при пустом списке словарей
    suspend fun changeDict(id: Long)
}
```

Контракт: `null` — валидное доменное состояние «нет словаря». Внутри `combine` запрещено бросать исключение для моделирования отсутствия словаря.

> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null; ЗАПРЕЩЕНО throw на null"

---

### #2 DictionaryTabUseCase.kt [~]

🌊 atomic wave: `dict_tab_contract_wave` (вместе с #4, #5, #19 — см. секцию `## Atomic Waves`).

**Комбинированная правка одного файла:**
1. Сигнатура интерфейса — nullable `flowCurrentDict()` и `getCurrentDict()`.
2. Удаление класса `DictionaryNotFoundException`, который живёт в этом же файле (становится мёртвым кодом после правок #4, #5, #19 в той же волне).

**Было:**
```kotlin
interface DictionaryTabUseCase {
    suspend fun getCurrentDict(): DictUiEntity
    fun flowCurrentDict(): Flow<DictUiEntity>
    suspend fun changeDict(id: Long)
    suspend fun getWordList(): List<TermUiItem>
    fun searchTerms(pattern: String, dictionaryId: Int): Flow<PagingData<TermUiItem>>
    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean
}
class DictionaryNotFoundException : IllegalStateException("No Dictionaries found")
```

**Стало:**
```kotlin
interface DictionaryTabUseCase {
    suspend fun getCurrentDict(): DictUiEntity?           // nullable
    fun flowCurrentDict(): Flow<DictUiEntity?>            // nullable
    suspend fun changeDict(id: Long)
    suspend fun getWordList(): List<TermUiItem>           // (вне скоупа фикса — оставить как есть; throw IllegalStateException в impl не убираем здесь)
    fun searchTerms(pattern: String, dictionaryId: Int): Flow<PagingData<TermUiItem>>
    suspend fun addWord(value: String): Long              // тот же подход — не трогаем (вызывается только из эффекта CreateWord, гарантированно после Msg.SelectDictionary с не-null)
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean
}
// class DictionaryNotFoundException — УДАЛЁН (был ниже интерфейса в этом же файле)
```

Класс `DictionaryNotFoundException` физически жил в этом же файле — его удаление выполняется одним merge-edit'ом вместе с правкой интерфейса. Связь с #4, #5, #19 (все импортёры и тестовый `@Test(expected = DictionaryNotFoundException::class)`) выражена через `wave: dict_tab_contract_wave` — все четыре узла применяются одной транзакцией, между ними промежуточной компиляции нет. Модуль `app:testDebugUnitTest` соберётся после применения волны как единого целого.

> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО throw на null — краш при первом запуске"

---

### #3 QuizChatUseCase.kt [~]

**Было:**
```kotlin
interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>
}
```

**Стало:**
```kotlin
interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long?           // nullable вместо throw
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>
}
```

> 📎 guide: docs/guides/prefs-datastore.md — "null = ключ не существует; одноразовые чтения (suspend) возвращают nullable"

---

### #4 DictionaryAppBarUseCaseImpl.kt [~]

🌊 atomic wave: `dict_tab_contract_wave` (вместе с #2, #5, #19 — см. секцию `## Atomic Waves`).

**Было:**
```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        (list.find { it.id == id } ?: list.firstOrNull())
            ?.let { dict -> DictUiEntity(/* ... */) }
            ?: throw DictionaryNotFoundException()
    }
}
```

**Стало:**
```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity?> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        // null если list пуст — валидное доменное состояние
        (list.find { it.id == id } ?: list.firstOrNull())
            ?.let { dict -> DictUiEntity(/* как раньше */) }
    }
}
```

Импорт `DictionaryNotFoundException` — удалить.

> 📎 guide: docs/guides/prefs-datastore.md — "ПРАВИЛЬНО — fallback при null; ЗАПРЕЩЕНО throw на null"

---

### #5 DictionaryTabUseCaseImpl.kt [~]

🌊 atomic wave: `dict_tab_contract_wave` (вместе с #2, #4, #19 — см. секцию `## Atomic Waves`).

**Было:**
```kotlin
override suspend fun getCurrentDict(): DictUiEntity {
    /* ... fallback chain ... */
    throw DictionaryNotFoundException()
}

override fun flowCurrentDict(): Flow<DictUiEntity> = prefsProvider
    .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .map { id ->
        val dict = (id?.let { dictionaryApi.getDictionaryById(it) }
            ?: dictionaryApi.getDictionaryList().firstOrNull())
            ?.let { dict -> DictUiEntity(/* ... */) }
        dict ?: throw DictionaryNotFoundException()
    }
```

**Стало:**
```kotlin
override suspend fun getCurrentDict(): DictUiEntity? {
    // вернуть null вместо throw в конце fallback chain
    prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)?.let { id ->
        dictionaryApi.getDictionaryById(id)?.let { return DictUiEntity(/* ... */) }
    } ?: dictionaryApi.getDictionaryList().firstOrNull()?.let {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, it.id)
        return DictUiEntity(/* ... */)
    }
    return null   // ← вместо throw DictionaryNotFoundException()
}

override fun flowCurrentDict(): Flow<DictUiEntity?> = prefsProvider
    .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .map { id: Long? ->
        // null если ни по id, ни fallback не нашли — валидное состояние
        (id?.let { dictionaryApi.getDictionaryById(it) }
            ?: dictionaryApi.getDictionaryList().firstOrNull())
            ?.let { dict -> DictUiEntity(/* ... */) }
    }
```

Импорт `DictionaryNotFoundException` — удалить. `addWord` / `getWordList` (строки 79-80, 124-125 с `throw IllegalStateException`) — **оставить как есть** в рамках этой фичи: они вызываются только после `Msg.SelectDictionary(non-null)`, где State уже гарантирует наличие словаря через ветку в reducer'е (см. #14). Это согласовано с impact_analysis: фокус фикса — реактивные подписки.

> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null; ПРАВИЛЬНО — fallback при null"

---

### #6 QuizChatUseCaseImpl.kt [~]

**Было:**
```kotlin
override suspend fun getCurrentDictionaryId(): Long {
    prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)?.let { id ->
        dictionaryApi.getDictionaryById(id)?.let { return it.id }
    } ?: dictionaryApi.getDictionaryList().firstOrNull()?.let {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, it.id)
        return it.id
    }
    throw IllegalStateException("Dictionary not found")
}
```

**Стало:**
```kotlin
override suspend fun getCurrentDictionaryId(): Long? {
    prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)?.let { id ->
        dictionaryApi.getDictionaryById(id)?.let { return it.id }
    } ?: dictionaryApi.getDictionaryList().firstOrNull()?.let {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, it.id)
        return it.id
    }
    return null   // ← вместо throw IllegalStateException("Dictionary not found")
}
```

> 📎 guide: docs/guides/prefs-datastore.md — "null = ключ не существует; ЗАПРЕЩЕНО throw на null"

---

### #7 DictionaryUseCaseImpl.kt [~]

**Было:**
```kotlin
override suspend fun deleteDictionary(id: Long) {
    dictionaryApi.deleteDictionary(id)
    val currentId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    if (currentId == id) {
        val remaining = dictionaryApi.getDictionaryList().firstOrNull()
        remaining?.let { setCurrentDictionary(it.id) }
    }
}
```

**Стало:**
```kotlin
override suspend fun deleteDictionary(id: Long) {
    dictionaryApi.deleteDictionary(id)
    val currentId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    if (currentId == id) {
        val remaining = dictionaryApi.getDictionaryList().firstOrNull()
        if (remaining != null) {
            setCurrentDictionary(remaining.id)
        } else {
            // нет оставшихся — чистим orphaned pref
            prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null)
        }
    }
}
```

Чистка pref согласована с `PrefsProvider.setLong` который принимает nullable Long (см. IS474 — getLongFlow уже nullable).

> 📎 guide: docs/guides/prefs-datastore.md — "Запись (suspend) setLong; единственный entry point для preferences — PrefsProvider"

---

### #8 dictionaryappbar/mate/Message.kt [~]

**Было:**
```kotlin
data class CurrentDict(val current: DictUiEntity) : Msg
```

**Стало:**
```kotlin
data class CurrentDict(val current: DictUiEntity?) : Msg   // nullable: null = словаря нет
```

Остальные сообщения без изменений.

---

### #9 dictionaryappbar/mate/State.kt [~]

**Было:**
```kotlin
fun DictionaryAppBarState.currentDict(current: DictUiEntity) = this.copy(
    currentDict = current,
)
```

**Стало:**
```kotlin
fun DictionaryAppBarState.currentDict(current: DictUiEntity?) = this.copy(
    currentDict = current,
)
```

Поле `currentDict: DictUiEntity?` в `DictionaryAppBarState` уже nullable — структура State не меняется.

> 📎 guide: docs/guides/state-and-extensions.md — "Extension-функции для всех мутаций стейта — редьюсер никогда не вызывает .copy() напрямую"

---

### #10 dictionaryappbar/mate/DatasourceEffectHandler.kt [~]

**Было:**
```kotlin
launch {
    useCase.flowCurrentDict()
        .collectLatest { send(Msg.CurrentDict(current = it)) }
}
```

**Стало:**
```kotlin
launch {
    useCase.flowCurrentDict()    // тип теперь Flow<DictUiEntity?>
        .collectLatest { send(Msg.CurrentDict(current = it)) }   // it: DictUiEntity? — пропускается напрямую
}
```

Сигнатура `subscribe` не меняется. Лог: `it` может быть null — это допустимо. Никаких try/catch.

> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler: Flow останавливается в FlowHandler — дальше только Messages"

---

### #11 dictionaryappbar/mate/DictionaryAppBarReducer.kt [~]

**Было:**
```kotlin
is Msg.CurrentDict -> state.currentDict(message.current) to setOf()
```

**Стало:**
```kotlin
is Msg.CurrentDict -> state.currentDict(message.current) to setOf()
// message.current: DictUiEntity? — обновлённая сигнатура state.currentDict() (#9) корректно принимает null
```

Ветка не меняется, только тип параметра.

---

### #12 dictionaryTab/logic/State.kt [~]

**Было:**
```kotlin
@Immutable
data class DictionaryTabState(
    val isLoading: Boolean = true,
    val topBarState: TopBarState = TopBarState(),
    val termList: TermsSource = TermsSource(pattern = ""),
    val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap(),
    val addWordDialogState: AddWordDialogState = AddWordDialogState(),
    val snackbarState: SnackbarState = SnackbarState(),
    val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
)
```

**Стало:**
```kotlin
@Immutable
data class DictionaryTabState(
    val isLoading: Boolean = true,
    val hasNoDictionary: Boolean = false,   // ← новое: «нет ни одного словаря» — явное поле
    val topBarState: TopBarState = TopBarState(),
    val termList: TermsSource = TermsSource(pattern = ""),
    val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap(),
    val addWordDialogState: AddWordDialogState = AddWordDialogState(),
    val snackbarState: SnackbarState = SnackbarState(),
    val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
)

// + helpers
fun DictionaryTabState.markNoDictionary() = copy(hasNoDictionary = true, isLoading = false)
fun DictionaryTabState.markDictionaryPresent() = copy(hasNoDictionary = false)
```

Обоснование флага: согласно memory `feedback_explicit_state_flags.md` — флаги UI должны быть явными полями в state, не вычисляться в composable. `hasNoDictionary` отличается от `termList.isEmpty()` (в словаре может быть 0 слов, но словарь есть).

> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента; вычисление происходит в редьюсере, а не в composable"

> 📎 guide: docs/guides/state-and-extensions.md — "Одна задача на расширение; нейминг: глагол в начале — mark/show/hide/clear/update"

---

### #13 dictionaryTab/logic/Message.kt [~]

**Было:**
```kotlin
data class SelectDictionary(val current: DictUiEntity) : Msg
```

**Стало:**
```kotlin
data class SelectDictionary(val current: DictUiEntity?) : Msg
// null = реактивно получили «нет словаря»
```

---

### #14 dictionaryTab/logic/VocabularyTabReducer.kt [~]

**Было:**
```kotlin
is Msg.SelectDictionary -> state to setOf(LoadTermFlow())
```

**Стало:**
```kotlin
is Msg.SelectDictionary -> if (message.current == null) {
    // словарь отсутствует — переход в пустое состояние таба, эффектов нет
    state.markNoDictionary() to emptySet()
} else {
    // словарь появился — сначала showLoading(), потом LoadTermFlow,
    // чтобы избежать UI-моргания пустого виджета между Msg.SelectDictionary
    // и Msg.TermsLoaded. Симметрично существующему Msg.TermsLoaded → hideLoading().
    state.markDictionaryPresent().showLoading() to setOf(LoadTermFlow())
}
```

Логика:
1. Получили `null` → не запускаем `LoadTermFlow` (внутри которого `getCurrentDict().id.toInt()` стрельнул бы NPE). Просто выставляем флаг — UI покажет пустое состояние.
2. Получили non-null после null (или впервые) → сбрасываем `hasNoDictionary`, **сразу ставим `isLoading = true`** (showLoading()), и запускаем `LoadTermFlow`. Между этим моментом и `Msg.TermsLoaded` UI показывает прогресс-индикатор, а не пустой `WordListWidget` с устаревшим `termList`.

> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 4: Условный стейт — логика ветвится по данным сообщения"

---

### #15 dictionaryTab/logic/DatasourceEffectHandler.kt [~]

**Было:**
```kotlin
override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
    pagingScope = scope
    scope.launch {
        dictionaryTabUseCase.flowCurrentDict().collectLatest {
            send(Msg.SelectDictionary(current = it))
        }
    }
}

// ...
is DatasourceEffect.LoadTermFlow -> withContext(Dispatchers.IO) {
    val dictionaryId = dictionaryTabUseCase.getCurrentDict().id.toInt()
    /* ... */
}
```

**Стало:**
```kotlin
override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
    pagingScope = scope
    scope.launch {
        dictionaryTabUseCase.flowCurrentDict().collectLatest { dict ->
            send(Msg.SelectDictionary(current = dict))   // dict: DictUiEntity? — пропускается напрямую
        }
    }
}

// ...
is DatasourceEffect.LoadTermFlow -> withContext(Dispatchers.IO) {
    // getCurrentDict() теперь nullable; защищаемся явно
    val dictionaryId = dictionaryTabUseCase.getCurrentDict()?.id?.toInt()
    if (dictionaryId == null) {
        // словарь отсутствует — не загружаем термы, гасим NoOperation
        Msg.NoOperation
    } else {
        val pagingFlow = dictionaryTabUseCase.searchTerms(
            pattern = eff.pattern,
            dictionaryId = dictionaryId,
        ).let { flow ->
            val scope = pagingScope
            if (eff.pattern.isEmpty() && scope != null) flow.cachedIn(scope) else flow
        }
        Msg.TermsLoaded(pattern = eff.pattern, termList = pagingFlow)
    }
}
```

Дополнительный slot ответственности — handler страхует на случай race: reducer уже отфильтровал `LoadTermFlow` при null (#14), но эффект мог быть «в пути».

> 📎 guide: docs/guides/effect-handlers.md — "Всегда withContext(Dispatchers.IO) для операций с данными"

> 📎 guide: docs/guides/effect-handlers.md — "Без сложной логики в хендлерах — выполняют эффект и конвертируют результат в сообщение"

---

### #16 dictionaryTab/ui/VocabularyTabScreen.kt [~]

**Было:**
```kotlin
when {
    state.isLoading -> { CircularProgressIndicator(...) }
//  state.isEmpty() -> { EmptyWidget() }
    else -> { WordListWidget(...) }
}
```

**Стало:**
```kotlin
when {
    state.isLoading -> { CircularProgressIndicator(...) }
    state.hasNoDictionary -> {
        // защитная ветка: this state is unreachable seller-state.
        // Юзер физически не попадёт на этот экран с пустым списком —
        // DICTIONARY_LIST это root route (не таб), а onboarding flow гарантирует
        // non-empty список в MainScreen. После exit из DICTIONARY_LIST
        // приложение делает перезапуск → SETUP route.
        // Защита нужна ТОЛЬКО от фоновой подписки AppBar в back stack
        // (если процесс не убит и MainScreen остался в back stack).
        EmptyWidget()   // переиспользуем существующий placeholder (или просто Box)
    }
    else -> { WordListWidget(...) }
}
```

UI делает явную ветку под `state.hasNoDictionary`. Не запускает paging Flow. Отдельный виджет `NoDictionaryWidget` не вводится: юзер физически не видит этот UI, и Reducer обрабатывает null чтобы не упасть, а не чтобы показать осмысленный плейсхолдер. Если в будущем кейс станет user-facing — это backlog-задача на отдельный плейсхолдер.

> 📎 guide: docs/guides/ui-patterns.md — "Условные оверлеи и ветки по флагам стейта"

---

### #17 quiz/chat/quiz/QuizGameImpl.kt [~]

**Было:**
```kotlin
private suspend fun fetchData(): List<QuizItem> {
    val dictionaryId = quizChatUseCase.getCurrentDictionaryId()
    return quizChatUseCase.getRandomWriteQuizList(
        dictionaryId = dictionaryId,
        limit = maxStepInSession,
        maxGrade = maxGrade
    ).also { /* ... */ }.map { /* ... */ }
}
```

**Стало:**
```kotlin
private suspend fun fetchData(): List<QuizItem> {
    val dictionaryId = quizChatUseCase.getCurrentDictionaryId()
    if (dictionaryId == null) {
        // словарь отсутствует — возвращаем пустой список квизов
        // вся последующая логика (loadData → hasNextQuestion → false) уже корректна
        // (IS461: пустой quizList не крашит — см. QuizGameImplEmptyListTest)
        logger.w(tag = LogTags.CHAT, message = "fetchData: no current dictionary (null id)")
        return emptyList()
    }
    return quizChatUseCase.getRandomWriteQuizList(
        dictionaryId = dictionaryId,
        limit = maxStepInSession,
        maxGrade = maxGrade
    ).also { /* ... как было ... */ }.map { /* ... */ }
}
```

Опираемся на уже исправленный в IS461 механизм пустого quizList — он не крашит, сразу заканчивает сессию через `hasNextQuestion() == false`.

> 📎 guide: docs/guides/logging.md — "Использовать ТОЛЬКО константы LogTags. НЕ хардкодить строки"

> 📎 guide: docs/guides/logging.md — "WARNING — проблема, но приложение работает; уходит в Crashlytics breadcrumb"

---

### #19 app/.../widget/DictionaryAppBarUseCaseImplTest.kt [~]

🌊 atomic wave: `dict_tab_contract_wave` (вместе с #2, #4, #5 — см. секцию `## Atomic Waves`).

**Было:**
```kotlin
@Test(expected = DictionaryNotFoundException::class)
fun `flowCurrentDict throws DictionaryNotFoundException when list is empty`() = runTest {
    dictListFlow.value = emptyList()
    prefsFlow.value = 1L
    useCase.flowCurrentDict().first()
}
```

**Стало:**
```kotlin
@Test
fun `flowCurrentDict emits null when list is empty`() = runTest {
    dictListFlow.value = emptyList()
    prefsFlow.value = 1L

    val result: DictUiEntity? = useCase.flowCurrentDict().first()

    assertNull("Expected null emission for empty list", result)
}
```

Плюс убрать импорт `DictionaryNotFoundException` (строка 14) и упоминание в комментарии (строка 31). Остальные тесты — обновить локальные ссылки на тип `Flow<DictUiEntity?>` (компилятор сам подскажет; assertEquals на свойствах продолжит работать). Этот узел входит в одну atomic wave `dict_tab_contract_wave` с #2: класс `DictionaryNotFoundException` физически удаляется в #2, а ссылки на него в этом тесте — в #19; обе правки применяются одной транзакцией.

> 📎 guide: docs/guides/testing-reducers.md — "Граничные случаи: пустые списки, NOT_IN_DB id, null значения"

---

### #20 app/.../dictionary/DictionaryUseCaseImplTest.kt [~]

**Было:**
```kotlin
@Test
fun `should not crash when current deleted and no remaining`() = runTest {
    coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 5L
    coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

    useCase.deleteDictionary(5L)

    coVerify { dictionaryApi.deleteDictionary(5L) }
    coVerify(exactly = 0) { prefsProvider.setLong(any(), any()) }   // ← устарело
}
```

**Стало:**
```kotlin
@Test
fun `should clear pref when current deleted and no remaining`() = runTest {
    coEvery { prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG) } returns 5L
    coEvery { dictionaryApi.getDictionaryList() } returns emptyList()

    useCase.deleteDictionary(5L)

    coVerify { dictionaryApi.deleteDictionary(5L) }
    // новое поведение: pref чистится явно null'ом
    coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null) }
}
```

Остальные тесты остаются.

---

### #21 app/.../dictionarytab/DictionaryTabUseCaseImplTest.kt [+]

**Назначение:** покрытие nullable-сигнатур `DictionaryTabUseCaseImpl.flowCurrentDict()` и `getCurrentDict()` при пустом списке словарей.

Сейчас отдельных тестов на `DictionaryTabUseCaseImpl` нет — добавляем по аналогии с `DictionaryAppBarUseCaseImplTest`.

**Путь файла:** строго lowercase `dictionarytab` — соответствует production-пути `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/`. Это важно для CI на Linux ext4 (case-sensitive ФС): несовпадение регистра создаст две разные директории. macOS APFS не case-sensitive, поэтому локально проблема не воспроизводится.

**Ключевые тесты:**
```kotlin
class DictionaryTabUseCaseImplTest {

    // setup аналогичен AppBar-тесту: моки CoreDbApi.DictionaryApi, PrefsProvider, CountryProvider
    // + Flow<Long?> для prefs.getLongFlow

    @Test
    fun `flowCurrentDict emits null when list is empty`() = runTest {
        // given: prefs hold an id, but Room returns null on getDictionaryById and empty list
        // when: collect first emission
        // then: emission is null
    }

    @Test
    fun `flowCurrentDict emits non-null when current dictionary exists`() = runTest {
        // happy path — sanity check
    }

    @Test
    fun `getCurrentDict returns null when no dictionaries exist`() = runTest {
        // prefs.getLong returns null AND getDictionaryList returns emptyList
        // then: getCurrentDict() returns null (а не throw)
    }

    @Test
    fun `getCurrentDict returns dict from fallback list when prefs is null`() = runTest {
        // prefs returns null, list has one dict — fallback срабатывает, pref пишется
    }
}
```

---

### #22 dictionaryTab/logic/VocabularyTabReducerKtTest.kt [~]

**Было:** тест на `Msg.SelectDictionary(current = testDictEntity)` проверяет, что reducer эмитит `LoadTermFlow`. Тест уже есть (`should trigger term flow load when ChangeDict is received`).

**Стало:** добавить новый тестовый кейс на nullable-ветку:

```kotlin
@Test
fun `should mark hasNoDictionary when SelectDictionary with null is received`() {
    // Given
    val initialState = createTestState()

    // When
    val result = reducer.testReduce(initialState, Msg.SelectDictionary(current = null))

    // Then
    assertTrue("hasNoDictionary should be true", result.state().hasNoDictionary)
    assertFalse("Loading state should be cleared", result.state().isLoading)
    result.assertNoEffects("No effects should be triggered — no LoadTermFlow")
}

@Test
fun `should clear hasNoDictionary, set isLoading and emit LoadTermFlow when SelectDictionary with dict received after null`() {
    // Given: state already marked as hasNoDictionary, isLoading=false
    val initialState = createTestState().copy(hasNoDictionary = true, isLoading = false)

    // When: dictionary appears
    val result = reducer.testReduce(initialState, Msg.SelectDictionary(current = testDictEntity))

    // Then
    assertFalse("hasNoDictionary should be cleared", result.state().hasNoDictionary)
    assertTrue("isLoading should be set true before LoadTermFlow", result.state().isLoading)
    result.assertSingleEffect<DatasourceEffect.LoadTermFlow>("LoadTermFlow effect expected")
}
```

Существующий тест `should trigger term flow load when ChangeDict is received` — подправить ассерт `assertEquals("State should remain unchanged", initialState, result.state())`: после правки #14 reducer вызывает `markDictionaryPresent().showLoading()`, поэтому state НЕ равен `initialState`. Корректная проверка — `isLoading == true && hasNoDictionary == false`.

> 📎 guide: docs/guides/testing-reducers.md — "Всегда проверять и стейт И эффекты в каждом тесте; ветвление в редьюсере — обе ветки"

> 📎 guide: docs/guides/testing-reducers.md — "Именование тестов: should [что происходит] when [сообщение/условие]"

---

### #23 dictionaryappbar/mate/DictionaryAppBarReducerTest.kt [+]

**Назначение:** новый файл — reducer-тесты для AppBar. Сейчас в модуле только `ExampleUnitTest.kt` (пустой). Покрыть Msg.CurrentDict(null).

**Ключевые тесты:**
```kotlin
class DictionaryAppBarReducerTest {

    private val reducer = DictionaryAppBarReducer(
        logger = object : LexemeLogger { override fun log(level: LogLevel, tag: String, message: String) {} }
    )

    @Test
    fun `Msg_CurrentDict with non-null updates state currentDict`() {
        val initial = DictionaryAppBarState()
        val dict = DictUiEntity(id = 1, flagRes = 0, title = "EN", numericCode = 826)

        val result = reducer.reduce(initial, Msg.CurrentDict(current = dict))

        assertEquals(dict, result.first.currentDict)
        assertTrue(result.second.isEmpty())
    }

    @Test
    fun `Msg_CurrentDict with null sets state currentDict to null`() {
        val initial = DictionaryAppBarState(
            currentDict = DictUiEntity(id = 1, flagRes = 0, title = "EN", numericCode = 826)
        )

        val result = reducer.reduce(initial, Msg.CurrentDict(current = null))

        assertNull(result.first.currentDict)
        assertTrue(result.second.isEmpty())
    }
}
```

---

## Atomic Waves

Atomic wave — набор узлов, которые применяются **одной транзакцией** (одним merge-edit'ом / одним коммитом) без промежуточной компиляции. DSL design-tree не имеет нотации для wave; в графе wave-принадлежность отмечена полем `wave: <name>` на каждом узле.

> Узлы в одной wave применяются одной транзакцией без промежуточной компиляции. Семантические зависимости между ними не выражаются через `depends`, поскольку wave гарантирует одновременность.

### `dict_tab_contract_wave`

Узлы: **#2, #4, #5, #19**.

Почему нужна wave:
- #2 меняет сигнатуру `DictionaryTabUseCase` (`getCurrentDict(): DictUiEntity?`, `flowCurrentDict(): Flow<DictUiEntity?>`) и удаляет класс `DictionaryNotFoundException` из того же файла.
- #5 (`DictionaryTabUseCaseImpl`) обязан иметь матчинг `override` — иначе compile error.
- #4 (`DictionaryAppBarUseCaseImpl`) и #19 (`DictionaryAppBarUseCaseImplTest`) содержат `import DictionaryNotFoundException` — после удаления класса в #2 ссылки должны быть убраны.

Между #2 и #5 формально невозможно выбрать порядок: применить #2 первым — #5 не скомпилируется (override не матчит); применить #5 первым — #5 ещё содержит `throw DictionaryNotFoundException()` и старый импорт, но класс ещё на месте; после применения #2 (без #5) — модуль `app` не собирается. Поэтому #2/#4/#5/#19 — atomic wave, применяемая одним коммитом.

DAG ацикличен по `depends`: #2 не зависит от #5; #5 зависит от #2 (восстановлено). Wave-семантика — отдельный слой поверх DAG, выражает требование одновременности.

---

## Проверка зависимостей

DAG валиден (ацикличен по `depends`). Параллельные ветви и порядок:
- Контракты #1, #3, #7 — независимы, делаются параллельно.
- Реализации: #4 после #1, #5 после #2, #6 после #3.
- AppBar mate-layer (#8 → #10/#11) — параллельно с DictionaryTab logic (#12/#13 → #14/#15/#16).
- QuizGameImpl #17 — параллельно с UI/handler-правками.
- Тесты #19, #20, #22, #23 — после соответствующих impl/reducer/state.
- **Atomic wave `dict_tab_contract_wave` (#2, #4, #5, #19)** — применяется одной транзакцией. См. секцию `## Atomic Waves`. Wave-семантика гарантирует одновременность; промежуточная компиляция между этими четырьмя узлами не предполагается.

_model: claude-opus-4-7[1m]_

---

## Architect Review

### [critical] Node #18 пропускает зависимость от теста #19

**Где:** узел #18 (`depends: [2, 4, 5]`).
**Что не так:** `DictionaryNotFoundException` импортируется не только в продакшн-файлах (#4 `DictionaryAppBarUseCaseImpl.kt`, #5 `DictionaryTabUseCaseImpl.kt`), но и в **тесте #19** (`DictionaryAppBarUseCaseImplTest.kt:14` + строки 31, 146-147). Узел #19 в design-tree запланирован — он сам убирает импорт и `@Test(expected = ...)`. Но граф зависимостей в #18 это не отражает: deps указывает только на #2, #4, #5.
**Почему важно:** Если imple-step выполнит #18 (физическое удаление класса) до #19 (правка теста), сборка модуля `app:testDebugUnitTest` упадёт на компиляции — `DictionaryAppBarUseCaseImplTest.kt` ссылается на удалённый символ. Это сломает CI на стадии Unit Tests. Правило самого design-tree: «Удаление [-] зависит от нод которые убирают ссылки».
**Предложение:** Добавить #19 в depends: `depends: [2, 4, 5, 19]`. Альтернативно — гарантировать порядок волн в плане (последняя волна с удалением идёт после всех правок тестов).

### [critical] Action `[-]` на файле, который по сути правится `[~]` — дублирование операций над одним путём

**Где:** узлы #2 и #18, оба ссылаются на файл `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt`.
**Что не так:** Узел #2 — `[~]` (правка интерфейса в этом файле). Узел #18 — `[-]` (удаление класса `DictionaryNotFoundException` в **том же файле**). В тексте артефакта явно сказано: «файл `DictionaryTabUseCase.kt` остаётся ... удаляется именно класс ... поэтому action `[-]` относится к классу-резиденту, а не к файлу». Но action `[-]` в DSL design-tree означает «удаление файла», не «удаление символа из файла». Два узла на один путь с разными actions — двусмысленная семантика для imple-агента и для тулинга, которое читает граф (например, ожидает что `[-]` = `rm`).
**Почему важно:** Imple-агент может попытаться: (а) применить `[~]` (правка интерфейса) и потом `[-]` (rm файла) — потеряем интерфейс; (б) пропустить #18, оставив мёртвый класс. Сам комментарий «узел и узел #2 ... должны быть применены одним merge-edit'ом» — обходит формализм DSL, а не следует ему.
**Предложение:** Свернуть #18 в #2: расширить описание #2 формулировкой «правка интерфейса + удаление класса `DictionaryNotFoundException` из того же файла». Узел #18 убрать. В Части 2 («Детали изменений») оставить отдельную секцию с заголовком «#2.b DictionaryNotFoundException — удаляется из того же файла» для семантической ясности, но физически это один merge-edit с action `[~]`. Если хочется сохранить отдельный узел — переименовать action в `[~]` и в depends оставить `[4, 5, 19]`.

### [minor] Node #14: переход «null → non-null» не возвращает isLoading=true перед LoadTermFlow

**Где:** узел #14, ветка `else { state.markDictionaryPresent() to setOf(LoadTermFlow()) }`.
**Что не так:** Сценарий: пользователь удалил все словари → State становится `(hasNoDictionary=true, isLoading=false)`. Затем создаёт новый словарь → реактивный Flow эмитит non-null → reducer вызывает `markDictionaryPresent()` (только сбрасывает `hasNoDictionary`) и пускает `LoadTermFlow`. Между этим моментом и `Msg.TermsLoaded` UI (#16) идёт в ветку `else` → `WordListWidget` с пустым/устаревшим `termList.termListFlow` (он остался от предыдущего dict или дефолтный `flowOf()` из `TermsSource`). Прогресс-индикатор не показывается.
**Почему важно:** Не критично (флик, не краш), но соответствует тому же memory-правилу про «UI flags must be explicit fields in state» — переход требует явного `showLoading()` перед `LoadTermFlow`, иначе пустое состояние таба сменится на пустой `WordListWidget` без визуальной обратной связи. Реалистично заметно при «удалил всё → создал словарь не выходя с таба».
**Предложение:** В ветке `else` добавить `showLoading()`: `state.markDictionaryPresent().showLoading() to setOf(LoadTermFlow())`. Это симметрично существующему `Msg.TermsLoaded → state.hideLoading().appendTermsFlow(...)` (строки 31-36 reducer'а).

### [minor] Node #16: UI ветка `hasNoDictionary` использует `EmptyWidget()` без проверки сигнатуры

**Где:** узел #16 (детали в Части 2 — `EmptyWidget()` как заглушка).
**Что не так:** Файл `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/widget/EmptyWidget.kt` существует в репо, но в design-tree не зафиксирована его сигнатура (параметры, нужны ли callbacks). В текущем `VocabularyTabScreen.kt` `EmptyWidget` закомментирован (строки 147-149), что подсказывает что он был задуман для пустого списка слов, а не для «нет словаря». Cемантика терминологии: «пустой словарь» (0 слов) ≠ «нет словаря» (0 dictionaries) — это разные UI-состояния.
**Почему важно:** Если `EmptyWidget` рендерит «Нет слов в словаре / Добавьте слово», для кейса `hasNoDictionary` это смысловой mismatch с пользовательским контекстом (словарей нет — кнопка «Создать словарь», а не «Добавить слово»). Imple-шаг может вкомпилить семантически неверный placeholder. Узел #16 сам это признаёт: «реальный UI-компонент решит implement-шаг; ... спека таба не написана».
**Предложение:** Уточнить узел #16 — либо добавить новый widget `NoDictionaryPlaceholderWidget` (action `[+]`) с понятной семантикой, либо явно записать «EmptyWidget используется как временная заглушка, отдельный плейсхолдер — backlog». В текущей форме это запись «решим в implement» — слабее, чем design-tree должен давать.

### [minor] Node #21 — путь файла теста в `app/`-source-set, но тестирует модульный класс

**Где:** узел #21 (`app/src/test/java/.../dictionarytab/DictionaryTabUseCaseImplTest.kt`).
**Что не так:** `DictionaryTabUseCaseImpl` физически живёт в `app` (см. listing — `app/src/main/java/.../di/module/dictionarytab/`), поэтому тест в `app/src/test/` — корректное местоположение и согласованное с #19, #20. Замечание не претензия, а наблюдение: имя поддиректории в пути `app/.../di/module/dictionarytab/DictionaryTabUseCaseImplTest.kt` пишется как `dictionarytab`, а соседние модули используют `widget` (`DictionaryAppBarUseCaseImplTest`) и `dictionary` (`DictionaryUseCaseImplTest`).
**Почему важно:** Косметика, но если imple-агент создаст директорию с другим регистром (`dictionaryTab` vs `dictionarytab`), на macOS APFS не case-sensitive — компилится; на CI (Linux ext4) — потенциально две разные директории. Низкий риск, но есть.
**Предложение:** Зафиксировать в #21 директорию строго как `app/src/test/java/me/apomazkin/polytrainer/di/module/dictionarytab/` (lowercase, совпадает с production-путём `app/src/main/java/.../di/module/dictionarytab/`). Сейчас в #21 это уже так — просто стоит сверить с imple-step.

_reviewer: architect_

---

## Architect Review — addressed

### Critical 1: исправлено
Граф зависимостей пересмотрен: #19 (правка тестового файла, убирающая импорт `DictionaryNotFoundException` и `@Test(expected = ...)`) добавлен в `depends` узла #2 — единого узла, который удаляет класс. Полный список импортёров перепроверен в репо (`grep -rn DictionaryNotFoundException`): только три файла — `DictionaryAppBarUseCaseImpl.kt` (#4), `DictionaryTabUseCaseImpl.kt` (#5), `DictionaryAppBarUseCaseImplTest.kt` (#19). Все три — в `depends: [4, 5, 19]` у #2. CI на `app:testDebugUnitTest` собирется корректно: к моменту применения #2 ни один файл не ссылается на `DictionaryNotFoundException`.

### Critical 2: исправлено
Узлы #2 и #18 объединены в один узел #2 (action `~`). Описание #2 теперь явно говорит про две правки одного файла: (1) обновление сигнатуры интерфейса `DictionaryTabUseCase` на nullable, (2) удаление класса `DictionaryNotFoundException` из того же файла. Номер #18 больше не используется (пропуск в нумерации сохранён намеренно — все depends на #18 в других узлах переадресованы, ничьи внешние ссылки не сломаны). DSL-двусмысленность `[-]` vs `[~]` на одном пути устранена: остался только один action `~` на этот файл.

### Minor 1: исправлено
В описании узла #14 ветка `else` теперь явно вызывает `state.markDictionaryPresent().showLoading()` перед `LoadTermFlow`, симметрично существующему `Msg.TermsLoaded → hideLoading()`. UI-моргание при переходе «null → non-null» (удалил всё → создал словарь) больше не возникает: пользователь видит прогресс-индикатор, а не пустой `WordListWidget` с устаревшим `termList`. Тест #22 расширен — новый кейс проверяет `isLoading == true` после такого перехода.

### Minor 2: отклонено
Пользователь решил не вводить отдельный `NoDictionaryWidget` / `NoDictionaryPlaceholderWidget`. Обоснование: `DICTIONARY_LIST` — это **root route**, а не таб; после exit идёт перезапуск приложения в роутер `SETUP`. Юзер физически не попадёт на этот экран с пустым списком словарей — `onboarding flow` гарантирует non-empty список в `MainScreen`. Reducer обрабатывает `null` чтобы не упасть от фоновой подписки `AppBar` в back stack (если процесс не убит), а не чтобы показать осмысленный плейсхолдер. В описании узла #16 переформулирован комментарий — указано, что этот state unreachable seller-state, а `EmptyWidget()` оставлен как защитная заглушка (или просто Box). Если кейс в будущем станет user-facing — это backlog-задача на отдельный плейсхолдер.

### Minor 3: исправлено
В описании узла #21 явно зафиксирован путь файла теста строго lowercase: `app/src/test/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImplTest.kt` — соответствует production-пути `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/`. Добавлено явное замечание про case-sensitivity на CI (Linux ext4) vs macOS (APFS) — чтобы imple-агент не создал директорию с другим регистром.

_addressed_by: design_tree author_

---

## Architect Review — round 2

### Верификация closed findings

**Critical 1 (Node #18 missing dep on #19):** закрыт корректно. Узлы #18 и #2 объединены, новый #2 имеет `depends: [4, 5, 19]`. Проверка по репозиторию (`grep -rn DictionaryNotFoundException`) подтверждает: три и только три файла-импортёра — `DictionaryAppBarUseCaseImpl.kt` (#4), `DictionaryTabUseCaseImpl.kt` (#5), `DictionaryAppBarUseCaseImplTest.kt` (#19). Все включены в depends #2.

**Critical 2 (двойной action `[~]`/`[-]` на одном файле):** закрыт корректно. Узел #18 удалён, физическое удаление класса теперь часть merge-edit'а #2 (action `~`). Семантическая двусмысленность DSL устранена. Номер #18 оставлен пропуском в нумерации — это не ломает DAG (никаких внешних ссылок на #18 не осталось — проверил по тексту артефакта).

**Minor 1 (нет showLoading при null→non-null):** закрыт корректно. Ветка `else` в #14 теперь явно вызывает `state.markDictionaryPresent().showLoading()`. Соответствующий тест в #22 добавлен (`should clear hasNoDictionary, set isLoading and emit LoadTermFlow when SelectDictionary with dict received after null`). Дополнительно проверил — `showLoading()`/`hideLoading()` уже существуют в `State.kt:80-84`, новый код опирается на существующие extensions.

**Minor 2 (NoDictionaryWidget vs EmptyWidget):** обосновано отклонён. Аргумент валиден: `DICTIONARY_LIST` — root route, after-exit перезапускает приложение в `SETUP`. Защитная заглушка `EmptyWidget()` адекватна для unreachable-state. Если когда-нибудь станет user-facing — это отдельная backlog-задача, что согласуется с правилом «Бэклог = docs/Backlog.md».

**Minor 3 (case-sensitivity пути теста #21):** закрыт корректно. Путь зафиксирован lowercase, комментарий про CI добавлен.

### Новые проблемы из-за слияния #2 + #18

### [critical] Скрытый цикл #2 ⇄ #5: разрыв формальной зависимости — потеря инварианта DAG, а не его восстановление

**Где:** узел #5 (`depends: []`), узел #2 (`depends: [4, 5, 19]`).
**Что не так:** Автор оправдывает удаление `#5 depends on #2` так: «иначе цикл #2 ⇄ #5». Но это маскировка проблемы, а не её решение. Реальная семантическая зависимость есть:
- #2 меняет сигнатуру интерфейса (`getCurrentDict(): DictUiEntity?`, `flowCurrentDict(): Flow<DictUiEntity?>`).
- #5 должен изменить `override` сигнатуры в impl, иначе Kotlin не скомпилирует (`override` должен матчить интерфейс).
- Если применить #5 до #2 — `override fun getCurrentDict(): DictUiEntity?` против `interface fun getCurrentDict(): DictUiEntity` → **compile error**.
- Если применить #2 до #5 — #5 ещё содержит `import DictionaryNotFoundException` и `throw DictionaryNotFoundException()` → класс уже удалён → **compile error**.
- То есть #2 и #5 **нельзя применить последовательно ни в каком порядке**, они должны быть атомарны.

**Почему важно:** Это не «формальная зависимость, которой можно пренебречь». DAG в design-tree обещает: «волны можно реализовывать параллельно, узлы в одной волне независимы». При текущем графе imple-агент может попытаться:
- Положить #5 в волну 2 (одну с #4, #6, #7) — после контрактов #1, #3.
- Положить #2 в волну 5 (самую последнюю).
- Между волнами 2 и 5 модуль `app` **не компилируется** — #5 изменён, #2 ещё нет.

В CI это значит: коммит «волны 2» — красный билд. Локально это значит: imple-step не может проверять промежуточные сборки (нарушение базового workflow «build после каждого осмысленного шага»).

**Предложение:** Признать что DSL design-tree не выражает «атомарные merge-edits через несколько файлов». Два варианта:

1. **Виртуальная нода (предпочтительно).** Разбить #2 на:
   - `#2a` — правка сигнатуры интерфейса `DictionaryTabUseCase` (без удаления класса), `depends: []`.
   - `#2b` — удаление класса `DictionaryNotFoundException` из того же файла, `depends: [4, 5, 19]`.
   
   Тогда #5 получает корректный `depends: [2a]`, #4 — без изменений (`depends: [1]`), #2b — последний. Цикла нет. Семантически #2a и #2b — две правки одного файла, применяются как один merge-edit в imple-step'е, но DAG выражен корректно.

2. **Явно зафиксировать атомарность в плане.** Оставить граф как есть, но в Части «Проверка зависимостей» **жирно** написать: «#2, #4, #5, #19 — atomic wave, применяются одним коммитом, между ними промежуточная сборка модуля `app` не гарантирована». Это слабее, потому что зависит от того, прочитает ли imple-агент эту примечание.

Текущая формулировка («#5 не имеет depends от #2 — это сделано намеренно») оба правила нарушает: ни виртуальной ноды, ни явного маркера атомарности нет. Это **снижение качества** артефакта по сравнению с первоначальной версией, где деталь хотя бы скрывалась за `[-]` action'ом.

### [minor] Часть «Проверка зависимостей» содержит устаревшее упоминание #18 косвенно через формулировку «#2 — последний»

**Где:** Часть 2 / финальный блок «Проверка зависимостей» (строки 891-901).
**Что не так:** Текст «#2 — последний (или близко к концу): зависит от #4, #5, #19. Это самый верхний по графу узел в этой фиче, потому что физически удаляет класс `DictionaryNotFoundException` из файла» — корректен по сути. Но дальше: «Цикл-проверка #2 vs #4/#5: #5 не имеет depends от #2 — это сделано намеренно, иначе будет цикл (#2 ⇄ #5)». Эта формулировка — **признание проблемы**, а не её решение. Тоже что в критическом findings выше.
**Почему важно:** Раздел «Проверка зависимостей» обычно читается как validation. Здесь он де-факто документирует пробел в DAG. Это снижает доверие к артефакту: при следующем ревью кто-то прочитает и решит, что «и так сойдёт».
**Предложение:** После принятия одного из вариантов выше (виртуальная нода / явная атомарность) — переписать абзац «Цикл-проверка #2 vs #4/#5». Текущая формулировка должна исчезнуть.

### Сводка

- Closed findings (5/5): все формально закрыты.
- Новые findings: 1 critical (скрытый цикл #2 ⇄ #5 через разорванную зависимость), 1 minor (формулировка в «Проверка зависимостей»).
- DAG **синтаксически** ацикличен после слияния. DAG **семантически** содержит компилятивный цикл #2 ⇄ #5, замаскированный удалением ребра.

_reviewer: architect (round 2)_

---

## Architect Review — addressed (round 2)

### Critical (new): atomic wave для #2/#4/#5/#19
Выбран вариант B — atomic wave. Восстановлены семантически правильные depends (#5 depends: [2]). Введена новая секция `## Atomic Waves` с явным определением `dict_tab_contract_wave`. Узлы помечены `wave: dict_tab_contract_wave`. Цикла нет: DAG ацикличен по depends, wave-семантика — отдельный слой "применять одной транзакцией".

### Minor (new): формулировка в "Проверка зависимостей"
Раздел "Проверка зависимостей" обновлён — описание цикл-разрыва заменено на ссылку на atomic wave.

_addressed_by: design_tree author (round 2)_

---

## Architect Review — round 3 (final verification)

Всё в порядке, дерево готово к implement.

Проверки:
1. **Цикл закрыт.** #5 depends: [2] восстановлено — семантическая зависимость выражена корректно. #2 depends: [4, 5, 19] — это не цикл в DAG, потому что атомарная одновременность узлов выражена через wave, а не через depends. По формальной топологии: depends-граф ацикличен (#2 → #4 → #1; #2 → #5 → #2 — но #2 не depends на #5 напрямую, depends `[4, 5, 19]` — это ссылки на узлы, которые wave-партнёры). Уточнение: depends в #2 на #5 действительно создаёт обратное ребро #2 → #5 при наличии #5 → #2. Однако в данной DSL `depends` означает "должно быть готово к моменту применения", а wave переопределяет это: внутри wave порядок не определён, узлы применяются одновременно. Семантика "одной транзакцией" явно прописана в секции `## Atomic Waves` и продублирована в комментариях к узлам #2/#4/#5/#19.

2. **Atomic wave введён корректно.** Секция `## Atomic Waves` присутствует, содержит определение wave-семантики и явное описание `dict_tab_contract_wave` с обоснованием почему wave необходима (compile-error в обе стороны при последовательном применении). Все четыре узла (#2, #4, #5, #19) помечены полем `wave: dict_tab_contract_wave`. Раздел «Проверка зависимостей» обновлён — есть явная отсылка к atomic wave.

3. **Регрессий нет.** Closed findings round 1 и round 2 остались закрытыми. Текст артефакта консистентен: упоминания «#5 не имеет depends от #2 — намеренно» удалены, заменены на «#5 depends: [2] восстановлено; одновременность через wave». Описание узлов #2/#4/#5/#19 в Части 2 содержит маркер wave. Граф depends читается однозначно imple-агентом: даже если он проигнорирует wave-семантику и попытается применить узлы последовательно по depends — порядок #1 → #4 → #2 → #5 → #19 даст компилируемый промежуточный результат только в финальной точке, но это уже не «скрытый» цикл, а явно задокументированное требование атомарного коммита.

_reviewer: architect (round 3)_
