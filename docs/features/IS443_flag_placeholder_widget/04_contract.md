# IS443 — Contract: FlagPlaceholderWidget

## State

### Изменения State

**Изменений нет.** Задача IS443 — перенос UI-виджета `FlagPlaceholderWidget` из `modules/screen/dictionary` в `modules/core/ui` и подстановка в трёх точках отображения. Все данные, необходимые для рендеринга placeholder (title/name словаря, flagRes/numericCode), уже присутствуют в State соответствующих экранов.

Не добавляется ни одного нового поля State, не удаляется ни одного существующего. Extension-функции не затрагиваются.

### Обоснование

- `DictDropDownWidget` уже получает `currentDict?.title` — первая буква извлекается в composable
- `ItemDictMenuWidget` уже получает `title` параметром — первая буква извлекается в composable
- `DictionaryListItemWidget` уже получает `item.name` — первая буква извлекается в composable
- `DictionaryFormWidget` уже использует `FlagPlaceholderWidget` — меняется только import

### Ключевые инварианты

- Извлечение первой буквы (`title.firstOrNull()?.toString() ?: ""`) — чистая UI-логика, не бизнес-правило, вычисляется в composable
- Выбор между `ImageFlagWidget` и `FlagPlaceholderWidget` — conditional rendering в composable, не state flag

---

## UI Messages

### Изменения UI Messages

**Изменений нет.** Задача не добавляет и не удаляет пользовательских действий (Messages). Пользователь не выполняет новых действий — placeholder рендерится автоматически при отсутствии флага. Existing messages (`OnDictSelect`, `OnDictCreate`, `OnDictDelete` и т.д.) не затрагиваются.

### Обоснование

- Показ placeholder — conditional rendering в composable: `if (flagRes == 0) FlagPlaceholderWidget(...) else ImageFlagWidget(...)`
- Это не действие пользователя → не Message
- Не требует нового состояния → не требует нового Msg для его изменения

---

## Effects и Datasource Messages

### Изменения Effects

**Изменений нет.** Задача не добавляет side-эффектов. Показ placeholder — чистый UI rendering без обращения к данным, навигации или внешним сервисам.

### Изменения Datasource Messages

**Изменений нет.** Нет новых эффектов → нет новых результатов → нет новых Datasource Messages.

### Цепочки

Существующие цепочки данных не затрагиваются. Новых цепочек нет.

_model: claude-opus-4-6-20250502_
