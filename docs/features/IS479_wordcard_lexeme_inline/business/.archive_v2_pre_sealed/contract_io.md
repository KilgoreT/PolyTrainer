# contract_io — IS479 wordcard_lexeme_inline (v3)

## Режим работы

**Режим 1 — макет-driven** (унаследовано из `contract_state.md` v2 и `contract_ui_msg.md` v2). Источники: Figma `w8GmGCdOZJUi99Cuv4q4W9` (frames `9154-82519`, `9154-82532`, `9154-82521`, `9154-82625`) + текущий код модуля `screen/wordcard` (`Message.kt` / `WordCardReducer.kt` / `DatasourceEffectHandler.kt` / `UiEffectHandler.kt` / `WordCardNavigationEffectHandler.kt`). Спека отсутствует — расхождения spec↔code не сверяются (см. отдельный раздел в конце).

Этот артефакт **канонизирует** forward-refs шага `contract_ui_msg` (Datasource Msg, точные сигнатуры эффектов, ветвления effect-handler'ов, edge cases). Shape `WordCardState` и формы UI Msg **не меняются** — это работа `contract_state` / `contract_ui_msg`.

## Решения по tech debt и approved findings

Перед эффектами фиксирую сводные решения после двух раундов ревью.

### F012 — Commit-empty / Remove-translation/definition: deferred подход

**Решение: deferred-подход в reducer'е `CommitTranslationEdit` ветка 1 (`edited.isEmpty()`) и UI Msg `RemoveTranslation` — соответствует `contract_ui_msg.md` v2.**

Reducer ветка 1 `CommitTranslationEdit`:
```
for lexeme with id == lexemeId:
  isEdit = false          // только режим переключаем
  // translation field НЕ трогаем — ждём RefreshTranslation от data
+ DatasourceEffect.RemoveTranslation(lexemeId)
```

После прихода `Msg.RefreshTranslation(lexemeId, translation = null)` от handler'а — reducer ставит `translation = null`.

**Что меняется относительно итерации 2 (откат immediate nullify):** ит.2 предлагала immediate nullify ради «отсутствия visual-flash» — но это означало бы переопределение reducer-логики UI Msg, что выходит за scope `contract_io`. Возвращаем deferred.

**View-mode семантика в transient-окне** (Edge case EC5 ниже):
- В transient (`isEdit=false`, ждём Refresh) `translation` ещё не null — поле остаётся с **старым `origin`**.
- UI в view-mode (selector `toValue(isEdit)`) показывает `origin` (старое значение) до прихода Refresh.
- Это **легитимное transient-окно** — пользователь увидит «удаление с задержкой». Фиксируем как часть контракта.
- Альтернатива (мгновенное визуальное удаление через спиннер на chip) — задача UI sub-flow, не reducer-логика.

**Failure без savedOrigin:** при failure `RemoveTranslation` effect reducer `RemoveTranslationFailed(lexemeId)` просто **не делает ничего со state** — translation ещё не был nullify'ed (deferred), `origin` не менялся. Notification через `UiEffect.ShowNotification`.

Аналогично `CommitDefinitionEdit` / `RemoveDefinition`.

### F003 — RefreshTranslation/Definition payload минимальный

Сигнатура (F003 approved в ит.1):
- `Msg.RefreshTranslation(lexemeId: Long, translation: String?)`
- `Msg.RefreshDefinition(lexemeId: Long, definition: String?)`

Не `TextValueState?`, не `Lexeme`. Только примитивный nullable `String`. Reducer собирает `TextValueState(origin = it, edited = it, isEdit = false)` локально. Handler делает маппинг `lexeme.translations.firstOrNull()?.value` один раз.

### F004 / откат F024 — payload `RemoveWord` UI Msg

`data class RemoveWord(val wordId: Long) : Msg` — как зафиксировано в `contract_ui_msg.md` v2. Форма UI Msg не меняется в этом шаге. Handler берёт `wordId` из payload.

### F008 — Update*Failed rollback с guard на isEdit

Reducer'ы `UpdateTranslationFailed(lexemeId)` / `UpdateDefinitionFailed(lexemeId)`:
- Если `lexeme.translation == null` ⇒ только `ShowNotification`, ничего не откатываем (объекта нет).
- Если `lexeme.translation.isEdit == true` ⇒ пользователь снова в edit-mode, печатает; **rollback запрещён** (затрёт ввод). Только `ShowNotification`.
- Иначе ⇒ `lexeme.translation.edited = lexeme.translation.origin` + `ShowNotification`.

См. таблицу guards в конце.

### F009 — Refresh*/Failed* reducer с find-guard

Все Msg, адресующие лексему по id (`RefreshLexeme` исключение — append, не адресация), используют:
```
val lexeme = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
```

Применено к: `RefreshTranslation`, `RefreshDefinition`, `UpdateTranslationFailed`, `UpdateDefinitionFailed`, `LexemeRemoved`, `RemoveLexemeFailed`, `RemoveTranslationFailed`, `RemoveDefinitionFailed`. Closes race lexeme-удалена-пока-Update-в-полёте.

### F005 / F010 — LoadWord failure

Новый Msg `WordLoadFailed`. Handler оборачивает `getTermById` в `try/catch`:
- `wordCardUseCase.getTermById(wordId)` ⇒ `Term?`.
- `Term?.let { Msg.WordLoaded(it) } ?: Msg.WordNotFound` при null.
- `catch (Throwable)` ⇒ `Msg.WordLoadFailed`.

Reducer `WordLoadFailed`: `isLoading = false` + `NavigationEffect.Back` (закрыть экран — открыть нечего, нельзя редактировать неизвестное). Альтернатива «остаться + ShowNotification» отвергнута: state остаётся `wordState.id == NOT_IN_DB` — все word-Msg будут блокированы guard'ом, пользователь увидит «пустой экран без выхода» → хуже чем явный Back.

### F011 — try/catch для всех Datasource effects

Канонизирую: каждый Datasource handler-case оборачивается в `try/catch (Throwable)`, эмитит соответствующий failure-Msg. Throw из use-case больше не убивает корутину handler'а.

Полный список failure-Msg (новые Datasource Msg в scope этого шага):
- `WordLoadFailed` (от `LoadWord`).
- `UpdateWordFailed` (от `UpdateWord`).
- `RemoveWordFailed` (от `RemoveWord`).
- `CreateLexemeFailed` (от `CreateLexeme`).
- `RemoveLexemeFailed(lexemeId)` (от `RemoveLexeme`).
- `UpdateTranslationFailed(lexemeId)` (от `UpdateLexemeTranslation`).
- `RemoveTranslationFailed(lexemeId)` (от `RemoveTranslation`; **без `savedOrigin`** — deferred-подход не nullify'ит, нечего восстанавливать).
- `UpdateDefinitionFailed(lexemeId)` (от `UpdateLexemeDefinition`).
- `RemoveDefinitionFailed(lexemeId)` (от `RemoveDefinition`).

### F001 / F006 — UiEffect.ShowNotification: textRes: Int

**Решение:** сигнатура `UiEffect.ShowNotification(@StringRes textRes: Int)` — StringRes Int, не String. Reducer строит effect передавая `R.string.X` напрямую (нет нужды в `Context`).

`UiEffectHandler` инжектирует `ResourceManager` (`me.apomazkin.ui.resource.ResourceManager`) и resolve'ит:
```kotlin
override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
    val msg = when (effect) {
        is UiEffect.ShowNotification ->
            Msg.ShowNotification(text = resourceManager.stringByResId(effect.textRes))
    }
    consumer(msg)
}
```

`Msg.ShowNotification(text: String)` — `text` уже resolved (handler сделал работу). Reducer пишет в `snackbarState.title = text`. Это согласовано: payload в Effect — `Int`, payload в return-Msg — `String`.

### F016 — сводная таблица guards «isLoading=true»

См. в конце раздела «Глобальные guards».

### F018 — defense-in-depth `UpdateLexemeTranslation` lexemeId != NOT_IN_DB

Handler `UpdateLexemeTranslation` / `UpdateLexemeDefinition`: до вызова use-case проверить `effect.lexemeId != NOT_IN_DB`. Если `NOT_IN_DB` — log + return `Msg.NoOperation`. Текущий код использует обратное соглашение (`if (lexemeId > -1) lexemeId else null`) — оставляет дверь для создания новой лексемы через update-путь. После F018: путь null зашивается.

### F019 — NavigationEffect.Back idempotency (откат)

**Решение: НЕ вводить флаг `isRemovingWord` в State.** Это изменение shape State — вне scope `contract_io`.

Альтернатива (idempotency на уровне инфраструктуры, без state-поля):
- `NavigationEffect.Back` идемпотентна на уровне `Navigator.back()`: вторая попытка `popBackStack()` на уже unmounted screen — silent no-op в Compose Navigation.
- `viewModelScope` отменяется после первого успешного Back ⇒ второй handler-вызов либо не успеет долететь (CancellationException), либо отработает на already-popped screen.
- БД-уровень: `deleteWord` идемпотентен (DELETE по уже-несуществующему id — no-op).
- Принимаемый tech debt: при очень быстром double-tap до закрытия диалога второй `RemoveWord` effect улетит в БД. Acceptable.

В sealed list Datasource Msg `RemoveWordFailed` остаётся — нужен для notification path (если первый Remove упал в БД до закрытия экрана). Reducer для него: только `UiEffect.ShowNotification`, без сброса несуществующего state-поля.

**См. также раздел `## Feedback в предыдущие шаги`** — для строгого исключения concurrent CreateLexeme/Translation/Definition после `RemoveWord` потребовалось бы выставлять `isLoading = true` в reducer'е UI Msg `RemoveWord`, что относится к scope `contract_ui_msg`. На текущий момент tech debt принят.

### F020 — Subscribers self-check note

В разделе «Проверка реактивности» вопрос 4 теперь содержит note: «при ТЕКУЩЕМ архитектурном решении DAO non-reactive ⇒ N; если в будущем мигрируем на reactive Room flow — пересмотреть».

### F021 (заменено F032) — UpdateWord race acceptable, не «сериализация»

**Корректное утверждение:** Mate запускает каждый effect в **независимой корутине** через `viewModelScope.launch { ... }`. Порядок завершения effects **не гарантирован** — `launch` не сериализует. Race между UPDATE/DELETE возможен на уровне БД — handler не имеет mutex'а.

**Acceptable для текущей фичи:**
- DELETE idempotent: повторный DELETE по несуществующему id — no-op.
- UPDATE last-write-wins: последний завершившийся UPDATE фиксирует значение; rare-case рассинхрон закрывается следующим `LoadWord` при возврате на экран.
- Если в будущем потребуется строгий порядок (например, при появлении сетевой синхронизации) — нужен mutex / sequential dispatch на уровне framework, не feature.

### F022 — Concurrent ShowNotification batching

В UI Effects: явно «Compose M3 `SnackbarHostState.showSnackbar()` suspending; два Show в один tick попадают в очередь; `state.snackbarState` показывает последний resolved text; acceptable».

### F035 — UpdateWord без savedValue rollback (wontfix, симметрия с deferred)

Reducer `CommitWordChanges` (см. `contract_ui_msg.md` v2): `wordState.value = wordState.edited`, `isEditMode = false`, `edited = ""`. Это **оптимистическая запись** в state. Если effect `UpdateWord` упал в БД — state показывает новое значение, БД хранит старое. На следующий вход в экран `LoadWord` вернёт старое value из БД — silent revert.

Это tech debt. **Чтобы устранить** — нужно либо:
- (а) Поменять reducer `CommitWordChanges` чтобы он не менял `value` сразу, ждать `WordUpdated(text)` от handler'а — это изменение reducer-логики UI Msg, scope `contract_ui_msg`. Feedback зафиксирован в разделе ниже.
- (б) Хранить `savedValue: String?` для rollback — это изменение shape State, scope `contract_state`. Не нужно: симметрия с deferred translation/definition (там тоже нет savedOrigin) — wontfix.

На текущей итерации: симметрия восстановлена с откатом F012 immediate nullify. Translation/definition reducer не меняют `origin` сразу — ждут Refresh; word reducer меняет `value` сразу. Асимметрия осталась, но обе схемы согласованы внутри себя (translation deferred — failure не требует rollback; word optimistic — silent revert через LoadWord).

## Effects

Три категории: **Datasource**, **Navigation**, **Ui**. Каждый Effect — целостный блок.

### Datasource Effects

```kotlin
package me.apomazkin.wordcard.mate

import me.apomazkin.mate.Effect

sealed interface DatasourceEffect : Effect {

    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect

    data class CreateLexeme(val wordId: Long) : DatasourceEffect
    data class RemoveLexeme(val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeTranslation(
        val wordId: Long,
        val lexemeId: Long,
        val translation: String,
    ) : DatasourceEffect

    data class RemoveTranslation(
        val lexemeId: Long,
    ) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        val lexemeId: Long,
        val definition: String,
    ) : DatasourceEffect

    data class RemoveDefinition(
        val lexemeId: Long,
    ) : DatasourceEffect
}
```

> 📎 guide: docs/guides/effect-handlers.md — "DatasourceEffect: операции с БД, сетью, preferences; всегда на Dispatchers.IO"

> 📎 guide: docs/guides/effect-handlers.md — "Handler не должен throw — failure представлять отдельным Msg-вариантом"

---

#### `LoadWord(wordId: Long)`

- **Source:** `initEffects` Mate (а не Msg от UI). Запускается **один раз** при создании ViewModel; payload — навигационный аргумент `wordId` через `@AssistedInject`. См. `WordCardViewModel.kt:28`. UI Msg `LoadingWord` удалён как антипаттерн (см. `contract_ui_msg.md` раздел «Удаляются (1 шт. — legacy / dead code)»).

> 📎 guide: docs/guides/mate-framework.md — "initEffects запускают первую загрузку при создании Mate; антипаттерн LaunchedEffect → Msg.LoadingWord"
>
> 📎 guide: docs/guides/viewmodel-wiring.md — "@AssistedInject для ViewModel: Navigator + runtime аргументы (wordId) через @Assisted"

- **Handler:** `DatasourceEffectHandler.onEffect` ⇒ `wordCardUseCase.getTermById(wordId)` на `Dispatchers.IO` **внутри try/catch** (F005/F010/F011).
- **Action:** SELECT по `wordId` из БД, маппинг в domain `Term?`.
- **Return Msg:**
  - success (`Term != null`): `Msg.WordLoaded(term: Term)`.
  - not found (`Term == null`): `Msg.WordNotFound`.
  - throw: `Msg.WordLoadFailed`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.LoadWord -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.getTermById(effect.wordId)
              ?.let { Msg.WordLoaded(it) }
              ?: Msg.WordNotFound
      } catch (e: Throwable) {
          Log.w(TAG, "LoadWord failed", e)
          Msg.WordLoadFailed
      }
  }
  ```

> 📎 guide: docs/guides/logging.md — "Только LexemeLogger через tag-константу (LogTags.WORDCARD); прямой android.util.Log запрещён"

- **Reducer-логика:**
  - `WordLoaded(term)` ⇒ `isLoading = false`, `wordState = WordState(id, value, added, ...)` маппится из `term`, `lexemeList = term.lexemeList.map { it.toLexemeState() }`. См. существующий `WordCardReducer.kt:15-18` — логика сохраняется.
  - `WordNotFound` ⇒ `isLoading = false` + `NavigationEffect.Back`. **Изменение от текущего кода:** в `WordCardReducer.kt:20` стоит `TODO("WordNotFound is not implemented")`; здесь канонизируется «закрыть экран».
  - `WordLoadFailed` ⇒ `isLoading = false` + `NavigationEffect.Back`. (Альтернатива «остаться + Snackbar» — отвергнута: `wordState.id == NOT_IN_DB`, все word-Msg заблокированы, пустой экран без выхода.)

> 📎 guide: docs/guides/reducer-patterns.md — "Reducer — чистая функция: state + msg → (newState, Set<Effect>); навигация только эффектом, не флагом"
>
> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое; никаких навигационных флагов (closeScreen, needClose)"

- **Edge cases:**
  - **Concurrent calls:** `initEffects` гарантированно отрабатывают один раз — concurrent не возможен на этом эффекте. UI Msg-trigger'а для повторного `LoadWord` нет.
  - **Failure (исключение в `getTermById`):** F005/F010/F011 покрыты — `WordLoadFailed` ⇒ Back.
  - **`wordId == NOT_IN_DB` (-1L):** не должно происходить (`wordId` из навигационного аргумента — только валидные id). Если случилось — `getTermById(-1L)` вернёт `null` ⇒ `WordNotFound` ⇒ exit. Дополнительный guard не нужен.

---

#### `UpdateWord(wordId: Long, value: String)`

- **Source:** `CommitWordChanges` UI Msg. Effect конструируется в reducer'е по snapshot `state.wordState.id` + `state.wordState.edited` **до** сброса.
- **Handler:** `wordCardUseCase.updateWord(wordId, value)` на `Dispatchers.IO` в try/catch.
- **Action:** UPDATE word.value в БД, возвращает `Boolean`.
- **Return Msg:**
  - success: `Msg.NoOperation`.
  - throw: `Msg.UpdateWordFailed`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.UpdateWord -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.updateWord(effect.wordId, effect.value)
          Msg.NoOperation
      } catch (e: Throwable) {
          Log.w(TAG, "UpdateWord failed", e)
          Msg.UpdateWordFailed
      }
  }
  ```
