## Задача

Изменить UI добавления лексемы в карточке слова (`modules/screen/wordcard`). Сейчас добавление происходит через bottom sheet (`AddLexemeBottomState`). Нужно заменить на inline-механизм — встроенный в основной layout карточки, без bottom sheet.

Что меняется:
1. Кнопка добавления лексемы заменяется на новую (Figma frame `9154-82532`).
2. Механизм добавления — больше не bottom sheet, а inline / встроенный UI (frame `9154-82519`).
3. Кнопка «Перевод» должна выглядеть как chip — образец стиля frame `9154-82521`.
4. Остальные кнопки на frame `9154-82625` — chip'ы того же стиля.

Что НЕ делаем:
- Пример (Example) — исключить полностью, его не должно быть в новом UI.

## Контекст

**GitHub issue:** https://github.com/KilgoreT/PolyTrainer/issues/479

**Figma файл:** `w8GmGCdOZJUi99Cuv4q4W9` (Lexeme — 21.07.2025)
URL: https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-

**Основные frames с новым UI:**
- [9154-82509](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-82509)
- [9154-86012](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-86012)
- [9154-86182](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-86182)
- [9154-86353](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-86353)
- [9154-86499](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-86499)

**Конкретные элементы:**
- Заменяемая кнопка добавления: [9154-82532](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-82532)
- Механизм добавления (inline): [9154-82519](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-82519)
- «Перевод» chip (образец стиля): [9154-82521](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-82521)
- Кнопки под chip-стиль: [9154-82625](https://www.figma.com/design/w8GmGCdOZJUi99Cuv4q4W9/Lexeme--21.07.2025-?node-id=9154-82625)

**MCP для Figma:** `mcp__figma__get_figma_data` доступен — последующие шаги могут подтянуть содержимое frames.

**Модули проекта (заведомо затронутые):**
- `modules/screen/wordcard` — основной модуль карточки слова. State / Reducer / Effect / Composable. Текущая реализация добавления лексемы — через `AddLexemeBottomState` (bottom sheet), её предстоит заменить inline-механизмом.

_model: Opus 4.7 (1M context)_
