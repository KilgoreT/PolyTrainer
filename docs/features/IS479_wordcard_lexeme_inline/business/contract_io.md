# contract_io v7

> ⚠ **Мини-патч ит.7 — UX-инвариант `isPendingDbOp`.** Все Effect-handler'ы ОБЯЗАНЫ на каждом control-path (success / failure) вернуть Msg, который reducer-веткой сбросит `state.isPendingDbOp = false`. Иначе UI зависнет в заблокированном состоянии. Список «разблокирующих» Msg: `WordLoaded` / `WordNotFound` / `RefreshWord` / `RefreshTranslation` / `RefreshDefinition` / `RefreshLexemeList` / `LexemeCascadeRemoved` / `NavigateBack` / `ShowNotification`. Set `isPendingDbOp = true` происходит в reducer-ветках UI Msg, отправляющих DB-Effect (см. `contract_ui_msg` v3.2). F062/F063/F066 (concurrent Commit races) сняты архитектурно: UI-блокировка через `isPendingDbOp` не пропустит второй пользовательский ввод между отправкой Effect и приходом confirm-Msg. F065 — `Msg.NavigateBack` зафиксирован как shared Msg между `contract_ui_msg` и `contract_io` (объявление в ui_msg, dispatch — отсюда). F064 atomicity-ремарка, F067 EC8 trade-off, F068 ordering UX-ремарка.
>
> ⚠ **Архитектурный пересмотр под NOT_IN_DB (ит.6).** Лексема создаётся в БД **только** при первом Commit Translation/Definition, а не при тапе FAB. Эффекта `CreateLexeme` больше нет, Msg `RefreshLexeme` и `CreateLexemeFailed` сняты. `UpdateLexemeTranslation` / `UpdateLexemeDefinition` принимают `lexemeId: Long?` — при `null` handler делает insert лексемы + insert суб-сущности атомарно и возвращает реальный id через `RefreshTranslation` / `RefreshDefinition`. Контракт синхронизирован с `contract_state` v2.5 и `contract_ui_msg` v3.2.
>
> Этот артефакт — финальный канон Effects + Datasource Msg + Edge cases. Subscriber отсутствует (см. `contract_io_approved.md`, approved findings ит.1). Cross-screen реактивности у WordCard нет: все обновления идут через handler-Msg от `suspend`-вызовов UseCase. `Mate.runEffect(effect, consumer)` допускает несколько вызовов `consumer` на один эффект — этим пользуемся в failure-ветках.

## Режим работы

**Режим 1 — макет-driven.** Источники: Figma `w8GmGCdOZJUi99Cuv4q4W9` (frames `9154-82519`, `9154-82532`, `9154-82521`, `9154-82625`) + `contract_state` v2.5 + `contract_ui_msg` v3.2 + существующий `DatasourceEffectHandler.kt` / `WordCardReducer.kt` / `WordCardUseCaseImpl.kt` как «что есть сейчас» для diff. Спека отсутствует (`spec_filename = null`).

## Scope артефакта

`contract_io` канонизирует:

1. **Effects** — `DatasourceEffect` (8 вариантов после удаления `CreateLexeme`) + переиспользование mate-овского `NavigationEffect.Back`. `UiEffect` нет (snackbar через State).
2. **Datasource Msg** — сигнатуры и reducer-логика для 9 Msg (см. § Datasource Msg).
3. **Edge cases** — concurrent / race / failure, не локализованные в одном effect.
4. **Расхождения spec ↔ code** — точечные изменения сигнатур `WordCardUseCase`.

> 📎 guide: docs/guides/effect-handlers.md — "DatasourceEffect: операции с БД, сетью, preferences; всегда на Dispatchers.IO"
> 📎 guide: docs/guides/effect-handlers.md — "Маппинг Effect → Message: handler конвертирует результат в Msg, consumer вызывается только при полезном msg"
> 📎 guide: docs/guides/messages.md — "Результаты эффектов: прошедшее время или существительное (*Loaded, *Update, *Skipped)"

## Datasource Msg (канон)

```kotlin
package me.apomazkin.wordcard.mate

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {

    // ... (UI Msg см. contract_ui_msg v3.2)

    // --- Datasource Msg ---

    /** Результат LoadWord (success). Несёт Term, лексемы внутри term.lexemeList. */
    data class WordLoaded(val word: Term) : Msg

    /** Результат LoadWord (БД не нашла слово по id). */
    data object WordNotFound : Msg

    /** Resync слова в state после UpdateWord. payload — fresh Term из БД. */
    data class RefreshWord(val word: Term) : Msg

    /**
     * Результат UpdateLexemeTranslation / RemoveTranslation (success).
     * Адресация по lexemeId — handler знает реальный id после insert/update в БД
     * (для первого Commit NOT_IN_DB → реальный id; reducer заменяет в lexemeList).
     * translation: String? — non-null = новое значение origin, null = translation удалён в БД.
     */
    data class RefreshTranslation(val lexemeId: Long, val translation: String?) : Msg

    /**
     * Результат UpdateLexemeDefinition / RemoveDefinition (success).
     * Симметрично RefreshTranslation, включая замену NOT_IN_DB → реальный id для первого Commit.
     */
    data class RefreshDefinition(val lexemeId: Long, val definition: String?) : Msg

    /** Результат RemoveLexeme (success). Обновлённый список лексем без удалённой. */
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg

    /**
     * Результат RemoveTranslation / RemoveDefinition (success), при котором data-слой
     * каскадно удалил лексему как последнюю с заполненной суб-сущностью. Адресация по lexemeId —
     * reducer удаляет лексему из lexemeList целиком. См. F045 (ит.5).
     */
    data class LexemeCascadeRemoved(val lexemeId: Long) : Msg

    /** Snackbar. Effect-handler шлёт после ошибки / спец-случая. */
    data class ShowNotification(val text: String) : Msg

    // Msg.NavigateBack объявлен в contract_ui_msg v3.2 (shared с UI back-кнопкой).
    // Datasource handler RemoveWord после success шлёт consumer(Msg.NavigateBack) —
    // используется тот же Msg, без дублирования.
}
```

> **F065 (ит.7) — `Msg.NavigateBack` shared:** объявление живёт в `contract_ui_msg` v3.2 (как UI Msg от тапа back). `contract_io` НЕ дублирует объявление. Datasource handler `RemoveWord` success вызывает `consumer(Msg.NavigateBack)`, который reducer-веткой (определённой в ui_msg) превращает в `setOf(NavigationEffect.Back)`. Одна reducer-ветка, два источника (UI-intent + effect-success-confirm).

