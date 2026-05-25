# IS479 — Business sub-flow design tree

Граф файлов для реализации business-логики IS479 (sealed `WordState`, `isPendingDbOp`, `closeAllEditModes`, sealed Result types, atomicity).

UI-узлы (виджеты, `WordCardScreen`) — out of scope, отдельный UI sub-flow.

> ⚠ **Compile-break — accepted intermediate state (F076 ит.1):** после применения business-узлов модуль `wordcard` НЕ компилируется (UI-файлы ссылаются на удалённые `UiMsg.ShowNotification(text, show)`, `ExitTranslationEditMode`, `OpenAddLexemeDialog`, `EnableTranslationCreation`, `AddLexemeBottomState`). Это **ожидаемое промежуточное состояние** — UI sub-flow стартует сразу после business и восстанавливает компиляцию под inline-механику (Figma). Business sub-flow не вводит compile-shim для UI-call-site.

> ⚠ **Atomicity (F077 ит.1, F064):** реализуется через **существующую** перегрузку `CoreDbApi.LexemeApi.addLexeme(wordId, translation: TranslationApiEntity): Long` (`core/core-db-api/.../CoreDbApi.kt:80-83`). Impl в `CoreDbApiImpl.kt:216-228` делает один `wordDao.addLexeme(LexemeDb(wordId, translation=value, addDate))` — атомарно по построению (один INSERT, Room suspend гарантирует атомарность). Никакого `withTransaction` / `AppDatabase`-инъекции не требуется. `WordCardUseCaseImpl.addLexemeTranslation(lexemeId=null, ...)` переключается на эту перегрузку. Симметрично для definition. Расширения `core-db-api` нет — out-of-scope IS479 соблюдён.

## Часть 1: Граф

```yaml
- id: 0
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt
  action: "~"
  depends: []

- id: 1
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt
  action: "~"
  depends: []

- id: 2
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt
  action: "~"
  depends: [0, 1]

- id: 3
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt
  action: "~"
  depends: [0, 2]

- id: 4
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/WordCardReducer.kt
  action: "~"
  depends: [1, 2, 3]

- id: 5
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/UiEffectHandler.kt
  action: "-"
  depends: [2]

- id: 6
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardViewModel.kt
  action: "~"
  depends: [3, 5]

- id: 7
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt
  action: "~"
  depends: [0]

- id: 8
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LoadingWordTest.kt
  action: "-"
  depends: [2]

- id: 9
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt
  action: "~"
  depends: [1, 4]

- id: 10
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordEditTest.kt
  action: "~"
  depends: [1, 4]

- id: 11
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DeleteWordDialogTest.kt
  action: "~"
  depends: [1, 4]

- id: 12
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt
  action: "~"
  depends: [1, 4]

- id: 13
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/TranslationManagementTest.kt
  action: "~"
  depends: [1, 4]

- id: 14
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DefinitionManagementTest.kt
  action: "~"
  depends: [1, 4]

- id: 15
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/NavigateBackTest.kt
  action: "~"
  depends: [1, 4]

- id: 16
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/NoOperationTest.kt
  action: "~"
  depends: [1, 4]

- id: 17
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ShowNotificationTest.kt
  action: "~"
  depends: [1, 4]

- id: 18
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/OpenTopBarMenuTest.kt
  action: "~"
  depends: [1, 4]

- id: 19
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/CloseTopBarMenuTest.kt
  action: "~"
  depends: [1, 4]

- id: 20
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/AddLexemeExtTest.kt
  action: "-"
  depends: [1]

- id: 21
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/WordExtTest.kt
  action: "~"
  depends: [1]

- id: 22
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/LexemeExtTest.kt
  action: "~"
  depends: [1]

- id: 23
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/SpecializedLexemeExtTest.kt
  action: "~"
  depends: [1]

- id: 24
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/LoadingExtTest.kt
  action: "~"
  depends: [1]

- id: 25
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/SnackbarExtTest.kt
  action: "~"
  depends: [1]

- id: 26
  file: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/TopBarExtTest.kt
  action: "~"
  depends: [1]
```

### Параллелизм

