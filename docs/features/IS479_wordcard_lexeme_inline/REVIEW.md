# IS479 Code Review

3 параллельных агента (architecture / bugs / yagni). После дедупа — **37 finding'ов**, сквозная нумерация по severity (major → minor → nit). Подробности в исходных отчётах: `/tmp/review_architecture.md`, `/tmp/review_bugs.md`, `/tmp/review_yagni.md`.

---

## Major

### 1. Reducer добавляет новый код по legacy state-based snackbar
- **Cat:** architecture (A1)
- **Где:** `WordCardReducer.kt:368,296`; `State.kt:73-77,313-327`; `Message.kt:47,58`; `WordCardScreen.kt:90`; `SnackbarLaunchEffect.kt`
- **Суть:** error-пути из `DatasourceEffectHandler` идут через `Msg.ShowNotification` → `state.snackbarState` → `SnackbarLaunchEffect`. Гайд `ui-patterns.md` прямо запрещает: "Не добавлять новый код по legacy-паттерну". Success-пути уже на UiHost.
- **Фикс:** перевести ошибки на `UiEffect.ShowErrorSnackbar(messageRes)`; удалить `SnackbarState`/`Msg.ShowNotification`/`Msg.DismissNotification`/`SnackbarLaunchEffect`.

### 2. Захардкоженные русские строки ошибок в DatasourceEffectHandler
- **Cat:** architecture + yagni + bug (A2 + Y16 + B7)
- **Где:** `DatasourceEffectHandler.kt:70,76,80,98,111,127,140,156,165,176-183`
- **Суть:** все error-сообщения — `String` хардкод на русском, в `Msg.ShowNotification(text: String)`. Каждое сообщение дублируется ≥2 раза (когда + catch). Нет `@StringRes`, нет ресурсов, generic "Не удалось" без локализации.
- **Фикс:** завести `R.string.word_card_error_*`, заменить `Msg.ShowNotification(String)` → `UiEffect.ShowErrorSnackbar(@StringRes Int)`. Связано с #1.

### 3. FQN `me.apomazkin.core_resources.R.string.*` в reducer
- **Cat:** architecture (A3)
- **Где:** `WordCardReducer.kt:378-379,392-393,415-417,423-425,444-445`
- **Суть:** reducer ссылается на R-класс полным путём (5 раз), портит читаемость; import нет.
- **Фикс:** добавить `import me.apomazkin.core_resources.R`, использовать `R.string.*` или вынести в константы.

### 4. RestoreLexeme при undo full-delete не атомарен
- **Cat:** bug (B1)
- **Где:** `WordCardUseCaseImpl.kt:136-141`
- **Суть:** для лексемы с translation+definition: `insertLexemeWithTranslation` затем отдельный `updateLexemeDefinition`. Если второй упадёт — в БД полу-лексема, в UI ошибка.
- **Фикс:** atomic API `addLexemeWithTranslationAndDefinition` или rollback (`deleteLexeme(id)`) в catch.

### 5. `commitAndCloseAllEdits` под NotLoaded мутирует lexemeList
- **Cat:** bug (B2)
- **Где:** `State.kt:189-190`
- **Суть:** при `wordState !is Loaded` возвращает `closeAllEditModes()`, который мапит `lexemeList`. Фактически на NotLoaded list пустой, но защита хрупкая.
- **Фикс:** под NotLoaded — `this to emptySet()` без мутаций.

### 6. `RefreshTranslation/RefreshDefinition` слепо переименует NOT_IN_DB
- **Cat:** bug (B3)
- **Где:** `WordCardReducer.kt:609-687`
- **Суть:** если `realExists == false && notInDbExists == true` — всегда переименовывает NOT_IN_DB в lexemeId. Нет проверки, что это была именно та лексема, для которой был INSERT.
- **Фикс:** различать Update (lexemeId != null) и Insert (lexemeId == null) на reducer-стороне; pending-insert marker.