**Финальный scope (8 Datasource Msg от handler'а + 1 shared) — F055 + F065 ит.7:**

| # | Msg | Источник | Объявление |
|---|---|---|---|
| 1 | `WordLoaded(term)` | `LoadWord` success | `contract_io` |
| 2 | `WordNotFound` | `LoadWord` 404 | `contract_io` |
| 3 | `RefreshWord(term)` | `UpdateWord` success | `contract_io` |
| 4 | `RefreshTranslation(lexemeId, translation?)` | `UpdateLexemeTranslation` success / `RemoveTranslation` (TranslationRemoved) | `contract_io` |
| 5 | `RefreshDefinition(lexemeId, definition?)` | `UpdateLexemeDefinition` success / `RemoveDefinition` (DefinitionRemoved) | `contract_io` |
| 6 | `RefreshLexemeList(lexemes)` | `RemoveLexeme` success | `contract_io` |
| 7 | `LexemeCascadeRemoved(lexemeId)` | `RemoveTranslation` / `RemoveDefinition` (LexemeCascadeRemoved) | `contract_io` |
| 8 | `ShowNotification(text)` | любой effect failure / спец-случай | `contract_io` |
| — | `NavigateBack` (shared) | `RemoveWord` success (handler dispatch) + UI back-кнопка (UI-intent) | **`contract_ui_msg` v3.2** |

**UX-инвариант (ит.7):** каждый из Msg 1–8 + `NavigateBack` (когда приходит от Datasource-confirm) — это **разблокирующий Msg**: reducer-ветка ОБЯЗАНА сбросить `state.isPendingDbOp = false`. См. `contract_ui_msg` v3.2 (раздел «Datasource Msg reducer-логика — сброс isPendingDbOp»). Без этого UI зависнет.

**Сняты в ит.6** (по сравнению с v5):
- `RefreshLexeme(lexeme)` — нет источника. Effect `CreateLexeme` удалён; лексема инсертится в БД при первом Commit Translation/Definition через `UpdateLexeme*` handler, реальный id возвращается через `Refresh*`.
- `CreateLexemeFailed` — нет источника. UI Msg `CreateLexeme` не порождает effect, нет failure-канала.

> 📎 guide: docs/guides/messages.md — "data class (с данными) для действий с параметрами от UI или эффектов"
> 📎 guide: docs/guides/messages.md — "Msg = sum type sealed interface; data object для no-payload, data class для payload"

**Payload-конвенции (для всех Refresh*-Msg):**

- `RefreshTranslation` / `RefreshDefinition` — payload `(lexemeId: Long, translation: String?)` / `(lexemeId: Long, definition: String?)`. Адресация — `lexemeId` (всегда реальный id из БД, даже если до Commit лексема жила локально с `NOT_IN_DB`). Семантика: «новое значение origin или удалено». `null` ⇒ суб-сущность удалена в БД (после Remove или contract-violation impl, см. F047); non-null ⇒ новое значение `origin`. `contract_io` гарантирует: **non-null `translation`/`definition` — всегда непустая строка** (БД не хранит пустых, see F037 в `contract_ui_msg`). Reducer-замена `NOT_IN_DB → реальный lexemeId` — обязательна для завершения «первого Commit» лексемы (см. `contract_ui_msg` v3.2, раздел «Datasource Msg reducer-логика — замена NOT_IN_DB»). Reducer-ветка также сбрасывает `isPendingDbOp = false` (UX-инвариант ит.7).
- `RefreshLexemeList` — payload `List<Lexeme>` (обновлённый список лексем слова после `RemoveLexeme`). На практике от `RemoveLexeme` приходит список без одной лексемы. Merge-by-id reducer-логика сохраняет UI-state существующих, добавляет недостающие, выкидывает отсутствующие. Источник — `WordCardUseCase.deleteLexeme(wordId, lexemeId): List<Lexeme>?`.
- `LexemeCascadeRemoved` — payload `lexemeId: Long` (реальный id). Семантика: data-слой при `RemoveTranslation`/`RemoveDefinition` обнаружил, что удаляемая суб-сущность — последняя у лексемы, и каскадно удалил саму лексему (см. `WordCardUseCaseImpl.deleteLexemeTranslation`/`deleteLexemeDefinition`). Reducer удаляет лексему из `lexemeList` целиком. Источник — sealed `RemoveTranslationResult.LexemeCascadeRemoved` / `RemoveDefinitionResult.LexemeCascadeRemoved` (F045 ит.5).
- `RefreshWord` — payload `Term`. После `UpdateWord` приводит `wordState.value`/`added` в согласие с БД, **не трогая** `isEditMode`/`edited`/`showWarningDialog` (UI-state).
- `WordLoaded` — payload `Term`. Лексемы внутри `term.lexemeList`. Атомарно конструирует `WordState.Loaded` + `lexemeList` в одном reduce-шаге (инв. 11 — `isLoading = false`).
- `ShowNotification(text)` — payload `String`, non-empty (инв. 8 — `show=true ⇒ title != ""`). Reducer-ветка ОБЯЗАНА сбросить `isPendingDbOp = false` (UX-инвариант ит.7) — иначе UI зависнет.
- `NavigateBack` (shared — объявление в `contract_ui_msg` v3.2) — без payload. Reducer-ветка одна (определена в ui_msg) → `setOf(NavigationEffect.Back)` + сброс `isPendingDbOp = false`. Datasource handler `RemoveWord` шлёт его как success-confirm.

## Effects

### Datasource Effect

```kotlin
package me.apomazkin.wordcard.logic

import me.apomazkin.mate.Effect

sealed interface DatasourceEffect : Effect {

    // --- Word level ---
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    // --- Lexeme level (CreateLexeme effect ОТСУТСТВУЕТ — лексема живёт локально с NOT_IN_DB) ---
    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    // --- Translation / Definition ---
    data class UpdateLexemeTranslation(
        val wordId: Long,
        val lexemeId: Long?,   // null ⇒ handler делает insert лексемы + insert translation атомарно
        val translation: String,
    ) : DatasourceEffect

    data class RemoveTranslation(val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        val lexemeId: Long?,   // null ⇒ handler делает insert лексемы + insert definition атомарно
        val definition: String,
    ) : DatasourceEffect

    data class RemoveDefinition(val lexemeId: Long) : DatasourceEffect
}
```

**8 эффектов** (после удаления `CreateLexeme` в ит.6).

> 📎 guide: docs/guides/effect-handlers.md — "DatasourceEffect: операции с БД, сетью, preferences; всегда на Dispatchers.IO"

---

#### `DatasourceEffect.LoadWord(wordId)`

- **Source:** `initEffects` ViewModel'я (`WordCardViewModel.kt`). Не UI Msg.
- **Action:** `WordCardUseCase.getTermById(wordId): Term?` на `Dispatchers.IO`. Возврат `Term` уже содержит `lexemeList: List<Lexeme>` — отдельный запрос лексем не нужен.
- **Return Msg:**
  - `Term != null` ⇒ `consumer(Msg.WordLoaded(term))`.
  - `Term == null` ⇒ `consumer(Msg.WordNotFound)`.

  ```kotlin
  is DatasourceEffect.LoadWord -> withContext(Dispatchers.IO) {
      try {
          val term = wordCardUseCase.getTermById(effect.wordId)
          if (term != null) {
              consumer(Msg.WordLoaded(term))   // success: разблокирующий Msg
          } else {
              consumer(Msg.WordNotFound)       // 404: разблокирующий Msg
          }
      } catch (e: Exception) {
          // БД-ошибка при initial load — silent exit (как WordNotFound).
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.WordNotFound)
      }
  }
  ```
  **UX-инвариант (ит.7):** в `LoadWord` UI ещё не блокирует (init-effect шлётся до пользовательского ввода; `isPendingDbOp = false` стартово). Reducer-ветки `WordLoaded` / `WordNotFound` тем не менее идемпотентно сбрасывают `isPendingDbOp = false` (safety).
- **Reducer-логика `WordLoaded(word)`:**
  ```
  when (state.wordState) {
      is WordState.Loaded ->
          // Defence-in-depth: не затирать локальный edit / лексемы, если init уже отработал
          state to emptySet()
      WordState.NotLoaded ->
          state.copy(
              wordState = WordState.Loaded(
                  id = word.wordId.id,
                  added = word.addedDate,
                  value = word.word.value,
                  isEditMode = false,
                  edited = "",
                  showWarningDialog = false,
              ),
              lexemeList = word.lexemeList.map { it.toLexemeState() },
              isLoading = false,
              isPendingDbOp = false,   // UX-инвариант ит.7
          ) to emptySet()
  }
  ```
  Атомарно. `isLoading = false` (инв. 11). Маппер см. § Mapper.
- **Reducer-логика `WordNotFound`:**
  ```
  state.copy(isLoading = false, isPendingDbOp = false) to setOf(NavigationEffect.Back)
  ```
  `wordState` остаётся `NotLoaded`. Silent exit — без snackbar (экран без слова бесполезен).
- **Edge cases:**
  - **wordId не существует в БД** ⇒ `WordNotFound` + Exit.
  - **Concurrent LoadWord не возникает** — `initEffects` улетает ровно один раз при создании Mate.
  - **БД-ошибка (IOException, SQLite-corruption)** — try/catch шлёт `WordNotFound` (silent exit). Альтернатива «snackbar + полу-пустой UI» — UX-яма.
- **Почему `WordLoaded(word: Term)`, а не `WordLoaded(word, lexemes)`:** `Term` уже содержит `lexemeList` — раздельный payload лишний.

---

#### `DatasourceEffect.RemoveWord(wordId)`

- **Source:** `RemoveWord(wordId)` UI Msg (после confirmation-диалога).
- **Action:** `WordCardUseCase.deleteWord(wordId): Int` на `Dispatchers.IO`. Каскадно удаляет лексемы через FK ON DELETE CASCADE.
- **Return Msg:**
  - Success ⇒ `consumer(Msg.NavigateBack)`.
  - Failure (исключение) ⇒ `consumer(Msg.ShowNotification("Не удалось удалить слово"))`.

  ```kotlin
  is DatasourceEffect.RemoveWord -> withContext(Dispatchers.IO) {
      try {
          wordCardUseCase.deleteWord(effect.wordId)
          consumer(Msg.NavigateBack)                                  // success: разблокирующий Msg (shared с UI back)
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.ShowNotification("Не удалось удалить слово")) // failure: разблокирующий Msg
      }
  }
  ```
  **UX-инвариант (ит.7):** `RemoveWord` отправляется из reducer-ветки UI Msg `RemoveWord`, которая ставит `isPendingDbOp = true`. До прихода либо `NavigateBack` (success), либо `ShowNotification` (failure) — диалог удаления закрыт, UI заблокирован. Обе ветки гарантированно разблокируют.
- **Reducer-логика `NavigateBack`** (объявление в `contract_ui_msg` v3.2, shared):
  ```
  state.copy(isPendingDbOp = false) to setOf(NavigationEffect.Back)
  ```
  Одна reducer-ветвь, два источника (UI-intent от back-кнопки и effect-success-confirm).
- **Edge cases:**
  - **Concurrent RemoveWord (двойной тап)** — UI закрывает диалог в reducer-ветке `RemoveWord`. Повторный Msg не родится. Даже если родится — второй `deleteWord` no-op + второй `popBackStack` no-op.
  - **БД-ошибка** ⇒ `ShowNotification("Не удалось удалить слово")` (симметрично F042 ит.4).
  - **Silent success (F036):** при успешном удалении snackbar не показываем — exit экрана сам по себе feedback. Failure показывает snackbar — визуально ничего не изменилось.
- **Почему `NavigateBack` Msg, а не handler-direct-Navigation:** единая точка триггера навигации в reducer'е, handler не знает про navigation-канал.

---

#### `DatasourceEffect.UpdateWord(wordId, value)`

- **Source:** `CommitWordChanges` UI Msg (ветвь 4 — Update после guard'ов `isEditMode ∧ !edited.isBlank()`).
- **Action:** `WordCardUseCase.updateWord(wordId, value): Boolean` на `Dispatchers.IO`. При success — повторный `getTermById(wordId)` для fresh `Term` (resync `value` после возможной БД-нормализации — trim/case-folding; F041 ит.4).
- **Return Msg:**
  - `updateWord` true + `getTermById` non-null ⇒ `consumer(Msg.RefreshWord(term))`.
  - `updateWord` true + `getTermById` null ⇒ `consumer(Msg.ShowNotification("Не удалось получить обновлённое слово"))` (F051 ит.5).
  - `updateWord` false ⇒ `consumer(Msg.ShowNotification("Не удалось сохранить"))`.
  - Exception ⇒ `consumer(Msg.ShowNotification("Не удалось сохранить"))`.

  ```kotlin
  is DatasourceEffect.UpdateWord -> withContext(Dispatchers.IO) {
      try {
          val success = wordCardUseCase.updateWord(effect.wordId, effect.value)
          if (!success) {
              consumer(Msg.ShowNotification("Не удалось сохранить")) // failure: разблокирующий Msg
              return@withContext
          }
          val term = wordCardUseCase.getTermById(effect.wordId)
          if (term != null) {
              consumer(Msg.RefreshWord(term))                        // success: разблокирующий Msg
          } else {
              // F051 (ит.5): defensive — БД consistency должна гарантировать non-null после success update;
              //  если всё-таки null — даём пользователю явный сигнал. State остаётся optimistic.
              consumer(Msg.ShowNotification("Не удалось получить обновлённое слово"))
          }
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.ShowNotification("Не удалось сохранить"))
      }
  }
  ```
  **UX-инвариант (ит.7):** все 4 control-path (success+term / success+null / !success / catch) шлют ровно один разблокирующий Msg. UI разблокируется в любом исходе.
- **Reducer-логика `RefreshWord(word)`:**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          if (w.id != word.wordId.id) state.copy(isPendingDbOp = false) to emptySet()
          else state.copy(
              wordState = w.copy(
                  value = word.word.value,
                  added = word.addedDate,
                  // isEditMode / edited / showWarningDialog не трогаем —
                  //  локальный UI-state, не приходит из БД.
              ),
              isPendingDbOp = false,   // UX-инвариант ит.7
          ) to emptySet()
      WordState.NotLoaded -> state.copy(isPendingDbOp = false) to emptySet()  // defence-in-depth + UX-инвариант ит.7
  }
  ```
  **F043 (ит.4):** `term.lexemeList`, `changedDate`, `removedDate` reducer отбрасывает — `UpdateWord` не меняет лексемы и timestamps вне scope экрана. Reducer мутирует только `value` и `added`.
- **Edge cases:**
  - **Race: пользователь снова `EnterWordEditMode` до прихода `RefreshWord`** — reducer мутирует только `value`/`added`, активный edit пользователя не сбрасывается.
  - **wordId не совпадает с текущим** — guard защищает от баги в handler'е.
  - **БД-ошибка** ⇒ `ShowNotification`, state optimistic.

---

#### `DatasourceEffect.RemoveLexeme(wordId, lexemeId)`

- **Source:** `RemoveLexeme(lexemeId)` UI Msg **только для лексем с реальным id** (`lexemeId > 0`). Reducer в `contract_ui_msg` v3.2: для `NOT_IN_DB`-лексемы effect не шлётся (локальный nullify + локальный cascade).
- **Action:** `WordCardUseCase.deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?` на `Dispatchers.IO`. Сигнатура расширена (см. § «Расхождения spec ↔ code»). Каскадно удаляет translation/definition через FK.
- **Return Msg:**
  - Success (non-null `List<Lexeme>`) ⇒ `consumer(Msg.RefreshLexemeList(lexemes))`.
  - Failure (null или exception) ⇒ `consumer(Msg.ShowNotification("Не удалось удалить значение"))`.

  ```kotlin
  is DatasourceEffect.RemoveLexeme -> withContext(Dispatchers.IO) {
      try {
          val lexemes = wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId)
          if (lexemes != null) {
              consumer(Msg.RefreshLexemeList(lexemes))                 // success: разблокирующий Msg
          } else {
              consumer(Msg.ShowNotification("Не удалось удалить значение"))
          }
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.ShowNotification("Не удалось удалить значение"))
      }
  }
  ```
  **UX-инвариант (ит.7):** все 3 control-path шлют ровно один разблокирующий Msg.
- **Reducer-логика `RefreshLexemeList(lexemes)`:**
  ```
  when (val w = state.wordState) {
      is WordState.Loaded ->
          state.copy(
              lexemeList = lexemes.map { lexeme ->
                  val existing = state.lexemeList.find { it.id == lexeme.lexemeId.id }
                  if (existing == null)
                      lexeme.toLexemeState()
                  else
                      existing.copy(
                          translation = lexeme.translation?.let { tr ->
                              if (existing.translation == null)
                                  TextValueState(origin = tr.value, isEdit = false, edited = "")
                              else
                                  existing.translation.copy(origin = tr.value)
                          },
                          definition = lexeme.definition?.let { df ->
                              if (existing.definition == null)
                                  TextValueState(origin = df.value, isEdit = false, edited = "")
                              else
                                  existing.definition.copy(origin = df.value)
                          },
                      )
              },
              isPendingDbOp = false,   // UX-инвариант ит.7
          ) to emptySet()
      WordState.NotLoaded -> state.copy(isPendingDbOp = false) to emptySet()  // UX-инвариант ит.7
      // Defence-in-depth: RefreshLexemeList приходит только из RemoveLexeme, который
      //  guarded на wordState is Loaded в contract_ui_msg.
  }
  ```
  Merge-by-id: удалённые исчезают автоматически (map по новому списку — отсутствующий id не попадёт в результат); существующие сохраняют `isEdit`/`edited`/`isMenuOpen`. **Локальная `NOT_IN_DB`-лексема в state не пострадает** — её id не из БД, в `lexemes` payload её нет, но `state.lexemeList.map` не строится по state — он строится по payload; **отсюда важный момент:** `RefreshLexemeList` приходит **только после `RemoveLexeme` для реальной лексемы**, который guarded на `wordState is Loaded`, и payload отражает текущее БД-состояние слова. Если в state была `NOT_IN_DB`-лексема — она **исчезнет** при merge (её нет в payload). Это ожидаемо: `RemoveLexeme` для реальной лексемы концептуально перезагружает список БД-лексем; локальная не-закоммиченная теряется. UX-trade-off: на практике пользователь не может одновременно держать активную `NOT_IN_DB`-лексему и удалять другую (FAB guard `lexemeList.any { it.id == NOT_IN_DB } ⇒ ignore`, но удаление другой реальной лексемы — отдельный жест из меню; reducer `RefreshLexemeList` всё равно затрёт `NOT_IN_DB`). **Принимается как accepted edge case ит.6 — backlog-кандидат для уточнения.**

**F074 (ит.7) — data-loss buffer:** при `RemoveLexeme` реальной лексемы во время существования `NOT_IN_DB`-лексемы с заполненным `translation.edited` (in-progress ввод, не закоммичен) — merge выкидывает `NOT_IN_DB` целиком, **включая typed text** в `edited`-буфере. `isCreatingLexeme` гарантирует только FAB-блок, не блок RemoveLexeme через меню других лексем. **Accepted out-of-scope IS479** (редкий жест: пользователь начал создавать лексему, ввёл текст в перевод, не закоммитил, открыл меню другой лексемы и удалил её). Backlog-кандидат: либо добавить guard «`isCreatingLexeme ⇒ блокировать RemoveLexeme других лексем», либо предупредить пользователя.
- **Edge cases:**
  - **Concurrent RemoveLexeme (двойной тап)** — UI Msg-guard `lexemeList.none { it.id == lexemeId } ⇒ ignore` отсекает после первого Refresh. До прихода Refresh оба пути допускают повторный effect; БД-handler идемпотентен.
  - **DB-ошибка** ⇒ `ShowNotification`; лексема остаётся в списке.
  - **FK violation** — UseCase impl вернёт `null` или текущий список.
  - **Idempotent no-op contract (F039, симметрично F033):** при удалении уже отсутствующей лексемы UseCase impl возвращает `List<Lexeme>` (актуальный список слова без неё), **не `null`**. `null` — только реальная БД-ошибка.
  - **F050 (ит.5) — ordering `RefreshLexemeList`:** payload `lexemes: List<Lexeme>` приходит из `termApi.getTermById(wordId).lexemes` — через Room `@Relation` без явного `ORDER BY`. **F054 (ит.6) — ordering признан undefined:** Room не гарантирует порядок без `ORDER BY`. Альтернатива (`@Query getLexemesByTermId ORDER BY addDate ASC` в `core_db`) — **out-of-scope IS479**, зафиксировано как **accepted tech debt**. Текущее наблюдаемое поведение (порядок по `rowid`/insert order для SQLite) принимается reducer'ом как канон. Если UI/UX потребует стабильный порядок — нужен явный `ORDER BY` в DAO, отдельная задача.
  - **F068 (ит.7) — visual ordering after RemoveLexeme:** `RefreshLexemeList(lexemes)` приходит с порядком из `termApi.getTermById(wordId).lexemes`. Room `@Relation` без `ORDER BY` — порядок Room-determined (обычно по PK ASC / insert order для SQLite). После RemoveLexeme оставшиеся лексемы сохраняют свой относительный порядок (visual order preserved). Если Room/Room-расширение когда-нибудь поменяет relation-порядок — UI может визуально переупорядочиться. **Accepted out-of-scope IS479** — поведение завязано на не-документированную деталь SQLite/Room, прикручивать `ORDER BY` имеет смысл вместе с F054 как единая задача в backlog.
  - **Silent success (F036):** при успешном удалении snackbar не показываем (visual change = feedback).
