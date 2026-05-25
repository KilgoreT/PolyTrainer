# Approved findings — contract_ui_msg v3, итерация 4

10 minor approved (без critical). Streak only-minor: 1.

## Точечные правки (10)

### F033 — `EnterWordEditMode` self-contradiction с F029

Pseudocode пишет `w.copy(...)` где `w = state.wordState as Loaded`, не `closed.wordState`. Это противоречит правилу F029 «обязаны писать `closed.wordState`».

**Что сделать:** Переписать pseudocode на:
```kotlin
when (val w = closed.wordState) {
    is WordState.Loaded -> closed.copy(wordState = w.copy(isEditMode = true, edited = w.value))
    NotLoaded -> state to emptySet()  // невозможно после guard
}
```

Или ослабить F029 в разделе хелпера: «для `EnterWordEditMode` допустимо `w.copy(...)` исходного `w`, потому что мутируемые поля `isEditMode/edited` явно перезаписываются, а остальные поля (`id/added/value/showWarningDialog`) хелпер не трогает».

Рекомендую **ослабить F029** — текущее решение работает, переписывание через двойной `when` усложнит pseudocode.

### F034 — CommitTranslationEdit empty-origin scenario

Если `Create*Edit` → `Commit*Edit` без ввода (origin="", edited=""): ветвь 1 шлёт `RemoveTranslation` effect на несуществующую в БД запись.

**Что сделать:** Добавь sub-ветвь в ветке 1:
```
if (edited.isEmpty() && origin.isEmpty()):
    // свежесозданный без ввода — локальный nullify без effect
    translation = null
    effect = emptySet()
else if (edited.isEmpty()):
    // обычная ветвь Remove
    translation.copy(isEdit=false, edited="")
    effect = RemoveTranslation(lexemeId)
```

Симметрично с `Cancel*Edit` для свежесозданного.

### F035 — Whitespace asymmetry

`CommitWordChanges` использует `edited.isBlank()` (отсекает whitespace), `CommitTranslationEdit` ветвь 1 — `edited.isEmpty()` (пропускает whitespace).

**Что сделать:** Привести `CommitTranslationEdit` ветвь 1 к `edited.isBlank()` (whitespace → ветвь Remove). Симметрично для definition. Зафиксируй: «whitespace-only считается пустым во всех Commit веток».

### F036 — Open*/Close* shortcut асимметрия

`OpenTopBarMenu`, `OpenDeleteWordDialog`, `OpenLexemeMenu(isShow=true)` — без shortcut-guard на «уже открыто».

**Что сделать:** Либо явно зафиксируй принцип «shortcut-ignore только для Close*/Dismiss*, потому что иконка Open недоступна когда уже открыто (UX-инвариант)», либо добавь симметричные guards. Рекомендую **первое** (UX-обоснование) — добавь однострочную ремарку в раздел про guards.

### F037 — Неявный cross-contract invariant

Зафиксируй в разделе «Открытые вопросы» / в Reducer-логике `Cancel*Edit`:
> **Cross-contract предположение:** `contract_io` гарантирует что `RefreshTranslation/RefreshDefinition` никогда не возвращает `TextValueState(origin="")`. По pessimistic-политике `Remove*` (F002) БД не хранит пустых строк. Это предположение делает корректным nullify-ветку `Cancel*Edit` при `origin.isEmpty()` — комбинация `(non-null, isEdit=false, origin="")` возникает только из свежего `Create*Edit` без ввода.

### F038 — F017 conditional-copy для target в OpenLexemeMenu

В `OpenLexemeMenu(isShow=true)` для целевой лексемы — conditional:
```kotlin
if (l.id == lexemeId) (if (l.isMenuOpen) l else l.copy(isMenuOpen = true))
else if (l.isMenuOpen) l.copy(isMenuOpen = false)
else l
```

Аналогично для `isShow=false`.

### F039 — Refresh* signatures mismatch

Раздел «Переносятся в `contract_io`» использует старые сигнатуры `RefreshTranslation(lexeme)`. Forward-ref таблица — новые `RefreshTranslation(lexemeId, translation?)`.

**Что сделать:** Обнови раздел «Переносятся» — пометь «переносится с переработкой сигнатуры на `(lexemeId, translation: String?)` — финал в contract_io».

### F040 — CommitTranslationEdit единый when

Сейчас guard + ветвь 3 — два независимых `when`. Перепиши через один внешний `when`:
```kotlin
when (val w = state.wordState) {
    is WordState.Loaded -> {
        val lexeme = state.lexemeList.find { it.id == lexemeId } ?: return state to emptySet()
        if (lexeme.translation == null || !lexeme.translation.isEdit) state to emptySet()
        else when {
            lexeme.translation.edited.isBlank() && lexeme.translation.origin.isEmpty() -> ...  // F034
            lexeme.translation.edited.isBlank() -> ...  // ветвь 1 Remove
            lexeme.translation.edited == lexeme.translation.origin -> ...  // ветвь 2 no-op
            else -> ...  // ветвь 3 Update
        }
    }
    NotLoaded -> state to emptySet()
}
```

### F041 — F022 dangling reference

В чек-листе инв.4 убери F-тег F022 или переоформи как «(ремарка из ит.2)».

### F042 — RemoveLexeme conditional copy

Симметрично F017/F038 — `if (l.id == lexemeId) (if (l.translation != null || l.definition != null || l.isMenuOpen) l.copy(...) else l) else l`.

---

## Задача итерации 5

Перепиши `contract_ui_msg.md` закрыв все 10 minor. Если на ит.5 ревью даст снова только minor — streak=2 → выход по правилу модуля review.
