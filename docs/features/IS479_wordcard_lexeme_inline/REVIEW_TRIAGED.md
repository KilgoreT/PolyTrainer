# IS479 Review — Triaged

Критическая фильтрация 37 findings из `REVIEW.md`. Отвергнуто 11 пунктов (см. секцию ниже с reason'ами).

**Apprоved: 22 пункта**, разбито по приоритету.

---

## P0 — Архитектура: миграция legacy snackbar (атомарный блок)

Все три пункта — одна задача, делать вместе.

### 1. Перевести error-пути на UiHost  **[TODO — вариант A: новый Msg.ShowError → UiEffect.ShowErrorSnackbar]**
- **Cat:** architecture (A1)
- **Где:** `WordCardReducer.kt:368,296`; `State.kt:73-77,313-327`; `Message.kt:47,58`; `WordCardScreen.kt:90`; `SnackbarLaunchEffect.kt`
- **Суть:** error-пути из `DatasourceEffectHandler` идут через `Msg.ShowNotification` → `state.snackbarState` → `SnackbarLaunchEffect`. Гайд `ui-patterns.md` прямо запрещает добавлять legacy state-based snackbar при наличии UiHost-канона. Сейчас фича смешала два канала в одном экране.
- **Фикс:** новый `UiEffect.ShowErrorSnackbar(@StringRes Int)` для ошибок; удалить `SnackbarState`, `Msg.ShowNotification`, `Msg.DismissNotification`, extensions `showSnackbar/hideSnackbar`, `SnackbarLaunchEffect`.

### 2. Захардкоженные русские строки ошибок  **[TODO — заодно с #1]**
- **Cat:** architecture + yagni (A2 + Y16 + B7)
- **Где:** `DatasourceEffectHandler.kt:70,76,80,98,111,127,140,156,165,176-183`
- **Суть:** все error-сообщения — `String` хардкод на русском. Каждое сообщение дублируется ≥2 раза (в основной when + в catch). Проект интернационализуется (есть `values-ru-rRU/`) — это нарушение базы.
- **Фикс:** завести `R.string.word_card_error_*` (ru+en), заменить `Msg.ShowNotification(String)` → `UiEffect.ShowErrorSnackbar(@StringRes Int)`.

### 3. FQN `me.apomazkin.core_resources.R.string.*` в reducer  **[TODO — заодно с #1]**
- **Cat:** architecture (A3) — **косметика**
- **Где:** `WordCardReducer.kt:378-379,392-393,415-417,423-425,444-445`
- **Фикс:** добавить `import ...R` или вынести в константы. Тривиально, делается заодно с #1/#2.

---

## P1 — Реальные баги

### 4. RestoreLexeme не атомарен  **[TODO — rollback в catch второго шага]**
- **Cat:** bug (B1)
- **Где:** `WordCardUseCaseImpl.kt:136-141`
- **Суть:** для лексемы с translation+definition — `insertLexemeWithTranslation` затем отдельный `updateLexemeDefinition`. Падение второго оставит в БД полу-лексему, в UI — ошибку. UX-инвариант undo: либо полное восстановление, либо явный fail без побочных эффектов.
- **Фикс:** в catch второго шага — `lexemeApi.deleteLexeme(id)` (rollback). 3 строки.

### 5. UiEffect.ShowSnackbarWithUndo — потеря undo первого при втором snackbar  **[BACKLOG]**
- **Cat:** bug (B9)
- **Где:** `UiEffectHandler.kt:19-29`
- **Суть:** `SnackbarHostState.showSnackbar` использует MutatorMutex — новый snackbar отменяет старый, его coroutine возвращает `Dismissed` → undoMsg не отправится. Пользователь быстро удалил два translation — undo первого потерян.
- **Фикс:** queue UI-эффектов, либо при cancellation отличать от dismiss. **Не тривиально**, возможно в backlog.

---

## P1 — Dead code (целые файлы и сущности)

### 6. `EditableText.kt` — целиком dead  **[DONE — файл удалён]**
- **Cat:** architecture + yagni (A5 + Y1 + Y19)
- **Где:** `widget/EditableText.kt` (весь файл)
- **Верифицировано:** 0 production callers (только internal previews). Внутри — закомментированный `TextFieldDefaults.outlinedTextFieldColors`.
- **Фикс:** удалить файл.

### 7. `PrimaryEditableWidget` — dead  **[TODO — удалить файл + LexemeEditableText.onFocusLost сделать non-nullable]**
- **Cat:** architecture + yagni (A7 + Y2)
- **Где:** `modules/core/ui/.../PrimaryEditableWidget.kt` (весь файл)
- **Верифицировано:** 0 production callers; `LexemeEditableText.onFocusLost = null` default держится только ради этого виджета.
- **Фикс:** удалить файл; после — `LexemeEditableText.onFocusLost: (String) -> Unit` без default.

### 8. `LogTags.kt` dead + дубль `"###WORDCARD###"`  **[TODO — вариант B: в WordCardUseCaseImpl использовать LogTags.WORDCARD, локальный TAG удалить]**
- **Cat:** yagni (Y3 + Y15)
- **Где:** `wordcard/LogTags.kt:4`; `WordCardUseCaseImpl.kt:25`
- **Верифицировано:** `LogTags.WORDCARD` — 0 grep-совпадений. `TAG = "###WORDCARD###"` в UseCaseImpl с тем же значением.
- **Фикс:** удалить `LogTags.kt`; либо использовать его (одна точка истины).

### 9. 12 dead State-extensions + их тесты  **[TODO — удалить все extensions + LoadingExtTest, часть LexemeExtTest, часть SpecializedLexemeExtTest]**
- **Cat:** yagni (Y4 + Y22)
- **Где:** `State.kt` — `addLexeme/setLexemeList/toggleLexemeMenu/showLoading/hideLoading/setOrigin/setEdited/enableEdit/disableEdit/resetToOrigin/toValue/isChanged/refreshLexemeTranslation/refreshLexemeDefinition`
- **Верифицировано:** grep по `state.<name>` / `<receiver>.<name>` вне самой `State.kt` и тестов → 0 production callers.
- **Фикс:** удалить функции + соответствующие тесты (`LoadingExtTest`, часть `LexemeExtTest`, часть `SpecializedLexemeExtTest`).

### 10. `UiHost.showSnackbar(messageRes)` без action dead  **[KEEP — после #1 будет использоваться для error-snackbar]**
- **Cat:** yagni (Y5)
- **Где:** `deps/UiHost.kt:13`; `UiHostImpl.kt:18`
- **Верифицировано:** 0 callers, используется только `showSnackbarWithAction`.
- **Фикс:** удалить из interface + impl.

---

## P2 — Dead Msg'и (тоже мёртвый код, но риск сложнее)

Каждый Msg тянет: variant в sealed + ветка в reducer + (часто) entry в `isGuardedByPending` + тест(ы).

### 11. `Msg.NoOperation` dead  **[KEEP — может пригодится как fallback]**
- **Cat:** yagni (Y7)
- **Верифицировано:** grep `Msg.NoOperation` вне Message.kt+reducer+тест → 0. Никто не consumer'ит, никто не sendMessage.
- **Фикс:** удалить variant + ветку reducer:302 + `NoOperationTest`.

### 12. `Msg.ExitWordEditMode` dead  **[TODO — удалить variant + ветку reducer:69 + тест в WordEditTest]**
- **Cat:** yagni (Y8)
- **Верифицировано:** UI использует `onFocusLost → CommitWordChanges`. `Exit` нигде не отправляется.
- **Фикс:** удалить variant + ветку reducer:69 + тест в WordEditTest.

### 13. `Msg.OpenLexemeMenu` + связанная инфраструктура dead  **[TODO — удалить Msg + ветку reducer:132 + entry в isGuardedByPending + LexemeState.isMenuOpen + setLexemeMenuOpen + toggleLexemeMenu + тесты]**
- **Cat:** yagni (Y9)
- **Суть:** в IS479 меню лексемы убрано (inline-edit). За собой тянет: `LexemeState.isMenuOpen`, `setLexemeMenuOpen`, `toggleLexemeMenu`.
- **Фикс:** удалить Msg + ветку reducer:132 + entry в `isGuardedByPending` + поле + extensions + тест.

### 14. `Msg.CancelTranslationEdit / CancelDefinitionEdit` dead  **[TODO — удалить оба variant + ветки reducer:171,246 + тесты TranslationManagementTest:368,387 + DefinitionManagementTest:284. Remove*Translation/*Definition остаются — они для удаления чипа.]**
- **Cat:** yagni (Y10)
- **Суть:** отмена реализована через `onFocusLost` с пустой строкой → `CommitTranslationEdit` повторяет логику cascade-delete (ветка 1a). Cancel-Msg избыточен.
- **Фикс:** удалить оба variant'а + ветки reducer:171,246 + тесты.

### 15. `Msg.LexemeCascadeRemoved` (legacy без undo) dead  **[TODO — удалить variant + ветку reducer:353 + тест LexemeManagementTest:158]**
- **Cat:** yagni (Y11) + ликвидирует bug B4
- **Суть:** handler эмитит только `LexemeCascadeRemovedWithUndo`. Legacy ветка в reducer мёртвая.
- **Фикс:** удалить variant + ветку reducer:353.

---

## P2 — Гигиена кода

### 16. Dead `modifier: Modifier = Modifier` defaults  **[KEEP — Compose-конвенция]**
- **Cat:** architecture + yagni (A4 + Y6)
- **Где:** `LexemeMeaningField.kt:42`; `DeleteLexemeButton.kt:38`; `SubentityChip.kt:43`
- **Верифицировано:** 0 call sites передают non-default.
- **Consistent:** мы уже применили это правило к `AddLexemeWidget` в этой же сессии.
- **Фикс:** удалить параметр; `modifier.foo()` → `Modifier.foo()`.

### 17. Write-only поля в domain entities  **[KEEP — данные из БД, могут понадобиться]**
- **Cat:** yagni (Y12)
- **Где:** `Lexeme.kt:18,20` (`category`, `changeDate`); `Term.kt:15,16` (`changedDate`, `removedDate`)
- **Верифицировано:** заполняются мэппером (`category = null` константой!), нигде в модуле не читаются. Entities wordcard-specific, не видны другим модулям.
- **Фикс:** удалить поля.

### 18. KDoc bloat — упоминания спецификации/инвариантов/билетов  **[TODO — пройтись по всем перечисленным местам, сократить до одной фразы + @param. Удалить упоминания "IS479", "F073", "Bug 2", "инвариант 9/10", "Ветка 1a/1/2/3"]**
- **Cat:** yagni (Y13)
- **Где:** `State.kt:108,163,178`; `WordCardReducer.kt:8,371,514,522,534,540,691-697`; `Message.kt:60-110`; `DatasourceEffectHandler.kt:24,33,40-43,168`; `UiHostImpl.kt:26`; `WordCardScreen.kt:90`
- **Суть:** упоминания "инвариант 9", "F073", "Bug 2", "IS479", "Ветка 1a/1/2/3" — гниющий контекст, нарушает только что внесённое правило code-style.md §"Минимализм API и комментарии (YAGNI)".
- **Фикс:** сократить до 1 фразы или удалить.

### 19. FAB `enabled = !isPendingDbOp && !isCreatingLexeme` → computed  **[TODO — добавить val canAddLexeme: Boolean в WordCardState; на сайте вызова enabled = state.canAddLexeme]**
- **Cat:** yagni (Y14)
- **Где:** `WordCardScreen.kt:121`
- **Суть:** двойное условие на сайте вызова. Соответствует правилу state-and-extensions.md "computed для derived state".
- **Фикс:** computed `state.canAddLexeme: Boolean` в `WordCardState`.

---

## P3 — Мелочи

### 20. `getDate` public top-level + `@Composable`  **[TODO — private fun без @Composable]**
- **Cat:** architecture + yagni (A6 + Y17)
- **Где:** `WordFieldWidget.kt:98-102`
- **Суть:** функция использует только `SimpleDateFormat`. `@Composable` лишнее, public top-level — без нужды.
- **Фикс:** `private fun getDate(date: Date): String` без `@Composable`.

### 21. `ExampleUnitTest` — AS-generated шум  **[TODO — удалить файл]**
- **Cat:** yagni (Y21)
- **Где:** `modules/screen/wordcard/src/test/.../ExampleUnitTest.kt`
- **Фикс:** удалить файл.

### 22. TODO без owner'а в SnackbarState  **[N/A — закрывается через #1, SnackbarState удалится целиком]**
- **Cat:** yagni (Y18)
- **Где:** `State.kt:72` — `// TODO: вынести в mate` над `SnackbarState`
- **Решается автоматически после #1** (SnackbarState удаляется).

---

## Отвергнутые (с обоснованием) — для прозрачности

**B2 (commitAndCloseAllEdits под NotLoaded мутирует lexemeList)** — defensive paranoia. Под `NotLoaded` `lexemeList = listOf()` всегда (нет пути его наполнить). Все Msg, дёргающие commitAndCloseAllEdits, в reducer **уже** проверяют `wordState as? Loaded` перед вызовом. Инвариант держится выше. REJECT.

**B3 (RefreshTranslation/Definition слепо переименует NOT_IN_DB)** — агент не понял intent. Ветка `notInDbExists` срабатывает только при `realExists = false`. Сценарий "несколько лексем одновременно" не реализуем: инвариант `isCreatingLexeme` гарантирует не более одной NOT_IN_DB в state. RefreshTranslation для real-id, когда real в state, → ветка `realExists`. Переименование NOT_IN_DB происходит только в правильном сценарии "first-commit translation для нового черновика". REJECT.

**B4 (Cascade-without-undo NOT_IN_DB-черновик блокирует FAB)** — Msg.LexemeCascadeRemoved (legacy) сейчас не эмитится handler'ом (только `WithUndo`-вариант). Решается удалением legacy Msg (см. #15). REJECT (отдельный finding не нужен — покрывается #15).

**B5 (RefreshLexemeList сохраняет NOT_IN_DB-черновик с активным edit)** — это **правильное поведение**. `RefreshLexemeList` ≠ commit. Пользователь редактирует NOT_IN_DB-черновик параллельно с restore другой лексемы — pending edit должен сохраниться, не потеряться. UX-инвариант inline-edit. REJECT.

**B6 (commitAndCloseAllEdits 2 INSERT'а на одну NOT_IN_DB)** — single edit инвариант гарантирует невозможность. Перед открытием любого edit вызывается `commitAndCloseAllEdits`, закрывающий все остальные. У NOT_IN_DB одновременно может быть только один pending edit. Защищаться от нарушения этого инварианта в commitAndCloseAllEdits — прятать большую проблему за safety net. REJECT.

**B8 (SnackbarLaunchEffect race на повторный ShowNotification)** — покрывается #1 (миграция на UiHost). После #1 этого кода нет. REJECT как самостоятельный finding.

**B10 (NavigateBack снимает pending до in-flight)** — агент сам пишет "не критично, ViewModel умрёт". REJECT.

**B11 (WordNotFound теряет undo-snackbar)** — экран всё равно уходит, snackbar и не должен пережить. REJECT.

**B12 (CancelTranslationEdit не сбрасывает lexemeIdPendingDelete)** — Cancel*Msg удаляется в #14, finding автоматически N/A. REJECT.

**B13 (focus management при удалении)** — PREDICTED, не верифицировано на реальном UI. Если пользователь не отметил проблему при тестировании — не делать. REJECT (можно завести при появлении репорта).

**B14 (refreshTranslation noise — повторный null overwrite)** — агент сам пишет "не критично". REJECT.

**B15 (UpdateInput не guarded)** — агент сам пишет "ничего, просто отметить инвариант". REJECT.

**B16 (UndoRemoveTranslation/Definition не валидирует существование)** — сценарий требует за 4 секунды snackbar успеть удалить translation + удалить лексему + нажать undo. Маловероятно. Защита defensive против гипотетического. REJECT (можно в backlog как nit).

**B17 (RemoveLexeme race для NOT_IN_DB)** — PREDICTED маловероятно, агент не может воспроизвести. REJECT.

**Y20 (WordCardNavigationEffectHandler.onScreenEffect пустой override)** — override обязателен от base. Комментарий объясняет почему пустой. Нормальный stub. REJECT.

---

## Группы зависимостей (порядок выполнения)

1. **Сначала** P0 (миграция snackbar) — закрывает #1+#2+#3, попутно #22.
2. **Параллельно** dead-files и dead-Msg (#6–#15) — простые, независимые удаления, тесты обновляются по списку.
3. **Затем** гигиена (#16–#19, #20–#21) — мелкие правки, проверка тестов.
4. **Backlog** (не для IS479):
   - #4 (atomic restore) — улучшение, не блокер.
   - #5 (snackbar queue) — UX-улучшение, не тривиальное.