- **Почему `RefreshLexemeList(List<Lexeme>)`, а не точечный `LexemeRemoved(lexemeId)`:** `deleteLexeme` impl использует `termApi.getTermById(wordId).lexemes` для resync — точечного `getLexemesByWord` в `CoreDbApi.LexemeApi` нет, добавление out-of-scope IS479 (F046 ит.5). Trade-off: одна лишняя query (JOIN word + lexemes), но переиспользует существующий API; merge-by-id даёт более робастный resync.

---

#### `DatasourceEffect.UpdateLexemeTranslation(wordId, lexemeId, translation)`

- **Source:** `CommitTranslationEdit(lexemeId)` UI Msg, ветвь 4 (Update) — после guard'ов `isEditMode ∧ !edited.isBlank() ∧ edited != origin`. **`lexemeId: Long?`** — `null` для лексемы с `id == NOT_IN_DB` (первый Commit, лексема ещё не в БД); non-null для лексемы с реальным id.
- **Action:** `WordCardUseCase.addLexemeTranslation(wordId, lexemeId: Long?, translation: String): Lexeme?` на `Dispatchers.IO`. **API уже поддерживает nullable `lexemeId`** — при `null` impl делает insert новой лексемы + insert translation атомарно; при non-null — update existing translation. Возвращает лексему с реальным id.

  > **F023 отменён (ит.6).** Раньше планировалось снять nullable `lexemeId`. Архитектурный пересмотр требует, чтобы handler умел создавать лексему «по требованию» при первом Commit Translation/Definition. Nullable `lexemeId` остаётся в API существенно — не dead code.
