# impl — approved findings (ит.1, для ит.2)

## F001 [minor] — LaunchedEffect cursor jump

**Description:** `LexemeValueFieldWidget.EditRow` синхронизирует `fieldValue` через `LaunchedEffect(value)` без сравнения по `selection`. Если parent трансформирует ввод — курсор прыгнет в конец из-за `TextRange(value.length)`.

**Закрыть:** В `LaunchedEffect(value)` — сохранить текущий cursor offset перед обновлением. Если text равен — не трогать. Если отличается — обновить text, но selection поставить минимум из (старый offset, длина нового value):

```kotlin
LaunchedEffect(value) {
    if (value != fieldValue.text) {
        val oldOffset = fieldValue.selection.start
        fieldValue = TextFieldValue(
            text = value,
            selection = TextRange(minOf(oldOffset, value.length))
        )
    }
}
```

## F002 [minor] — SubentityChip Active ripple

**Description:** `SubentityChip` state=Active использует `InputChip(onClick = {})` — chip остаётся кликабельным (ripple срабатывает) при тапе по телу.

**Закрыть:** Заменить `InputChip(selected = true, onClick = {})` на `Surface` с pill-формой + Row(label + trailing icon) — без `clickable`, без ripple. Trailing `IconButton(onClick = onDeactivate)` сохраняет cursor для ✕. ИЛИ обернуть в `CompositionLocalProvider(LocalRippleConfiguration provides null)` локально для Active-state, если хочется оставить InputChip API.

Предпочтительный вариант — Surface+Row (явный контроль, меньше M3 API)

## F003 [minor] — лишние @OptIn(ExperimentalMaterial3Api)

**Description:** `@file:OptIn(ExperimentalMaterial3Api::class)` в `SubentityChip.kt` и `LexemeValueFieldWidget.kt`.

**Закрыть:** Удалить `@file:OptIn(ExperimentalMaterial3Api::class)` из обоих файлов. Если компилятор начнёт ругаться — вернуть только в один из файлов где реально нужно. Чек: запустить `./gradlew :modules:screen:wordcard:compileDebugKotlin` (или ту же тестовую таску) и убедиться что компиляция ОК.

# F004 — rejected (не трогать)

`@Suppress("UNUSED_PARAMETER") order: Int` — documented decision, остаётся.
