---
name: infra_design_tree
output: infra_design_tree.md
output_criteria:
  - файл существует
  - каждый узел имеет полный путь файла
  - каждый файл помечен [+] создание, [~] изменение или [-] удаление
  - каждый узел пронумерован
  - зависимости корректны (нет циклов)
  - для каждого узла есть секция с деталями
  - "стало" содержит сигнатуры методов и псевдокод, не реализацию
---

Тонкая обёртка над общим [`design_tree.md`](../../forgeflow/steps/design_tree.md) — содержимое и правила идентичны. Префикс `infra_` нужен только чтобы шаги не конфликтовали по имени в master plan при разворачивании четырёх subflow (`infra` / `business` / `ui` / `data`).

Прочитай спеку из input (`scope_analysis.output` + `infra_walkthrough.output`). Исследуй код — найди существующие файлы, прочитай их. Составь граф файлов для реализации **infra-слоя** (Gradle модули, build.gradle.kts, settings.gradle.kts, DI, ProGuard, Manifest, CI).

Граф — DAG (направленный ациклический граф). Каждый узел — файл. Зависимости определяют порядок: узел реализуется только когда все его зависимости готовы. Независимые узлы выполняются параллельно.

Проверь что файлы для изменения реально существуют (прочитай их через `Read`). Не выдумывай пути.

Пометки: `[+]` создание, `[~]` изменение, `[-]` удаление.

## Часть 1: Граф

YAML-формат. Полные пути. Зависимости по номерам.

```yaml
- id: 0
  file: modules/domain/lexeme/build.gradle.kts
  action: "+"
  depends: []

- id: 1
  file: settings.gradle.kts
  action: "~"
  depends: [0]

- id: 2
  file: modules/screen/wordcard/build.gradle.kts
  action: "~"
  depends: [0]

# ... и т.д.
```

Параллельность выводится из графа: узлы без общих зависимостей выполняются одновременно.

## Часть 2: Детали изменений

Для каждого узла — описание изменения.

**[+]** — назначение + ключевые сигнатуры / содержимое:

### #0 modules/domain/lexeme/build.gradle.kts [+]

Pure-Kotlin Gradle модуль (`org.jetbrains.kotlin.jvm`). Аналог: `modules/core/logger/build.gradle.kts` (см. infra_walkthrough). Toolchain: jvmToolchain(17). Без Android SDK.

**[~]** — было → стало:

### #1 settings.gradle.kts [~]

**Было:**
```kotlin
// Screen modules
include(":modules:screen:wordcard")
// ...
```

**Стало:**
```kotlin
// Domain modules
include(":modules:domain:lexeme")

// Screen modules
include(":modules:screen:wordcard")
// ...
```

**[-]** — что удаляется и почему.

## Правила

- Полные пути от корня проекта.
- Каждый узел — файл с пометкой `[+]` `[~]` `[-]` и зависимостями.
- Нумерация — сквозная.
- Для `[+]` — назначение + ключевые сигнатуры / шаблон содержимого.
- Для `[~]` — было → стало: сигнатуры + псевдокод, не реализация.
- Для `[-]` — что удаляется и почему.
- Зависимости — только прямые (не транзитивные).

## Infra-специфика

- Опирайся на факты из `infra_walkthrough.output` — найденные конвенции (project("path" to ":..."), kts напрямую без convention-plugins, `jvmToolchain(17)` для pure-Kotlin модулей).
- При проектировании `app/build.gradle.kts` правки — указывай **точную причину** dep (mapper vs UseCaseImpl).
- ProGuard / R8 / Manifest / CI — только если walkthrough зафиксировал необходимость. Иначе не выдумывай.
