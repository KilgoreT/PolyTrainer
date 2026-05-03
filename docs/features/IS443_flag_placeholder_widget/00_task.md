## Задача

В AppBar иконка словаря пустая/невидимая если для словаря не задан флаг. Нужно показывать FlagPlaceholderWidget (серый круг с первой буквой названия) вместо пустоты. Вынести виджет из modules/screen/dictionary/form/widget/ в общий UI модуль (modules/core/ui — кандидат). Использовать в AppBar (modules/widget/dictionarypicker/), в списке словарей (DictionaryListItemWidget), в форме (уже используется).

## Контекст

- Ticket: IS443
- Branch: IS443-flag-placeholder-widget
- FlagPlaceholderWidget уже существует в modules/screen/dictionary/form/widget/
- Целевые места использования:
  1. AppBar — modules/widget/dictionarypicker/
  2. Список словарей — DictionaryListItemWidget
  3. Форма словаря — уже используется (текущее расположение)
- Целевой общий модуль: modules/core/ui

_model: claude-opus-4-6-20250502_