- **Reducer-логика:**
  - `NoOperation` ⇒ `state to emptySet()`.
  - `UpdateWordFailed` ⇒ `UiEffect.ShowNotification(textRes = R.string.word_card_update_word_failed)`. State уже оптимистически содержит новое `value` (записано в reducer'е `CommitWordChanges`, см. `contract_ui_msg` v2); полный rollback требует хранения `savedValue` — не вводим (F035 wontfix, симметрия с deferred translation/definition). UI покажет уведомление + следующий `LoadWord` при возврате на экран синхронизирует.
- **Edge cases:**
  - **Race UPDATE/UPDATE (F021/F032):** Mate запускает каждый effect в независимой корутине через `viewModelScope.launch`; порядок завершения effects не гарантирован. Два UPDATE в полёте — последний завершившийся фиксирует значение в БД (last-write-wins). Acceptable.
  - **Concurrent edits в UI:** двойной `CommitWordChanges` отфильтрован guard'ом `!isEditMode` (первый Commit уже выключил edit-mode) — повторный Msg идёт в empty.
  - **Failure при втором edit (F038):** пользователь снова вошёл в edit-mode после первого Commit ⇒ `isEditMode = true`, `edited = wordState.value` (где `value` уже оптимистически содержит первый коммит). Затем приходит первый `UpdateWordFailed` — reducer только эмитит `ShowNotification`, **не трогает `value`** (нет savedValue) и не трогает `isEditMode/edited` (пользователь в edit, затирать нельзя). Acceptable.

---

#### `RemoveWord(wordId: Long)`

- **Source:** `RemoveWord(wordId)` UI Msg (форма `data class` сохранена согласно `contract_ui_msg.md` v2). Effect конструируется по payload Msg.
- **Handler:** `wordCardUseCase.deleteWord(wordId)` на `Dispatchers.IO` в try/catch.
- **Action:** DELETE word из БД (каскадом удаляются связанные lexeme).
- **Return Msg:**
  - success: `Msg.NavigateBack`.
  - throw: `Msg.RemoveWordFailed`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.RemoveWord -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.deleteWord(effect.wordId)
          Msg.NavigateBack
      } catch (e: Throwable) {
          Log.w(TAG, "RemoveWord failed", e)
          Msg.RemoveWordFailed
      }
  }
  ```
- **Reducer-логика:**
  - `NavigateBack` ⇒ `state to setOf(NavigationEffect.Back)` (см. `contract_ui_msg.md`).
  - `RemoveWordFailed` ⇒ `UiEffect.ShowNotification(textRes = R.string.word_card_remove_word_failed)`. State-поле не сбрасываем (нет `isRemovingWord` — F019 откат). Notify через snackbar; пользователь может повторить через меню.
- **Почему `NavigateBack` UI Msg, а не прямой `NavigationEffect.Back`:** handler не имеет доступа к Mate-effect-emitter (только Msg-consumer); путь через UI Msg → reducer → `NavigationEffect.Back` — единственный архитектурно валидный.

> 📎 guide: docs/guides/navigation.md — "Закрытие экрана выражается NavigationEffect.Back, не флагом в State; обработка в MateNavigationEffectHandler → Navigator.back()"

- **Edge cases:**
  - **Double-RemoveWord (F019 без state-флага):** второй `RemoveWord` Msg от UI **не блокируется в reducer'е** на уровне `contract_io` (нет `isRemovingWord` поля). Защита идёт от инфраструктуры: (а) `popBackStack()` на already-popped screen — silent no-op; (б) `viewModelScope` отменяется после первого Back ⇒ второй handler либо не успеет долететь, либо отработает на already-cancelled scope. (в) БД-уровень: повторный `deleteWord` по несуществующему id — no-op. См. EC6.
  - **Handler cancellation acceptable (F036):** если `viewModelScope` отменился до завершения `deleteWord`, БД-операция может не завершиться. Acceptable: retry на следующем входе через `LoadWord` (если запись осталась) → пользователь снова инициирует Remove.
  - **Failure (F040):** notification, menu уже закрыт reducer'ом UI Msg `RemoveWord` (см. `contract_ui_msg.md`) — failure не трогает menu-флаги.

---

#### `CreateLexeme(wordId: Long)`

- **Source:** `CreateLexeme` UI Msg (тап FAB `9154-82532`). Effect конструируется по `state.wordState.id`.
- **Handler:** `wordCardUseCase.addLexeme(wordId)` на `Dispatchers.IO` в try/catch (F011).
- **Action:** INSERT новой пустой lexeme в БД, возвращает domain `Lexeme?`.
- **Return Msg:**
  - success (`Lexeme != null`): `Msg.RefreshLexeme(lexeme: Lexeme)`.
  - null или throw: `Msg.CreateLexemeFailed`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.CreateLexeme -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.addLexeme(effect.wordId)
              ?.let { Msg.RefreshLexeme(it) }
              ?: Msg.CreateLexemeFailed
      } catch (e: Throwable) {
          Log.w(TAG, "CreateLexeme failed", e)
          Msg.CreateLexemeFailed
      }
  }
  ```
  **Изменение от текущего кода:** `DatasourceEffectHandler.kt:78` throw'ит `IllegalStateException("Lexeme not found")` при null — это краш. Канонизирую safe-handling (F011).