- **Return Msg:**
  - Success (non-null `Lexeme`) ⇒ `consumer(Msg.RefreshTranslation(lexemeId = lexeme.lexemeId.id, translation = lexeme.translation?.value))`. **Реальный id** в payload — независимо от того, был ли effect.lexemeId null или non-null. Reducer-логика в `contract_ui_msg` v3.2 заменяет `NOT_IN_DB → реальный id` в `lexemeList` для первого Commit (см. там же раздел «Datasource Msg reducer-логика — замена NOT_IN_DB») + сбрасывает `isPendingDbOp = false`.
  - Failure (null `Lexeme` или exception) ⇒ `consumer(Msg.ShowNotification("Не удалось сохранить перевод"))`. State не лечим — UI покажет старый `origin` (или останется `NOT_IN_DB` с локальным `edited`) до следующей попытки.

  ```kotlin
  is DatasourceEffect.UpdateLexemeTranslation -> withContext(Dispatchers.IO) {
      try {
          val lexeme = wordCardUseCase.addLexemeTranslation(
              wordId = effect.wordId,
              lexemeId = effect.lexemeId,   // nullable: UseCase создаст новую лексему если null
              translation = effect.translation,
          )
          if (lexeme != null) {
              // F047 (ит.5): lexeme.translation?.value корректно даёт null если impl нарушил контракт
              //  и вернул лексему без translation после success Update — reducer обнулит state.
              consumer(Msg.RefreshTranslation(                           // success: разблокирующий Msg
                  lexemeId = lexeme.lexemeId.id,   // реальный id (новый или существующий)
                  translation = lexeme.translation?.value,
              ))
          } else {
              consumer(Msg.ShowNotification("Не удалось сохранить перевод"))
          }
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          //  Особенно важно для NOT_IN_DB first-Commit failure (нет dictionary в Prefs —
          //  симметрично F049 для старого CreateLexeme): без этого UI остался бы навсегда заблокирован.
          consumer(Msg.ShowNotification("Не удалось сохранить перевод"))
      }
  }
  ```
  **UX-инвариант (ит.7):** все 3 control-path шлют ровно один разблокирующий Msg.
- **Reducer-логика `RefreshTranslation(lexemeId, translation)`:** см. `contract_ui_msg` v3.2, раздел «Datasource Msg reducer-логика — замена NOT_IN_DB» + сброс `isPendingDbOp = false`. Краткая семантика:
  - Если в state есть лексема с `id == lexemeId` (обычный Update реальной лексемы) ⇒ обновляем `origin` (`translation != null`) или обнуляем `translation` (`translation == null`).
  - Если такой лексемы нет, но есть лексема с `id == NOT_IN_DB` ⇒ **завершение «первого Commit»**: заменяем `NOT_IN_DB → реальный lexemeId`, конструируем/обновляем `TextValueState(origin = translation, isEdit = false, edited = "")`.
  - `isEdit`/`edited` не трогаем у существующих лексем — Refresh не имеет права закрывать активный edit пользователя.
- **Edge cases:**
  - **Concurrent Commit + EnterEditMode (transient window)** — пользователь нажал Commit, `UpdateLexemeTranslation` улетел. До прихода Refresh пользователь снова вошёл в edit (`isEdit = true, edited = origin (старое)`). Refresh обновит `origin`, **не трогая** `isEdit`/`edited`. Легитимный transient-state.
  - **F047 (ит.5) — `lexeme.translation == null` после success Update** — нарушение контракта impl; handler передаёт `null` в `Refresh*`, reducer обнуляет state в согласие с БД (safer чем удержание stale origin). Snackbar не показывается.
  - **FK violation** — UseCase impl поймает БД-ошибку и вернёт null ⇒ `ShowNotification`.
  - **Первый Commit для NOT_IN_DB → handler делает insert лексемы (F049-aware):** impl `addLexemeTranslation` при `lexemeId == null` должна (1) `lexemeApi.addLexeme(wordId)` → `lexemeApi.updateLexemeTranslation(newId, ...)` → `getLexemeById(newId).toDomainEntity()`. Possible failure modes симметричны F049 для старого `CreateLexeme`: отсутствие dictionary в Prefs → impl бросит exception → handler ловит → `ShowNotification("Не удалось сохранить перевод")`. State: `NOT_IN_DB`-лексема остаётся в `lexemeList` с `edited` буфером пользователя — попытка повторного Commit возможна.
- **Почему non-null `translation: String` в payload effect'а (не `String?`):** invariant на стороне UI (см. `contract_ui_msg` v3.2, `CommitTranslationEdit` ветвь 4: `edited.isBlank()` уходит в ветви 1/1a — Remove или local-nullify, до Update не доходит). `Update*Effect` всегда несёт non-empty string.

---

#### `DatasourceEffect.RemoveTranslation(lexemeId)`

- **Source:**
  - `RemoveTranslation(lexemeId)` UI Msg (явный пункт меню) **только для реальных id** (`lexemeId > 0`); для `NOT_IN_DB` — локальный nullify в reducer, effect не шлётся (см. `contract_ui_msg` v3.2).
  - `CommitTranslationEdit(lexemeId)` ветвь 1 (pessimistic Remove при `edited.isBlank() ∧ origin.isNotEmpty()`) — тоже только для реальных id (для `NOT_IN_DB` `origin` всегда `""`, ветвь 1 не достигается; см. `contract_ui_msg` v3.2).
- **Action:** `WordCardUseCase.deleteLexemeTranslation(lexemeId): RemoveTranslationResult?` на `Dispatchers.IO`. **Сигнатура расширяется** (см. § «Расхождения spec ↔ code») — sealed result различает «translation удалён, лексема осталась» и «лексема каскадно ушла» (F045 ит.5).
- **Return Msg:**
  - `RemoveTranslationResult.TranslationRemoved(lexeme)` ⇒ `consumer(Msg.RefreshTranslation(lexemeId = effect.lexemeId, translation = lexeme.translation?.value))`. После delete `?.value` даёт `null`; reducer обнулит state-translation.
  - `RemoveTranslationResult.LexemeCascadeRemoved` ⇒ `consumer(Msg.LexemeCascadeRemoved(effect.lexemeId))`. Reducer удалит лексему из `lexemeList` целиком.
  - `null` ⇒ `consumer(Msg.ShowNotification("Не удалось удалить перевод"))`.
  - Exception ⇒ `consumer(Msg.ShowNotification("Не удалось удалить перевод"))`.

  ```kotlin
  is DatasourceEffect.RemoveTranslation -> withContext(Dispatchers.IO) {
      try {
          when (val result = wordCardUseCase.deleteLexemeTranslation(effect.lexemeId)) {
              null ->
                  consumer(Msg.ShowNotification("Не удалось удалить перевод"))
              is RemoveTranslationResult.TranslationRemoved ->
                  consumer(Msg.RefreshTranslation(                          // success non-cascade: разблокирующий Msg
                      lexemeId = effect.lexemeId,
                      translation = result.lexeme.translation?.value,
                  ))
              RemoveTranslationResult.LexemeCascadeRemoved ->
                  consumer(Msg.LexemeCascadeRemoved(effect.lexemeId))       // success cascade: разблокирующий Msg
          }
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.ShowNotification("Не удалось удалить перевод"))
      }
  }
  ```
  **UX-инвариант (ит.7):** все 4 control-path шлют ровно один разблокирующий Msg.
- **Reducer-логика `LexemeCascadeRemoved(lexemeId)`:**
  ```
  when (val w = state.wordState) {
      WordState.NotLoaded -> state.copy(isPendingDbOp = false) to emptySet()  // defence-in-depth + UX-инвариант ит.7
      is WordState.Loaded -> state.copy(
          lexemeList = state.lexemeList.filterNot { it.id == lexemeId },
          isPendingDbOp = false,   // UX-инвариант ит.7: сброс даже на no-op-ветке (F075)
      ) to emptySet()
  }
  ```
  **F058 (ит.6) — idempotent для уже-удалённой лексемы:** `filterNot` no-op'ит, если лексемы с таким id уже нет в state (race с другим путём удаления). Никаких ошибок не возникает, reducer возвращает `state to emptySet()` фактически без изменений. Это закрывает race F033 + F045 (повторный delete уже отсутствующей лексемы из data-слоя → idempotent cascade no-op → ещё один `LexemeCascadeRemoved` → reducer no-op).
