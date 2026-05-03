# FlagPlaceholderWidget

Виджет-заглушка для словарей без назначенного флага. Отображает серый круг с первой буквой названия словаря.

## Расположение

`modules/core/ui/src/main/java/me/apomazkin/ui/FlagPlaceholderWidget.kt`

Package: `me.apomazkin.ui`

## API

```kotlin
@Composable
fun FlagPlaceholderWidget(
    letter: String,
    modifier: Modifier = Modifier,
)
```

- `letter` — строка, обычно первая буква названия словаря. Отображается uppercase.
- `modifier` — стандартный Compose modifier для управления размером и расположением.

## Визуал

- Размер по умолчанию: 24dp (совпадает с `ImageFlagWidget`). В форме словаря используется 48dp через `Modifier.size(48.dp)`.
- Фон: `grayTextColor.copy(alpha = 0.2f)`, форма — круг.
- Текст: `LexemeStyle.BodyL`, цвет `grayTextColor`.
- Буква центрирована внутри круга.

## Точки использования

### AppBar — DictDropDownWidget

Иконка текущего словаря. Если у словаря есть флаг (`flagRes != 0`) — `ImageFlagWidget`. Если нет — `FlagPlaceholderWidget` с первой буквой `title`.

### Dropdown menu — ItemDictMenuWidget

`leadingIcon` пункта выпадающего меню. Если у словаря есть флаг (`iconRes != 0`) — `ImageFlagWidget`. Если нет — `FlagPlaceholderWidget` с первой буквой `title`.

### Список словарей — DictionaryListItemWidget

Иконка элемента списка. Если у словаря есть флаг (`flagRes != null`) — `ImageFlagWidget`. Если нет — `FlagPlaceholderWidget` с первой буквой `name`.

### Форма словаря — DictionaryFormWidget

Превью выбранного флага рядом с полем названия. Если флаг не выбран — `FlagPlaceholderWidget` с первой буквой введённого названия.

## Получение первой буквы

Во всех точках: `name.firstOrNull()?.toString() ?: ""`.

Если название пустое — отображается пустой серый круг.