- #0, #1 — корни, выполняются параллельно.
- #2 ждёт #0 (новые sealed Result типы) + #1 (новые State-типы для маппера).
- #3 ждёт #0 (новая сигнатура UseCase) + #2 (новые Msg-варианты).
- #4 ждёт #1, #2, #3 (использует State, Msg, Effect).
- #5 удаление, зависит только от #2 (удалить вызовы из Reducer'а — внутри #4).
- #6 (ViewModel) ждёт #3 (initEffects) + #5 (удалённый uiHandler).
- #7 (UseCaseImpl) ждёт только #0 — может идти параллельно с #2, #3, #4, ничего из mate/ не импортирует.
- Тесты `mate/*Test.kt` (#9–#19) ждут #1 + #4 — пишутся параллельно после готовности reducer'а.
- Тесты `mate/ext/*Test.kt` (#21–#26) ждут только #1 — параллельно с reducer-тестами.
- Удалённые тесты (#8 LoadingWordTest, #20 AddLexemeExtTest) — independent, могут уйти первыми.

## Часть 2: Детали изменений

### #0 WordCardUseCase.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt`

**Было** (текущий interface):

```kotlin
interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(lexemeId: Long): Boolean
    suspend fun addLexeme(wordId: Long): Lexeme?
    suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long)
    suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long)
}
```

**Стало** (per contract_usecase.md v1 — 8 методов, sealed result):

```kotlin
interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
    suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?
    suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?
}

sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
```

Удалён метод `addLexeme(wordId)` — лексема теперь создаётся локально (NOT_IN_DB) и подтягивается в БД atomic-инсертом внутри `addLexemeTranslation(lexemeId = null, ...)` / `addLexemeDefinition(lexemeId = null, ...)`. `deleteLexeme` теперь принимает `wordId` и возвращает обновлённый список (idempotent). `deleteLexeme*Translation/Definition` возвращают sealed result чтобы handler различал «суб-сущность ушла, лексема жива» vs «лексема каскадно удалена».

---

### #1 State.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`

**Было:** `WordCardState` содержит `addLexemeBottomState: AddLexemeBottomState`, `wordState: WordState` (плоский data class с `id = NOT_IN_DB`, nullable `added`). Помимо state — большой набор extension-функций (showAddLexemeBottom, setTranslationCheck, setDefinitionCheck, updateLexeme, refreshLexemeTranslation, etc.) и mapper `Lexeme.toLexemeState()`.

**Стало** (per contract_state.md v2.5):

```kotlin
@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

// computed (НЕ хранится)
val WordCardState.isCreatingLexeme: Boolean
    get() = lexemeList.any { it.id == NOT_IN_DB }

sealed interface WordState {
    data object NotLoaded : WordState
    data class Loaded(
        val id: Long,
        val added: Date,
        val value: String,
        val isEditMode: Boolean = false,
        val edited: String = "",
        val showWarningDialog: Boolean = false,
    ) : WordState
}
```

Изменения:
- `WordState` — sealed sum `NotLoaded | Loaded` (раньше плоский data class).
- Добавлено поле `isPendingDbOp: Boolean = false` (глобальный pending guard).
- Computed `isCreatingLexeme` — extension-property, derived из `lexemeList`.
- `TextValueState.isEdit` default меняется с `true` на `false` (безопасный дефолт для маппера из БД).
- `LexemeState`, `TopBarState`, `SnackbarState`, `TextValueState` — без structural изменений.
- **Удалить:** `data class AddLexemeBottomState` целиком (старый bottom sheet исключён в IS479).

Extension-функции (state mutation helpers) — переработать:
- **Удалить:** `showAddLexemeBottom`, `hideAddLexemeBottom`, `setTranslationCheck`, `setDefinitionCheck`, `setWordId`, `setWordAdded`, `setWordValue`, `setTerm` (последняя заменяется маппером в `WordLoaded` ветке).
- **Добавить:** `closeAllEditModes(): WordCardState` — хелпер атомарного закрытия всех edit-mode перед открытием нового (инв. 9). Pseudocode:
  ```kotlin
  fun WordCardState.closeAllEditModes(): WordCardState {
      val newWordState = (wordState as? WordState.Loaded)
          ?.copy(isEditMode = false, edited = "")
          ?: wordState
      val newList = lexemeList.map { l ->
          l.copy(
              translation = l.translation?.copy(isEdit = false, edited = ""),
              definition = l.definition?.copy(isEdit = false, edited = ""),
          )
      }
      return copy(wordState = newWordState, lexemeList = newList)
  }
  ```
- **Адаптировать существующие** под sealed `WordState.Loaded` (где это применимо): `enableWordEdit`, `disableWordEdit`, `updateWordEdited`, `showWordWarningDialog`, `hideWordWarningDialog` — теперь оперируют над `wordState` только если он `is WordState.Loaded` (guard на уровне reducer'а).
- `setLexemeList`, `addLexeme(LexemeState)`, `updateLexeme(id, update)`, `removeLexeme(id)`, `toggleLexemeMenu(id)`, `setLexemeMenuOpen(id, open)`, `createLexemeTranslation(id)`, `updateLexemeTranslationText(id, text)`, `enableLexemeTranslationEdit(id)`, `refreshLexemeTranslation(id, newOrigin)` и симметричные для definition — остаются (используются reducer'ом).
- `showSnackbar(title)`, `hideSnackbar()`, `showLoading`, `hideLoading` — остаются.

Mapper:

```kotlin
internal fun Lexeme.toLexemeState(): LexemeState = LexemeState(
    id = lexemeId.id,
    translation = translation?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    definition  = definition?.let { TextValueState(origin = it.value, isEdit = false, edited = "") },
    isMenuOpen  = false,
)
```

Mapper уже соответствует контракту по существу — нужно убедиться что `edited = ""` (а не дефолтное `edited = origin`).

---

### #2 Message.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt`

**Было:** Плоский `sealed interface Msg` + отдельный `internal sealed interface UiMsg : Msg` с `ShowNotification(text, show)`. Содержит `LoadingWord`, `RefreshLexeme(Lexeme)`, `OpenAddLexemeDialog`/`CloseAddLexemeDialog`, `EnableTranslationCreation`/`EnableDefinitionCreation`, `ExitTranslationEditMode`/`ExitDefinitionEditMode`, `RefreshTranslation(Lexeme)`/`RefreshDefinition(Lexeme)`.

**Стало** (per contract_ui_msg.md v3.2):

```kotlin
sealed interface Msg {
    // --- Top bar menu ---
    data object OpenTopBarMenu : Msg
    data object CloseTopBarMenu : Msg

    // --- Delete word ---
    data object OpenDeleteWordDialog : Msg
    data object CloseDeleteWordDialog : Msg
    data class RemoveWord(val wordId: Long) : Msg

    // --- Word edit ---
    data object EnterWordEditMode : Msg
    data class UpdateWordInput(val value: String) : Msg
    data object ExitWordEditMode : Msg
    data object CommitWordChanges : Msg

    // --- Lexeme ---
    data object CreateLexeme : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg
    data class OpenLexemeMenu(val lexemeId: Long, val isShow: Boolean) : Msg

    // --- Translation chip ---
    data class CreateTranslation(val lexemeId: Long) : Msg
    data class UpdateTranslationInput(val lexemeId: Long, val value: String) : Msg
    data class EnterTranslationEditMode(val lexemeId: Long) : Msg
    data class CommitTranslationEdit(val lexemeId: Long) : Msg
    data class CancelTranslationEdit(val lexemeId: Long) : Msg
    data class RemoveTranslation(val lexemeId: Long) : Msg

    // --- Definition chip ---
    data class CreateDefinition(val lexemeId: Long) : Msg
    data class UpdateDefinitionInput(val lexemeId: Long, val value: String) : Msg
    data class EnterDefinitionEditMode(val lexemeId: Long) : Msg
    data class CommitDefinitionEdit(val lexemeId: Long) : Msg
    data class CancelDefinitionEdit(val lexemeId: Long) : Msg
    data class RemoveDefinition(val lexemeId: Long) : Msg

    // --- Navigation + feedback ---
    data object NavigateBack : Msg
    data object DismissNotification : Msg
    data object NoOperation : Msg

    // --- Datasource Msg ---
    data class WordLoaded(val word: Term) : Msg
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg
    data class RefreshTranslation(val lexemeId: Long, val translation: String?) : Msg
    data class RefreshDefinition(val lexemeId: Long, val definition: String?) : Msg
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg
    data class LexemeCascadeRemoved(val lexemeId: Long) : Msg
    data class ShowNotification(val text: String) : Msg
}
```

Удалить:
- `LoadingWord` — loading теперь идёт только через `initEffects` ViewModel'а; ручной refresh-флоу не нужен (после Update вместо повторного LoadWord приходит `RefreshWord`/`RefreshTranslation`/`RefreshDefinition`).
- `RefreshLexeme(Lexeme)` — заменяется на `RefreshLexemeList(List<Lexeme>)` (после `deleteLexeme`) и точечные `RefreshTranslation/RefreshDefinition` (после first-Commit, перехватывают NOT_IN_DB → real id replacement).
- `OpenAddLexemeDialog`, `CloseAddLexemeDialog`, `EnableTranslationCreation`, `EnableDefinitionCreation` — bottom sheet «выбери чекбоксами» исключён.
- `ExitTranslationEditMode`, `ExitDefinitionEditMode` — заменяются на парные `CommitTranslationEdit` / `CancelTranslationEdit` (явное намерение пользователя).
- `RefreshTranslation(Lexeme)` / `RefreshDefinition(Lexeme)` со старой сигнатурой (Lexeme) — заменяются на `RefreshTranslation(lexemeId, translation?)` / `RefreshDefinition(lexemeId, definition?)` (плоский primitive payload).
- `internal sealed interface UiMsg` целиком — `ShowNotification` теперь в основном `Msg` (per contract_ui_msg.md). Никакого `show: Boolean` — есть отдельный `DismissNotification`.

Добавить:
- `CommitTranslationEdit(lexemeId)`, `CancelTranslationEdit(lexemeId)` + симметричные для definition.
- `LexemeCascadeRemoved(lexemeId)` — финальный Msg для серверной cascade (RemoveTranslation/Definition уносит лексему).
- `RefreshLexemeList(List<Lexeme>)` — финальный Msg для `RemoveLexeme` (возвращает актуальный список лексем слова).
- `RefreshWord(Term)` — финальный Msg для `UpdateWord` (resync после `updateWord` + `getTermById`).
- `DismissNotification` — UI-явный snackbar dismiss.
- `ShowNotification(text: String)` — единый Msg для всех handler failures (`show` поле не нужно, `DismissNotification` отдельный).

Изменить сигнатуры:
- `WordLoaded(term)` — параметр переименован `val word: Term` (per spec).

---

### #3 DatasourceEffectHandler.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt`

**Было:** `DatasourceEffect` — 9 вариантов включая `CreateLexeme(wordId)`. Handler выполняет `wordCardUseCase.*` под `withContext(IO)`, без try/catch — exceptions пробрасываются runtime. После некоторых эффектов отдаёт `Msg.LoadingWord` для перезагрузки.

**Стало** (per contract_io.md v7):

```kotlin
sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class RemoveWord(val wordId: Long) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveLexeme(val wordId: Long, val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeTranslation(
        val wordId: Long,
        val lexemeId: Long?,   // null ⇒ atomic insert lexeme + translation
        val translation: String,
    ) : DatasourceEffect
    data class RemoveTranslation(val lexemeId: Long) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        val lexemeId: Long?,   // null ⇒ atomic insert lexeme + definition
        val definition: String,
    ) : DatasourceEffect
    data class RemoveDefinition(val lexemeId: Long) : DatasourceEffect
}
```

Изменения в `DatasourceEffect`:
- Удалить `CreateLexeme(wordId)` — лексема создаётся локально в reducer, БД-insert вшит в `UpdateLexemeTranslation/Definition(lexemeId = null, ...)`.
- `UpdateLexemeTranslation.lexemeId` — теперь `Long?` (раньше `Long` с условным маппингом >-1 → null в handler'е). null теперь явно сигнализирует «atomic insert лексемы + суб-сущности».
- `UpdateLexemeDefinition.lexemeId` — аналогично.
- `RemoveLexeme` получает `wordId` (нужен UseCase для возврата `List<Lexeme>` слова).

Handler — переработка:

```kotlin
override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
    val msg: Msg = withContext(Dispatchers.IO) {
        try {
            when (effect) {
                is DatasourceEffect.LoadWord ->
                    wordCardUseCase.getTermById(effect.wordId)
                        ?.let { Msg.WordLoaded(it) }
                        ?: Msg.WordNotFound

                is DatasourceEffect.RemoveWord -> {
                    val deleted = wordCardUseCase.deleteWord(effect.wordId)
                    if (deleted > 0) Msg.NavigateBack
                    else Msg.ShowNotification("Не удалось удалить слово")
                }

                is DatasourceEffect.UpdateWord -> {
                    val ok = wordCardUseCase.updateWord(effect.wordId, effect.value)
                    if (!ok) Msg.ShowNotification("Не удалось сохранить")
                    else wordCardUseCase.getTermById(effect.wordId)
                        ?.let { Msg.RefreshWord(it) }
                        ?: Msg.ShowNotification("Не удалось получить обновлённое слово")
                }

                is DatasourceEffect.RemoveLexeme ->
                    wordCardUseCase.deleteLexeme(effect.wordId, effect.lexemeId)
                        ?.let { Msg.RefreshLexemeList(it) }
                        ?: Msg.ShowNotification("Не удалось удалить значение")

                is DatasourceEffect.UpdateLexemeTranslation ->
                    wordCardUseCase.addLexemeTranslation(effect.wordId, effect.lexemeId, effect.translation)
                        ?.let { lex -> Msg.RefreshTranslation(lex.lexemeId.id, lex.translation?.value) }
                        ?: Msg.ShowNotification("Не удалось сохранить перевод")

                is DatasourceEffect.RemoveTranslation ->
                    when (val r = wordCardUseCase.deleteLexemeTranslation(effect.lexemeId)) {
                        is RemoveTranslationResult.TranslationRemoved ->
                            Msg.RefreshTranslation(r.lexeme.lexemeId.id, r.lexeme.translation?.value)
                        RemoveTranslationResult.LexemeCascadeRemoved ->
                            Msg.LexemeCascadeRemoved(effect.lexemeId)
                        null ->
                            Msg.ShowNotification("Не удалось удалить перевод")
                    }

                is DatasourceEffect.UpdateLexemeDefinition ->
                    wordCardUseCase.addLexemeDefinition(effect.wordId, effect.lexemeId, effect.definition)
                        ?.let { lex -> Msg.RefreshDefinition(lex.lexemeId.id, lex.definition?.value) }
                        ?: Msg.ShowNotification("Не удалось сохранить определение")

                is DatasourceEffect.RemoveDefinition ->
                    when (val r = wordCardUseCase.deleteLexemeDefinition(effect.lexemeId)) {
                        is RemoveDefinitionResult.DefinitionRemoved ->
                            Msg.RefreshDefinition(r.lexeme.lexemeId.id, r.lexeme.definition?.value)
                        RemoveDefinitionResult.LexemeCascadeRemoved ->
                            Msg.LexemeCascadeRemoved(effect.lexemeId)
                        null ->
                            Msg.ShowNotification("Не удалось удалить определение")
                    }
            }
        } catch (e: Throwable) {
            // UX-инвариант: каждый control-path возвращает разблокирующий Msg
            when (effect) {
                is DatasourceEffect.LoadWord -> Msg.WordNotFound
                is DatasourceEffect.RemoveWord -> Msg.ShowNotification("Не удалось удалить слово")
                is DatasourceEffect.UpdateWord -> Msg.ShowNotification("Не удалось сохранить")
                is DatasourceEffect.RemoveLexeme -> Msg.ShowNotification("Не удалось удалить значение")
                is DatasourceEffect.UpdateLexemeTranslation -> Msg.ShowNotification("Не удалось сохранить перевод")
                is DatasourceEffect.RemoveTranslation -> Msg.ShowNotification("Не удалось удалить перевод")
                is DatasourceEffect.UpdateLexemeDefinition -> Msg.ShowNotification("Не удалось сохранить определение")
                is DatasourceEffect.RemoveDefinition -> Msg.ShowNotification("Не удалось удалить определение")
            }
        }
    }
    consumer(msg)
}
```

Ключевые отличия от текущего:
- Try/catch вокруг всего `when` — каждый control-path возвращает разблокирующий Msg (UX-инвариант: `isPendingDbOp = true` не залипает).
- Удалена ветка `CreateLexeme` (вместе с Effect-вариантом).
- `LoadWord` — exception → silent `WordNotFound` (per контракт).
- `RemoveWord` — exception/0 → `ShowNotification`, success → `NavigateBack`.
- `UpdateWord` — success требует follow-up `getTermById` для `RefreshWord(term)`; null после true → специальный текст.
- `RemoveLexeme` — возвращает `RefreshLexemeList(lexemes)` либо `ShowNotification`.
- `RemoveTranslation/RemoveDefinition` — sealed result branching (3 исхода: success / cascade / null=failure).

---

### #4 WordCardReducer.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/WordCardReducer.kt`

**Было:** Reducer обрабатывает старый набор Msg. Нет глобального `isPendingDbOp` guard, нет `closeAllEditModes`. `WordLoaded` собирает state через `hideLoading().setTerm(...).setLexemeList(...)`. `WordNotFound` = `TODO()`. `RefreshLexeme` обрабатывает BottomSheet-флоу.

**Стало** (per contract_ui_msg.md v3.2 + contract_io.md v7):

Структура:

```kotlin
class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {

    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        // Глобальный guard isPendingDbOp применяется к UI Msg, способным породить Effect
        // (исключения: DismissNotification, Close*, Cancel*Edit, ExitWordEditMode, Update*Input, NavigateBack, NoOperation, любые Datasource Msg)
        if (state.isPendingDbOp && message.isGuardedByPending()) {
            return state to emptySet()
        }
        return when (message) {
            // ... ветки ниже
        }
    }
}

// helper — список Msg, которые НЕ игнорируются под isPendingDbOp
private fun Msg.isGuardedByPending(): Boolean = when (this) {
    is Msg.DismissNotification,
    is Msg.CloseTopBarMenu,
    is Msg.CloseDeleteWordDialog,
    is Msg.CancelTranslationEdit,
    is Msg.CancelDefinitionEdit,
    is Msg.ExitWordEditMode,
    is Msg.UpdateWordInput,
    is Msg.UpdateTranslationInput,
    is Msg.UpdateDefinitionInput,
    is Msg.NavigateBack,
    is Msg.NoOperation,
    is Msg.WordLoaded, is Msg.WordNotFound, is Msg.RefreshWord,
    is Msg.RefreshTranslation, is Msg.RefreshDefinition, is Msg.RefreshLexemeList,
    is Msg.LexemeCascadeRemoved, is Msg.ShowNotification -> false
    else -> true
}
```

Ключевые ветки (pseudocode, не реализация):

- **WordLoaded(word):** атомарная сборка:
  ```
  state.copy(
    isLoading = false,
    isPendingDbOp = false,
    wordState = WordState.Loaded(id = word.wordId.id, added = word.addedDate, value = word.word.value),
    lexemeList = word.lexemeList.map { it.toLexemeState() }
  ) to emptySet()
  ```

- **WordNotFound:** `state.copy(isLoading = false, isPendingDbOp = false) to setOf(NavigationEffect.Back)`.

- **RefreshWord(word):** только обновление title + сброс edit-mode + `isPendingDbOp = false`. wordState уже Loaded.

- **OpenTopBarMenu / CloseTopBarMenu / OpenDeleteWordDialog / CloseDeleteWordDialog:** локальные toggles без Effect; OpenDeleteWordDialog guarded by `wordState is Loaded`.

- **EnterWordEditMode:** `state.closeAllEditModes().enableWordEdit() to emptySet()`. Guard: `wordState !is Loaded`.

- **UpdateWordInput(value):** локальный update `edited`. Guard: `!isEditMode`.

- **ExitWordEditMode:** `state.disableWordEdit() to emptySet()` (cancel — buffer затирается).

- **CommitWordChanges:** guard `edited.isBlank()` → `state to emptySet()`. Иначе:
  ```
  state.copy(isPendingDbOp = true).disableWordEdit() to setOf(DatasourceEffect.UpdateWord(loaded.id, loaded.edited))
  ```

- **RemoveWord(wordId):** guard `wordState !is Loaded ∨ id != msg.wordId`. Success:
  ```
  state.copy(isPendingDbOp = true).hideWordWarningDialog().hideMenu() to setOf(DatasourceEffect.RemoveWord(wordId))
  ```

- **CreateLexeme:** guard `wordState !is Loaded ∨ lexemeList.any { it.id == NOT_IN_DB }`. Чисто локально:
  ```
  state.closeAllEditModes().let { closed ->
      closed.copy(lexemeList = closed.lexemeList + LexemeState(id = NOT_IN_DB, translation = null, definition = null, isMenuOpen = false))
  } to emptySet()
  ```
  Никаких Effect — лексема живёт локально до первого `Commit*Edit`.

- **RemoveLexeme(lexemeId):** для `NOT_IN_DB` — локальное удаление `state.removeLexeme(...)`; для real id:
  ```
  state.copy(isPendingDbOp = true).setLexemeMenuOpen(lexemeId, false) to setOf(DatasourceEffect.RemoveLexeme(loaded.id, lexemeId))
  ```

- **OpenLexemeMenu(lexemeId, isShow=true):** `state.closeAllEditModes().setLexemeMenuOpen(lexemeId, true) to emptySet()`. `isShow=false`: просто toggle off.

- **CreateTranslation(lexemeId):** guard `lexeme.translation != null`. Сначала `closeAllEditModes()`, затем на возвращённом state вызывать `createLexemeTranslation(lexemeId)` (поверх `closed.lexemeList`, не `state.lexemeList`!).

- **EnterTranslationEditMode(lexemeId):** guard `translation == null`. `state.closeAllEditModes().enableLexemeTranslationEdit(lexemeId)`.

- **UpdateTranslationInput(lexemeId, value):** локальный update `edited`. Guard `translation == null ∨ !isEdit`.

- **CancelTranslationEdit(lexemeId):** локальный reset `isEdit = false, edited = ""`. Если `translation.origin.isEmpty()` (свежесозданный chip) — nullify `translation = null`. После nullify — проверить cascade (translation == null ∧ definition == null) для `NOT_IN_DB`: убрать лексему из списка.

- **CommitTranslationEdit(lexemeId):** 4 ветки (pseudocode):
  ```
  val lex = lexemeList.first { it.id == lexemeId }
  val t = lex.translation ?: return state to emptySet()
  val edited = t.edited
  val origin = t.origin
  when {
      edited.isBlank() && origin.isEmpty() -> {
          // ветка 1a: локальный nullify
          val nullified = state.updateLexeme(lexemeId) { it.copy(translation = null) }
          val final = if (lex.id == NOT_IN_DB && lex.definition == null) nullified.removeLexeme(lexemeId) else nullified
          final to emptySet()
      }
      edited.isBlank() && origin.isNotEmpty() -> {
          // ветка 1: pessimistic remove (real id only — NOT_IN_DB сюда не попадает: origin не бывает != "")
          state.copy(isPendingDbOp = true)
               .updateLexeme(lexemeId) { it.copy(translation = it.translation?.copy(isEdit = false, edited = "")) } to
               setOf(DatasourceEffect.RemoveTranslation(lexemeId))
      }
      edited == origin -> {
          // ветка 2: no-op commit
          state.updateLexeme(lexemeId) { it.copy(translation = it.translation?.copy(isEdit = false, edited = "")) } to emptySet()
      }
      else -> {
          // ветка 3: Update / first-Commit
          val effectLexemeId: Long? = if (lex.id == NOT_IN_DB) null else lex.id
          state.copy(isPendingDbOp = true)
               .updateLexeme(lexemeId) { it.copy(translation = it.translation?.copy(isEdit = false, edited = "")) } to
               setOf(DatasourceEffect.UpdateLexemeTranslation(loaded.id, effectLexemeId, edited))
      }
  }
  ```

- **RemoveTranslation(lexemeId):** для `NOT_IN_DB` — локальный nullify + локальный cascade check; для real id:
  ```
  state.copy(isPendingDbOp = true).setLexemeMenuOpen(lexemeId, false) to setOf(DatasourceEffect.RemoveTranslation(lexemeId))
  ```

- Зеркальные ветки для **Definition**: `CreateDefinition`, `EnterDefinitionEditMode`, `UpdateDefinitionInput`, `CommitDefinitionEdit`, `CancelDefinitionEdit`, `RemoveDefinition` — структурно идентичны Translation.

- **RefreshTranslation(lexemeId, translation):**
  ```
  val realExists = state.lexemeList.any { it.id == lexemeId }
  val notInDbExists = state.lexemeList.any { it.id == NOT_IN_DB }
  val newList = when {
      realExists -> state.lexemeList.map { l ->
          if (l.id != lexemeId) l
          else if (translation == null) l.copy(translation = null)
          else l.copy(translation = l.translation?.copy(origin = translation) ?: TextValueState(origin = translation, isEdit = false, edited = ""))
      }
      notInDbExists -> state.lexemeList.map { l ->
          if (l.id != NOT_IN_DB) l
          else l.copy(id = lexemeId, translation = translation?.let { TextValueState(origin = it, isEdit = false, edited = "") })
      }
      else -> state.lexemeList
  }
  state.copy(isPendingDbOp = false, lexemeList = newList) to emptySet()
  ```
  Refresh **не** закрывает активный edit пользователя (если он успел зайти в edit повторно во время pending).

- **RefreshDefinition(lexemeId, definition):** зеркально RefreshTranslation.

- **RefreshLexemeList(lexemes):** заменяет `lexemeList` целиком, сохраняет локальную `NOT_IN_DB` лексему если она существует и не пересекается с возвращёнными id.
  ```
  val mapped = lexemes.map { it.toLexemeState() }
  val keepLocal = state.lexemeList.firstOrNull { it.id == NOT_IN_DB }
  state.copy(isPendingDbOp = false, lexemeList = mapped + listOfNotNull(keepLocal)) to emptySet()
  ```

- **LexemeCascadeRemoved(lexemeId):** `state.copy(isPendingDbOp = false).removeLexeme(lexemeId) to emptySet()`.

- **NavigateBack:** shared ветка (UI back + handler-confirm после RemoveWord/WordNotFound):
  ```
  state.copy(isPendingDbOp = false) to setOf(NavigationEffect.Back)
  ```

- **ShowNotification(text):** `state.copy(isPendingDbOp = false).showSnackbar(text) to emptySet()`.

- **DismissNotification:** guard `!snackbarState.show`. `state.hideSnackbar() to emptySet()`.

- **NoOperation:** `state to emptySet()`.

Удалить ветки: `LoadingWord`, `RefreshLexeme`, `OpenAddLexemeDialog`, `CloseAddLexemeDialog`, `EnableTranslationCreation`, `EnableDefinitionCreation`, `ExitTranslationEditMode`, `ExitDefinitionEditMode`, `UiMsg.ShowNotification`.

Импорт `DatasourceEffect.LoadWord` остаётся (нужен для ничего внутри reducer'а — фактически loading инициирует ViewModel). Можно удалить unused import.

---

### #5 UiEffectHandler.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/UiEffectHandler.kt`

Удаляется целиком. `sealed interface UiEffect` и `class UiEffectHandler` становятся мёртвым кодом:
- Контракт IO v7: «UI Effects отсутствуют. Snackbar реализуется через snackbarState в State».
- `UiMsg.ShowNotification(text, show)` заменён на `Msg.ShowNotification(text)` + `Msg.DismissNotification` — handler как мост между Effect и Msg больше не нужен.

Импортируется только `WordCardViewModel.kt` — удаление зависит от обновления конструктора ViewModel (#6).

---

### #6 WordCardViewModel.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardViewModel.kt`

**Было:**

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {
    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        ...
        effectHandlerSet = setOf(datasourceHandler, uiHandler, navHandlerFactory.create(navigator)),
    )
}
```

**Стало:** удалить `uiHandler: UiEffectHandler` параметр и `uiHandler` из `effectHandlerSet`. Остальное без изменений — `initEffects` уже корректный (`LoadWord(wordId)`), `WordCardState()` по-прежнему вызывается без аргументов (новые дефолты внутри #1).

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {
    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        coroutineScope = viewModelScope,
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(datasourceHandler, navHandlerFactory.create(navigator)),
    )
    ...
}
```

Удалить импорт `UiEffectHandler`.

---

### #7 WordCardUseCaseImpl.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`

**Было:** 8 методов, среди них `addLexeme(wordId)` отдельным методом (insert лексемы + addWriteQuiz + getLexemeById). `deleteLexeme(lexemeId)` принимает только lexemeId, возвращает `Boolean`. `addLexemeTranslation` для `lexemeId == null` фактически делает no-op — возвращает `null` (всё условно завязано на не-null lexemeId). `deleteLexemeTranslation/Definition` — `Unit` return; внутри ветвление translation-vs-cascade без явного результата.

**Стало** (per contract_usecase.md v1 + atomicity contract из contract_io.md v7):

```kotlin
class WordCardUseCaseImpl @Inject constructor(
    private val wordApi: CoreDbApi.WordApi,
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val quizApi: CoreDbApi.QuizApi,
    private val prefsProvider: PrefsProvider,
) : WordCardUseCase {

    override suspend fun getTermById(wordId: Long): Term? = ...  // без изменений

    override suspend fun deleteWord(wordId: Long): Int =
        wordApi.deleteWordSuspend(wordId)

    override suspend fun updateWord(wordId: Long, value: String): Boolean =
        wordApi.updateWordSuspend(wordId, value)

    override suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>? = try {
        lexemeApi.deleteLexeme(lexemeId)
        termApi.getTermById(wordId)?.lexemeList?.map { it.toDomainEntity() }
    } catch (_: Throwable) { null }

    override suspend fun addLexemeTranslation(
        wordId: Long, lexemeId: Long?, translation: String,
    ): Lexeme? = try {
        val id = if (lexemeId == null) {
            // ATOMIC: один INSERT с переводом через existing overload (CoreDbApi.kt:80-83).
            // Импл (CoreDbApiImpl.kt:216-228) — один wordDao.addLexeme(LexemeDb(wordId, translation=value, addDate)).
            // Атомарно по построению Room (один suspend insert).
            val newId = insertLexemeWithTranslation(wordId, translation)
            return@try lexemeApi.getLexemeById(newId)?.toDomainEntity()
        } else {
            lexemeId
        }
        lexemeApi.updateLexemeTranslation(id, TranslationApiEntity(translation))
        lexemeApi.getLexemeById(id)?.toDomainEntity()
    } catch (_: Throwable) { null }

    override suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult? = try {
        val lexeme = lexemeApi.getLexemeById(lexemeId)
        if (lexeme?.canRemoveTranslation() == true) {
            lexemeApi.updateLexemeTranslation(lexemeId, translation = null)
            val updated = lexemeApi.getLexemeById(lexemeId)?.toDomainEntity()
                ?: return null
            RemoveTranslationResult.TranslationRemoved(updated)
        } else {
            lexemeApi.deleteLexeme(lexemeId)
            RemoveTranslationResult.LexemeCascadeRemoved
        }
    } catch (_: Throwable) { null }

    override suspend fun addLexemeDefinition(
        wordId: Long, lexemeId: Long?, definition: String,
    ): Lexeme? = try {
        val id = if (lexemeId == null) {
            val newId = insertLexemeWithDefinition(wordId, definition)
            return@try lexemeApi.getLexemeById(newId)?.toDomainEntity()
        } else {
            lexemeId
        }
        lexemeApi.updateLexemeDefinition(id, DefinitionApiEntity(definition))
        lexemeApi.getLexemeById(id)?.toDomainEntity()
    } catch (_: Throwable) { null }

    override suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult? = try {
        val lexeme = lexemeApi.getLexemeById(lexemeId)
        if (lexeme?.canRemoveDefinition() == true) {
            lexemeApi.updateLexemeDefinition(lexemeId, definition = null)
            val updated = lexemeApi.getLexemeById(lexemeId)?.toDomainEntity()
                ?: return null
            RemoveDefinitionResult.DefinitionRemoved(updated)
        } else {
            lexemeApi.deleteLexeme(lexemeId)
            RemoveDefinitionResult.LexemeCascadeRemoved
        }
    } catch (_: Throwable) { null }

    // Atomic INSERT через existing CoreDbApi.LexemeApi overload + write quiz.
    private suspend fun insertLexemeWithTranslation(wordId: Long, translation: String): Long {
        val currentDictionaryId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?: throw IllegalStateException("Dictionary not found")
        val dictionaryId = dictionaryApi.getDictionaryById(currentDictionaryId)?.id
            ?: throw IllegalStateException("Dictionary not found")
        val newId = lexemeApi.addLexeme(wordId, TranslationApiEntity(translation))
        quizApi.addWriteQuiz(dictionaryId = dictionaryId, lexemeId = newId)
        return newId
    }

    private suspend fun insertLexemeWithDefinition(wordId: Long, definition: String): Long {
        val currentDictionaryId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?: throw IllegalStateException("Dictionary not found")
        val dictionaryId = dictionaryApi.getDictionaryById(currentDictionaryId)?.id
            ?: throw IllegalStateException("Dictionary not found")
        val newId = lexemeApi.addLexeme(wordId, DefinitionApiEntity(definition))
        quizApi.addWriteQuiz(dictionaryId = dictionaryId, lexemeId = newId)
        return newId
    }
}
```

Ключевые изменения:
- Удалён `addLexeme(wordId)` (заменён private helper'ами `insertLexemeWithTranslation` / `insertLexemeWithDefinition`).
- `addLexemeTranslation/Definition(lexemeId = null, ...)` — atomic через **existing** `lexemeApi.addLexeme(wordId, TranslationApiEntity(...))` (один INSERT, БД-атомарно по построению). `withTransaction` НЕ нужен — атомарность одной row insert тривиально гарантируется Room. Lexeme сразу создаётся с заполненной суб-сущностью; пустая лексема в БД НЕ появляется.
- `deleteLexeme(wordId, lexemeId): List<Lexeme>?` — idempotent, использует `termApi.getTermById(wordId).lexemeList` для resync.
- `deleteLexemeTranslation/Definition` — возвращает sealed result (`*Removed(lexeme)` / `LexemeCascadeRemoved` / `null` на exception).
- Все методы с записью обёрнуты в `try { ... } catch { null }` — отказы возвращаются как `null`, handler конвертирует в `ShowNotification`.
- НЕТ инъекции `AppDatabase`. Конструктор остаётся прежним (6 параметров).

> **F077 (ит.1 review): atomicity достигается existing API.** Перегрузки `lexemeApi.addLexeme(wordId, translation)` и `lexemeApi.addLexeme(wordId, definition)` объявлены в `core/core-db-api/.../CoreDbApi.kt:80-88` и реализованы в `core/core-db-impl/.../CoreDbApiImpl.kt:216-242` как один `wordDao.addLexeme(LexemeDb(wordId, translation=value OR definition=value, addDate))`. Это один INSERT — атомарно. Никакого расширения data-слоя / `withTransaction` / `@Transaction` не требуется.

---

### #8 LoadingWordTest.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LoadingWordTest.kt`

Удаляется целиком. `Msg.LoadingWord` удалён (loading инициируется только ViewModel'ом через `initEffects`, ручной refresh заменён точечными `Refresh*`).

---

### #9 WordLoadedTest.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt`

Адаптация под новый sealed `WordState`. `WordLoaded(term)` теперь конструирует `WordState.Loaded(...)` атомарно + сбрасывает `isLoading = false`, `isPendingDbOp = false`. Тесты:
- «Из `NotLoaded + isLoading=true` после `WordLoaded(term)` → `wordState is Loaded`, `lexemeList = mapped`, `isPendingDbOp = false`».
- Old assertions на `wordState.id`/`wordState.value` (плоский data class) → `assertTrue(state.wordState is WordState.Loaded)` + smart cast.

---

### #10 WordEditTest.kt [~]

Адаптация под sealed `WordState.Loaded`. Все стартовые фикстуры с `WordState(id = ..., value = ...)` → `WordState.Loaded(id, added = Date(), value)`. Добавить тесты:
- `CommitWordChanges` с `edited.isBlank()` → `state to emptySet()` (инв. 10).
- `EnterWordEditMode` под активным `translation.isEdit = true` другой лексемы → `closeAllEditModes` сбрасывает chip.

---

### #11 DeleteWordDialogTest.kt [~]

Адаптация под `WordState.Loaded.showWarningDialog`. Guard-тесты: `OpenDeleteWordDialog` при `wordState is NotLoaded` → no-op.

---

### #12 LexemeManagementTest.kt [~]

Перепись под новый контракт:
- `CreateLexeme` — локальное добавление NOT_IN_DB, без Effect; guard повторного создания при существующей NOT_IN_DB.
- `RemoveLexeme` для `NOT_IN_DB` — локальное удаление; для real id — Effect + `isPendingDbOp = true`.
- `RefreshLexemeList(lexemes)` ветка.
- `LexemeCascadeRemoved(lexemeId)` ветка.
- Удалить тесты на `OpenAddLexemeDialog`/`CloseAddLexemeDialog`/`EnableTranslationCreation`/`EnableDefinitionCreation`/`RefreshLexeme` (Msg удалены).

---

### #13 TranslationManagementTest.kt [~]

Перепись под новый контракт chip Translation:
- `CreateTranslation`: guard `translation != null`; `closeAllEditModes` + `createLexemeTranslation`.
- `EnterTranslationEditMode`: closeAllEditModes сбрасывает другие edit.
- `UpdateTranslationInput`: локально, без Effect.
- `CommitTranslationEdit` — 4 ветки (1a локальный nullify, 1 pessimistic remove, 2 no-op, 3 update); включая first-Commit replacement NOT_IN_DB → real id через `RefreshTranslation(realId, "hello")`.
- `CancelTranslationEdit`: для свежесозданного chip с origin="" — nullify + локальный cascade; для существующего — просто reset.
- `RemoveTranslation` для `NOT_IN_DB` (локальный) и real id (Effect).
- `RefreshTranslation(lexemeId, null)` — nullify ветка.
- `RefreshTranslation` при активном edit пользователя — origin обновлён, isEdit/edited сохранены.

Удалить тесты на старый `ExitTranslationEditMode` (Msg удалён).

---

### #14 DefinitionManagementTest.kt [~]

Зеркально #13 для Definition.

---

### #15 NavigateBackTest.kt [~]

Адаптация под `state.copy(isPendingDbOp = false) to setOf(NavigationEffect.Back)`. Добавить тест: `NavigateBack` сбрасывает `isPendingDbOp` (shared ветка для handler-confirm после `RemoveWord` success).

---

### #16 NoOperationTest.kt [~]

Минимальная адаптация: новые поля state (`isPendingDbOp`, sealed wordState) в фикстурах. Логика `state to emptySet()` без изменений.

---

### #17 ShowNotificationTest.kt [~]

Был `UiMsg.ShowNotification(text, show)`. Стало `Msg.ShowNotification(text)` без `show`. Тесты:
- `ShowNotification("...")` → `state.snackbarState.show = true`, `title = "..."`, `isPendingDbOp = false`.
- Новый Msg `DismissNotification` → `state.snackbarState.show = false`; guard `!show` → `state to emptySet()`.

---

### #18 OpenTopBarMenuTest.kt [~]

Минимальная адаптация фикстур (новые поля state). Логика без изменений.

---

### #19 CloseTopBarMenuTest.kt [~]

Минимальная адаптация фикстур. Добавить guard-тест: `CloseTopBarMenu` при `!isMenuOpen` → `state to emptySet()`.

---

### #20 AddLexemeExtTest.kt [-]

Удаляется целиком. Все тестируемые extensions (`showAddLexemeBottom`, `hideAddLexemeBottom`, `setTranslationCheck`, `setDefinitionCheck`) удалены из `State.kt` (#1) вместе с `AddLexemeBottomState`.

---

### #21 WordExtTest.kt [~]

Адаптация под sealed `WordState.Loaded`. Все тесты `setWordId`/`setWordAdded`/`setWordValue`/`setTerm` удалены (extensions удалены). Тесты `enableWordEdit`/`disableWordEdit`/`updateWordEdited`/`showWordWarningDialog`/`hideWordWarningDialog` — переписать под `WordState.Loaded` фикстуры. Добавить тест: extension на `WordCardState` с `wordState is NotLoaded` → no-op (или explicitly throws — по решению impl).

---

### #22 LexemeExtTest.kt [~]

Адаптация фикстур (новые поля state, sealed wordState). Сами extensions (`setLexemeList`, `addLexeme`, `updateLexeme`, `removeLexeme`, `toggleLexemeMenu`) — без structural изменений.

---

### #23 SpecializedLexemeExtTest.kt [~]

Адаптация фикстур + опционально тест нового хелпера `closeAllEditModes()`.

---

### #24 LoadingExtTest.kt [~]

Минимальная адаптация фикстур (новый `isPendingDbOp = false` default в WordCardState()).

---

### #25 SnackbarExtTest.kt [~]

Минимальная адаптация фикстур. Сами `showSnackbar`/`hideSnackbar` — без structural изменений.

---

### #26 TopBarExtTest.kt [~]

Минимальная адаптация фикстур.

---

## Лог итераций

### ит.1 (2026-05-19T22:15:00-0600)

Составлен граф из 27 узлов (0–26).

**Распределение по action:**
- [+] 0 файлов — все типы данных (sealed `WordState`, sealed `RemoveTranslationResult` / `RemoveDefinitionResult`, helper `closeAllEditModes`) встроены в существующие `State.kt` / `WordCardUseCase.kt`.
- [~] 24 файла — 6 production (#0 UseCase, #1 State, #2 Message, #3 Handler, #4 Reducer, #6 ViewModel) + #7 UseCaseImpl (app-модуль) = 7 production [~], плюс 17 test-файлов [~].
- [-] 3 файла — #5 `UiEffectHandler.kt` (мёртвый код per contract_io v7 «UI Effects отсутствуют»), #8 `LoadingWordTest.kt` (`Msg.LoadingWord` удалён), #20 `AddLexemeExtTest.kt` (`AddLexemeBottomState` удалён).

**Главные зависимости:**
- #0 (UseCase interface + sealed result types) — корень для #2 (Msg), #3 (Handler), #7 (Impl).
- #1 (State + sealed WordState + `isPendingDbOp` + `closeAllEditModes`) — корень для #2, #4 и всех тестов.
- #4 (Reducer) — конечная точка для всех reducer-тестов (#9–#19), зависит от #1, #2, #3.
- #5 (удаление UiEffectHandler) → #6 (правки ViewModel) — серийная цепочка.

**Out of scope (UI sub-flow):** `WordCardScreen.kt`, все виджеты под `widget/` (включая `AddLexemeBottomWidget.kt`, `LexemeMeaningWidget.kt` — будут переписаны/удалены на UI этапе), `WordCardNavigator*`, инициация Mate в Screen, маппинг новых Msg в UI handlers.

**Параллельные группы:**
- Группа 1 (старт): #0, #1.
- Группа 2: #2, #7 (#7 не зависит от mate, идёт параллельно с #2–#4).
- Группа 3: #3, #5.
- Группа 4: #4, #6.
- Группа 5 (фан-аут тестов): #8–#26 параллельно (общая зависимость от #1 + #4).

DAG verified: циклов нет (зависимости строго от меньших id к большим).

### ит.2 (2026-05-19T22:25:00-0600) — conductor-патч после review

F076/F077/F078/F079 закрыты conductor-патчем (без отдельного execute субагента):
- **F076 critical accepted** — compile-break принят как intermediate state, ремарка добавлена в шапку. UI sub-flow восстановит компиляцию.
- **F077 critical rejected** — atomicity достигается существующей перегрузкой `CoreDbApi.LexemeApi.addLexeme(wordId, translation)` / `addLexeme(wordId, definition)` (`CoreDbApi.kt:80-88`, impl `CoreDbApiImpl.kt:216-242`). Никакого `withTransaction` / `AppDatabase`-инъекции не нужно. Pseudocode #7 переписан под `lexemeApi.addLexeme(wordId, TranslationApiEntity(...))` — один atomic INSERT.
- **F078 minor approved** — фикстуры тестов #10/#11/#15 опираются на `WordState.Loaded`, зависимость от #1 усилена (по факту так и было).
- **F079 minor rejected** — `UiEffect` извне модуля не ссылается, удаление безопасно.

DAG не меняется (количество узлов 27, action distribution 24/3 [~]/[-]). Меняется только pseudocode #7 и шапка артефакта.

---

_model: claude opus 4.7 (1M context)_