- **Reducer-логика:**
  - `RefreshLexeme(lexeme)`:
    1. find-guard избыточен (append, не адресация).
    2. `isCreatingLexeme = false`.
    3. `lexemeList = lexemeList + LexemeState(id = lexeme.lexemeId.id, translation = null, definition = null, isMenuOpen = false)`.
    4. **Изменение от текущего кода:** существующий reducer (`WordCardReducer.kt:73-97`) маппит translation/definition исходя из `addLexemeBottomState.isTranslationCheck/isDefinitionCheck`. Эти чекбоксы удаляются (`contract_state` v2) — лексема всегда создаётся пустой.
  - `CreateLexemeFailed`:
    1. `isCreatingLexeme = false`.
    2. `UiEffect.ShowNotification(textRes = R.string.word_card_create_lexeme_failed)`.
- **Edge cases:**
  - **Concurrent calls:** двойной FAB-тап отфильтрован guard'ом `isCreatingLexeme` в reducer'е.
  - **Failure recovery:** `CreateLexemeFailed` сбрасывает `isCreatingLexeme = false` → FAB снова доступен → retry.
  - **`wordId == NOT_IN_DB`:** отфильтрован guard'ом `wordState.id == NOT_IN_DB` в reducer'е `CreateLexeme`.
  - **Concurrent CreateLexeme + RemoveWord (F033/F034 acceptable):** в текущем контракте (без `isLoading=true` на RemoveWord) теоретически возможна orphan-lexeme race: пользователь успевает тапнуть FAB между `RemoveWord` Msg и закрытием экрана. Acceptable: либо CreateLexeme отработает раньше (создаст лексему, потом каскад DELETE её удалит при `deleteWord`), либо позже (DELETE word уже выполнен, INSERT лексемы упадёт по foreign-key constraint — `CreateLexemeFailed` ⇒ notify, но экран уже закрывается, snackbar не виден). См. feedback в `contract_ui_msg`.

---

