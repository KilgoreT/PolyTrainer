# IS443 — FlagPlaceholderWidget: вынос в core/ui и применение

## Проблема

Когда у словаря не задан флаг (numericCode = null), в трёх местах UI показывается пустота:
1. **AppBar dropdown** (`DictDropDownWidget`) — иконка текущего словаря не рендерится вообще (`if flagRes != 0` → ничего)
2. **Пункт dropdown** (`ItemDictMenuWidget`) — аналогично, `if iconRes != 0` → пусто
3. **Список словарей** (`DictionaryListItemWidget`) — fallback на generic иконку `ic_tab_vocabulary` вместо осмысленного placeholder

При этом `FlagPlaceholderWidget` (серый круг с первой буквой названия) уже существует и используется в форме словаря. Но он лежит в `modules/screen/dictionary/form/widget/` — приватный для модуля screen/dictionary.

## Что удаляется

- Файл `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagPlaceholderWidget.kt` — удаляется из текущего расположения.

## Что остаётся без изменений

- API `FlagPlaceholderWidget(letter, modifier)` — параметры не меняются
- Визуальный вид: серый круг 48dp, буква uppercase, цвет `grayTextColor`, alpha 0.2f фона
- `ImageFlagWidget` — продолжает использоваться для словарей с флагом
- Логика выбора флага в форме, фильтрация, бизнес-логика словарей
- `DictUiEntity`, `DictionaryListItem` — data-классы не меняются

## Что добавляется / меняется

### 1. Перенос виджета в core/ui

`FlagPlaceholderWidget` переносится в `modules/core/ui/src/main/java/me/apomazkin/ui/FlagPlaceholderWidget.kt`. Package: `me.apomazkin.ui`.

Содержимое близко к текущему, но:
- Package меняется на `me.apomazkin.ui`
- Дефолтный размер меняется с 48dp на 24dp (совпадение с `ImageFlagWidget` default). В форме, где нужен 48dp, размер передаётся через `modifier = Modifier.size(48.dp)`

### 2. DictDropDownWidget — иконка текущего словаря в AppBar

**Было:** если `flagRes == 0` — ничего не рендерится (пустой `icon` slot).

**Стало:** если `flagRes == 0` — показываем `FlagPlaceholderWidget` с первой буквой `title` текущего словаря.

Для этого `DictDropDownWidget` получает доступ к `title` текущего словаря (уже есть через `currentDict?.title`). Нужно извлечь первую букву: `currentDict?.title?.firstOrNull()?.toString() ?: ""`.

### 3. ItemDictMenuWidget — пункт выпадающего меню

**Было:** если `iconRes == 0` — `leadingIcon` slot пустой.

**Стало:** если `iconRes == 0` — показываем `FlagPlaceholderWidget` с первой буквой `title`.

Параметр `title` уже передаётся. Первая буква: `title.firstOrNull()?.toString() ?: ""`.

### 4. DictionaryListItemWidget — элемент списка словарей

**Было:** если `flagRes == null` — показывается `Icon(ic_tab_vocabulary)`.

**Стало:** если `flagRes == null` — показываем `FlagPlaceholderWidget` с первой буквой `name`.

Параметр `name` доступен через `item.name`. Первая буква: `item.name.firstOrNull()?.toString() ?: ""`.

### 5. Форма словаря (DictionaryFormWidget)

Import меняется с `me.apomazkin.dictionary.form.widget.FlagPlaceholderWidget` на `me.apomazkin.ui.FlagPlaceholderWidget`. Логика использования не меняется.

## Обоснование

- Виджет generic (не привязан к конкретному экрану), поэтому core/ui — правильный уровень
- core/ui уже содержит `ImageFlagWidget`, `IconBoxed` и подобные переиспользуемые компоненты
- Все три целевых места уже зависят от core/ui — новых зависимостей не добавляется
- `ImageFlagWidget` использует 24dp по умолчанию (dropdown, список), в форме — 48dp через modifier. FlagPlaceholderWidget должен работать аналогично: дефолтный размер снижается до 24dp, в форме передаётся через modifier `.size(48.dp)`

## Затронутые модули

| Модуль | Изменение |
|--------|-----------|
| `modules/core/ui` | Новый файл FlagPlaceholderWidget.kt |
| `modules/screen/dictionary` | Удаление старого файла, обновление import в форме, замена fallback в списке |
| `modules/widget/dictionarypicker` | Замена пустоты на placeholder в DictDropDownWidget и ItemDictMenuWidget |

## log_messages
- Создана фичовая спека: перенос FlagPlaceholderWidget в core/ui, применение в трёх точках (AppBar, dropdown item, список)
- Три проблемных места идентифицированы: AppBar показывает пустоту, dropdown item пустой, список показывает generic иконку

_model: claude-opus-4-6-20250502_