### 7. Cascade-without-undo оставляет пустой NOT_IN_DB-черновик → FAB заблокирован
- **Cat:** bug (B4)
- **Где:** `WordCardReducer.kt:353-366`; `WordCardScreen.kt:121`
- **Суть:** `Msg.LexemeCascadeRemoved` превращает лексему в NOT_IN_DB-черновик с null/null. `isCreatingLexeme = true` → FAB блокирован; пользователь не понимает что делать. Inconsistency с `LexemeCascadeRemovedWithUndo`.
- **Фикс:** unify поведение — оба либо удаляют целиком, либо оба оставляют черновик. См. также #25 (Y11) — `LexemeCascadeRemoved` сейчас вообще не эмитится handler'ом.

### 8. `RefreshLexemeList` сохраняет NOT_IN_DB-черновик с активным edit
- **Cat:** bug (B5)
- **Где:** `WordCardReducer.kt:342-351`
- **Суть:** `keepLocal = lexemeList.firstOrNull { id == NOT_IN_DB }` сохраняет черновик целиком, включая `isEdit=true, edited="..."`. Возможна коллизия при последующем commit.
- **Фикс:** перед сохранением проверять что нет active edit, либо разрулить сценарий явно.

### 9. `commitAndCloseAllEdits` может породить 2 INSERT'а на одну NOT_IN_DB-лексему
- **Cat:** bug (B6) — **самый серьёзный по последствиям**
- **Где:** `State.kt:212-241`
- **Суть:** если у NOT_IN_DB-лексемы pending edits на обоих полях — emits `UpdateLexemeTranslation(lexemeId=null) + UpdateLexemeDefinition(lexemeId=null)`. Handler делает 2 atomic INSERT'а → в БД две лексемы вместо одной.
- **Фикс:** для NOT_IN_DB с двумя pending edits — единый atomic Effect (например, `InsertLexemeWithBoth`), либо последовательность с awaited id.

### 10. `EditableText.kt` — целиком dead
- **Cat:** architecture + yagni (A5 + Y1 + Y19)
- **Где:** `widget/EditableText.kt` (весь файл)
- **Суть:** 0 production callers (только internal previews). Внутри — закомментированный `TextFieldDefaults.outlinedTextFieldColors(...)`.
- **Фикс:** удалить файл.

### 11. `PrimaryEditableWidget` — dead
- **Cat:** architecture + yagni (A7 + Y2)
- **Где:** `modules/core/ui/.../PrimaryEditableWidget.kt` (весь файл)
- **Суть:** 0 production callers. Держит `onFocusLost = null` default только ради себя.
- **Фикс:** удалить файл; после этого `LexemeEditableText.onFocusLost` можно сделать non-nullable.

### 12. `LogTags.kt` dead + дубль `"###WORDCARD###"`
- **Cat:** yagni (Y3 + Y15)
- **Где:** `wordcard/LogTags.kt:4`; `WordCardUseCaseImpl.kt:25`
- **Суть:** `const val WORDCARD = "###WORDCARD###"` не используется; в `WordCardUseCaseImpl` свой `private const val TAG` с тем же значением.
- **Фикс:** удалить `LogTags.kt`, либо использовать его в UseCaseImpl (одна точка истины).

### 13. ~12 dead State-extensions + их тесты
- **Cat:** yagni (Y4 + Y22)
- **Где:** `State.kt`: `showLoading/hideLoading/addLexeme/setLexemeList/toggleLexemeMenu/refreshLexemeTranslation/refreshLexemeDefinition/TextValueState.setOrigin/setEdited/enableEdit/disableEdit/resetToOrigin/toValue/isChanged`
- **Суть:** 0 production callers. Тесты в `LoadingExtTest`, `LexemeExtTest` — тестируют мёртвый код.
- **Фикс:** удалить extensions + тесты.

### 14. `UiHost.showSnackbar(messageRes)` (без action) dead
- **Cat:** yagni (Y5)
- **Где:** `deps/UiHost.kt:13`; `UiHostImpl.kt:18`
- **Суть:** 0 production callers. Используется только `showSnackbarWithAction`.
- **Фикс:** удалить из interface + impl.

---

## Minor

### 15. Dead `modifier: Modifier = Modifier` в 3 виджетах
- **Cat:** architecture + yagni (A4 + Y6)
- **Где:** `LexemeMeaningField.kt:42`; `DeleteLexemeButton.kt:38`; `SubentityChip.kt:43`
- **Суть:** 0 call sites передают non-default modifier.
- **Фикс:** удалить параметр; заменить `modifier.fillMaxWidth()` → `Modifier.fillMaxWidth()`.