#### `RemoveLexeme(lexemeId: Long)`

- **Source:** `RemoveLexeme` UI Msg (контекстное меню лексемы).
- **Handler:** `wordCardUseCase.deleteLexeme(lexemeId)` на `Dispatchers.IO` в try/catch (F011).
- **Action:** DELETE lexeme из БД (каскадом translation + definition).
- **Return Msg:**
  - success: `Msg.LexemeRemoved(lexemeId: Long)`.
  - throw: `Msg.RemoveLexemeFailed(lexemeId)`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.RemoveLexeme -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.deleteLexeme(effect.lexemeId)
          Msg.LexemeRemoved(effect.lexemeId)
      } catch (e: Throwable) {
          Log.w(TAG, "RemoveLexeme failed", e)
          Msg.RemoveLexemeFailed(effect.lexemeId)
      }
  }
  ```
  **Изменение от текущего кода:** handler возвращал `Msg.LoadingWord` — заменено на таргетный `LexemeRemoved(lexemeId)` (whitelist «локальный таргетный update эффективнее full-reload»).
- **Reducer-логика:**
  - `LexemeRemoved(lexemeId)`:
    1. find-guard (F009): если `lexemeList.none { it.id == lexemeId }` ⇒ `state to emptySet()`. (race: другой путь уже удалил.)
    2. `lexemeList = lexemeList.filter { it.id != lexemeId }`.
  - `RemoveLexemeFailed(lexemeId)`:
    1. find-guard.
    2. `UiEffect.ShowNotification(textRes = R.string.word_card_remove_lexeme_failed)`. Лексема остаётся в state. (`isMenuOpen` уже выставлен в `false` reducer'ом `RemoveLexeme` UI Msg — пользователь может ретраить через переоткрытие меню.)
- **Edge cases:**
  - **F017 — double-Remove timing:** между двумя `RemoveLexeme(lexemeId)` Msg может пройти time когда первый эффект в полёте, лексема ещё в `lexemeList`, guard reducer'а `RemoveLexeme` UI Msg не сработает. Второй DELETE улетит. БД idempotent (DELETE по несуществующему id — no-op). Два `LexemeRemoved` придут последовательно: первый удалит из state, второй — find-guard отсечёт. Acceptable.
  - **F042 — двойной Commit-empty translation:** если пользователь дважды быстро коммитит пустой translation, два `RemoveTranslation` effect улетают; reducer ветки 1 второй раз — guard `!isEdit` после первого reduce уже выключит, второй Commit отсечётся. Защита на уровне UI Msg guard'а.
  - **Failure recovery:** retry через переоткрытие меню лексемы.

---

#### `UpdateLexemeTranslation(wordId: Long, lexemeId: Long, translation: String)`

- **Source:** `CommitTranslationEdit` UI Msg, ветвь 3 (`edited != origin && edited.isNotEmpty()`).
- **Handler:** `wordCardUseCase.addLexemeTranslation(wordId, lexemeId, translation)` на `Dispatchers.IO` в try/catch (F011) с defense-in-depth assert (F018).
- **Action:** UPSERT translation в БД.
- **Return Msg:**
  - success (`Lexeme != null`): `Msg.RefreshTranslation(lexemeId: Long, translation: String?)` (F003 — payload `String?`, не `Lexeme`/`TextValueState?`).
  - null или throw: `Msg.UpdateTranslationFailed(lexemeId)`.
  - `lexemeId == NOT_IN_DB` (defense-in-depth F018): log + `Msg.NoOperation`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.UpdateLexemeTranslation -> withContext(Dispatchers.IO) {
      if (effect.lexemeId == NOT_IN_DB) {
          // F018 defense-in-depth: inline-flow всегда работает с существующей лексемой
          Log.w(TAG, "UpdateLexemeTranslation with NOT_IN_DB lexemeId — ignored")
          return@withContext Msg.NoOperation
      }
      try {
          wordCardUseCase.addLexemeTranslation(
              wordId = effect.wordId,
              lexemeId = effect.lexemeId,
              translation = effect.translation,
          )
              ?.let { lex ->
                  val translationValue = lex.translations.firstOrNull()?.value
                  Msg.RefreshTranslation(effect.lexemeId, translationValue)
              }
              ?: Msg.UpdateTranslationFailed(effect.lexemeId)
      } catch (e: Throwable) {
          Log.w(TAG, "UpdateLexemeTranslation failed", e)
          Msg.UpdateTranslationFailed(effect.lexemeId)
      }
  }
  ```
  **Изменение от текущего кода:** `lexemeId > -1 ? lexemeId : null` (current `DatasourceEffectHandler.kt:81`) ⇒ assert + NoOperation. Use-case вызывается с не-nullable `lexemeId`.
- **Reducer-логика:**
  - `RefreshTranslation(lexemeId, translation: String?)` (F003 / F009 / F015):
    ```
    val lex = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()  // F009
    val nextTranslation: TextValueState? = when {
        translation == null -> null
        lex.translation == null -> TextValueState(origin = translation, edited = translation, isEdit = false)
        lex.translation.isEdit -> lex.translation.copy(origin = translation)  // F015: edit активен, edited не трогаем
        else -> lex.translation.copy(origin = translation, edited = translation, isEdit = false)
    }
    update lex.translation = nextTranslation
    ```
    F028 — лишний `?.` снят (выше работаем с уже non-null `lex`).
  - `UpdateTranslationFailed(lexemeId)` (F008 / F009):
    ```
    val lex = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
    val notify = setOf(UiEffect.ShowNotification(textRes = R.string.word_card_update_translation_failed))
    when {
        lex.translation == null -> state to notify  // нет объекта — нечего откатывать
        lex.translation.isEdit -> state to notify   // F008: пользователь снова редактирует — rollback запрещён
        else -> state.copy(
            lexemeList = lexemeList.map { l ->
                if (l.id == lexemeId) l.copy(translation = l.translation.copy(edited = l.translation.origin))
                else l
            }
        ) to notify
    }
    ```
- **Edge cases:**
  - **F018 `lexemeId == NOT_IN_DB`:** handler возвращает `NoOperation`, БД не трогается. В рамках inline-механики `id == NOT_IN_DB` невозможен (инвариант 3 state), но defense-in-depth.
  - **F015 Concurrent edit during Refresh:** reducer-логика `RefreshTranslation` не трогает `edited` если `isEdit == true` (пользователь снова в edit). Acceptable transient: `origin != edited` — нормально в edit-mode.
  - **F013 — double Commit during edit:** двойной Commit с разными `edited` ⇒ две effect-операции в полёте. Mate **не сериализует** (F021/F032) — порядок завершения не гарантирован. Финальный Refresh определяет `origin` по last-write-wins. EC2 (ниже) — детально.
  - **Failure:** покрыто `UpdateTranslationFailed` — guarded rollback + notification (F008).

---

#### `RemoveTranslation(lexemeId: Long)`

Payload минимальный (`savedOrigin` убран — deferred подход не требует rollback в state).

- **Source:** двух источников:
  1. `RemoveTranslation` UI Msg (контекстное меню лексемы) — reducer не трогает `translation`, шлёт effect.
  2. `CommitTranslationEdit` UI Msg ветка 1 (`edited.isEmpty()`) — reducer ставит `isEdit = false`, не трогает `translation` field, шлёт effect.
- **Handler:** `wordCardUseCase.deleteLexemeTranslation(lexemeId)` на `Dispatchers.IO` в try/catch (F011).
- **Action:** DELETE translation из БД.
- **Return Msg:**
  - success: `Msg.RefreshTranslation(lexemeId, translation = null)` — синхронизирует state через единый канал.
  - throw: `Msg.RemoveTranslationFailed(lexemeId)`.
- **Handler pseudocode:**
  ```kotlin
  is DatasourceEffect.RemoveTranslation -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.deleteLexemeTranslation(effect.lexemeId)
          Msg.RefreshTranslation(effect.lexemeId, translation = null)
      } catch (e: Throwable) {
          Log.w(TAG, "RemoveTranslation failed", e)
          Msg.RemoveTranslationFailed(effect.lexemeId)
      }
  }
  ```
  **Изменение от текущего кода (`DatasourceEffectHandler.kt:100`):** возвращался `Msg.LoadingWord` — full-reload. Замена на таргетный `RefreshTranslation(lexemeId, null)` (через который state наконец-то ставит `translation = null`) и явный failure-Msg.
