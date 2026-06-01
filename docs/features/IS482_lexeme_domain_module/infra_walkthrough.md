# infra_walkthrough — IS482

> Discovery infra. Только факты, без design-решений.

## 1. Структура `settings.gradle.kts` и категоризация

`settings.gradle.kts` (root) — единственная точка `include`. Категории разделены **однострочными комментариями** `//Core`, `//Features`, `//Widget`, `//Libraries`, `//Old`. Перед `//Old` ещё лежит «безымянная» строка `include(":modules:datasource:prefs")` (категория `datasource` существует физически, но не имеет комментария-разделителя).

- `settings.gradle.kts:31` — `include(":app")`
- `settings.gradle.kts:33` — `//Core`
- `settings.gradle.kts:34-39` — шесть `include(":modules:core:<name>")`
- `settings.gradle.kts:41` — `//Features`
- `settings.gradle.kts:42-50` — девять feature-модулей; `quiz/chat` записан как `":modules:screen:quiz:chat"` (двойная вложенность через двоеточие — это не Gradle-конвенция «дочерних проектов», а просто полный путь, проверь — нужно смотреть на `projectDir` если есть, но в этом проекте `projectDir` не переопределяется ⇒ путь на диске совпадает с include-path).
- `settings.gradle.kts:52` — `//Widget`
- `settings.gradle.kts:53-56` — четыре widget'а
- `settings.gradle.kts:58` — `include(":modules:datasource:prefs")` (без своего комментария-разделителя; перед строкой пустая строка)
- `settings.gradle.kts:60` — `//Libraries`
- `settings.gradle.kts:61` — один `include(":modules:library:flags")`
- `settings.gradle.kts:63` — `//Old`
- `settings.gradle.kts:64-67` — четыре `:core:core-*` (legacy)

**Точный синтаксис строки include**: `include(":modules:<category>:<name>")` без префиксов и опций. `projectDir` нигде не переопределяется (есть закомментированная строка `settings.gradle.kts:117` для prefs — fallback не нужен, файлы физически лежат по include-path).

**Категория `modules/domain/` отсутствует:** ни в `settings.gradle.kts`, ни на диске (`ls /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain` → `No such file or directory`). Никакого подготовленного комментария-разделителя нет — нужно создать новый блок `//Domain` (по аналогии с `//Core`, `//Widget`).

**Версионирование плагинов** объявлено в `settings.gradle.kts:13-23` (pluginManagement → plugins). Релевантные строки для pure-Kotlin модуля:

- `settings.gradle.kts:21` — `id("org.jetbrains.kotlin.jvm") version "1.9.10"` (объявлен **без** `apply false` — то есть готов к применению сразу, без указания версии в дочернем `build.gradle.kts`).

⚠️ **Факт-несоответствие версий:** для Android-модулей применяется Kotlin Android Plugin версии **2.0.20** (`settings.gradle.kts:16`). Для JVM — версия **1.9.10**. Это не ошибка (Kotlin JVM-плагин и Kotlin Android-плагин в проекте задеплоены раздельно), но если новый модуль хочет ту же Kotlin-версию что и Android-часть — нужно либо поднять JVM-версию в `settings.gradle.kts`, либо явно указать version override в `modules/domain/lexeme/build.gradle.kts`. Logger использует `1.9.10` без override.

## 2. Pure-Kotlin модуль — есть, единственный

**`modules/core/logger`** — единственный pure-Kotlin модуль в проекте. Это прямой аналог для IS482.

`modules/core/logger/build.gradle.kts` целиком:

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

— 13 строк, ноль dependencies, ноль namespace, ноль android-блока. Никаких `java-library` plugin — только `kotlin.jvm`. `jvmToolchain(17)` зафиксирован.

Source layout:
- `modules/core/logger/src/main/java/me/apomazkin/logger/` — корень пакета модуля **без `.entity` суффикса** (`LogLevel.kt`, `LogSink.kt`, `LexemeLogger.kt`). Это прецедент: pure-Kotlin модуль использует корень пакета `me.apomazkin.<module>` без подпакета — прямо соответствует решению из scope (`package_path_drift_rule`).

Содержимое классов простое (`LogLevel.kt:1-3`):
```kotlin
package me.apomazkin.logger

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
```

Никакого `proguard-rules.pro` у logger'а **нет** (`find` подтверждает). Никаких `consumerProguardFiles`. Это согласуется с тем, что pure-Kotlin модуль не публикует aar и не требует consumer-rules.

