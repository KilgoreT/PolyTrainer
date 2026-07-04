# Approved findings — business_implement.md, iteration 3

5 minor + 3 carryover (F116, F119, F120 ещё open). Iter 4 cleanup.

## F142 [minor] — failureLabel extension дубликат

В обоих `Reducer.kt` (CM + PerDict) объявлен `private fun Throwable.failureLabel(): String` идентично.

**Что исправить:** Вынести в shared util. Опции:
1. `:modules:domain:lexeme` — `me.apomazkin.lexeme.FailureLabelExt.kt` (но lexeme не domain Throwable extension).
2. `:modules:core:tools` — `me.apomazkin.tools.ThrowableExt.kt` (правильнее, общеcore util).

Recommended: вариант 2. После — обновить imports в обоих Reducer и удалить локальные copies.

## F143 [minor] — Vocabulary.kt literal route

В `modules/screen/main/.../Vocabulary.kt:63` (или около):
```kotlin
navigate(route = "per_dict_components/$dictionaryId")
```

**Что исправить:** заменить на:
```kotlin
navigate(route = "$PER_DICT_COMPONENTS_ROUTE/$dictionaryId")
```

(Константа `PER_DICT_COMPONENTS_ROUTE` уже declared на line ~11 в том же файле, parity с `Settings.kt`'s use of `COMPONENTS_MANAGER_ROUTE`.)

## F144 [minor] — ImpactPreviewFailed snackbar guard

В Reducer ветке `Msg.ImpactPreviewFailed`:
```kotlin
state.deleteConfirm?.let { dlg ->
    if (dlg.typeId == msg.typeId) {
        state.copy(...) to setOf(UiEffect.Snackbar("Failed to load impact"))
    } else state to emptySet()
} ?: (state to setOf(UiEffect.Snackbar("Failed to load impact")))  // ← FIX
```

**Что исправить:** если `state.deleteConfirm == null` (user закрыл диалог) — НЕ показывать snackbar:
```kotlin
state.deleteConfirm?.let { dlg ->
    if (dlg.typeId == msg.typeId) {
        state.copy(deleteConfirm = dlg.copy(isLoadingImpact = false)) to setOf(UiEffect.Snackbar("Failed to load impact"))
    } else state to emptySet()
} ?: (state to emptySet())  // dialog closed → silent ignore
```

Применить в **обоих** Reducer.kt (CM + PerDict).

## F145 [minor] — distinct outcome для null vs error

В `DatasourceEffectHandler.LoadImpact`:
```kotlin
val impact = useCase.previewDeletionImpact(typeId)
    ?: throw IllegalStateException("preview returned null")
```

Проблема: useCase может вернуть null **либо** потому что typeId не найден (legitimate), **либо** потому что DB error (already swallowed by useCase try/catch).

**Что исправить:** В useCase impl `previewDeletionImpact` различать not-found (return null) и error (return через outcome). Options:

(a) Изменить signature `previewDeletionImpact(typeId): DeletionImpact?` → `PreviewOutcome` sealed (NotFound / Loaded(impact) / Failure(cause)). Это требует update API contract + всех call-sites.

(b) Минимально: в Handler различать через rethrow CancellationException + explicit null check без exception:
```kotlin
val impact = useCase.previewDeletionImpact(typeId)
if (impact == null) {
    send(Msg.ImpactPreviewFailed(typeId, reason = NotFound))
    return
}
send(Msg.ImpactPreviewLoaded(typeId, impact))
```

Recommended **минимум (b)** для текущего iter — добавить `reason` field в `Msg.ImpactPreviewFailed`. UseCase exception → Failure outcome; useCase null → NotFound. Reducer snackbar text differentiated.

## F146 [minor] — CancellationException test symmetry

В `DatasourceEffectHandlerTest.kt` (CM + PerDict) добавить:
- `given LoadImpact effect, when useCase.previewDeletionImpact throws CancellationException, then exception propagates (not wrapped in Msg)`.
- `given SoftDelete effect, when useCase.softDeleteComponent throws CancellationException, then exception propagates`.

(Patterns parity с existing tests на CreateComponent / RenameComponent.)

## F116 [minor carryover from iter 1] — NameTooLong validation

Из iter 1 review approved но не applied (deferred to iter 5 cleanup, теперь iter 4).

В `ComponentsManagerUseCaseImpl.createUserDefinedComponent`:
```kotlin
if (name.length > NAME_MAX_LEN) return CreateOutcome.NameTooLong
```

Где `NAME_MAX_LEN: Int` — companion constant. Возможные значения: 64. Проверь convention существующих fields (`Dictionary.name` — какой max?).

В `:modules:domain:lexeme/ComponentType.kt` или новый `Component.kt` объявить companion `const val NAME_MAX_LEN = 64`.

Возможно нужно ввести в `createUserDefinedComponent` ветку.

Update test 1.2.X в `ComponentsManagerUseCaseImplTest.kt`: `given name with length > NAME_MAX_LEN, when createUserDefinedComponent, then returns NameTooLong`.

## F119 [minor carryover] — dictionaryApi unused в ComponentsManagerUseCaseImpl

Verify: используется ли сейчас `dictionaryApi: CoreDbApi.DictionaryApi` в ctor `ComponentsManagerUseCaseImpl`? Если нет — удалить из ctor + обновить DI wiring.

(После Pass 5 миграции — может уже стал нужен; verify первым.)

## F120 [minor carryover] — test count doc fix

В `business_implement.md` § «Тесты» обновить counts по реальным `@Test` annotation counts. Sub-agent делал retrofit — счётчики могли измениться (Pass 3 итог 69, Pass 4 итог 77 — но retrofit добавил еще tests).

Reread test files, посчитать, update в artifact.
