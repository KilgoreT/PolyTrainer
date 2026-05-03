# IS443 — Design Tree: FlagPlaceholderWidget

## Часть 1: Граф

```yaml
- id: 0
  file: modules/core/ui/src/main/java/me/apomazkin/ui/FlagPlaceholderWidget.kt
  action: "+"
  depends: []

- id: 1
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagPlaceholderWidget.kt
  action: "-"
  depends: [0]

- id: 2
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/DictionaryFormWidget.kt
  action: "~"
  depends: [0, 1]

- id: 3
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/widget/DictionaryListItemWidget.kt
  action: "~"
  depends: [0]

- id: 4
  file: modules/widget/dictionarypicker/src/main/java/me/apomazkin/dictionarypicker/DictDropDownWidget.kt
  action: "~"
  depends: [0]

- id: 5
  file: modules/widget/dictionarypicker/src/main/java/me/apomazkin/dictionarypicker/items/ItemDictMenuWidget.kt
  action: "~"
  depends: [0]
```

Параллельность: #2, #3, #4, #5 зависят только от #0 (и #2 дополнительно от #1), поэтому после создания #0 и удаления #1 узлы #3, #4, #5 выполняются параллельно. #2 выполняется после #1.

## Часть 2: Детали изменений

### #0 modules/core/ui/.../FlagPlaceholderWidget.kt [+]

Composable-виджет placeholder для словаря без флага. Серый круг с первой буквой названия (uppercase).

> 📎 guide: docs/guides/ui-patterns.md — виджет stateless, без side-эффектов

```kotlin
package me.apomazkin.ui

@Composable
fun FlagPlaceholderWidget(
    letter: String,
    modifier: Modifier = Modifier.size(24.dp),
)
```

Размер через modifier default — `Modifier.size(24.dp)`. Внутри composable НЕ хардкодить `.size()`. Вызывающий код перебивает через `modifier = Modifier.size(48.dp)`.

Отличия от текущей версии в `screen/dictionary`:
- Package: `me.apomazkin.ui` (вместо `me.apomazkin.dictionary.form.widget`)
- Дефолтный размер: `24.dp` через modifier (вместо хардкода `48.dp` внутри Box)
- Остальное без изменений: `CircleShape`, `grayTextColor.copy(alpha = 0.2f)`, `LexemeStyle.BodyL`

---

### #1 modules/screen/dictionary/.../form/widget/FlagPlaceholderWidget.kt [-]

Удаляется целиком. Функциональность перенесена в #0 (`core/ui`). Единственный потребитель — `DictionaryFormWidget` (#2) — переходит на импорт из `me.apomazkin.ui`.

---

### #2 modules/screen/dictionary/.../form/widget/DictionaryFormWidget.kt [~]

**Было:**
```kotlin
import me.apomazkin.dictionary.form.widget.FlagPlaceholderWidget
// ...
FlagPlaceholderWidget(
    letter = formState.name.firstOrNull()?.toString() ?: "",
)
```

**Стало:**
```kotlin
import me.apomazkin.ui.FlagPlaceholderWidget
// ...
FlagPlaceholderWidget(
    letter = formState.name.firstOrNull()?.toString() ?: "",
    modifier = Modifier.size(48.dp),
)
```

Изменения:
- Import меняется на `me.apomazkin.ui.FlagPlaceholderWidget`
- Добавляется `modifier = Modifier.size(48.dp)` — компенсация смены дефолтного размера с 48dp на 24dp

---

### #3 modules/screen/dictionary/.../list/widget/DictionaryListItemWidget.kt [~]

**Было:**
```kotlin
if (item.flagRes != null) {
    ImageFlagWidget(flagRes = item.flagRes)
} else {
    Icon(
        painter = painterResource(id = R.drawable.ic_tab_vocabulary),
        contentDescription = null,
    )
}
```

**Стало:**
```kotlin
import me.apomazkin.ui.FlagPlaceholderWidget
// ...
if (item.flagRes != null) {
    ImageFlagWidget(flagRes = item.flagRes)
} else {
    FlagPlaceholderWidget(
        letter = item.name.firstOrNull()?.toString() ?: "",
    )
}
```

Изменения:
- Добавляется import `me.apomazkin.ui.FlagPlaceholderWidget`
- Fallback `Icon(ic_tab_vocabulary)` заменяется на `FlagPlaceholderWidget` с первой буквой `item.name`
- Дефолтный размер 24dp совпадает с `ImageFlagWidget` — modifier не нужен

---

### #4 modules/widget/dictionarypicker/.../DictDropDownWidget.kt [~]

**Было:**
```kotlin
icon = {
    val flagRes = currentDict?.flagRes ?: 0
    if (flagRes != 0) {
        ImageFlagWidget(flagRes = flagRes)
    }
},
```

**Стало:**
```kotlin
import me.apomazkin.ui.FlagPlaceholderWidget
// ...
icon = {
    val flagRes = currentDict?.flagRes
    if (flagRes != null && flagRes != 0) {
        ImageFlagWidget(flagRes = flagRes) // smart cast: Int
    } else {
        FlagPlaceholderWidget(
            letter = currentDict?.title?.firstOrNull()?.toString() ?: "",
        )
    }
},
```

Изменения:
- Добавляется import `me.apomazkin.ui.FlagPlaceholderWidget`
- `?: 0` убран, проверка через smart cast `flagRes != null && flagRes != 0`
- Добавляется `else` ветка — показывает placeholder с первой буквой `currentDict?.title`
- Дефолтный размер 24dp — modifier не нужен

---

### #5 modules/widget/dictionarypicker/.../items/ItemDictMenuWidget.kt [~]

**Было:**
```kotlin
leadingIcon = {
    if (iconRes != 0) {
        ImageFlagWidget(
            flagRes = iconRes,
            contentDescription = title
        )
    }
},
```

**Стало:**
```kotlin
import me.apomazkin.ui.FlagPlaceholderWidget
// ...
leadingIcon = {
    if (iconRes != 0) {
        ImageFlagWidget(
            flagRes = iconRes,
            contentDescription = title
        )
    } else {
        FlagPlaceholderWidget(
            letter = title.firstOrNull()?.toString() ?: "",
        )
    }
},
```

Изменения:
- Добавляется import `me.apomazkin.ui.FlagPlaceholderWidget`
- Добавляется `else` ветка — при `iconRes == 0` показывает placeholder с первой буквой `title`
- Дефолтный размер 24dp — modifier не нужен

## checklist_items
- root: "Пользователь открывает AppBar dropdown для словаря без флага"
  items:
    - DictDropDownWidget: else-ветка с FlagPlaceholderWidget, letter = currentDict?.title?.firstOrNull() [design_tree]
- root: "Пользователь видит пункт dropdown-меню для словаря без флага"
  items:
    - ItemDictMenuWidget: else-ветка с FlagPlaceholderWidget, letter = title.firstOrNull() [design_tree]
- root: "Пользователь видит список словарей для словаря без флага"
  items:
    - DictionaryListItemWidget: замена Icon(ic_tab_vocabulary) на FlagPlaceholderWidget, letter = item.name.firstOrNull() [design_tree]
- root: "Пользователь открывает форму словаря без флага"
  items:
    - DictionaryFormWidget: import → me.apomazkin.ui, добавлен modifier = Modifier.size(48.dp) [design_tree]

## log_messages
- Граф из 6 узлов: 1 создание (core/ui), 1 удаление (screen/dictionary), 4 изменения (form, list, appbar dropdown, menu item)
- Все пути файлов верифицированы чтением реального кода, зависимости от core:ui уже есть во всех модулях

_model: claude-opus-4-6-20250502_
