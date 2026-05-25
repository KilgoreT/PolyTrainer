# ui_layout — approved findings от инквизитора (ит.3)

## F006 [critical]

**Description:** AddLexemeWidget зафиксирован как inline-кнопка в Column (не в `Scaffold.floatingActionButton`), с придуманным обоснованием "inline-механика IS479". Это противоречит project_decision #4 из plan.context (`fab_scaffold_slot`: "AddLexeme — FAB icon-only в Scaffold.floatingActionButton"), Figma `9154:82532` (icon-only FAB) и промпту шага (артефакт = финальное состояние ПОСЛЕ реализации фичи, не текущий код).

**Verdict:** Явное нарушение project_decision из контекста + неправильная интерпретация семантики артефакта (snapshot финального состояния, не текущего кода).

# Что закрыть

1. **AddLexeme** должен быть в `Scaffold.floatingActionButton` (FAB icon-only):
   - В Карте экрана: `↘️ FAB-slot` содержит `🔄 <AddLexemeWidget>`, **не пустой** ⊘.
   - В Анализе виджета `<AddLexemeWidget>`:
     - `type:` = `M3 FloatingActionButton (Figma componentId=29907:178121, icon-only, Show Label=false)` (не `M3 Button` / `inline`).
     - `slots/content:` = только `icon` (без label).
     - `params:` без `text`/`label` — только icon, enabled, onAddLexeme.
     - `notes:` — убрать придуманный DRIFT про "не должна перекрывать контент"; оставить project_decision #4 как 🚨: "FAB размещён в Scaffold.floatingActionButton (proj. decision), Figma даёт абсолют x=340 y=720 — эквивалентно".
2. **WordCardScreen** (секция МЕНЯЕМ): убрать строку "снят слот Scaffold.floatingActionButton" — наоборот, слот **используется**. Inline-блок AddLexemeWidget из Column убирается.
3. **В Карте экрана** убрать `└─ 🔄 <AddLexemeWidget>` из Column (сразу перед FAB-slot строки) — он переезжает в FAB-slot.
4. **Spacer height=16** перед удалённым inline AddLexemeWidget — тоже убрать (был для отступа от inline-кнопки).