- **Edge cases:**
  - **F045 (ит.5) — cascade-семантика data-слоя как принятая:** существующий `WordCardUseCaseImpl.deleteLexemeTranslation(lexemeId)` хранит инвариант для БД-уровня — «лексема в БД имеет ≥ 1 суб-сущность» (см. F052 ниже). При удалении последней суб-сущности через `delete*` лексема каскадно удаляется через `lexemeApi.deleteLexeme(id)` (см. `WordCardUseCaseImpl.kt:75-82`). Контракт `RemoveTranslationResult.LexemeCascadeRemoved` явно сигнализирует UI этот path.
  - **F052 (ит.6) — инвариант формулируется ТОЛЬКО для БД-слоя:** «В БД лексема имеет ≥ 1 суб-сущность (translation либо definition non-null). Локально (state) — пустая лексема с `id = NOT_IN_DB` (`translation = null ∧ definition = null`) **возможна временно** между UI Msg `CreateLexeme` (тап FAB) и первым `Commit Translation/Definition`.» Эта формулировка — синхронизация с пересмотром: локальный state имеет другую жизненную модель чем БД. Cascade в data-слое валиден ТОЛЬКО для лексем, фактически существующих в БД (с реальным id).
  - **F053 (ит.6) — граница cascade-path:**
    - Для лексемы с `translation = null, definition != null` (БД-состояние) → `canRemoveTranslation() = true` (definition остаётся) → impl делает `updateLexemeTranslation(id, null)` (no-op в БД, translation и так null) → возвращает `TranslationRemoved(lexeme)` (idempotent F033).
    - Для лексемы с `translation = null, definition = null` (теоретически — только локально `NOT_IN_DB`) → **невозможно отправить effect** (UI reducer для `NOT_IN_DB` не шлёт `RemoveTranslation` effect; см. `contract_ui_msg` v3.2).
    - Cascade-path в data-слое (`lexemeApi.deleteLexeme(id)`) возможен только для лексемы с **одной** суб-сущностью (`translation != null, definition = null` при `RemoveTranslation`; зеркально для `RemoveDefinition`). Это валидное БД-состояние согласно F052.
  - **Concurrent Remove + Commit (race)** — описан в § UpdateLexemeTranslation как transient-окно.
  - **DB-failure** ⇒ `null` от UseCase, Refresh не приходит, `origin` остаётся; translation виден.
  - **Dedup concurrent Remove** — UI Msg-guard F027 (`lexeme.translation == null ⇒ ignore`) защищает на reducer-уровне после первого Refresh. До прихода Refresh — оба пути допускают повторный effect; БД idempotent.
  - **Idempotent no-op contract (F033):** при `deleteLexemeTranslation(lexemeId)` для лексемы у которой translation уже отсутствует — UseCase impl возвращает `TranslationRemoved(lexeme)` с текущей лексемой (с `translation = null`), **не `null`**.
  - **F061 (ит.6) — idempotent cascade no-op race F033 + F045:** концептуально для cascade-варианта: если лексема уже каскадно удалена в БД (после первого delete), повторный `deleteLexemeTranslation(lexemeId)` для несуществующей лексемы — `lexemeApi.getLexemeById(lexemeId)` вернёт null, ветка `canRemoveTranslation == true` не сработает (null), упадёт в ветку `else` → `lexemeApi.deleteLexeme(id)` для уже несуществующей строки → no-op в БД. Impl должна вернуть `LexemeCascadeRemoved` (а не `null`) — это **idempotent cascade no-op contract**. Reducer уже-удалённой лексемы безопасно no-op'ит (F058). **Конкретное поведение impl (null-возврат vs `LexemeCascadeRemoved`)** при `getLexemeById == null` нужно специфицировать на impl-шаге; контракт UI: оба исхода (либо `null + ShowNotification`, либо `LexemeCascadeRemoved + reducer no-op`) безопасны. Если impl выбирает `null` — пользователь увидит снек на повторный жест (приемлемо: жест явный); если `LexemeCascadeRemoved` — silent. Backlog-кандидат, не блокер IS479.
  - **F059 (ит.6) — query count в impl:** non-cascade path делает `getLexemeById(id)` (1 query для проверки `canRemoveTranslation`) + `updateLexemeTranslation(id, null)` (1 query) + `getLexemeById(id)` снова (1 query для возврата `TranslationRemoved(lexeme)`) = **3 query на non-cascade path**. Cascade path: `getLexemeById(id)` (1 query, проверка) + `deleteLexeme(id)` (1 query) = **2 query на cascade path**. Trade-off: можно оптимизировать (1 query на проверку → один update без re-fetch, конструировать `TranslationRemoved(lexeme.copy(translation = null))`), но это работа impl-шага, не контракт. Зафиксировано в EC для прозрачности impl-стоимости.
  - **F060 (ит.6) — sub-case в EC1 (pessimistic Remove cascade):** в `CommitTranslationEdit` ветвь 1 (pessimistic Remove): **если лексема имеет только одну суб-сущность (translation), pessimistic Remove приведёт к `LexemeCascadeRemoved` — исчезает вся лексема**. Reducer ветви 1 не различает — он только сбрасывает `isEdit = false, edited = ""` и шлёт effect; финал (`RefreshTranslation(null)` либо `LexemeCascadeRemoved(lexemeId)`) приходит через handler. UI пользователь видит исчезновение лексемы целиком (а не только обнуление translation) — это сюрприз, но соответствует БД-инварианту F052. Документировать в UX-обзоре отдельно; на уровне контракта — accepted.
  - **F056 (ит.6) — dangling reference в EC RemoveTranslation:** в v5 EC ссылался на «удалённое поле `isCreatingLexeme`» / некорректную формулировку про `isMenuOpen`. Здесь явно проверено: ссылок на удалённые поля нет. ✅ Чисто.
- **Pessimistic Remove path в `CommitTranslationEdit` ветвь 1:** UI reducer не различает финал — оба Msg валидны после Commit-ветки 1: `RefreshTranslation(null)` обнулит translation, `LexemeCascadeRemoved` удалит всю лексему.

---

#### `DatasourceEffect.UpdateLexemeDefinition(wordId, lexemeId, definition)`

- **Source:** `CommitDefinitionEdit(lexemeId)` UI Msg, ветвь 4 (Update). **`lexemeId: Long?`** — симметрично `UpdateLexemeTranslation`.
- **Action:** `WordCardUseCase.addLexemeDefinition(wordId, lexemeId: Long?, definition: String): Lexeme?` на `Dispatchers.IO`. API уже поддерживает nullable `lexemeId` — при `null` impl делает insert лексемы + insert definition; при non-null — update existing.
- **Return Msg:**
  - Success (non-null `Lexeme`) ⇒ `consumer(Msg.RefreshDefinition(lexemeId = lexeme.lexemeId.id, definition = lexeme.definition?.value))`.
  - Failure ⇒ `consumer(Msg.ShowNotification("Не удалось сохранить определение"))`.

  ```kotlin
  is DatasourceEffect.UpdateLexemeDefinition -> withContext(Dispatchers.IO) {
      try {
          val lexeme = wordCardUseCase.addLexemeDefinition(
              wordId = effect.wordId,
              lexemeId = effect.lexemeId,   // nullable: UseCase создаст новую лексему если null
              definition = effect.definition,
          )
          if (lexeme != null) {
              // F047 (ит.5): lexeme.definition?.value корректно даёт null если impl нарушил контракт
              //  и вернул лексему без definition после success Update — reducer обнулит state.
              consumer(Msg.RefreshDefinition(                            // success: разблокирующий Msg
                  lexemeId = lexeme.lexemeId.id,   // реальный id (новый или существующий)
                  definition = lexeme.definition?.value,
              ))
          } else {
              consumer(Msg.ShowNotification("Не удалось сохранить определение"))
          }
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.ShowNotification("Не удалось сохранить определение"))
      }
  }
  ```
  **UX-инвариант (ит.7):** все 3 control-path шлют ровно один разблокирующий Msg.
- **Reducer-логика `RefreshDefinition(lexemeId, definition)`:** **структурно симметрично** `RefreshTranslation` (см. `contract_ui_msg` v3.2, раздел «Datasource Msg reducer-логика — замена NOT_IN_DB»). Включая замену `NOT_IN_DB → реальный id` для первого Commit + сброс `isPendingDbOp = false`.
- **Edge cases:** симметрично `UpdateLexemeTranslation`. Не дублирую.

---

#### `DatasourceEffect.RemoveDefinition(lexemeId)`

- **Source:**
  - `RemoveDefinition(lexemeId)` UI Msg только для реальных id; для `NOT_IN_DB` — локальный nullify.
  - `CommitDefinitionEdit(lexemeId)` ветвь 1 (pessimistic Remove) — только для реальных id.
- **Action:** `WordCardUseCase.deleteLexemeDefinition(lexemeId): RemoveDefinitionResult?` на `Dispatchers.IO`. Sealed result симметрично `RemoveTranslation` (F045 ит.5).
- **Return Msg:**
  - `RemoveDefinitionResult.DefinitionRemoved(lexeme)` ⇒ `consumer(Msg.RefreshDefinition(lexemeId, definition = lexeme.definition?.value))`.
  - `RemoveDefinitionResult.LexemeCascadeRemoved` ⇒ `consumer(Msg.LexemeCascadeRemoved(effect.lexemeId))`.
  - `null` / Exception ⇒ `consumer(Msg.ShowNotification("Не удалось удалить определение"))`.

  ```kotlin
  is DatasourceEffect.RemoveDefinition -> withContext(Dispatchers.IO) {
      try {
          when (val result = wordCardUseCase.deleteLexemeDefinition(effect.lexemeId)) {
              null ->
                  consumer(Msg.ShowNotification("Не удалось удалить определение"))
              is RemoveDefinitionResult.DefinitionRemoved ->
                  consumer(Msg.RefreshDefinition(                            // success non-cascade: разблокирующий Msg
                      lexemeId = effect.lexemeId,
                      definition = result.lexeme.definition?.value,
                  ))
              RemoveDefinitionResult.LexemeCascadeRemoved ->
                  consumer(Msg.LexemeCascadeRemoved(effect.lexemeId))        // success cascade: разблокирующий Msg
          }
      } catch (e: Exception) {
          // UX-инвариант ит.7: гарантированный consumer(...) на catch-пути.
          consumer(Msg.ShowNotification("Не удалось удалить определение"))
      }
  }
  ```
  **UX-инвариант (ит.7):** все 4 control-path шлют ровно один разблокирующий Msg.
- **Reducer-логика / Edge cases:** симметрично `RemoveTranslation`. Включая F045, F052, F053, F058, F059, F060, F061. Cascade-семантика data-слоя (`WordCardUseCaseImpl.deleteLexemeDefinition:97-104`) симметрична — при удалении последней суб-сущности лексема каскадно удаляется. Idempotent contract F033/F039 / cascade no-op F061.

---

### Navigation Effect

**Используется существующий mate-овский `NavigationEffect.Back`** (тип `me.apomazkin.mate.NavigationEffect`). Per-screen sealed (`WordCardNavigationEffect`) не вводится — экран не имеет wordcard-специфичной навигации.

#### `NavigationEffect.Back`

- **Source (reducer-ветки):**
  - `Msg.NavigateBack` (shared с `contract_ui_msg` v3.2): UI-intent от back-кнопки **либо** handler-dispatch после `RemoveWord` success. Одна reducer-ветка, два источника отправки.
  - `Msg.WordNotFound` (после `LoadWord` 404).
- **Navigator method:** `Navigator.back()` → `navController.popBackStack()`.
- **Edge cases:**
  - **Двойной Back (race RemoveWord-success + user-Back)** — `popBackStack` идемпотентен.
  - **ExitScreen во время незавершённого effect'а** — effect-handler в scope ViewModel; Room atomicity. Худший случай — запись не успела зафиксироваться, при следующем `LoadWord` пользователь видит старое значение.

