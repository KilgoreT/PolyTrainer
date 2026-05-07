# Spec: IS453 — WordCard input field UX

## Проблема

Файл: `modules/core/ui/src/main/java/me/apomazkin/ui/text/base/LexemeEditableText.kt`

Edit mode Row:
```
[BasicTextField (weight=1f, fill=false, IntrinsicSize.Min)] — 8dp — [ic_close 12dp]
```

1. `fill=false` + `IntrinsicSize.Min` — поле ужимается до минимума контента. Трудно попасть для фокуса.
2. `ic_close` (крестик X, 12dp, чёрный) — вызывает `onCloseEditMode`. Семантически это «применить и закрыть», но крестик считывается как «отмена». Нужна зелёная галочка.
3. Иконка смещается — привязана к концу текста (из-за `fill=false`), а не к краю контейнера.

## Решение

### Поле ввода

```
Было:  BasicTextField(modifier = Modifier.weight(1f, fill = false).width(IntrinsicSize.Min))
Стало: BasicTextField(modifier = Modifier.weight(1f))
```

Убрать `fill = false` и `IntrinsicSize.Min`. Поле займёт всё доступное пространство.

### Иконка подтверждения

Заменить `ic_close` (крестик) на новую иконку:
- Зелёный круг с белой галочкой
- Размер: 24dp (увеличить с 12dp — 12 слишком мелко для тапа)
- Drawable: `ic_confirm` (новый) или `ic_check_circle`
- Цвет: зелёный (`statLearnedFg` или кастомный)

### Фиксированная позиция

Иконка привязана к правому краю Row (уже так через `weight(1f)` на TextField). После фикса ширины поля — иконка автоматически встанет на место.

## Затронутые файлы

| Файл | Действие |
|------|----------|
| `modules/core/ui/.../text/base/LexemeEditableText.kt` | Изменить: weight, убрать IntrinsicSize.Min |
| `core/core-resources/res/drawable/ic_confirm.xml` | Создать: зелёный круг + белая галочка |
| `modules/core/ui/.../text/base/LexemeEditableText.kt` | Изменить: ic_close → ic_confirm, size 12→24 |

## Не меняется

- `LexemeValueFieldWidget.kt` — контейнер с delete-иконкой (отдельный виджет)
- `PrimaryEditableWidget.kt` — обёртка с заголовком
- Логика стейта, сообщения, reducer
- View mode (отображение текста)
