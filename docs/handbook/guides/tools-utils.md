# Утилиты (core/tools)

Расположение: `modules/core/tools/src/main/java/me/apomazkin/tools/ModifyFiltered.kt`

## Работа со списками

### Вставка элементов

```kotlin
val list = listOf(1, 2, 3)

list.insertToBegin(0)   // [0, 1, 2, 3]
list.insertToEnd(4)     // [1, 2, 3, 4]
```

### Поиск

```kotlin
val items = listOf(LexemeState(id = 1), LexemeState(id = 2), LexemeState(id = 3))

// Найти по предикату или вернуть первый
items.getFilteredOrFirst { it.id == 2L }  // LexemeState(id=2)
items.getFilteredOrFirst { it.id == 99L } // LexemeState(id=1) — первый элемент
```

### Модификация

```kotlin
// Изменить элементы по предикату
state.copy(
    lexemeList = state.lexemeList.modifyFiltered(
        predicate = { it.id == lexemeId },
        action = { it.copy(isMenuOpen = true) }
    )
)

// Изменить первый элемент
state.copy(
    lexemeList = state.lexemeList.modifyFirst { it.copy(isSelected = true) }
)

// Изменить по предикату или первый (если не найден)
state.copy(
    lexemeList = state.lexemeList.modifyFilteredOrFirst(
        predicate = { it.id == targetId },
        action = { it.copy(value = newValue) }
    )
)
```

Эти утилиты активно используются в reducer'ах для иммутабельной модификации списков в state.