## 3. Как logger подключён к графу — критический факт для app

`grep -rn ":modules:core:logger"` показывает четыре consumer'а:

- `modules/core/ui/build.gradle.kts:27` — `api(project("path" to ":modules:core:logger"))` ← **api**, не implementation
- `modules/screen/wordcard/build.gradle.kts:35` — `implementation(...)`
- `core/core-db/build.gradle.kts:31` — `implementation(...)`
- `core/core-db-impl/build.gradle.kts:40` — `implementation(...)`

**`app/build.gradle.kts` НЕ объявляет прямую зависимость на `:modules:core:logger`.** Но `app/.../LoggerModule.kt:9-11` импортирует `me.apomazkin.logger.{LexemeLogger,LogLevel,LogSink}`. Цепочка работает только через **`api` в `modules/core/ui`**: `app → :modules:core:ui (api) → :modules:core:logger`.

**Вывод-факт для IS482:** для `app/`'s mapper (`LexemeApiEntity → Lexeme`) нужен `import me.apomazkin.lexeme.Lexeme`. Если **ни один** из upstream-модулей app'а не объявит `:modules:domain:lexeme` через `api`, импорт не разрешится. По умолчанию feature-модули используют `implementation` (см. wordcard:28-37). Варианты для дизайна (НЕ решение):
1. `app/build.gradle.kts` добавляет собственную `implementation(project(":modules:domain:lexeme"))` — прямой, симметричный consumer.
2. Один из feature-модулей экспонирует `api(project(":modules:domain:lexeme"))` — но тогда лексема растекается как «view» через api, что нарушает изоляцию.

Решение — `infra_design_tree`.

## 4. Три feature-модуля — текущая Gradle-конфигурация

Все три используют **тот же шаблон**: `com.android.library` + `org.jetbrains.kotlin.android` + `org.jetbrains.kotlin.plugin.compose` + `com.google.devtools.ksp`. `compileSdk = 35`, `minSdk = 23`, `targetSdk = 35`, JVM 17. **Нет** общей convention-plugin'ы (build-logic / build-settings есть, но эти модули её не применяют — каждый build.gradle.kts повторяет блок `android { ... }` руками).