> 📎 guide: docs/guides/navigation.md — "Закрытие экрана — это эффект, не state; reducer возвращает NavigationEffect.Back → Navigator.back() → navController.popBackStack()"

---

### UI Effect

**Нет UI Effect.** Snackbar реализуется через State (`snackbarState: SnackbarState` — см. `contract_state` v2.5). Effect-handler шлёт `ShowNotification(text)` Msg → reducer пишет в `snackbarState` + сбрасывает `isPendingDbOp = false`.

#### Reducer-логика `ShowNotification(text)`

```
state.copy(
    snackbarState = SnackbarState(title = text, show = true),
    isPendingDbOp = false,   // UX-инвариант ит.7: разблокировать UI после любого failure
) to emptySet()
```
Повторный `show=true` с новым text — перезаписывает title (последний выигрывает). Инв. 8 удерживается (`show=true ⇒ title != ""`). `isPendingDbOp = false` гарантирует разблокировку UI после любого failure-пути любого handler'а.

- **Почему через State, а не side-channel:** snackbar должен retain'иться через config change (rotation). Side-channel при rotation теряется. State-канал работает из коробки.
- **Trade-off:** dismiss snackbar требует UI Msg (`DismissNotification`). Принимаем — retain важнее.

---

## Mapper

```kotlin
// modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt (или extension-файл)

internal fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    translation = translation?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    definition = definition?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    isMenuOpen = false,
)
```