- **Reducer-логика:**
  - `RefreshTranslation(lexemeId, translation = null)` (общая ветка, см. выше): `nextTranslation = null` ⇒ `lex.translation = null`. Это **финальная синхронизация** для deferred-подхода: пользователь видит исчезновение chip-значения здесь.
  - `RemoveTranslationFailed(lexemeId)`:
    ```
    val lex = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
    val notify = setOf(UiEffect.ShowNotification(textRes = R.string.word_card_remove_translation_failed))
    state to notify
    // translation НЕ был nullify'ed (deferred), origin/edited остались как до Remove → state согласован
    ```
    F040 — failure не трогает `isMenuOpen` (он уже закрыт reducer'ом UI Msg `RemoveTranslation`).
- **Edge cases:**
  - **Transient view-mode окно (EC5):** между UI Msg `RemoveTranslation` / `CommitTranslationEdit` (ветка 1) и приходом `RefreshTranslation(lexemeId, null)` — в state `translation` ещё содержит старый `origin`. UI в view-mode (`isEdit=false`) показывает `origin` (старое значение). Это **легитимное transient-окно** — пользователь видит «удаление с задержкой». UI sub-flow может рассмотреть спиннер.
  - **Failure при deferred:** `translation` не менялся в state ⇒ failure не требует rollback. Только notification.
  - **Idempotent DELETE:** если effect улетел до того, как параллельный путь уже удалил в БД — `deleteLexemeTranslation` no-op'ит. Success ⇒ `RefreshTranslation(null)` ставит `translation = null` (если ещё не null). Acceptable.

---

#### `UpdateLexemeDefinition(wordId: Long, lexemeId: Long, definition: String)`

Симметрично `UpdateLexemeTranslation`.

- **Source:** `CommitDefinitionEdit` UI Msg, ветвь 3.
- **Handler:** `wordCardUseCase.addLexemeDefinition(wordId, lexemeId, definition)` на `Dispatchers.IO` в try/catch + F018 assert.
- **Action:** UPSERT definition в БД.
- **Return Msg:**
  - success: `Msg.RefreshDefinition(lexemeId, definition: String?)`.
  - null или throw: `Msg.UpdateDefinitionFailed(lexemeId)`.
  - `lexemeId == NOT_IN_DB` (F018): `NoOperation`.
- **Reducer-логика:** симметрично `RefreshTranslation` / `UpdateTranslationFailed` (только для поля `definition`).
- **Edge cases:** симметрично `UpdateLexemeTranslation`.

---

#### `RemoveDefinition(lexemeId: Long)`

Симметрично `RemoveTranslation`.

- **Source:** двух источников:
  1. `RemoveDefinition` UI Msg.
  2. `CommitDefinitionEdit` UI Msg ветка 1 (`edited.isEmpty()`).
- **Handler:** `wordCardUseCase.deleteLexemeDefinition(lexemeId)` на `Dispatchers.IO` в try/catch.
- **Action:** DELETE definition из БД.
- **Return Msg:**
  - success: `Msg.RefreshDefinition(lexemeId, definition = null)`.
  - throw: `Msg.RemoveDefinitionFailed(lexemeId)`.
- **Reducer-логика:** симметрично `RemoveTranslation` (deferred: финальный nullify приходит через `RefreshDefinition(null)`).
- **Edge cases:** симметрично `RemoveTranslation` (transient view-mode окно для definition).

---

### Datasource Msg (сводно — return Msg от Datasource effects)

```kotlin
package me.apomazkin.wordcard.mate

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {
    // ... UI Msg (из contract_ui_msg) ...

    // --- Datasource return Msg (канон в contract_io) ---
    data class WordLoaded(val term: Term) : Msg
    data object WordNotFound : Msg
    data object WordLoadFailed : Msg                          // F005/F010

    data object UpdateWordFailed : Msg                        // F011
    data object RemoveWordFailed : Msg                        // F011

    data class RefreshLexeme(val lexeme: Lexeme) : Msg
    data object CreateLexemeFailed : Msg
    data class LexemeRemoved(val lexemeId: Long) : Msg
    data class RemoveLexemeFailed(val lexemeId: Long) : Msg   // F011

    data class RefreshTranslation(
        val lexemeId: Long,
        val translation: String?,                             // F003 — String?, не TextValueState?
    ) : Msg
    data class UpdateTranslationFailed(val lexemeId: Long) : Msg
    data class RemoveTranslationFailed(                       // F011 (без savedOrigin — deferred)
        val lexemeId: Long,
    ) : Msg

    data class RefreshDefinition(
        val lexemeId: Long,
        val definition: String?,                              // F003
    ) : Msg
    data class UpdateDefinitionFailed(val lexemeId: Long) : Msg
    data class RemoveDefinitionFailed(
        val lexemeId: Long,
    ) : Msg

    // --- inbound от UiEffectHandler (не от UI) ---
    data class ShowNotification(val text: String) : Msg       // F001/F006 — handler уже resolve'ил
}
```

> 📎 guide: docs/guides/messages.md — "Результаты эффектов: прошедшее время или существительное (*Loaded, *Update, *Skipped)"

**Замечания по сигнатурам:**

- `RefreshLexeme(lexeme: Lexeme)` — payload остаётся domain `Lexeme` (handler получает его от use-case как есть; reducer маппит в `LexemeState`).
- `LexemeRemoved(lexemeId: Long)` — payload только id.
- `RefreshTranslation/Definition` — после **F003**: payload `String?`, не `TextValueState?`. Reducer собирает `TextValueState` локально (см. F015 reducer-логику).
- `RemoveTranslationFailed`/`RemoveDefinitionFailed` — **новые**, payload только `lexemeId` (без `savedOrigin` — deferred подход не nullify'ит state, нечего восстанавливать).
- `WordLoadFailed`, `UpdateWordFailed`, `RemoveWordFailed`, `RemoveLexemeFailed` — новые (F011 — все Datasource effects покрыты failure-Msg).
- `ShowNotification(text: String)` — `text` уже resolved (handler через `ResourceManager`). См. F001/F006.

**Удаляется (legacy):**
- `Msg.LoadingWord` — антипаттерн, удалён в `contract_ui_msg`.

### Navigation Effect

```kotlin
package me.apomazkin.mate

sealed interface NavigationEffect : Effect {
    data object Back : NavigationEffect
}
```

**Замечание:** `NavigationEffect.Back` — базовый из `mate` framework, не per-screen sealed. Per-screen variant'ов нет.

> 📎 guide: docs/guides/navigation.md — "Базовый NavigationEffect.Back обрабатывается MateNavigationEffectHandler через WordCardNavigator.back() → navController.popBackStack()"

---

#### `NavigationEffect.Back`

- **Source:**
  1. `NavigateBack` UI Msg (системный back / тап стрелки в TopBar).
  2. `WordNotFound` Datasource Msg (после `LoadWord` — слово не найдено в БД).
  3. `WordLoadFailed` Datasource Msg (F005/F010 — исключение в `LoadWord`).
  4. `NavigateBack` Datasource Msg (после успешного `RemoveWord` effect — handler шлёт `Msg.NavigateBack`).
- **Navigator method:** `WordCardNavigator.back()` (унаследовано из `Navigator` интерфейса).
- **F019 idempotency без state-флага:** см. блок `RemoveWord` Edge cases. Защита через инфраструктуру (Navigator no-op + viewModelScope cancellation + БД idempotency), не через reducer-guard. Concurrent Back возможен в edge-case double-tap — acceptable.
- **Почему один Effect-вариант на все источники:** действие одно (popBackStack), все пути идентичны для пользователя. См. `docs/guides/navigation.md`.

---

### UI Effects

```kotlin
package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import me.apomazkin.mate.Effect

sealed interface UiEffect : Effect {
    data class ShowNotification(@StringRes val textRes: Int) : UiEffect   // F001/F006
}
```

#### `UiEffect.ShowNotification(textRes: Int)`

- **Source:** Datasource effect-handler failure cases (F011) — reducer'ы failure-Msg эмитят:
  1. `WordLoadFailed` ⇒ путь Back (без Snackbar — экран закрывается; альтернативное «остаться + Snackbar» отвергнуто, см. LoadWord reducer-секцию).
  2. `UpdateWordFailed` ⇒ `ShowNotification(R.string.word_card_update_word_failed)`.
  3. `RemoveWordFailed` ⇒ `ShowNotification(R.string.word_card_remove_word_failed)`.
  4. `CreateLexemeFailed` ⇒ `ShowNotification(R.string.word_card_create_lexeme_failed)`.
  5. `RemoveLexemeFailed` ⇒ `ShowNotification(R.string.word_card_remove_lexeme_failed)`.
  6. `UpdateTranslationFailed` ⇒ `ShowNotification(R.string.word_card_update_translation_failed)`.
  7. `RemoveTranslationFailed` ⇒ `ShowNotification(R.string.word_card_remove_translation_failed)`.
  8. `UpdateDefinitionFailed` ⇒ `ShowNotification(R.string.word_card_update_definition_failed)`.
  9. `RemoveDefinitionFailed` ⇒ `ShowNotification(R.string.word_card_remove_definition_failed)`.
- **UI Action:** Snackbar — `SnackbarHostState.showSnackbar(text)`.
- **Pattern: через State** (`snackbarState`), не side-channel.

> 📎 guide: docs/guides/theme-and-resources.md — "ResourceManager.stringByResId(@StringRes id) — resolve строки в handler; payload Effect — @StringRes Int, локализация из коробки"

- **Effect-handler:** `UiEffectHandler.onEffect` (`UiEffectHandler.kt`) принимает `UiEffect.ShowNotification(textRes)`, resolve'ит через инжектированный `ResourceManager` (`me.apomazkin.ui.resource.ResourceManager`):
  ```kotlin
  class UiEffectHandler @Inject constructor(
      private val resourceManager: ResourceManager,
  ) : MateTypedEffectHandler<Msg, UiEffect>() {
      override fun filter(effect: Effect): UiEffect? = effect as? UiEffect
      override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
          val msg = when (effect) {
              is UiEffect.ShowNotification ->
                  Msg.ShowNotification(text = resourceManager.stringByResId(effect.textRes))
          }
          consumer(msg)
      }
  }
  ```
- **Reducer-логика inbound `ShowNotification(text)`:** `snackbarState = state.snackbarState.copy(title = text, show = true)`.
- **Edge cases:**
  - **F022 — Concurrent ShowNotifications:** Compose M3 `SnackbarHostState.showSnackbar()` — suspending. Два Show в один tick идут в очередь; `snackbarState.title` показывает последний resolved text (предыдущий перетёрт). Acceptable — пользователь видит свежее сообщение.
  - **F041 — DismissNotification batching:** если пользователь дёрнул snackbar (DismissNotification UI Msg) пока следующий `ShowNotification` уже летит — dismiss применится первым (`show = false`), следующий `ShowNotification` снова выставит `show = true`. Acceptable.
  - **Локализация:** payload — `@StringRes Int`. Resolve в handler через `ResourceManager.stringByResId(id)`. Multi-locale поддерживается из коробки.
  - **i18n с параметрами:** если нужен `String.format(arg)` — добавить `vararg formatArgs: Any` в `UiEffect.ShowNotification` и `ResourceManager.stringByResId(id, value)` (уже есть в `ResourceManager.kt:5`). На текущей итерации все error-сообщения статичные — не нужно. Backlog (F030 минор: null vs throw разграничение тоже в backlog).

**Почему pattern «через State», не side-channel (Channel/SharedFlow):**
- `SnackbarState` retain через config change бесплатно.
- Side-channel (Channel) ⇒ snackbar пропадает при rotate. Не подходит.

> 📎 guide: docs/guides/ui-patterns.md — "Показ snackbar — UI-эффект через флаг в state допустим для toast/snackbar"

## Глобальные guards / lifecycle (F016)

> 📎 guide: docs/guides/state-modeling.md — "Считать варианты State: guards отсекают невалидные комбинации (isLoading=true, id=NOT_IN_DB) на уровне reducer'а"

Сводная таблица — что блокируется на каких state-условиях. Все guards — shortcut-ignore (`state to emptySet()`). **Все guards основаны на существующих state-полях** (F019 откат — `isRemovingWord` отсутствует).

| State condition | Заблокированные UI Msg |
|---|---|
| `isLoading == true` | `CreateLexeme`, `RemoveWord`, `OpenDeleteWordDialog`, `EnterWordEditMode`, `CommitWordChanges` |
| `wordState.id == NOT_IN_DB` | `CreateLexeme`, `RemoveWord`, `OpenDeleteWordDialog`, `EnterWordEditMode`, `CommitWordChanges` |
| `isCreatingLexeme == true` | `CreateLexeme` (duplicate-FAB guard) |
| `wordState.isEditMode == true` | `EnterWordEditMode` (повторный вход) |
| `wordState.isEditMode == false` | `UpdateWordInput`, `ExitWordEditMode`, `CommitWordChanges` |
| `lexemeList.none { id == lexemeId }` | все Msg с `lexemeId` (Remove*/Update*/Create*Sub/Enter*/Cancel*/Commit*) |
| `lexeme.translation == null` | `EnterTranslationEditMode`, `UpdateTranslationInput`, `CommitTranslationEdit`, `CancelTranslationEdit`, `RemoveTranslation` (F003 minor) |
| `lexeme.translation != null` | `CreateTranslation` |
| `!lexeme.translation.isEdit` | `UpdateTranslationInput`, `CommitTranslationEdit`, `CancelTranslationEdit` |
| `lexeme.translation.isEdit` | `EnterTranslationEditMode` |
| `lexeme.definition == ...` / `.isEdit` | симметрично translation |

**Datasource Msg find-guard (F009):** все Msg, адресующие лексему по id, начинаются с:
```
val lex = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
```

Применяется к: `RefreshTranslation`, `RefreshDefinition`, `UpdateTranslationFailed`, `UpdateDefinitionFailed`, `LexemeRemoved`, `RemoveLexemeFailed`, `RemoveTranslationFailed`, `RemoveDefinitionFailed`.

**F037 — общая сноска:** `OpenLexemeMenu` не в таблице — это переключатель, его reducer применяет find-guard как часть общей сноски `contract_ui_msg` («лексема не найдена ⇒ empty»), не отдельная строка.

**Logger TAG (F039):** все `Log.w(...)` в Datasource handler'ах используют `private const val TAG = "DatasourceEffectHandler"` — единое значение для grep'ания в logcat.

## Subscribers

> 📎 guide: docs/guides/effect-handlers.md — "MateFlowHandler — долгоживущая подписка на реактивные источники (Room Flow, DataStore); subscribe при init, unsubscribe при dispose"

**Subscribers не требуются.**

Wordcard — замкнутая feature: экран открывается с навигационным аргументом `wordId`, читает данные один раз через `initEffects`, мутирует БД только через свои собственные effects (которые сразу возвращают `Refresh*` Msg для синхронизации state). Сторонних писателей в `Term` / `Lexeme` нет (см. бизнес-инварианты ниже).

Subscribers были бы нужны если бы:
- Другой экран (Settings, ProfileTab, и т.п.) мог менять слово/лексему в той же сессии. **Нет**: wordcard — terminal-экран.
- Был бы flow от prefs / DataStore меняющий рендер. **Нет**.

Self-check 4 вопроса — в разделе ниже.

## Проверка реактивности

1. **Может ли state экрана устареть, если другой экран изменит данные в БД?** — **N**. Wordcard — terminal-экран в навигационной иерархии: пока он открыт, никакой другой экран не активен (single-task Compose Navigation, parent (DictionaryTab) — в backstack). Cross-screen edit на ту же `Term` невозможен без возврата назад, а возврат уничтожает ViewModel.

2. **Есть ли настройки/пользовательские preferences, изменение которых должно отразиться?** — **N**. WordCard не читает prefs. Локаль текста — через Compose-локали, не через state.

3. **Активный экран должен реагировать на push-нотификации / system events / lifecycle?** — **N**. Lifecycle — через ViewModel. Push'ей в проекте нет. БД-sync с сервером нет (offline-only).

4. **Есть ли DAO-flow / DataStore-flow / другие реактивные источники эмитящие изменения данных этого экрана?** — **N**. `WordCardUseCase` экспортирует только `suspend` функции (`getTermById`, `deleteWord`, ...) — нет `Flow`-методов.

   > **Note (F020):** ответ зафиксирован при ТЕКУЩЕМ архитектурном решении DAO non-reactive. Если в будущем мигрируем на reactive Room flow (через `Flow<Term>` из DAO) — этот вопрос пересматривается; вероятно потребуется subscriber на `getTermFlow(wordId)` + удалить ручные `Refresh*` Msg.

**Ни одного Y → subscribers не требуются.** Self-check пройден.

## Бизнес-инварианты

Список инвариантов, обосновавших отсутствие subscribers:

1. **Wordcard — terminal-экран.** Пока wordcard открыт, никакой другой экран приложения не может писать в `Term(wordId)` или связанные `Lexeme`. Запись в БД на эти сущности идёт **только** через сам wordcard.
2. **Возврат назад уничтожает ViewModel.** `viewModelScope` отменяется при popBackStack ⇒ subscribe-корутины автоматически останавливаются.
3. **БД offline-only.** Проект не синхронизируется с сервером ⇒ нет внешних writer'ов.
4. **DAO у `WordCardUseCase` неreactive.** Архитектурное решение: для wordcard достаточно одноразового `getTermById` + локальной `Refresh*`-синхронизации после собственных мутаций.

**Tech debt по F035 (word commit оптимистичный):** reducer `CommitWordChanges` записывает `wordState.value = wordState.edited` сразу, до подтверждения БД. При failure `UpdateWord` БД хранит старое значение, state — новое. Silent revert через следующий `LoadWord` при возврате на экран. См. Feedback в `contract_ui_msg`.

Если хотя бы один из 4 инвариантов нарушится в будущем (например: появится cross-screen edit, или backend-sync, или DAO станет reactive — F020 note) — потребуется ввести subscriber. На текущем этапе IS479 — не нужны.

## Edge cases (cross-effect)

### EC1 — Race: пользователь тапает FAB во время пришедшего failure

**Сценарий:**
1. FAB ⇒ `CreateLexeme` ⇒ `isCreatingLexeme = true`, effect ушёл.
2. Effect возвращает `CreateLexemeFailed` (F011 try/catch) ⇒ `isCreatingLexeme = false` + `ShowNotification`.
3. FAB снова ⇒ `CreateLexeme` ⇒ effect ушёл, `isCreatingLexeme = true`.

**Поведение:** корректно. Retry работает.

### EC2 — Race: Commit translation во время приходящего Refresh (F013/F015/F021)

**Сценарий:**
1. Commit translation (ветвь 3) ⇒ `UpdateLexemeTranslation` effect улетел, `translation.isEdit = false`, `edited` отличен от `origin`.
2. До прихода `RefreshTranslation`, пользователь снова входит в edit-mode (`EnterTranslationEditMode` — допустимо, guard пропускает: `isEdit == false`) → меняет → Commit (ветвь 3) ⇒ второй effect улетел.
3. **Mate не сериализует effects** (F021/F032 corrected): порядок завершения не гарантирован. Который из двух `RefreshTranslation` придёт первым — undefined.
4. Reducer F015-logic применяется к каждому Refresh независимо: `lex.translation.isEdit` — текущее состояние? Зависит от race.
   - Если пользователь между 2 и 3 успел снова войти в edit → `isEdit == true` → обновляется только `origin`, `edited` не трогаем. Acceptable.
   - Если пользователь вышел из edit (после второго Commit `isEdit == false`) → reducer обновит `origin` и `edited = origin` под значение того Refresh, который пришёл. Acceptable transient: на короткое окно `edited` может показать промежуточное значение. Потом приходит второй Refresh — финальный state определяется last-arrived.

**Поведение:** допустимо. Финальный state согласован с **last-written в БД** (которое тоже не гарантировано совпадает с last-committed-from-UI при race UPDATE/UPDATE — F021 last-write-wins на БД).

**Альтернатива (отвергается):** очередь Commit'ов с lock'ом до прихода Refresh. Over-engineering.

### EC3 — Race: Remove lexeme во время edit translation/definition

**Сценарий:**
1. Translation в edit-mode.
2. Меню лексемы → `RemoveLexeme` ⇒ effect улетел, `isMenuOpen = false`.
3. До прихода `LexemeRemoved`, пользователь продолжает печатать ⇒ `UpdateTranslationInput` ⇒ reducer пишет в `lexemeList[i].translation.edited`.
4. `LexemeRemoved` приходит ⇒ `lexemeList` filter'ит лексему — `translation` исчезает.

**Поведение:** корректно. Update'ы между Remove и `LexemeRemoved` бессмысленны, но не вредят.

UI sub-flow может рассмотреть disable inputs после `RemoveLexeme` ⇒ progress-индикатор. Не в скоупе IS479.

### EC4 — `WordNotFound` параллельно с попыткой `CreateLexeme`

**Сценарий:** `initEffects` шлёт `LoadWord`, до возврата `WordNotFound` пользователь успевает тапнуть FAB.

**Поведение:** `CreateLexeme` UI Msg отфильтрован guard'ом `isLoading == true` (`isLoading = true` пока `WordLoaded`/`WordNotFound`/`WordLoadFailed` не пришло). Race заблокирован.

### EC5 — Deferred nullify + transient view-mode окно (legitimate)

**Сценарий:**
1. Commit translation ветка 1 (`edited.isEmpty()`) ⇒ reducer ставит `translation.isEdit = false`, **не трогает `translation` field**. Effect `RemoveTranslation(lexemeId)` улетел.
2. **Transient окно** (несколько ms до прихода `RefreshTranslation(null)` от data):
   - State: `lex.translation = TextValueState(origin = "перевод", edited = "", isEdit = false)`.
   - View-mode selector (`toValue(isEdit=false)`) показывает `origin = "перевод"` — старое значение перевода.
   - **Пользователь видит «старое значение» доли секунды до того как оно исчезнет.** Это легитимное transient-окно, явно фиксируется как часть контракта.
3. `RefreshTranslation(lexemeId, translation = null)` приходит ⇒ reducer: `lex.translation = null`. Chip снова в состоянии «Перевод» (плейсхолдер «нет значения»).

**Поведение:** корректно по контракту deferred-подхода. Альтернатива (immediate nullify в reducer) — изменение reducer-логики UI Msg, вне scope.

**Sub-сценарий (failure):**
4. Если effect фейлится ⇒ `RemoveTranslationFailed(lexemeId)`. Reducer: только notification. `translation` не менялся — state согласован с тем, что в БД (старое значение). Пользователь видит chip со старым переводом + Snackbar «не удалось удалить».

**Поведение:** корректно. Failure не требует rollback (нечего восстанавливать).

### EC6 — Double-RemoveWord без state-флага (F019 откат)

**Сценарий:**
1. Confirmation-диалог → тап «Удалить» ⇒ `RemoveWord(wordId)` UI Msg ⇒ reducer закрывает диалог + меню, effect `RemoveWord(wordId)` улетел.
2. UI ещё не закрылся (диалог пропадает доли секунды) — пользователь тапает «Удалить» второй раз.
3. Второй `RemoveWord(wordId)` UI Msg ⇒ reducer: guard на `wordState.id == NOT_IN_DB || isLoading` (если `isLoading == false` — guard не сработает); effect `RemoveWord(wordId)` улетает второй раз.
4. Два DELETE в БД — первый удаляет, второй no-op (БД idempotent).
5. Первый handler возвращает `NavigateBack` ⇒ `NavigationEffect.Back` ⇒ экран pop'ается, `viewModelScope` cancellable.
6. Второй handler либо отрабатывает на already-popped screen (Navigator no-op), либо отменяется CancellationException.

**Поведение:** только один visible Back. Tech debt: возможно второй DELETE улетит в БД. Acceptable (idempotent).

**Альтернатива (вне scope):** выставлять `isLoading = true` в reducer'е UI Msg `RemoveWord` — закроет double-tap полностью + предотвратит concurrent CreateLexeme/Translation/Definition. **Feedback в `contract_ui_msg`** (см. ниже).

## Feedback в предыдущие шаги

Раздел фиксирует архитектурные точки, которые **не могут быть решены в рамках `contract_io`** (не меняют ни Datasource Msg, ни effect-handler — требуют правок в shape State или reducer-логике UI Msg). Conductor решает: принять feedback (откатить нужный шаг и провести правки) или зафиксировать в backlog.

### 1. `contract_ui_msg` — reducer `RemoveWord` Msg может выставлять `isLoading = true`

**Что:** в reducer-логике UI Msg `RemoveWord` дополнительно выставлять `state.isLoading = true` одновременно с эмитом effect `DatasourceEffect.RemoveWord(wordId)`.

**Почему:**
- Блокирует concurrent `CreateLexeme` / `EnterTranslationEditMode` / `EnterDefinitionEditMode` / повторный `RemoveWord` через существующий guard `isLoading == true` (см. таблицу выше).
- Закрывает F033/F034 (orphan-lexeme race) и часть F019 (double-RemoveWord) **без введения нового state-поля** — переиспользует существующее `isLoading`.

**Почему не решено в `contract_io`:**
- Reducer-логика UI Msg — scope `contract_ui_msg`, не `contract_io`. `contract_io` канонизирует Datasource Msg, но не переопределяет reducer UI Msg.

**Tech debt если не принять:**
- Двойной `RemoveWord` effect возможен (idempotent на БД-уровне — acceptable).
- Orphan-lexeme race возможен (CreateLexeme после RemoveWord ⇒ INSERT упадёт по FK или будет каскадно удалён — acceptable).

### 2. `contract_state` — tech debt: `CommitWordChanges` оптимистическая запись без savedValue

**Что:** reducer `CommitWordChanges` (см. `contract_ui_msg.md` v2) пишет `wordState.value = wordState.edited` сразу, до подтверждения БД. При failure `UpdateWord` state и БД рассинхронизированы; silent revert через следующий `LoadWord` при возврате на экран.

**Симметрия с translation/definition:** translation/definition reducer **не** меняют `origin` сразу (deferred — ждут Refresh). Word reducer меняет `value` сразу. Асимметрия.

**Варианты разрешения:**
- (а) **scope `contract_ui_msg`:** изменить reducer `CommitWordChanges` чтобы он не менял `value` сразу, ждать новый Datasource Msg `WordUpdated(text)` от handler'а `UpdateWord`. Симметрия восстанавливается.
- (б) **scope `contract_state`:** добавить `savedValue: String?` в `WordState` для rollback при failure. Shape state change.

**Tech debt принят (wontfix):**
- F035 — симметрия с deferred translation/definition тоже не идеальная (там failure не показывает старое значение визуально, потому что reducer не nullify'ил — но и не обновлял после Commit; пока приходит Refresh `origin = edited` — UI показывает старое `origin` через `toValue(false)`). Word и translation/definition оба имеют свои transient-окна; ни один не «правильнее» другого без введения savedValue.
- Решение в backlog.

### 3. `contract_ui_msg` — forward-ref таблица минимальна (F027)

**Что:** forward-ref таблица в `contract_ui_msg.md` v2 (раздел «Канон в contract_io») перечисляет 7 Datasource Msg: `WordLoaded`, `WordNotFound`, `RefreshLexeme`, `CreateLexemeFailed`, `RefreshTranslation`, `RefreshDefinition`, `ShowNotification`. **Не включает** failure-семейство, канонизированное в этом шаге: `WordLoadFailed`, `UpdateWordFailed`, `RemoveWordFailed`, `RemoveLexemeFailed`, `UpdateTranslationFailed`, `RemoveTranslationFailed`, `UpdateDefinitionFailed`, `RemoveDefinitionFailed`, `LexemeRemoved`.

**Почему может потребоваться правка:**
- Полный канон Msg ↔ Effect живёт в `contract_io` (источник истины). Расширение forward-ref в `contract_ui_msg` дублирует информацию, но повышает связность для reviewer'а UI Msg.

**Tech debt принят (wontfix):** разделение «UI-сторона в ui_msg, Datasource-сторона в io» считается достаточным. `contract_ui_msg` намеренно остаётся минимальным forward-ref. Полный список — в этом артефакте.

## Расхождения spec ↔ code

**Не применимо.** Режим работы — 1 (макет-driven), не 2 (spec-driven). Спека отсутствует (`spec_filename = null`). Сверка с источником истины (Figma + текущий код) встроена в блоки effects:

- **F005/F010 — `LoadWord` failure** — handler try/catch + новый `WordLoadFailed` Msg, reducer-ветка `WordNotFound`/`WordLoadFailed` ⇒ `NavigationEffect.Back` (вместо `TODO()` в `WordCardReducer.kt:20`).
- **F011 — все Datasource handlers оборачиваются в try/catch.** Throw на null от use-case (`DatasourceEffectHandler.kt:78/87/96`) заменяется на failure-Msg.
- **F001/F006 — `UiEffect.ShowNotification(textRes: Int)`** — переход с `String` на `@StringRes Int`. `UiEffectHandler` инжектирует `ResourceManager` и resolve'ит.
- **F003 — `RefreshTranslation/Definition(lexemeId, value: String?)`** — payload `String?` вместо `Lexeme` (`Message.kt:35,42`) или `TextValueState?`.
- **F008 — `Update*Failed` rollback** — guard на `isEdit` в reducer'е.
- **F009 — find-guard** во всех адресных Datasource Msg.
- **F015 — `RefreshTranslation/Definition` при isEdit=true** — обновляется только `origin`, `edited` не трогаем.
- **F018 — defense-in-depth `lexemeId != NOT_IN_DB` assert** в `UpdateLexemeTranslation/Definition` handler.
- **F020 — note в Subscribers Q4** о пересмотре при миграции на reactive DAO.
- **F021/F032 — UpdateWord race acceptable, не «сериализация»** — явно зафиксировано: Mate запускает каждый effect в независимой корутине, порядок не гарантирован.
- **F022 — Snackbar batching** — явно зафиксировано в UI Effects Edge cases.
- **F039 — `private const val TAG = "DatasourceEffectHandler"`** — единый logger TAG в Datasource handler'ах.
- **Удаление маппинга в `RefreshLexeme` reducer** — старый маппинг на `isTranslationCheck`/`isDefinitionCheck` уходит.
- **Изменение return Msg `UpdateWord` handler'а** — с `LoadingWord` на `NoOperation`/`UpdateWordFailed`.
- **Изменение return Msg `Remove*` handler'ов (translation/definition)** — с `LoadingWord` на `RefreshTranslation/Definition(null)` (deferred финальный nullify) / `Remove*Failed` (failure).
- **Удаление `Msg.LoadingWord`** (legacy/dead).
- **Удаление `UiMsg` sub-interface + `UiMsg.ShowNotification`** — `ShowNotification` теперь прямой член `Msg` (inbound от UiEffectHandler).

### Не меняется (откаты ит.2 → ит.3)

- **`isRemovingWord: Boolean` поле в State** — не вводится (F019 откат). Idempotency через инфраструктуру.
- **`RemoveWord` UI Msg payload** — остаётся `data class(wordId: Long)` как в `contract_ui_msg.md` v2 (F024 откат).
- **Reducer-логика `CommitTranslationEdit` ветка 1 / `RemoveTranslation` UI Msg** — остаётся deferred (F012 откат immediate nullify). Финальный nullify приходит через `RefreshTranslation(null)`.
- **`savedOrigin` payload в `RemoveTranslation/Definition` effect** — не вводится (deferred не требует rollback).
- **`savedOrigin` поле в `RemoveTranslationFailed/RemoveDefinitionFailed` Msg** — не вводится.

---

## log_messages

- contract_io v3: откаты ит.2 — `isRemovingWord` не вводится (F019), `RemoveWord(wordId)` сохранён (F024), deferred-подход для Commit-empty/Remove (F012), `savedOrigin` payload снят, F021/F032 переписан на «race acceptable».
- F005/F010 (WordLoadFailed), F011 (try/catch всех Datasource), F001/F006 (UiEffect.ShowNotification textRes:Int + ResourceManager), F003/F008/F009/F015 (payload-минимизация, guards) — сохранены.
- Раздел `Feedback в предыдущие шаги` добавлен: contract_ui_msg (isLoading=true в RemoveWord reducer), contract_state (savedValue для UpdateWord), forward-ref таблица в ui_msg минимальна (wontfix).

---

_model: claude opus 4.7 (1M context)_
