# infra_implement — IS482

Реализация infra-слоя IS482: создание Gradle-категории `modules/domain/` и первого pure-Kotlin модуля `:modules:domain:lexeme`. Все правки строго по `infra_design_tree.md` (DAG из 6 узлов).

> 📎 guide: docs/guides/project-architecture.md — "Граф модулей / modules/<category>/<name>"
>
> 📎 guide: docs/guides/naming.md — "Domain (feature `modules/screen/.../entity/`, общий `modules/domain/<name>/`) → без суффикса"

## Изменённые / созданные файлы

| Узел | Action | Файл |
|---|---|---|
| 0 | `[+]` | `modules/domain/lexeme/build.gradle.kts` |
| 0 | `[+]` | `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` (placeholder) |
| 1 | `[~]` | `settings.gradle.kts` |
| 2 | `[~]` | `app/build.gradle.kts` |
| 3 | `[~]` | `modules/screen/wordcard/build.gradle.kts` |
| 4 | `[~]` | `modules/screen/quiz/chat/build.gradle.kts` |
| 5 | `[~]` | `modules/screen/dictionaryTab/build.gradle.kts` |

Удалений нет.

## Детали по узлам

### Узел 0 — `modules/domain/lexeme/build.gradle.kts`

Создан 1-в-1 как `modules/core/logger/build.gradle.kts` (13 строк, без trailing blank):
- plugin `org.jetbrains.kotlin.jvm`
- `java { source/targetCompatibility = VERSION_17 }`
- `kotlin { jvmToolchain(17) }`
- Без `dependencies { }`, без `android { }`, без `namespace`/proguard/manifest

> 📎 guide: docs/guides/code-style.md — "Java target: 17"

Дополнительно создан placeholder `src/main/java/me/apomazkin/lexeme/Lexeme.kt` с содержимым:

```kotlin
package me.apomazkin.lexeme

// Placeholder — заполнит business sub-flow IS482.
```

> 📎 guide: docs/guides/naming.md — "Пакеты: me.apomazkin.<module>"
>
> 📎 guide: docs/guides/code-style.md — "Язык комментариев и документации: английский"

**Обоснование placeholder.** `org.jetbrains.kotlin.jvm` не падает без source-файлов, но IDE индексация и `:dependencies` work-flow проходят гладко при наличии хотя бы одного валидного `.kt`. Placeholder также фиксирует package `me.apomazkin.lexeme` (соответствует `docs/guides/naming.md`). Файл будет переписан / заменён в business sub-flow.

### Узел 1 — `settings.gradle.kts`

Добавлен блок `//Domain` после `//Libraries`, перед `//Old`:

```
//Libraries
include(":modules:library:flags")

//Domain
include(":modules:domain:lexeme")

//Old
...
```

Стиль комментария-разделителя (`//Domain` без пробела) симметричен существующим `//Core`, `//Features`, `//Widget`, `//Libraries`, `//Old`.

### Узел 2 — `app/build.gradle.kts`

Добавлена строка `implementation(project("path" to ":modules:domain:lexeme"))` между блоком `:modules:datasource:prefs` и `:core:core-resources` — как «последняя из modules-группы», обоснование в infra_design_tree § Узел 2.

Тип dep — `implementation` (app терминальный, `api` не нужен). Прямая dep, не через api-экспозицию feature-модуля — domain не «течёт» через api.

> 📎 guide: docs/guides/project-architecture.md — "Feature модули зависят от core модулей. Никогда наоборот."

### Узлы 3-5 — feature `build.gradle.kts`

- **wordcard** — `implementation(project("path" to ":modules:domain:lexeme"))` после `:modules:core:logger`, перед `:core:core-resources`.
- **quiz/chat** — после `:modules:datasource:prefs`, перед `:modules:widget:iconDropDowned`.
- **dictionaryTab** — после `:core:core-resources`, перед `:modules:widget:iconDropDowned`.

Все позиции по infra_design_tree. TODO-комментарии в dictionaryTab (про «избавиться от сущностей ui») **не удалены** — out of scope IS482 (второй TODO — отдельная задача, UI-обёртки остаются).

## Валидация

### gradle sync / resolve

```bash
./gradlew :modules:domain:lexeme:dependencies --console=plain
```

Результат: `BUILD SUCCESSFUL`. Граф зависимостей нового модуля:
- `compileClasspath` / `runtimeClasspath`: только `org.jetbrains.kotlin:kotlin-stdlib:2.2.0 → org.jetbrains:annotations:13.0`.
- Проектных deps нет (что и требовалось — domain не зависит ни от чего).
- Никаких ошибок резолва, модуль корректно зарегистрирован.

### feature build (wordcard)

```bash
./gradlew :modules:screen:wordcard:assembleDebug --console=plain
```

Результат: `BUILD SUCCESSFUL` (exit code = 0). Compile errors отсутствуют. Предупреждения только pre-existing (`@param`-targeting в `Message.kt`/`UiEffect.kt`) — не связаны с IS482. Новая dep `:modules:domain:lexeme` подключилась чисто.

## Нетривиальные решения

1. **Placeholder `.kt` файл создан несмотря на то, что `kotlin.jvm` не падает без source.** Причина — IDE/gradle UX: пустая `src/main/java/me/apomazkin/lexeme/` без `.kt` файла не индексируется IDE как kotlin package; placeholder фиксирует package и облегчает business sub-flow (директория уже есть). Файл — одна строка комментария плюс декларация пакета, business sub-flow её перепишет.
2. **CI workflow не правится.** Зафиксировано в infra_design_tree § «Что НЕ входит в граф». Если business sub-flow добавит юнит-тесты в `modules/domain/lexeme/src/test/` — follow-up задача расширит workflow на `./gradlew test testDebugUnitTest`.
3. **Версия `kotlin.jvm` (1.9.10) vs `kotlin.android` (2.0.20).** Не выравниваем — следуем logger-прецеденту. Если в business sub-flow возникнут проблемы binary compat с `.kotlin_module` — финализирующий шаг повысит версию.

## Отклонения от infra_design_tree

Нет. Все 6 узлов реализованы 1-в-1 по спецификации.

## log_messages

- IS482 infra: создан pure-Kotlin модуль `:modules:domain:lexeme` (плюс placeholder Lexeme.kt) + 5 правок (settings + 4 build.gradle.kts).
- Gradle sync (`:modules:domain:lexeme:dependencies`) и feature build (`:modules:screen:wordcard:assembleDebug`) — оба BUILD SUCCESSFUL.
- CI workflow не трогаем; placeholder Lexeme.kt будет переписан business sub-flow.

_model: Opus 4.7 (1M context)_