### 16. SnackbarLaunchEffect race
- **Cat:** bug (B8, PREDICTED)
- **Где:** `widget/SnackbarLaunchEffect.kt:14-18`
- **Суть:** ключ `LaunchedEffect(snackState.show)`. Если новый `ShowNotification("B")` приходит при `show=true` от `"A"`, effect не перезапускается → "B" не покажется.
- **Фикс:** ключ — `snackState` целиком или `snackState.title to snackState.show`. (Решается само если делать #1.)

### 17. UiEffect.ShowSnackbarWithUndo — два snackbar подряд, undo первого теряется
- **Cat:** bug (B9, PREDICTED)
- **Где:** `UiEffectHandler.kt:19-29`
- **Суть:** `SnackbarHostState.showSnackbar` использует MutatorMutex — новый snackbar отменяет старый, его coroutine возвращает `Dismissed` → undoMsg не отправится.
- **Фикс:** queue UI-эффектов, либо при cancellation отличать от dismiss.

### 18. Focus management при удалении лексемы/chip отсутствует
- **Cat:** bug (B13, PREDICTED)
- **Где:** `WordCardScreen.kt`
- **Суть:** при удалении элемента, на котором был focus, фокус "виснет"; клавиатура может остаться.
- **Фикс:** `LaunchedEffect(state.lexemeList) { focusManager.clearFocus() }` при изменении списка, либо в `onRemove`.

### 19. `UndoRemoveTranslation/Definition` не валидирует, что лексема ещё в state
- **Cat:** bug (B16, PREDICTED)
- **Где:** `WordCardReducer.kt:456-480`
- **Суть:** после `TranslationDeleted` пользователь успел удалить и саму лексему. Затем undo translation → `UpdateLexemeTranslation` для несуществующего lexemeId → ошибка.
- **Фикс:** перед эффектом проверить наличие в state; fallback на re-INSERT через `lexemeId = null`.

### 20. `Msg.NoOperation` не используется
- **Cat:** yagni (Y7)
- **Где:** `Message.kt:48`; `WordCardReducer.kt:302`; `NoOperationTest`
- **Фикс:** удалить Msg, ветку, тест.

### 21. `Msg.ExitWordEditMode` не отправляется из UI
- **Cat:** yagni (Y8)
- **Где:** `Message.kt:19`; reducer:69; `WordEditTest.kt:123`
- **Суть:** потеря фокуса вызывает `CommitWordChanges`, не `Exit`.
- **Фикс:** удалить Msg, ветку, тест.

### 22. `Msg.OpenLexemeMenu` не отправляется из UI
- **Cat:** yagni (Y9)
- **Где:** `Message.kt:27`; reducer:132; `LexemeManagementTest.kt:183`
- **Суть:** в `LexemeCard` нет menu trigger; `LexemeState.isMenuOpen` тоже dead.
- **Фикс:** удалить Msg, ветку, поле `isMenuOpen`, extension `setLexemeMenuOpen/toggleLexemeMenu`, тест.

### 23. `Msg.CancelTranslationEdit / CancelDefinitionEdit` не отправляются из UI
- **Cat:** yagni (Y10)
- **Где:** `Message.kt:34,42`; reducer:171,246; тесты в TranslationMgmtTest/DefinitionMgmtTest
- **Суть:** отмена реализована через `onFocusLost` с пустым значением → `onRemove`.
- **Фикс:** удалить оба Msg, ветки, тесты.

### 24. `Msg.LexemeCascadeRemoved` (legacy без undo) — dead
- **Cat:** yagni (Y11)
- **Где:** `Message.kt:57`; reducer:353; `LexemeManagementTest.kt:158`
- **Суть:** handler эмитит только `LexemeCascadeRemovedWithUndo`. См. также #7 — связано.
- **Фикс:** удалить Msg + ветку + тест. Решает #7 (B4) механически.

### 25. Write-only поля в domain entities
- **Cat:** yagni (Y12)
- **Где:** `Lexeme.kt:18,20` (`category`, `changeDate`); `Term.kt:15,16` (`changedDate`, `removedDate`)
- **Суть:** заполняются мэппером, нигде в модуле не читаются.
- **Фикс:** удалить поля; `category = null` константа точно лишняя.

### 26. KDoc bloat — упоминания спецификации/инвариантов/билетов
- **Cat:** yagni (Y13)
- **Где:** `State.kt:108,163,178`; `WordCardReducer.kt:8,371,514,522,534,540,691-697`; `Message.kt:60-110`; `DatasourceEffectHandler.kt:24,33,40-43,168`; `UiHostImpl.kt:26`; `WordCardScreen.kt:90`
- **Суть:** упоминания "инвариант 9", "F073", "Bug 2", "IS479", "Ветка 1a/1/2/3" — гниющий контекст в коде.
- **Фикс:** сократить до 1 фразы или удалить.

### 27. FAB `enabled = !isPendingDbOp && !isCreatingLexeme` — нечитаемое условие
- **Cat:** yagni (Y14)
- **Где:** `WordCardScreen.kt:121`
- **Суть:** двойное условие на сайте вызова не сразу читается.
- **Фикс:** computed `state.canAddLexeme: Boolean` в `WordCardState`.

---

## Nit

### 28. `getDate` public top-level + `@Composable` без необходимости
- **Cat:** architecture + yagni (A6 + Y17)
- **Где:** `WordFieldWidget.kt:98-102`
- **Фикс:** `private fun getDate(date: Date): String` без `@Composable`.

### 29. NavigateBack снимает pending до in-flight (косметика)
- **Cat:** bug (B10)
- **Где:** `WordCardReducer.kt:295-296`
- **Фикс:** не критично.

### 30. WordNotFound теряет undo-snackbar при уходе с экрана
- **Cat:** bug (B11)
- **Фикс:** не критично, экран всё равно умирает.

### 31. `CancelTranslationEdit` для NOT_IN_DB не сбрасывает `lexemeIdPendingDelete`
- **Cat:** bug (B12, PREDICTED — диалог блокирует жесты)
- **Где:** `WordCardReducer.kt:175-185,250-259`
- **Фикс:** добавить `.copy(lexemeIdPendingDelete=null)` если совпадал.

### 32. `RefreshTranslation/Definition` повторяет null overwrite (noise)
- **Cat:** bug (B14)
- **Где:** `WordCardReducer.kt:619,659`
- **Фикс:** не критично.

### 33. `UpdateInput` не guarded — invariant заметка
- **Cat:** bug (B15)
- **Суть:** UpdateInput не в guard-листе, но проверяет `isEditMode`. Безопасно.
- **Фикс:** ничего, просто отметить.

### 34. `RemoveLexeme` для NOT_IN_DB — гипотетическая гонка (PREDICTED маловероятно)
- **Cat:** bug (B17)
- **Фикс:** не критично.

### 35. TODO без owner'а
- **Cat:** yagni (Y18)
- **Где:** `State.kt:72` — `// TODO: вынести в mate` над `SnackbarState`
- **Фикс:** удалить TODO (или сделать сразу, если #1 закроет проблему).

### 36. `WordCardNavigationEffectHandler.onScreenEffect` — пустой override + комментарий-заглушка
- **Cat:** yagni (Y20)
- **Где:** `WordCardNavigationEffectHandler.kt:14-16`
- **Фикс:** убрать override если base позволяет; иначе сократить комментарий.

### 37. `ExampleUnitTest` — AS-generated шум
- **Cat:** yagni (Y21)
- **Где:** `modules/screen/wordcard/src/test/.../ExampleUnitTest.kt:11`
- **Фикс:** удалить.

---

## Группы зависимостей

- **#1 ⇄ #2 ⇄ #3 ⇄ #35** — все про legacy snackbar / hardcoded strings. Делать вместе.
- **#7 (B4) ⇄ #24 (Y11)** — `LexemeCascadeRemoved` сейчас не эмитится. Если удалить (#24) — #7 уходит механически.
- **#10 (A5/Y1) ⇄ #19 (Y19, in Y1)** — закомментированный код внутри dead-файла.
- **#11 (A7/Y2) ⇄ KDoc nit** — при удалении PrimaryEditableWidget `LexemeEditableText.onFocusLost` можно сделать non-nullable.
- **#13 (Y4) ⇄ #22 (Y9)** — `toggleLexemeMenu/setLexemeMenuOpen` относятся к dead-Msg `OpenLexemeMenu`.
