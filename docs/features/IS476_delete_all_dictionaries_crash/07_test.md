# Тесты — IS476

## Решение

Тесты нужны. Фикс IS476 меняет публичные контракты:

- `DictionaryAppBarUseCase.flowCurrentDict(): Flow<DictUiEntity?>` — было throw на пустом списке, стало эмит `null`.
- `DictionaryTabUseCase.flowCurrentDict(): Flow<DictUiEntity?>` + `getCurrentDict(): DictUiEntity?` — было throw, стало nullable.
- `DictionaryUseCaseImpl.deleteDictionary()` — чистит orphaned pref (`setLong(null)`) при удалении последнего словаря.
- `Msg.CurrentDict(current: DictUiEntity?)` (AppBar) — параметр nullable, новая ветка reducer'а.
- `Msg.SelectDictionary(current: DictUiEntity?)` (DictionaryTab) — новый кейс null + новый флаг `hasNoDictionary` в state.

Для бага — регресс-тест воспроизводящий «пустой список словарей → null а не throw». Для новых контрактов — позитивные кейсы по nullable-ветке. По TDD: тесты компилируются на новом контракте, на текущем (master) коде падают — это правильно.

## Созданные / обновлённые файлы

### [~] /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/test/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImplTest.kt

Узел #19. Изменения:
- Удалён `import me.apomazkin.dictionarytab.deps.DictionaryNotFoundException` + добавлен `assertNull`.
- Test case #5 `flowCurrentDict throws DictionaryNotFoundException when list is empty` → `flowCurrentDict emits null when list is empty` (`expected = ...` snnotation убрана; ассерт `assertNull` на `useCase.flowCurrentDict().first()`).
- Тип возврата `useCase.flowCurrentDict().first()` теперь `DictUiEntity?` — все остальные тесты приведены к safe-call (`result?.id`, `result?.title` и т.д.).
- В док-комментарии класса описание case #5 переписано на "Boundary: emits null when list is empty (IS476)".

### [~] /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/test/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImplTest.kt

Узел #20. Изменения:
- Test case #11 `should not crash when current deleted and no remaining` → `should clear orphaned pref when current deleted and no remaining`.
- Был ассерт `coVerify(exactly = 0) { prefsProvider.setLong(any(), any()) }` (старое поведение — pref не трогался). Стало `coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null) }` (новое поведение — pref чистится явно null'ом).
- В док-комментарии класса описание case #11 уточнено: "clears orphaned pref to null (IS476)".

### [+] /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/test/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImplTest.kt

Узел #21. Новый файл. Путь lowercase `dictionarytab/` — соответствует production-пути `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/` (case-sensitivity на CI Linux ext4).

Тесты (6 случаев, JUnit 4 + mockk):

- `flowCurrentDict emits null when list is empty` — IS476 регресс: prefs null + empty list → null.
- `flowCurrentDict emits current dict matching prefs ID` — happy path.
- `flowCurrentDict emits first dict when prefs is null but list non-empty` — fallback.
- `getCurrentDict returns null when no dictionaries exist` — IS476 регресс на suspend методе.
- `getCurrentDict returns dict from prefs ID when present` — happy path.
- `getCurrentDict returns first dict from list when prefs is null and writes id back` — fallback + verify setLong записи.

Setup: mockк `CoreDbApi.DictionaryApi`, `CoreDbApi.WordApi`, `CoreDbApi.TermApi`, `PrefsProvider`, `CountryProvider`. `prefsFlow: MutableStateFlow<Long?>` — управление эмиссиями.

### [~] /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionaryTab/src/test/java/me/apomazkin/dictionarytab/logic/VocabularyTabReducerKtTest.kt

Узел #22. Изменения:

- В док-комментарии класса добавлены case #19 (Boundary: SelectDictionary(null)) и case #20 (Standard: переход null → non-null).
- Test case #2 `should trigger term flow load when ChangeDict is received` — переписан. Был ассерт `assertEquals("State should remain unchanged", initialState, result.state())`; теперь reducer вызывает `markDictionaryPresent().showLoading()` → state НЕ равен initialState. Корректные ассерты: `hasNoDictionary == false` и `isLoading == true` (плюс `assertSingleEffect<LoadTermFlow>`).
- Новый тест #19 `should mark hasNoDictionary and emit no effects when SelectDictionary with null received` — boundary IS476: `Msg.SelectDictionary(null)` → `hasNoDictionary = true`, `isLoading = false`, без LoadTermFlow effect (защита от NPE в `getCurrentDict().id.toInt()`).
- Новый тест #20 `should clear hasNoDictionary, set isLoading and emit LoadTermFlow when SelectDictionary with dict received after null` — переход null → non-null. Initial state: `hasNoDictionary=true, isLoading=false`. После reduce: `hasNoDictionary=false, isLoading=true` + `LoadTermFlow` effect (Minor 1 из Architect Review — нет UI-моргания).

### [+] /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/test/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducerTest.kt

Узел #23. Новый файл. Раньше в модуле был только пустой `ExampleUnitTest.kt`.

Тесты (6 случаев):

- `should update state currentDict when CurrentDict with non-null received` — happy path + immutability checks.
- `should set state currentDict to null when CurrentDict with null received on default state` — IS476 boundary на default state.
- `should clear currentDict when CurrentDict with null received after non-null` — переход «был словарь → удалили все» (важный регресс).
- `should set list and hide loading when AvailableDict received` — sanity для базового reducer-flow.
- `should open dropdown when DictMenuOn received`.
- `should close dropdown when DictMenuOff received`.

Setup: `DictionaryAppBarReducer(logger = anonymous LexemeLogger)`. Без mockk — reducer чистый, dependency только logger (заглушка).

### [~] /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/build.gradle.kts

Добавлен `testImplementation(project(":modules:core:mate"))` — для доступа к `MateTestHelper.kt` (testReduce / assertNoEffects). Без него тестовый файл #23 не скомпилируется.

## TDD статус

Тесты не компилируются на текущем (master) коде:

- `DictionaryAppBarUseCaseImplTest` — `Msg.CurrentDict.current` ещё non-null, поэтому `result?.id` на не-nullable ссылке — warning, но `assertNull(result)` упадёт на компиляции потому что `first()` возвращает `DictUiEntity`, не `DictUiEntity?`.
- `DictionaryTabUseCaseImplTest` (новый) — обращается к `flowCurrentDict()` и `getCurrentDict()`, которые сейчас non-null. Безуспешно компилируется на текущем коде.
- `VocabularyTabReducerKtTest` — `Msg.SelectDictionary(current = null)` не скомпилируется (параметр сейчас non-null), `state.hasNoDictionary` — поля нет в `DictionaryTabState`.
- `DictionaryAppBarReducerTest` (новый) — `Msg.CurrentDict(current = null)` не скомпилируется (параметр сейчас non-null).
- `DictionaryUseCaseImplTest` — компилируется, но падает рантайм: фактический `deleteDictionary()` не вызывает `setLong(_, null)` при отсутствии remaining.

После реализации фикса (узлы #1–#7, #8–#16 design tree) тесты должны позеленеть.

_model: claude-opus-4-7[1m]_
