# Design Tree: IS453 — WordCard input UX

## Graph

```yaml
- id: 0
  file: core/core-resources/src/main/res/drawable/ic_confirm.xml
  action: "+"
  depends: []

- id: 1
  file: modules/core/ui/src/main/java/me/apomazkin/ui/text/base/LexemeEditableText.kt
  action: "~"
  depends: [0]
```

## Details

### #0 ic_confirm.xml [+]

Новая иконка: монохромная — белая галочка в круге, 24dp viewport. Цвет задаётся через `colorEnabled` в `IconBoxed`, не в drawable.

### #1 LexemeEditableText.kt [~]

> 📎 guide: docs/guides/ui-patterns.md — "Конвенции по лейауту: размер иконок 24dp стандарт"

Три изменения в edit mode (строки 73-98):

**a) Ширина поля ввода:**
```kotlin
// Было:
.weight(1f, fill = false)
.width(IntrinsicSize.Min)

// Стало:
.weight(1f)
```
Убрать `fill = false` и `IntrinsicSize.Min`. Поле займёт всё пространство.

**b) Иконка:**
```kotlin
// Было:
iconRes = iconConfirm,  // ic_close (крестик)
size = iconSize,      // 12dp
colorEnabled = blackColor,

// Стало:
iconRes = R.drawable.ic_confirm,  // монохромная галочка в круге
size = 24,
colorEnabled = confirmColor,      // зелёный цвет через параметр
```

**c) Дефолты параметров:**
```kotlin
// Было:
@DrawableRes iconConfirm: Int = R.drawable.ic_close,
iconSize: Int = DEFAULT_ICON_SIZE,  // 12

// Стало:
@DrawableRes iconConfirm: Int = R.drawable.ic_confirm,
iconSize: Int = 24,
```

Убрать `import IntrinsicSize` и `import width` если больше не используются.

## Notes

- View mode (строки 100-126) — не меняется
- `onCloseEditMode` callback — не меняется (семантика: применить + закрыть)
- TODO на строке 82 про курсор — не трогаем