Единый mapper для всех случаев:
- После `WordLoaded` — для каждой лексемы.
- После `RefreshLexemeList` — для отсутствовавших ранее (для существующих — merge-логика в reducer'е, см. блок выше).
- После `RefreshTranslation`/`RefreshDefinition` — маппер **не** используется напрямую (reducer обновляет существующий `LexemeState` точечно через `copy`, либо конструирует `TextValueState` для замены `NOT_IN_DB → реальный id`; см. `contract_ui_msg` v3.2).
- `isEdit = false` явно — защита от случайной регрессии дефолта.

## Subscribers

**Subscribers отсутствуют.**

WordCard — экран без cross-screen реактивности. Все обновления данных происходят через handler-Msg, возвращаемые `suspend`-вызовами `WordCardUseCase`. Подробное обоснование — см. `contract_io_approved.md` (решение conductor'а по ит.1: subscriber на `observeLexemesByWordId` выкинут как «надуманный»).

Что это значит на практике:
- Изменения лексем/translation/definition, инициированные **этим** экраном, — обновляются через `Refresh*` Msg от соответствующего handler'а.
- Изменения данных слова, инициированные **другими экранами** (например cross-screen delete word), **не отражаются** на открытом WordCard. На текущем уровне фичи такого UI нет.

## Проверка реактивности

1. **Может ли state экрана устареть, если другой экран изменит данные в БД?** — **N.**
   На текущем уровне фичи нет UI, изменяющего данные слова/лексем извне WordCard'а. YAGNI — subscriber не вводим. Backlog-кандидат, если когда-либо появится такой источник.

2. **Есть ли настройки/пользовательские preferences, изменение которых должно отразиться?** — **N.**
   WordCard не зависит от user preferences (учебный режим, голос TTS). Тема приложения и i18n — application-level.

3. **Активный экран должен реагировать на push-нотификации / system events / lifecycle?** — **N.**
   Push в PolyTrainer не реализованы. Lifecycle обрабатывается Mate (`StateFlow` retain через `viewModelScope`).

4. **Есть ли DAO-flow / DataStore-flow / другие реактивные источники?** — **N.**
   `WordCardUseCase` экспонирует только `suspend`-методы — ни одного `Flow`/`observe`.

## Бизнес-инварианты

**N/A.** Subscriber'ов нет — инвариантов «X должно реактивно обновиться» не существует. Все обновления state — реакция на handler-Msg от user-action.

## Edge cases (cross-effect)

### Concurrency policy (ит.7) — closes F062 / F063 / F066

**UI-блокировка через `state.isPendingDbOp`** (см. `contract_state` v2.5 и `contract_ui_msg` v3.2) предотвращает любой пользовательский ввод между отправкой Effect и приходом confirm-Msg. Reducer-ветки UI Msg, отправляющих DB-Effect (`CommitWordChanges`/Update, `RemoveWord`, `CommitTranslationEdit`/Update/Remove, `CommitDefinitionEdit`/Update/Remove, `RemoveTranslation`, `RemoveDefinition`, `RemoveLexeme`), ставят `isPendingDbOp = true`. Datasource Msg reducer-ветки сбрасывают `isPendingDbOp = false` (см. UX-инвариант в каждом handler-блоке выше).

Из-за этого следующие race-сценарии **устранены архитектурно**:

- **F062** — «повторный Commit Translation до прихода `RefreshTranslation`» — невозможен: UI заблокирован, второй Commit не пройдёт guard.
- **F063** — «concurrent Commit Translation + Commit Definition для одной NOT_IN_DB-лексемы» — невозможен: после первого Commit UI заблокирован до прихода `Refresh*`, второй Commit не запустится.
- **F066** — «race RemoveWord + Commit*» — невозможен: после `RemoveWord` UI заблокирован, Commit не дойдёт до effect-отправки.

Эти 3 critical из ит.6 **сняты UX-блокировкой**, формально без точечных edge-case-ремарок. Теоретически Room на микросекундной шкале race не воспроизводится, а UI-блокировка убирает само окно для второго пользовательского ввода. Accepted as architecturally resolved.

### EC1: Race `CommitTranslationEdit` + `EnterTranslationEditMode` (transient window after pessimistic Remove)

**Сценарий:** пользователь стёр перевод полностью и нажал Commit (ветвь 1 — pessimistic Remove). Reducer: `translation.isEdit = false, edited = ""`, шлёт `RemoveTranslation`. До прихода `RefreshTranslation(lexemeId, null)` пользователь снова тапнул на chip «Перевод» — `EnterTranslationEditMode`. Guard: `lexeme.translation == null ⇒ ignore` — но translation сейчас ещё non-null (`origin = старое, isEdit=false, edited=""`). Пользователь снова в edit.

**Затем приходит `RefreshTranslation(lexemeId, null)`.** Reducer: `translation = null`. Активный edit пользователя пропадает на следующий recompose.

**Защита:** известный transient-state, accepted (см. F002).

**F060 (ит.6) — cascade sub-case:** если лексема имела **только translation** (одну суб-сущность), pessimistic Remove приведёт к `LexemeCascadeRemoved` — вся лексема исчезает из `lexemeList`. Reducer не различает финал в ветви 1 — оба Msg валидны. UI: пользователь видит «карточка лексемы исчезла полностью» (а не только обнулённый chip перевода) — соответствует БД-инварианту F052. Документировать в UX-обзоре.

### EC2: ~~`CreateLexemeFailed` после success-RefreshLexeme~~ — удалено в ит.6

EC из v5 базировался на effect `CreateLexeme`, который в ит.6 удалён. Failure канала больше нет — нет EC.

### EC3: Concurrent `RemoveLexeme` (двойной тап)

**Сценарий:** пользователь нажал «Удалить лексему». Reducer шлёт `RemoveLexemeEffect`. Handler delete + `RefreshLexemeList`.

**Двойной тап** — UI Msg-guard `lexemeList.none { it.id == lexemeId } ⇒ ignore` отсечёт второй Msg после прихода Refresh. До Refresh оба Msg долетят; БД-handler идемпотентен (F039) — второй Refresh приведёт state в то же состояние (no-op).

### EC4: Concurrent `Update*` + `Refresh*` (handler in-flight) — closed by ит.7

**F072 (ит.7) — closed архитектурно:** сценарий «пользователь в быстрой последовательности коммитит translation дважды» невозможен из-за UI-блокировки через `state.isPendingDbOp` (см. Concurrency policy выше + `contract_ui_msg` v3.2 глобальный guard). Первый Commit ставит `isPendingDbOp = true`, второй Commit no-op до прихода confirm-Msg. EC4 сохранён только как историческая ссылка; активно — Concurrency policy.

### EC5: Exit во время pending effect

**Сценарий:** пользователь нажал «Удалить лексему», эффект улетел, и сразу NavigateBack. ViewModel уничтожается, effect-handler scope cancellation'ится.

**Безопасно.** Room atomicity + ViewModel-scoped coroutines. При следующем заходе пользователь увидит актуальный snapshot из БД.

### EC6: `WordNotFound` после init

**Сценарий:** `initEffects` шлёт `LoadWord(wordId)`. БД не находит запись. Handler возвращает `WordNotFound`. Reducer: `isLoading = false` + `NavigationEffect.Back`.

**Безопасно.** Silent exit — корректный UX.

### EC7 (ит.6): Первый Commit Translation/Definition для NOT_IN_DB-лексемы (success path)

**Сценарий:** пользователь тапнул FAB → в state добавилась `LexemeState(id = NOT_IN_DB, translation = null, definition = null, isMenuOpen = false)`. Пользователь нажал chip «Перевод» → `CreateTranslation` (локальный, без effect) → ввёл текст → `CommitTranslationEdit` ветвь 4 → `UpdateLexemeTranslation(wordId, lexemeId = null, translation = "newValue")` улетает.

**Handler:** `addLexemeTranslation(wordId, null, "newValue")` → impl делает insert лексемы (получает newId), insert translation для newId, возвращает `Lexeme(lexemeId = LexemeId(newId), translation = Translation("newValue"), ...)`. Handler: `consumer(Msg.RefreshTranslation(lexemeId = newId, translation = "newValue"))`.

**Reducer (`contract_ui_msg` v3.2, раздел «замена NOT_IN_DB»):** не найдена лексема с `id == newId`, но есть `id == NOT_IN_DB` → ветка replacement → `l.copy(id = newId, translation = TextValueState(origin = "newValue", isEdit = false, edited = ""))`. Дополнительно reducer сбрасывает `isPendingDbOp = false` (UX-инвариант ит.7).

**Финал:** `lexemeList` содержит лексему с реальным id, корректным `origin`, без активного edit. Инв. 2 (≤ 1 NOT_IN_DB) удерживается — `NOT_IN_DB` исчез.

### EC8 (ит.6, переформулировано ит.7 — F067): Первый Commit для NOT_IN_DB-лексемы (failure path) — accepted trade-off

**Сценарий:** идентичен EC7 до точки `UpdateLexemeTranslation` улетает. Impl `addLexemeTranslation` бросает exception (например, нет dictionary в Prefs — симметрично F049 для старого `CreateLexeme`).

**Handler:** ловит exception → `consumer(Msg.ShowNotification("Не удалось сохранить перевод"))` (UX-инвариант ит.7: гарантированный разблокирующий Msg в catch-ветке).

**Reducer:** snackbar показан, `isPendingDbOp = false` (сброшен веткой `ShowNotification`). State: `NOT_IN_DB`-лексема **остаётся в `lexemeList`** с `translation = TextValueState(origin = "", isEdit = false, edited = "")` (после Commit ветви 4 reducer сбросил `isEdit = false, edited = ""`).

**EC8 — accepted trade-off ит.7:** после failure pessimistic Remove ветви 1 (или first-Commit failure) `edited = ""` сохраняется как есть — пользователю придётся ввести значение заново. Альтернатива «откатить state в `isEdit = true, edited = <предыдущий ввод>`» требует хранения предыдущего ввода (отдельное поле в state) — **не вводим в IS479**, out-of-scope. Принимаемое поведение: «висит пустой chip, пользователь нажимает повторно на chip и вводит заново». Diagnostic-бедность snackbar-текста («Не удалось сохранить перевод» без различения «нет словаря» / «БД-сбой») — также accepted, разделение out-of-scope.

> ⚠ EC8 фиксируется как accepted trade-off без претензии на UX-win — формулировка ит.6 «лучше чем альтернатива» снята; обе альтернативы имеют свои UX-проблемы, выбираем текущую как минимально-инвазивную для IS479.

**F071 (ит.7) — F037 cross-contract assumption обновлён:** комбинация `(translation != null, isEdit = false, origin = "")` теперь достижима не только из `CreateTranslation` без Commit (как декларировал F037 в `contract_ui_msg`), но и из EC8 (Commit-failure для NOT_IN_DB). **Это не ломает Cancel-логику:** для лексемы с `id == NOT_IN_DB` БД-записи нет ни в одном из двух источников (Cancel-ветка `origin.isEmpty() ⇒ translation = null` — локальный nullify безопасен). Для реальной лексемы (`id > 0`) форма `(translation != null, isEdit = false, origin = "")` **не достижима** ни из одного источника — для реальной лексемы Commit с `edited.isBlank()` идёт по ветви Remove (origin сохраняется до Refresh, после Refresh `translation = null`). Future-relax (если когда-нибудь Commit-failure начнёт обнулять origin для реальной лексемы) — потребует пересмотра F037.

## Расхождения spec ↔ code

**Режим 1** — спека отсутствует. Сверка идёт с текущим кодом `DatasourceEffectHandler.kt` + `WordCardUseCase.kt` + `WordCardUseCaseImpl.kt`. Зафиксированы изменения:

### Изменения в API `WordCardUseCase`

**F045 (ит.5) — новые sealed result типы** для `delete*` методов суб-сущностей лексемы:

```kotlin
package me.apomazkin.wordcard.deps  // или mate — где живёт WordCardUseCase

sealed interface RemoveTranslationResult {
    /** Translation удалён в БД; лексема осталась (есть definition). Содержит обновлённую лексему. */
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    /** Translation был последней суб-сущностью; лексема каскадно удалена data-слоем. */
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    /** Definition удалён в БД; лексема осталась (есть translation). Содержит обновлённую лексему. */
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    /** Definition был последней суб-сущностью; лексема каскадно удалена data-слоем. */
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

Семантика возврата `Result?`:
- `null` — реальная БД-ошибка / failure (исключение / FK violation / lexeme не существует — за исключением idempotent no-op, см. F033/F061).
- `*Removed(lexeme)` — суб-сущность удалена, лексема осталась.
- `LexemeCascadeRemoved` — последняя суб-сущность удалена, лексема каскадно ушла из БД.

| Метод | Текущая сигнатура | Новая сигнатура | Зачем |
|---|---|---|---|
| `addLexeme` | `suspend fun addLexeme(wordId: Long): Lexeme?` | **УДАЛЯЕТСЯ из API.** | **Ит.6 (архитектурный пересмотр):** лексема создаётся в БД только при первом Commit Translation/Definition. Effect `CreateLexeme` снят. Создание новой лексемы в БД переехало внутрь `addLexemeTranslation` / `addLexemeDefinition` при `lexemeId == null`. Существующий impl `addLexeme` (с побочкой `quizApi.addWriteQuiz`) — становится **internal step** внутри `addLexemeTranslation` / `addLexemeDefinition` impl, либо вынесен в private helper. Внешний API больше не экспонирует «создать пустую лексему». |
| `deleteLexeme` | `suspend fun deleteLexeme(lexemeId: Long): Boolean` | `suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?` | F040 ит.4: `wordId` явным параметром (`Lexeme` entity не имеет поля `wordId`). F046 ит.5: resync через `termApi.getTermById(wordId).lexemes`. F039: idempotent no-op (для уже отсутствующей лексемы вернёт актуальный список, не `null`). |
| `addLexemeTranslation` | `suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?` | **БЕЗ ИЗМЕНЕНИЙ.** Nullable `lexemeId` сохраняется. | **F023 отменён (ит.6).** Nullable `lexemeId` используется существенно — при `null` impl создаёт лексему. Раньше планировалось снять как dead code (F023 ит.3) — теперь это валидный путь. |
| `addLexemeDefinition` | `suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?` | **БЕЗ ИЗМЕНЕНИЙ.** Nullable `lexemeId` сохраняется. | Симметрично `addLexemeTranslation`. |
| `deleteLexemeTranslation` | `suspend fun deleteLexemeTranslation(lexemeId: Long)` | `suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?` | F045 ит.5: cascade-aware sealed result. F033: idempotent no-op. F061: cascade no-op contract. |
| `deleteLexemeDefinition` | `suspend fun deleteLexemeDefinition(lexemeId: Long)` | `suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?` | Симметрично. |

**F064 (ит.7) — atomicity contract:** UseCase impl `addLexemeTranslation(wordId, lexemeId = null, translation)` ОБЯЗАН выполнять `addLexeme + updateLexemeTranslation` в одной Room-транзакции (`@Transaction`-метод DAO или явная `withTransaction { ... }` обёртка). Аналогично `addLexemeDefinition(wordId, lexemeId = null, definition)`. Без транзакции failure после успешного `addLexeme`, но до `updateLexemeTranslation`, оставит **пустую лексему в БД** (без translation и без definition), что нарушает БД-инвариант F052 («БД-лексема имеет ≥ 1 суб-сущность»). Также при success path лексема становится видна для других readers (`termApi.getTermById(wordId)`) только после commit всей транзакции — никакой полупустой промежуточный snapshot не утечёт. Отвечает data-слой / impl-шаг; контракт UI: после `consumer(Msg.RefreshTranslation(...))` БД-инвариант F052 удерживается, либо handler словил exception и пришёл `ShowNotification` без БД-изменений.

**Финальный API `WordCardUseCase` (после ит.6):**

```kotlin
interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean

    // addLexeme УДАЛЁН — создание лексемы внутри addLexeme*

    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?

    suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?

    suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?
}
```

### Изменения в handler-Msg

| Текущий код | Новое поведение |
|---|---|
| `LoadWord` ⇒ `WordLoaded` / `WordNotFound` (без try/catch) | ⇒ try/catch добавлен; exception ⇒ `WordNotFound` (silent exit). |
| `RemoveWord` ⇒ `Msg.NavigateBack` (без try/catch) | ⇒ try/catch добавлен; failure ⇒ `ShowNotification("Не удалось удалить слово")` (F042 ит.4). |
| `UpdateWord` ⇒ `Msg.LoadingWord` (full re-fetch) | ⇒ `RefreshWord(term)` (точечный resync); failure ⇒ `ShowNotification`. Defensive null-после-true (F051) ⇒ `ShowNotification("Не удалось получить обновлённое слово")`. |
| `CreateLexeme` effect и handler-ветка | **УДАЛЯЮТСЯ ЦЕЛИКОМ.** Effect снят, UI Msg `CreateLexeme` не порождает effect (см. `contract_ui_msg` v3.2). |
| `UpdateLexemeTranslation` / `UpdateLexemeDefinition` — fallback `if (effect.lexemeId > -1) effect.lexemeId else null` | **УБРАН.** `lexemeId: Long?` теперь честно nullable; handler передаёт значение напрямую без преобразования (`NOT_IN_DB` фильтрация переехала в UI reducer — см. `contract_ui_msg` v3.2, в effect шлётся уже либо null, либо реальный id). Failure ⇒ `ShowNotification`, **не** `throw IllegalStateException` (это вешало coroutine). |
| `RemoveTranslation` / `RemoveDefinition` ⇒ `Msg.LoadingWord` | ⇒ sealed result handling: `*Removed(lexeme)` ⇒ `Refresh*(lexemeId, lexeme.*?.value)`; `LexemeCascadeRemoved` ⇒ `LexemeCascadeRemoved(lexemeId)`; `null` ⇒ `ShowNotification`. Try/catch добавлен. |
| `RemoveLexeme` ⇒ `Msg.LoadingWord` (full re-fetch) | ⇒ `RefreshLexemeList(lexemes)` (точечный resync). Try/catch. |

### Без изменений (зафиксировано как контракт)

- `DatasourceEffect` sealed interface — **8 вариантов** после удаления `CreateLexeme` (`LoadWord` / `RemoveWord` / `UpdateWord` / `RemoveLexeme` / `UpdateLexemeTranslation` / `RemoveTranslation` / `UpdateLexemeDefinition` / `RemoveDefinition`). `RemoveLexeme` расширен на `(wordId, lexemeId)` в ит.4. `Update*` payload `lexemeId: Long?` — ит.6.
- `RemoveWord` ⇒ `Msg.NavigateBack` ⇒ reducer возвращает `NavigationEffect.Back` — текущий паттерн сохраняется.
- `LoadWord` ⇒ `Msg.WordLoaded(term)` / `Msg.WordNotFound` — текущий паттерн сохраняется.
- `F057 (ит.6) — Forward-ref в `contract_ui_msg` v3.1` — **Synced.** Forward-ref таблица в `contract_ui_msg` v3.1 точно соответствует финальному scope 9 Datasource Msg (`WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `ShowNotification`, `NavigateBack`). `RefreshLexeme` и `CreateLexemeFailed` явно помечены удалёнными в v3.1.

