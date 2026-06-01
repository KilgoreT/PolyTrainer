# infra_design_tree — IS482

> DAG infra-слоя для создания категории `modules/domain/` и модуля `modules/domain/lexeme`. Только инфраструктура (Gradle / settings / dependencies / CI). Содержимое `.kt`-файлов — domain типов, mapper'а, миграции консьюмеров — относится к business sub-flow и здесь не описывается.

## Часть 1: Граф

```yaml
- id: 0
  file: modules/domain/lexeme/build.gradle.kts
  action: "+"
  depends: []

- id: 1
  file: settings.gradle.kts
  action: "~"
  depends:
    - 0

- id: 2
  file: app/build.gradle.kts
  action: "~"
  depends:
    - 1

- id: 3
  file: modules/screen/wordcard/build.gradle.kts
  action: "~"
  depends:
    - 1

- id: 4
  file: modules/screen/quiz/chat/build.gradle.kts
  action: "~"
  depends:
    - 1

- id: 5
  file: modules/screen/dictionaryTab/build.gradle.kts
  action: "~"
  depends:
    - 1
```

**Обоснование зависимостей:**

- Узел `0` — корень: `build.gradle.kts` нового модуля должен существовать на диске, иначе `include(":modules:domain:lexeme")` в `settings.gradle.kts` приведёт к ошибке Gradle sync (`Project 'lexeme' could not be found`).
- Узел `1` (`settings.gradle.kts`) зависит от `0`: после правки `include` Gradle при следующем sync уже ожидает найти физический модуль.
- Узлы `2-5` (правки `build.gradle.kts` четырёх consumer'ов) — параллельные, зависят только от `1`: пока `:modules:domain:lexeme` не зарегистрирован в settings, `project("path" to ":modules:domain:lexeme")` не резолвится.
- CI-узел отсутствует — обоснование в § «Не входит в граф».

**Что НЕ входит в граф (явные исключения):**

- `.github/workflows/on_feature_push.yml` — не меняем. CI запускает `./gradlew testDebugUnitTest`, у `kotlin.jvm`-модуля такого task нет (есть `test`). В IS482 в `modules/domain/lexeme/src/test/` юнит-тесты на computed extensions **могут появиться** (см. scope `needs_tests = true`, тесты `LexemeExt`). Однако:
  1. Решение по добавлению `LexemeExt` (и его тестов) делегировано business sub-flow — на момент infra-шага неизвестно будут ли вообще тесты в новом модуле.
  2. Основная масса тестов IS482 (mapper) живёт в `app/src/test/` — туда CI попадает через `testDebugUnitTest`.
  3. Правка workflow ради «может быть» тестов — преждевременно и расширяет scope IS482.
  - **Фиксация:** CI не трогаем в IS482. Если на business sub-flow появится `LexemeExtTest.kt` — финализирующий шаг (или follow-up задача) добавит `./gradlew test testDebugUnitTest` в workflow. Зафиксировать как заметку в `log_messages`.
- `modules/domain/lexeme/proguard-rules.pro` — НЕ создаём. Pure-Kotlin модуль публикуется как `.jar`, не `.aar`. `consumerProguardFiles` для `kotlin.jvm`-плагина не применим. Аналог `modules/core/logger` proguard-файла не имеет.
- `modules/domain/lexeme/src/main/AndroidManifest.xml` — не существует у pure-Kotlin модулей (logger подтверждает: `find` по logger дал пусто).
- `app/proguard-rules.pro` — не трогаем. Domain `data class Lexeme(...)` без аннотаций (reflection/Gson/Room) не требует keep-правил R8 (`module_kotlin_only` из scope).
- DI-файлы (`app/.../di/module/...`) — НЕ входят в infra. Domain не имеет Dagger-модулей (pure-Kotlin без DI-аннотаций). DI feature-модулей в IS482 не меняется: новые типы — это просто доменные `data class`, не сервисы / repository / use-case.
- `modules/domain/lexeme/src/main/java/...` и `src/test/...` — это **business-слой** (исходники `.kt`). Не infra. Сам каталог создаётся побочно при создании первого `.kt` файла в business sub-flow.

## Часть 2: Детали изменений

### Узел 0 — `[+]` `modules/domain/lexeme/build.gradle.kts`

**Назначение.** Декларация нового pure-Kotlin Gradle-модуля для категории `modules/domain/`. Один-в-один копия `modules/core/logger/build.gradle.kts` (13 строк, `org.jetbrains.kotlin.jvm` + `jvmToolchain(17)`). Это первый модуль новой категории — задаёт шаблон для будущих доменов.

> 📎 guide: docs/guides/project-architecture.md — "Feature модули зависят от core модулей. Никогда наоборот."
>
> 📎 guide: docs/guides/naming.md — "Domain (feature `modules/screen/.../entity/`, общий `modules/domain/<name>/`) → без суффикса (`Word`, `Lexeme`)"

**Содержимое (шаблон, 1-в-1 как logger):**

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
```

> 📎 guide: docs/guides/code-style.md — "Java target: 17"

**Решения зафиксированы / обоснованы:**

- `plugins`: только `org.jetbrains.kotlin.jvm`. **БЕЗ** `java-library`, `com.android.library`, `kotlin.android`, `kotlin.plugin.compose`, `ksp`. Domain — pure-Kotlin (Dependency Rule: domain не зависит от Android SDK / Compose / DI-генерации).

> 📎 guide: docs/guides/project-architecture.md — "Core модули не зависят друг от друга (кроме: `ui` зависит от `theme`)"
>
> 📎 guide: docs/guides/code-style.md — "Плагины: `com.android.library` + `org.jetbrains.kotlin.android` + `org.jetbrains.kotlin.plugin.compose`" (стандартный набор для Android-модулей; domain отступает от него как pure-Kotlin)

- `kotlin.jvm` версия `1.9.10` (из `settings.gradle.kts:21`, объявлена без `apply false` → применяется без override). Это **тот же** уровень, что у logger'а. Несоответствие с Kotlin Android `2.0.20` — известный звоночек (`.kotlin_module` metadata между 1.9 и 2.0 формально совместимо для `data class` без новых language features). Override **не добавляем** — следуем logger-прецеденту. Если в business-слое возникнут проблемы компиляции consumer'ов с `2.0.20` против `.kotlin_module` от `1.9.10` — финализирующий шаг повысит версию.
- `jvmToolchain(17)` — фиксирует JDK 17 для компиляции (как у logger). Совместимо с `JavaVersion.VERSION_17` у всех остальных модулей.

> 📎 guide: docs/guides/code-style.md — "Java target: 17"

- `dependencies { }` блок **отсутствует**. Domain не должен зависеть ни от чего проектного. Стандартная библиотека Kotlin/JDK подключается автоматически плагином. `java.util.Date` для полей дат — JDK, ок.
- `namespace`, `compileSdk`, `minSdk`, `android { }` — отсутствуют (не Android-модуль).

> 📎 guide: docs/guides/code-style.md — "Namespace = lowercase package: `me.apomazkin.dictionarytab`" (применимо только к Android-модулям; pure-Kotlin namespace не задаёт)

- `proguard-rules.pro` / `consumerProguardFiles` — не применимо для jvm-плагина.

**Чек для будущих ревью / автотестов:** файл должен быть **ровно 13 строк** (без trailing blank), без лишних пустых блоков `dependencies { }`. Это критерий «следование прецеденту logger 1-в-1».

### Узел 1 — `[~]` `settings.gradle.kts`

**Было (релевантный фрагмент, `:33-67`):**

```
//Core
include(":modules:core:mate")
...
include(":modules:core:tools")

//Features
include(":modules:screen:splash")
...
include(":modules:screen:quiz:chat")

//Widget
include(":modules:widget:dictionaryappbar")
...
include(":modules:widget:chipPicker")

include(":modules:datasource:prefs")

//Libraries
include(":modules:library:flags")

//Old
include(":core:core-resources")
...
```

**Стало (псевдокод правки, добавляем блок `//Domain` ПОСЛЕ `//Libraries`, ПЕРЕД `//Old`):**

```
//Libraries
include(":modules:library:flags")

//Domain
include(":modules:domain:lexeme")

//Old
include(":core:core-resources")
...
```

> 📎 guide: docs/guides/naming.md — "Пакеты: `me.apomazkin.<module>`" (drift_rule § Пакеты — physical path `modules/domain/lexeme` соответствует package `me.apomazkin.lexeme`)

**Решения / drift-fixации:**

- **Позиция блока:** ПОСЛЕ `//Libraries`, ПЕРЕД `//Old`. Обоснование: новая категория — это «современный» слой архитектуры (как `Core`, `Features`, `Widget`, `Libraries`); `//Old` — legacy `:core:core-*`. Domain логически принадлежит к «современным» категориям, не legacy. Альтернатива — между `//Core` и `//Features` (как «фундамент») — не выбрана: scope не позиционирует domain как core/foundation, это самостоятельный слой между core и feature. Финальная позиция — последняя из «современных» категорий, симметрично существующему порядку (`Core → Features → Widget → datasource → Libraries → Domain → Old`).
- **Синтаксис строки:** `include(":modules:domain:lexeme")` — без `projectDir`-override, без префиксов, по образцу всех остальных include-строк (`settings.gradle.kts:34-67`). Physical path `modules/domain/lexeme/` совпадает с include-path.
- **Комментарий-разделитель:** `//Domain` без пробела после `//`, верхний регистр первой буквы — следуем прецеденту `//Core`, `//Features`, `//Widget`, `//Libraries`, `//Old` (`settings.gradle.kts:33,41,52,60,63`).
- Никаких правок `pluginManagement { plugins { } }` — `org.jetbrains.kotlin.jvm` уже объявлен (`settings.gradle.kts:21`).
- Никаких правок `versionCatalogs` / `dependencyResolutionManagement` — pure-Kotlin модуль каталоги не использует (нет dependencies).

### Узел 2 — `[~]` `app/build.gradle.kts`

**Было (релевантный фрагмент, `:111-138`):**

```kotlin
dependencies {
    implementation(fileTree("dir" to "libs", "include" to ("*.jar")))

    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))

    implementation(project("path" to ":modules:screen:splash"))
    ...
    implementation(project("path" to ":modules:screen:settingstab"))

    implementation(project("path" to ":modules:widget:dictionarypicker"))
    implementation(project("path" to ":modules:widget:dictionaryappbar"))

    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:datasource:prefs"))

    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":core:core-db"))
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    ...
}
```

**Стало (псевдокод правки — добавляем одну строку, позиция: рядом с другими `:modules:*` проектными deps, логически — между `:modules:datasource:prefs` и `:core:core-resources` либо в новой группе):**

```kotlin
dependencies {
    ...
    implementation(project("path" to ":modules:library:flags"))
    implementation(project("path" to ":modules:datasource:prefs"))

    implementation(project("path" to ":modules:domain:lexeme"))   // NEW: для mapper'а LexemeApiEntity → Lexeme и UseCaseImpl

    implementation(project("path" to ":core:core-resources"))
    ...
}
```

> 📎 guide: docs/guides/data-layer.md — "UseCase интерфейс в feature модуле, реализация в app модуле"
>
> 📎 guide: docs/guides/data-layer.md — "API → Domain (в UseCase модуле)" (mapper `LexemeApiEntity.toDomainEntity()` живёт в app, рядом с UseCaseImpl)

**Решения / обоснование:**

- **Прямая dep в `app/`, не через api-экспозицию feature-модуля.** Из `infra_walkthrough` § 3: `app/` не имеет прямой dep на logger, работает через `api(...)` в `modules/core/ui`. Для `:modules:domain:lexeme` такой путь **не подходит** — это нарушение изоляции (domain «течёт как view» через api одного из feature). Сцена: `app/` владеет общим mapper'ом `LexemeApiEntity.toDomain(): Lexeme` (`mapper_location` зафиксировано) + тремя `UseCaseImpl`. Этим четырём файлам в `app/` нужен прямой `import me.apomazkin.lexeme.Lexeme`. Поэтому `app/build.gradle.kts` объявляет собственный `implementation`.

> 📎 guide: docs/guides/project-architecture.md — "Screen модули не зависят от других screen модулей" (изоляция feature-модулей: domain не должен «течь» через api одного feature к app)

- **Тип dep — `implementation`, не `api`**. `app/` — терминальный модуль (никто из него ничего не транзитивно потребляет), `api` смысла не имеет.
- **Синтаксис строки:** `implementation(project("path" to ":modules:domain:lexeme"))` — map-стиль, как все остальные проектные deps в app (`app/build.gradle.kts:115-137`).
- **Позиция строки:** между `:modules:datasource:prefs` (последний `:modules:*`) и `:core:core-resources` (начало legacy `:core:*` блока). Логически — последняя из «modules»-группы. Альтернатива — рядом с `:modules:screen:*` (т.к. domain питает screen) — не выбрана: domain не feature-screen, отдельная категория, отдельная группа в dependencies-блоке.
- Никаких других правок `app/build.gradle.kts` (plugins, android-блок, signingConfigs, buildTypes) **не требуется**.

### Узел 3 — `[~]` `modules/screen/wordcard/build.gradle.kts`

**Было (`:27-37`):**

```kotlin
dependencies {
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:core:tools"))
    implementation(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))
    ...
}
```

**Стало (псевдокод — одна новая строка, позиция: рядом с другими `:modules:core:*` либо отдельным блоком, конкретно — после `:modules:core:logger`, перед `:core:core-resources`):**

```kotlin
dependencies {
    ...
    implementation(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":modules:domain:lexeme"))   // NEW: domain entity Lexeme заменяет локальный wordcard.entity.Lexeme

    implementation(project("path" to ":core:core-resources"))
    ...
}
```

> 📎 guide: docs/guides/naming.md — "Domain enum → reuse из API, не дублировать" (а data class — выносится в общий `modules/domain/<name>/`, локальные `wordcard.entity.Lexeme` устраняются)

**Решения:**

- Тип dep — `implementation`. Wordcard не публикует domain-типы наружу через api (`api` не используется ни в одном из его текущих deps).
- Синтаксис — map-стиль (проектная конвенция, `wordcard/build.gradle.kts:28-37`).
- Позиция — последняя из core-deps, перед `:core:core-resources`. Альтернатива (отдельная группа «// Domain») — преждевременно: пока единственный domain-модуль, отдельный блок-комментарий внутри feature не нужен. Когда доменов станет >2 в одном feature — выделится.
- Удаление `:modules:core:logger` или другие правки **не требуются** (domain не зависит от logger, и наоборот).

### Узел 4 — `[~]` `modules/screen/quiz/chat/build.gradle.kts`

**Было (`:27-38`):**

```kotlin
dependencies {
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation("javax.inject:javax.inject:1")
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:datasource:prefs"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))
    ...
}
```

**Стало (псевдокод — одна новая строка, позиция: рядом с другими `:modules:core:*` / `:modules:datasource:*`, конкретно — после `:modules:datasource:prefs`, перед `:modules:widget:iconDropDowned`):**

```kotlin
dependencies {
    ...
    implementation(project("path" to ":modules:datasource:prefs"))
    implementation(project("path" to ":modules:domain:lexeme"))   // NEW

    implementation(project("path" to ":modules:widget:iconDropDowned"))
    ...
}
```

**Решения:**

- Тип / синтаксис — те же что для узла 3 (`implementation` + map-стиль).
- Позиция — между `:modules:datasource:prefs` и `:modules:widget:iconDropDowned`. Это означает «не-widget» секция; domain симметричен core/datasource в плане «слой ниже widget».
- Никаких правок plugins / android / тестовых deps.

### Узел 5 — `[~]` `modules/screen/dictionaryTab/build.gradle.kts`

**Было (`:31-41`):**

```kotlin
dependencies {
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:widget:iconDropDowned"))
    implementation(project("path" to ":modules:widget:chipPicker"))
    //TODO kilg 29.06.2025 10:39 избавиться от зависимости
    //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей, и избавиться от сущностей ui
    implementation(project("path" to ":modules:widget:dictionarypicker"))
    ...
}
```

**Стало (псевдокод — одна новая строка, позиция: рядом с другими `:modules:core:*`, после `:core:core-resources`):**

```kotlin
dependencies {
    ...
    implementation(project("path" to ":core:core-resources"))
    implementation(project("path" to ":modules:domain:lexeme"))   // NEW: domain.Lexeme мапится в LexemeUiItem внутри dictionaryTab

    implementation(project("path" to ":modules:widget:iconDropDowned"))
    ...
}
```

> 📎 guide: docs/guides/naming.md — "UI data class → префикс `Ui` + тип: `*UiEntity` (обёртка одного домен-объекта), `*UiItem` (элемент списка)" (Domain → UI mapping: `Lexeme` → `LexemeUiItem`)

**Решения:**

- Тип / синтаксис — `implementation` + map-стиль.
- Позиция — после `:core:core-resources`, перед widget-группой (симметрично узлам 3/4).
- **TODO-комментарии (`build.gradle.kts:39-40`) НЕ удаляются** в этом узле. Они частично адресуются IS482 (domain layer вводится), но второй TODO («избавиться от сущностей ui») — out of scope IS482 (scope явно фиксирует: `LexemeUiItem` остаётся как UI-слой). Удаление TODO — задача отдельной фичи (когда dictionaryTab откажется от UI-обёрток). В IS482 — оставляем как есть.
- Никаких правок plugins / android / buildFeatures / тестовых deps.

## Сводка по action

| Action | Кол-во | Файлы |
|---|---|---|
| `[+]` | 1 | `modules/domain/lexeme/build.gradle.kts` |
| `[~]` | 5 | `settings.gradle.kts`, `app/build.gradle.kts`, `modules/screen/{wordcard,quiz/chat,dictionaryTab}/build.gradle.kts` |
| `[-]` | 0 | — |

**Удалений нет.** Удаление `modules/screen/{wordcard,quiz/chat}/src/main/java/.../entity/Lexeme.kt` (исходники локальных domain-классов) — это **business-слой**, не infra (это `.kt`-файлы с типами, не Gradle-конфигурация).

## Параллелизация

После узла 1 (`settings.gradle.kts`) узлы `2, 3, 4, 5` независимы и могут выполняться параллельно. Однако в практическом порядке реализации обычно `app/` идёт **последним** (он импортирует и mapper, и domain-типы — компилируется когда все feature-модули и domain-модуль уже валидны). Но это уровень build-ordering Gradle, не уровень design — DAG корректно описывает infra-зависимости.

## log_messages

- DAG infra IS482: 1 создание (`modules/domain/lexeme/build.gradle.kts`, копия logger), 5 правок (settings + 4 build.gradle.kts), 0 удалений.
- CI gotcha (`testDebugUnitTest` не покрывает `kotlin.jvm`) явно ВНЕ scope IS482: workflow не правим. Если business sub-flow добавит тесты в `modules/domain/lexeme/src/test/` — финализация / follow-up.
- Прямая dep `:modules:domain:lexeme` объявлена в `app/` (не через api-экспозицию feature) — domain не «течёт» через api consumer'ов, изоляция сохранена.

_model: Opus 4.7 (1M context)_