- `modules/screen/wordcard/build.gradle.kts:1-25` — плагины + `android { namespace = "me.apomazkin.wordcard" ... }`
- `modules/screen/wordcard/build.gradle.kts:27-44` — dependencies; зависимости на `:modules:core:{di,mate,theme,ui,tools,logger}` + `:core:core-resources` + `:modules:widget:iconDropDowned`. **Нет** зависимости на data-слой (`:core:core-db-api` отсутствует — типы из data резолвятся через UseCase-deps от app'а).
- `modules/screen/quiz/chat/build.gradle.kts:27-50` — близко к wordcard, но дополнительно `:modules:datasource:prefs`, плюс mockk-зависимости в test.
- `modules/screen/dictionaryTab/build.gradle.kts:22-24` — отличается `buildFeatures { buildConfig = true }`; зависит на `:modules:widget:dictionarypicker` и `:modules:widget:chipPicker`.

**Шаблон строки project-dep:** `implementation(project("path" to ":modules:..."))` — все три feature используют синтаксис map (`"path" to "..."`), не однострочный `implementation(project(":..."))`. Это **проектная конвенция** — нужно повторить её и для нового модуля.

**Точные строки куда добавлять dependency на `:modules:domain:lexeme`:**
- `modules/screen/wordcard/build.gradle.kts:28-37` (блок `implementation(project(...))`)
- `modules/screen/quiz/chat/build.gradle.kts:29-38`
- `modules/screen/dictionaryTab/build.gradle.kts:32-41`

## 5. `app/build.gradle.kts` — структура зависимостей

`app/build.gradle.kts:111-172` — секция dependencies. Все project-deps записаны тем же синтаксисом (`project("path" to ":...")`). Зависит явно на все feature-модули (`splash, dictionary, main, dictionaryTab, wordcard, quiztab, quiz:chat, stattab, settingstab`) — но **не** на core/logger, не на core/di, не на core/tools (импорты для них приходят транзитивно).

Mapper и UseCaseImpl живут в:
- `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/{WordCardModule.kt, WordCardUseCaseImpl.kt}`
- `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/{QuizChatModule.kt, QuizChatUseCaseImpl.kt}`
- `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/{DictionaryTabModule.kt, DictionaryTabUseCaseImpl.kt}`

Заметь — нейминг папок неконсистентен (`wordCard` camelCase vs `quizchat`/`dictionarytab` lowercase). Это не часть IS482, но при размещении нового mapper'а нужно учесть: scope упоминает варианты `app/.../polytrainer/mapper/LexemeMapper.kt` (новая папка) или `app/.../polytrainer/di/module/common/LexemeMapper.kt` (papka под DI). Папки `mapper/` или `common/` **сейчас не существует** — обе будут созданы с нуля. Подтверждение: `ls .../di/module/` показывает только feature-папки + `EnvModule.kt`, `LoggerModule.kt`, `ResourceModule.kt`, `widget/`, `flags/`.

## 6. ProGuard / R8

- В корне `app/proguard-rules.pro` есть proguard для finalize-сборки.
- 22 модуля имеют свой `proguard-rules.pro` (видно из find) — но **только в Android-модулях**. Logger (pure-Kotlin) — **не имеет**.
- Pure-Kotlin модуль публикуется как `.jar`, не `.aar` — `consumerProguardFiles` для jvm-плагина не применим. Никаких keep-правил для нового `modules/domain/lexeme` создавать **не нужно**, если в нём не появится reflection / serialization / Dagger-аннотаций. Domain-сущности с `data class Lexeme(...)` без аннотаций безопасны при R8.
- Релевантный прецедент: `core/core-db-impl/proguard-rules.pro` (Room/reflection) + недавний коммит `71ea14d IS472. ProGuard: keep country_data classes for Gson reflection` — оба касаются reflection, не data-классов.

## 7. CI/CD

`.github/workflows/on_feature_push.yml` — основной workflow для веток `IS**` / `MT**`. Три job'а:
- `lint_job` (`:33`) — `./gradlew :app:lintDebug` — единая lint-задача для всего графа через app.
- `test_job` (`:47`) — `./gradlew testDebugUnitTest` — глобальный таргет, **не перечисляет модули**. Новый модуль автоматически попадёт в выполнение, если у него есть `src/test/`.
- `build_job` (`:80`) — `./gradlew assembleDebug -PIS_CI_DEV_BUILD=true` — единственная финальная сборка.

**Никаких модуль-специфичных перечислений в CI нет** — добавление `:modules:domain:lexeme` не требует правок workflow'ов.

Релевант: pure-Kotlin модуль c `kotlin.jvm` plugin **не имеет** task'а `testDebugUnitTest` — у него только `test`. Если на нём появятся юнит-тесты (scope: `modules/domain/lexeme/src/test/...LexemeExtTest.kt`), глобальный `testDebugUnitTest` их **не запустит**. Это потенциальная дыра в CI. Прецедент: у logger'а сейчас тестов нет, так что проблема не всплывала. Решение — `infra_design_tree` (либо переход CI на `./gradlew test testDebugUnitTest`, либо отдельная task, либо умолчание «тесты лежат в `app/src/test/` рядом с маппером», что согласуется со scope для mapper'а — тесты mapper'а в app, тесты `LexemeExt` — в самом domain-модуле, и тогда CI их не запустит без правки).

## 8. Прочие кандидаты на аналог

Проверено через `grep -l "org.jetbrains.kotlin.jvm\|java-library"` по всем `build.gradle.kts` модулей:

- **`modules/core/mate`** — `com.android.library` + `kotlin.android` (`modules/core/mate/build.gradle.kts:2-3`). НЕ pure-Kotlin, несмотря на то что мейт-фреймворк по природе чисто Kotlin'овый. Возможно из-за `androidTestImplementation` в зависимостях. Это **не** аналог.
- `modules/core/di`, `modules/core/theme`, `modules/core/tools`, `modules/core/ui` — все `com.android.library`. Не аналоги.
- `core/core-db-api` — `com.android.library` (`build.gradle.kts:2`). Несмотря на то что API-слой по идее чистый Kotlin (типы данных + интерфейсы Flow), он живёт под Android Library plugin. Не аналог.
- `modules/library/flags`, `modules/datasource/prefs` — Android Library. Не аналоги.

**Итого:** единственный pure-Kotlin образец — `modules/core/logger`. Других нет.

## 9. Существующие планы / следы новой категории

- `modules/domain/` на диске **нет** (`ls` подтверждает).
- В `settings.gradle.kts` **нет** ни комментария-разделителя `//Domain`, ни закомментированных `include` для `domain`.
- В `docs/Backlog.md` § «ВекторныйПиздеж» (контекстно по scope) — там описана сама задача, но это бэклог, не подготовленная инфраструктура.
- TODO-комментарий в `modules/screen/dictionaryTab/build.gradle.kts:39-40`:
  ```
  //TODO kilg 29.06.2025 10:39 избавиться от зависимости
  //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей, и избавиться от сущностей ui
  ```
  — **прямое подтверждение** что IS482 был замыслен ещё в июне 2025. Слой доменных сущностей запланирован, но никаких заготовок (ни папок, ни include'ов) — нет.

## 10. build-logic / build-settings

В `pluginManagement` (`settings.gradle.kts:5`) подключён `includeBuild("build-logic")`. Закомментирована строка `//includeBuild("build-settings")` (`settings.gradle.kts:29`). У трёх feature-модулей **convention-plugin не применяется** — все блоки `android { ... }` дублируются вручную. Значит шаблон для нового модуля — **прямой `build.gradle.kts` без convention-plugin**, по образцу logger'а.

В `app/build.gradle.kts:6-7` лежит TODO от 24.05.2025 что `app.plugin111` не работает — convention-plugin'ы фактически не используются.

## Вердикт

**Аналог: НАЙДЕН.**

`modules/core/logger` — pure-Kotlin модуль (`org.jetbrains.kotlin.jvm` + `jvmToolchain(17)`, без `android { }`, без dependencies, без proguard). 13-строчный `build.gradle.kts`, source layout `src/main/java/me/apomazkin/<module>/` с корневым пакетом без `.entity` суффикса. Modules `:modules:core:ui`, `:modules:screen:wordcard`, `:core:core-db`, `:core:core-db-impl` подключают его как `implementation`/`api(project("path" to ":modules:core:logger"))` — синтаксис map, не однострочный. Это **полностью работающий прецедент** который IS482 может скопировать 1-в-1 на `modules/domain/lexeme`.

**Критическое отличие от logger'а — пакет.** Logger живёт в `me.apomazkin.logger` (без `.entity`). По scope IS482, новый модуль — `me.apomazkin.lexeme` (тоже без `.entity`). Это **прямое следование прецеденту**, а не новый drift.

**Критические факты для `infra_design_tree`:**
1. **`app/` не имеет прямой dep на logger** — работает через `api` в `modules/core/ui`. Для `:modules:domain:lexeme` `app` потребуется собственная прямая зависимость (или явное `api`-экспонирование в одном из feature-модулей — нежелательно).
2. **Категория `modules/domain/`** не существует ни на диске, ни как комментарий. Нужно создать новый комментарий-блок `//Domain` (по аналогии с `//Core`, `//Widget`, `//Libraries`).
3. **CI глобально вызывает `testDebugUnitTest`**, у pure-Kotlin модуля такого task нет — есть `test`. Если в `modules/domain/lexeme/src/test/` появятся тесты, они **не попадут в CI** без правки workflow или alternative task. У logger'а тестов нет, поэтому раньше не вылезало.
4. **Kotlin JVM версия `1.9.10`** vs Kotlin Android `2.0.20` (`settings.gradle.kts:16,21`). Logger живёт на 1.9.10. Совместимость на уровне `.jar`-консьюмеров (`.kotlin_module` метаданные) — нужно перепроверить если в новой версии Kotlin есть breaking changes для метадаты (на 1.9 ↔ 2.0 формально совместимо, но это **звоночек для design**).
5. **Convention-plugins не применяются** в feature-модулях — каждый `build.gradle.kts` руками. Шаблон нового модуля — прямой kts, без convention.
6. **`projectDir`-override не используется** — physical path совпадает с include-path. Соответственно `modules/domain/lexeme/` на диске = `:modules:domain:lexeme` в Gradle.

## log_messages

- Найден аналог: `modules/core/logger` — pure-Kotlin (`kotlin.jvm` + `jvmToolchain(17)`, 13 строк, без android-блока, без proguard).
- `app/` не имеет прямой dep на logger (работает через `api` в `modules/core/ui`); для `:modules:domain:lexeme` потребуется явная dep либо в `app/`, либо через api-экспозицию.
- `modules/domain/` не существует — категория появится впервые: нужен новый комментарий-блок `//Domain` в `settings.gradle.kts` (после `//Old` или перед/после `//Libraries`).

_model: Opus 4.7 (1M context)_