## Требуется feedback в предыдущие шаги

**Не требуется.**

`contract_state` v2.5 и `contract_ui_msg` v3.2 уже отражают архитектурный пересмотр под NOT_IN_DB + UX-инвариант `isPendingDbOp`. Все формы State (sealed `WordState`, `LexemeState`, computed `isCreatingLexeme`, явное поле `isPendingDbOp: Boolean`, инв.2 ослаблен до `≤ 1`) и UI Msg (без `CreateLexeme` effect, без `RefreshLexeme`/`CreateLexemeFailed`, reducer-логика замены `NOT_IN_DB → реальный id` для `Refresh*`, set `isPendingDbOp = true` в Effect-sending ветках, сброс false в Datasource Msg reducer-ветках, shared `Msg.NavigateBack`) — синхронизированы.

Forward-ref таблица в `contract_ui_msg` v3.2 точно соответствует финальным 8 Datasource Msg + shared `NavigateBack` этого артефакта.

## Лог итераций

### ит.1 (2026-05-19T17:35:00-0600) — отменён

Введён выдуманный subscriber на `LexemeRepository.observeLexemesByWordId(wordId)` + `RefreshLexemeList` как subscriber-emit. Conductor выкинул subscriber решением ит.2.

### ит.2 (2026-05-19T18:00:00-0600)

Переписан с нуля без subscriber'а. EC2 удалён. `WordLoaded(word: Term)`. `LoadingWord` после mutation-effect'а заменён точечными `Refresh*` Msg.

### ит.3 (2026-05-19T18:30:00-0600) — закрытие 2 critical + 8 minor

F029 (Refresh* payload → `(lexemeId, String?)`), F023 (планировалось снять nullable lexemeId), F024-F027, F030-F036.

### ит.4 (2026-05-19T19:00:00-0600) — закрытие 1 critical + 6 minor

F038 (try/catch CreateLexeme), F039 (idempotent deleteLexeme), F040 (RemoveLexeme wordId), F041-F044.

### ит.5 (2026-05-19T19:36:00-0600) — закрытие 2 critical + 5 minor

F045 (cascade-aware Remove* + LexemeCascadeRemoved), F046 (deleteLexeme через full term fetch), F047 (defensive null после success), F048-F051.

### ит.6 (2026-05-19T20:30:00-0600) — архитектурный пересмотр под NOT_IN_DB

**Перекройка под `contract_state` v2.4 + `contract_ui_msg` v3.1:** лексема создаётся в БД только при первом Commit Translation/Definition, не при тапе FAB.

**Удалено навсегда:**
- `DatasourceEffect.CreateLexeme(wordId)` — нет effect. Лексема живёт локально с `NOT_IN_DB` до первого Commit.
- `Msg.RefreshLexeme(lexeme)` — нет источника (effect снят).
- `Msg.CreateLexemeFailed` — нет источника.
- Метод `addLexeme(wordId): Lexeme?` из `WordCardUseCase` API — становится internal step внутри `addLexemeTranslation` / `addLexemeDefinition` impl.

**Изменено:**
- `UpdateLexemeTranslation` / `UpdateLexemeDefinition` payload: `lexemeId: Long` → `lexemeId: Long?`. При `null` handler делает insert лексемы + insert суб-сущности атомарно. Реальный id возвращается через `RefreshTranslation` / `RefreshDefinition`. Fallback `if (effect.lexemeId > -1) ... else null` в DatasourceEffectHandler убран (filtering переехал в UI reducer).
- **F023 отменён.** Nullable `lexemeId` в `addLexemeTranslation` / `addLexemeDefinition` остаётся в API — теперь это валидный путь, не dead code. Строка про снятие nullable из таблицы «Расхождения spec ↔ code» убрана.
- Финальный scope Datasource Msg — **9** (без `RefreshLexeme`, без `CreateLexemeFailed`).

**Сохранено из ит.5:**
- `RemoveTranslation` / `RemoveDefinition` — cascade-aware sealed result (`RemoveTranslationResult` / `RemoveDefinitionResult`), `Msg.LexemeCascadeRemoved(lexemeId)`. Cascade всё ещё существует в data-слое для БД-лексем (`WordCardUseCaseImpl.canRemoveTranslation/Definition`).

**Findings ит.6 закрыты:**

- **F045 (critical)** — сохранён как валидный (cascade-aware result + `LexemeCascadeRemoved` Msg для БД-уровня).
- **F052 (critical, переформулирован)** — инвариант «лексема имеет ≥ 1 суб-сущность» теперь формулируется **ТОЛЬКО для БД-уровня**. Локально (state) — пустая лексема с `NOT_IN_DB` возможна временно. Cascade в data-слое валиден ТОЛЬКО для лексем с реальным id.
- **F053** — граница cascade-path уточнена: cascade возможен только для лексемы с одной суб-сущностью; для `NOT_IN_DB` effect не отправляется (UI reducer для `NOT_IN_DB` не шлёт `RemoveTranslation`/`RemoveDefinition` effect).
- **F054** — ordering `lexemeList` признан **undefined** (Room `@Relation` без `ORDER BY`). Альтернатива (`getLexemesByTermId ORDER BY addDate ASC` в `core_db`) — out-of-scope IS479, accepted tech debt.
- **F055** — финальный scope-список Datasource Msg = **9** (без `RefreshLexeme`, без `CreateLexemeFailed`).
- **F056 (minor)** — dangling reference в EC `RemoveTranslation`: проверено, чисто.
- **F057 (minor)** — forward-ref в `contract_ui_msg` v3.1 — **Synced**.
- **F058 (minor)** — EC ремарка `LexemeCascadeRemoved` для уже-удалённой лексемы: `filterNot` no-op'ит. Добавлено в reducer-pseudocode.
- **F059 (minor)** — impl-pseudocode `delete*`: non-cascade path = 3 query (getLexemeById + update + getLexemeById), cascade path = 2 query (getLexemeById + deleteLexeme). Зафиксировано в EC `RemoveTranslation` для прозрачности impl-стоимости.
- **F060 (minor)** — EC1 cascade-вариант (pessimistic Remove с одной суб-сущностью ⇒ `LexemeCascadeRemoved` ⇒ вся лексема исчезает). Добавлено в EC1.
- **F061 (minor)** — F033 + F045 race: idempotent cascade no-op возвращает `LexemeCascadeRemoved` при повторном delete на несуществующую лексему (либо `null` + snackbar — обе ветки безопасны). Конкретное поведение impl — backlog-кандидат, не блокер.

**Critical из ит.5 автоматически сняты архитектурным пересмотром:**
- **F038** (CreateLexeme exception stuck) — нет effect, нет ветки, нет stuck.
- **F049** (addLexeme «Dictionary not found») — нет такого вызова в effect-handler'е (impl `addLexemeTranslation` теперь делает insert лексемы внутри себя; failure ловится try/catch в handler'е `UpdateLexemeTranslation`).
- **F052** (ложный инвариант) — переформулирован: валиден для БД-уровня (см. выше).

### ит.7 (2026-05-19T21:10:00-0600) — мини-патч isPendingDbOp + точечные правки

**Closed findings:**
- **F062 / F063 / F066** — сняты архитектурно (UI-блокировка через `state.isPendingDbOp` из `contract_state` v2.5 + `contract_ui_msg` v3.2). Race-сценарии «повторный Commit до Refresh» / «concurrent Commit Translation+Definition для одной NOT_IN_DB-лексемы» / «race RemoveWord + Commit\*» — устранены: UI не пропустит второй клик между отправкой Effect и приходом confirm-Msg. Добавлен раздел «Concurrency policy» в Edge cases.
- **F065** — `Msg.NavigateBack` зафиксирован как **shared Msg** между `contract_ui_msg` v3.2 (объявление) и `contract_io` (handler dispatch после `RemoveWord` success). Объявление в Datasource Msg-блоке удалено; добавлена явная ремарка про shared и единую reducer-ветку (которая возвращает `setOf(NavigationEffect.Back)` + сбрасывает `isPendingDbOp = false`).
- **F064** — atomicity contract: добавлена ремарка про `@Transaction` обёртку для `addLexemeTranslation(lexemeId = null, ...)` / `addLexemeDefinition(lexemeId = null, ...)`. Без транзакции failure после `addLexeme` оставит пустую лексему в БД (нарушение F052). Отвечает data-слой.
- **F067** — EC8 переформулировано как **accepted trade-off** без претензии на UX-win. Альтернатива «откатить state в isEdit=true, edited=<предыдущий ввод>» требует хранения предыдущего ввода — out-of-scope IS479.
- **F068** — visual ordering after RemoveLexeme: добавлена ремарка про сохранение относительного порядка оставшихся лексем (Room `@Relation` без `ORDER BY` — порядок Room-determined, обычно по PK ASC). Accepted out-of-scope (вместе с F054).

**UX-инвариант:** все handler-pseudocode-блоки проверены на гарантированный `consumer(...)` на каждом control-path (success / failure / catch). В каждом handler-блоке добавлена ремарка «UX-инвариант (ит.7): все N control-path шлют ровно один разблокирующий Msg». Reducer-ветки `ShowNotification`, `RefreshWord`, `RefreshTranslation`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `NavigateBack`, `WordLoaded`, `WordNotFound` обязаны сбрасывать `isPendingDbOp = false`.

**Версия:** v6 → v7.

---

_model: claude opus 4.7 (1M context)_
