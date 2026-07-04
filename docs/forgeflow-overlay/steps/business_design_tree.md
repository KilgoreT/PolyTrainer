---
name: business_design_tree
output: business_design_tree.md
output_criteria:
  - файл существует
  - каждый узел имеет полный путь файла
  - каждый файл помечен [+] создание, [~] изменение или [-] удаление
  - каждый узел пронумерован
  - зависимости корректны (нет циклов)
  - для каждого узла есть секция с деталями
  - "стало" содержит сигнатуры методов и псевдокод, не реализацию
  - НИ ОДИН узел графа не указывает на UI-путь (см. § Layer boundary)
  - присутствует секция `## UI dependencies` (минимум пустая) — декларация UI-задач для ui sub-flow
---

Прочитай спеку (входной документ). Исследуй код — найди существующие файлы, прочитай их. Составь граф файлов **для бизнес-слоя** для реализации.

Граф — DAG (направленный ациклический граф). Каждый узел — файл. Зависимости определяют порядок: узел реализуется только когда все его зависимости готовы. Независимые узлы выполняются параллельно.

Проверь что файлы для изменения реально существуют (прочитай их). Не выдумывай пути.

Пометки: `[+]` создание, `[~]` изменение, `[-]` удаление.

## Layer boundary — ЖЕЛЕЗНОЕ ПРАВИЛО

**business_design_tree описывает ТОЛЬКО бизнес-слой.** UI узлы — обязательная вотчина ui sub-flow.

### Что считается UI (ЗАПРЕЩЕНО в этом графе)

| Признак | Примеры |
|---|---|
| Файл с `@Composable` annotation | `widget/SomeWidget.kt`, `composables/SomeScreen.kt` |
| Файл в `modules/core/ui/` или `modules/core/theme/` | `core/ui/dropdown/LexemeSubmenuMenuItem.kt` |
| Файл в `modules/widget/<name>/` | `modules/widget/iconDropDowned/MenuItem.kt` |
| Файл в `modules/screen/*/widget/` | `screen/quiz/chat/widget/appbar/menu/QuizComponentMenuItem.kt` |
| Файл в `modules/screen/*/composables/` | `screen/wordcard/composables/WordCardScreen.kt` |
| ResIds: `core-resources/src/main/res/values*/*.xml` | `strings.xml`, `colors.xml` |

### Что входит в business (РАЗРЕШЕНО)

- `modules/screen/<feature>/src/main/.../logic/*.kt` — State, Msg, Reducer, Effect
- `modules/screen/<feature>/src/main/.../mate/*.kt` — FlowHandler, EffectHandler
- `modules/screen/<feature>/src/main/.../deps/*.kt` — UseCase interfaces
- `modules/domain/<name>/` — доменные типы и extensions
- `modules/datasource/<name>/` (без `core-resources`) — Api / Provider interfaces (не impl)
- `app/src/main/.../di/module/*UseCaseImpl.kt` — реализации UseCase
- Тесты бизнес-кода: `src/test/.../*Test.kt` в перечисленных выше модулях

### Если для фичи нужен UI

В этом графе UI-файлы **НЕ перечисляются**. Вместо этого — секция `## UI dependencies` (см. ниже) декларирует требуемые composables / widgets с минимальным API contract, без реализации. ui sub-flow забирает эту декларацию как input для `ui_walkthrough` / `ui_layout` / `ui_design_tree`.

### Failure mode

Architect review **обязан** прогнать grep по всем `file:` полям графа против UI-патернов выше. Если хотя бы один узел — UI, review = `changes_requested` с явной формулировкой «layer boundary violation: <узел> → перенести в ui sub-flow». Шаг повторяется до устранения.

## Часть 1: Граф

YAML-формат. Полные пути. Зависимости по номерам.

```yaml
- id: 0
  file: modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt
  action: "~"
  depends: []

- id: 1
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/State.kt
  action: "~"
  depends: [0]

- id: 2
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/Message.kt
  action: "~"
  depends: [0]

- id: 3
  file: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/ChatReducer.kt
  action: "~"
  depends: [1, 2]
```

Параллельность выводится из графа: узлы без общих зависимостей выполняются одновременно.

## Часть 2: Детали изменений

Для каждого узла — описание изменения.

**[+]** — назначение + ключевые сигнатуры:

### #0 ComponentTypeRef.kt [+]

Sealed-иерархия для ref'ов компонентов. Варианты: `BuiltIn(key: BuiltInComponent)`, `UserDefined(name: String)`.

**[~]** — было → стало:

### #1 State.kt [~]

**Было:**
```kotlin
data class ItemsState(
    val earliest: Earliest,
    val frequentMistakes: FrequentMistakes,
    val debug: Debug,
)
```

**Стало:**
```kotlin
data class ItemsState(
    val earliest: Earliest,
    val frequentMistakes: FrequentMistakes,
    val debug: Debug,
    val quizComponent: QuizComponent,
)
data class QuizComponent(
    val availableTypes: List<ComponentType>,
    val selectedRef: ComponentTypeRef?,
)
```

**[-]** — что удаляется:

### #N old_file.kt [-]

Мёртвый код, заменён на #M. Никто не импортирует.

## Часть 3: UI dependencies (обязательная секция)

Декларация UI-задач для ui sub-flow. **Только контракт, не реализация.** Если фича не трогает UI — оставить секцию пустой с пометкой `(нет UI-зависимостей)`.

Формат:

```
## UI dependencies

- **Имя composable / widget:** что нужно сделать (новый / изменить / удалить)
  - params: имя1: Тип1, имя2: Тип2
  - callbacks: onX → Msg.Y, onZ → Msg.W
  - location: предполагаемый module (`core/ui/`, `widget/<name>/`, `screen/<feature>/widget/`)
  - rationale: зачем (одна строка)
```

Пример:

```
## UI dependencies

- **LexemeSubmenuMenuItem** (новый Tier 1 primitive):
  - params: title: String, content: @Composable ColumnScope.() -> Unit
  - callbacks: —
  - location: modules/core/ui/dropdown/
  - rationale: переиспользуемый submenu для radio-групп

- **QuizComponentMenuItem** (новый Tier 3 wrapper в feature):
  - params: state: ItemsState.QuizComponent, onSelect: (ComponentTypeRef) -> Unit
  - callbacks: onSelect → Msg.SelectQuizComponent(ref)
  - location: modules/screen/quiz/chat/widget/appbar/menu/
  - rationale: рендерит picker меню с radio-группой компонентов

- **ActionsWidget** (изменить):
  - params: добавить state.itemsState.quizComponent
  - callbacks: добавить onSelectQuizComponent
  - location: modules/screen/quiz/chat/widget/appbar/ (existing)
  - rationale: встроить QuizComponentMenuItem между MistakesMenuItem и debug-блоком
```

## Правила

- Полные пути от корня проекта
- Каждый узел — файл с пометкой [+] [~] [-] и зависимостями
- Нумерация — сквозная
- Для [+] — назначение + ключевые сигнатуры
- Для [~] — было → стало: сигнатуры + псевдокод, не реализация
- Для [-] — что удаляется и почему
- Зависимости — только прямые (не транзитивные)
- **UI узлы в графе ЗАПРЕЩЕНЫ** — см. § Layer boundary
- **Секция `## UI dependencies` ОБЯЗАТЕЛЬНА** — даже если пустая
