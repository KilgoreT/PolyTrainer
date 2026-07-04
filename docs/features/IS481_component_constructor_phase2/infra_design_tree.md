# Infra design tree: IS481 component_constructor phase 2

Scope (по `02_scope.md` § Sub-flow → Infrastructure + `infra_walkthrough.md`):

1. **LogTags.COMPONENT_CONSTRUCTOR** — новая shared константа `"###ComponentConstructor###"` в новом файле `:modules:core:logger/LogTags.kt` (по walkthrough §1: файла нет в logger-модуле, аналогов shared LogTags-объекта в нём не существует).
2. **Build deps** — добавить `implementation(project(":modules:widget:component_widgets"))` в build-файлы двух screen-модулей (по walkthrough §8: ни один screen-модуль НЕ имеет deps на shared widget).
3. **Migration logger** — точечная правка `Migration_012_to_013.kt`: импортировать shared `LogTags.COMPONENT_CONSTRUCTOR` (Узел 1) + `android.util.Log.d(LogTags.COMPONENT_CONSTRUCTOR, ...)` per-step (9 шагов по списку из `02_scope.md` § Аспекты `migration_logging` → F020). По walkthrough §6.1: `Migration` — `object`, не Dagger-injectable; аналогов «инжектируемой миграции» в проекте нет; минимальное вмешательство — `android.util.Log` напрямую с shared feature-tag, без архитектурного перелопачивания (best-guess для phase 2 — см. инструкцию шага). Dep на `:modules:core:logger` уже присутствует в `:core:core-db-impl/build.gradle.kts:40`, отдельной gradle-правки не требуется.

**Out of scope (этой phase / этого шага)**:
- DI binding для `flowDictionaries` — по walkthrough §4 никаких `@Provides`/`@Binds` не нужно, `dictionaryApi` подтянется через ctor-инжект `UseCaseImpl` (это **business** sub-flow, не infra).
- Compose-tooling dep в `:modules:widget:component_widgets/build.gradle.kts` — по walkthrough §3.2 уже транзитивно доступен через `:modules:core:ui` (debugApi composePreview bundle).
- Module registration в `settings.gradle.kts` / `app/build.gradle.kts` — по walkthrough §7 все три модуля уже зарегистрированы.
- Caller-site логирование `quizConfigDao.updateComponentRefs` в `LexemeApiImpl.cascadeRenameInQuizConfigs` — относится к **data** sub-flow (orchestration внутри `CoreDbApiImpl`), не к infra.

## Часть 1: Граф (YAML)

```yaml
- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/logger/src/main/java/me/apomazkin/logger/LogTags.kt
  action: "+"
  depends: []

- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/build.gradle.kts
  action: "~"
  depends: []

- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/build.gradle.kts
  action: "~"
  depends: []

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt
  action: "~"
  depends: [1]
```

Узлы 1/2/3 независимы. Узел 4 импортирует `LogTags.COMPONENT_CONSTRUCTOR` из Узла 1 — его реализация должна следовать за Узлом 1. Узлы 2/3 не пересекаются с 1/4 (build-file правки vs Kotlin-файлы), могут реализовываться параллельно.

## Часть 2: Детали изменений

### Узел 1 — `:modules:core:logger/.../LogTags.kt` [+]

**Назначение.** Shared feature-tag константа для smoke-фильтрации логов phase 2 IS481 (Migration + UseCaseImpl + screen reducers). По решению F014 (зафиксировано в `02_scope.md` § Логгер): отдельный shared файл в `:modules:core:logger`, потому что feature-tag используется в 4 потребителях (`components_manager`, `per_dictionary_components`, `core-db-impl`, `app`) и все они уже имеют dep на `:modules:core:logger`. Per-module `LogTags.kt` в screen-модулях остаются как есть (двойная ось: module-tag для debug + feature-tag для smoke).

**Package.** `me.apomazkin.logger` (совпадает с package остальных файлов модуля — `LexemeLogger.kt` / `LogLevel.kt` / `LogSink.kt`, walkthrough §1).

**Ключевые сигнатуры.**
```kotlin
package me.apomazkin.logger

object LogTags {
    const val COMPONENT_CONSTRUCTOR: String = "###ComponentConstructor###"
}
```

**Согласие с гайдами.**
- `logging.md` — «###СЛОВО### — тройные решётки, БОЛЬШИЕ буквы; слово = область/модуль; каждый модуль имеет LogTags.kt; использовать ТОЛЬКО константы».
- `naming.md` — `object LogTags { const val X = "###XXX###" }` — convention из walkthrough §2 (12 из 14 файлов используют именно этот формат).

**Note (асимметрия formatting).** По walkthrough §2 Факт-1 — два существующих screen-LogTags (`COMPONENTS_MANAGER = "ComponentsManager"` / `PER_DICT_COMPONENTS = "PerDictComponents"`) намеренно используют module-tag без `###` markers для двойной оси. Новый shared LogTags идёт по стандартному `###СЛОВО###` format — это и есть feature-tag-ось (для smoke-фильтрации `adb logcat | grep '###ComponentConstructor###'`).

### Узел 2 — `:modules:screen:components_manager/build.gradle.kts` [~]

**Было** (`build.gradle.kts:27-46`):
```kotlin
dependencies {
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":modules:core:tools"))
    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
    implementation(composeLibs.activityCompose)

    testImplementation(testLibs.junit)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.coroutinesTest)
}
```

**Стало.** Добавить одну строку рядом с прочими `:modules:*` deps:
```kotlin
implementation(project("path" to ":modules:widget:component_widgets"))
```

**Псевдокод правки** (точка вставки — после строки с `:modules:domain:lexeme`, чтобы держать widget-deps в конце `:modules:*` блока перед `:core:*`):
```
... :modules:domain:lexeme
+ :modules:widget:component_widgets
... :core:core-resources
```

**Причина.** По walkthrough §8 Факт: ни один screen-модуль не имеет dep на shared widget. После выноса 8 widget-файлов из `widget/` package в `:modules:widget:component_widgets` (UI sub-flow) screen-модуль должен импортировать их из `me.apomazkin.component_widgets.*` — без gradle-dep import не разрешится.

### Узел 3 — `:modules:screen:per_dictionary_components/build.gradle.kts` [~]

**Было.** Симметрично узлу 2 (walkthrough §8: deps структура идентична `components_manager`).

**Стало.** Та же одна строка вставки:
```kotlin
implementation(project("path" to ":modules:widget:component_widgets"))
```

**Точка вставки.** После `:modules:domain:lexeme` — parity с узлом 2 для maintenance-однообразия.

**Причина.** Та же, что и у узла 2 — UI sub-flow выносит widgets (включая `PerDictRowWidget.kt`) в shared, import paths переезжают на `me.apomazkin.component_widgets.*`.

### Узел 4 — `:core:core-db-impl/.../Migration_012_to_013.kt` [~]

**Было** (`Migration_012_to_013.kt:42-80`):
```kotlin
object Migration_012_to_013 : Migration(12, 13) {

    override fun migrate(connection: SQLiteConnection) {
        migrateImpl(connection, failAfterStep = null)
    }

    internal fun migrateImpl(connection: SQLiteConnection, failAfterStep: Int? = null) {
        renameComponentTypesRemoveDate(connection)
        maybeFail(1, failAfterStep)

        addComponentTypesNewColumns(connection)
        maybeFail(2, failAfterStep)
        // ... ещё 7 шагов с maybeFail
    }
    ...
}
```

**Стало.** Импортировать shared `me.apomazkin.logger.LogTags` (single source of truth для feature-tag — Узел 1) + `android.util.Log` и логировать после каждого `private fun XxxStep(c)` блока: `android.util.Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step N <name>: <metric>")`. Dep `:modules:core:logger` уже присутствует в `:core:core-db-impl/build.gradle.kts:40` (walkthrough §6.2) — отдельной gradle-правки не нужно.

**Псевдокод (delta-only)**:
```kotlin
package me.apomazkin.core_db_impl.room.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import me.apomazkin.logger.LogTags

object Migration_012_to_013 : Migration(12, 13) {

    override fun migrate(connection: SQLiteConnection) {
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "migration M12→M13: start")
        migrateImpl(connection, failAfterStep = null)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "migration M12→M13: done")
    }

    internal fun migrateImpl(connection: SQLiteConnection, failAfterStep: Int? = null) {
        renameComponentTypesRemoveDate(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 1 renameRemoveDate: ok")
        maybeFail(1, failAfterStep)

        addComponentTypesNewColumns(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 2 addTypeCols: ok")
        maybeFail(2, failAfterStep)

        dropUniqueComponentTypesDictName(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 3 dropUniqueDictName: ok")
        maybeFail(3, failAfterStep)

        addComponentValuesNewColumns(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 4 addValueCols: ok")
        maybeFail(4, failAfterStep)

        dropUniqueComponentValuesLexemeType(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 5 dropUniqueLexemeType: ok")
        maybeFail(5, failAfterStep)

        createComponentValuesLexemeIdIndex(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 6 createLexemeIdIndex: ok")
        maybeFail(6, failAfterStep)

        consolidateLongTextTemplateKey(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 7 consolidateLongText: ok")
        maybeFail(7, failAfterStep)

        rewriteTextJson(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 8 rewriteTextJson: ok")
        maybeFail(8, failAfterStep)

        rewriteImageJson(connection)
        Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step 9 rewriteImageJson: ok")
        maybeFail(9, failAfterStep)
    }

    // private fun maybeFail / step-функции — без изменений
}
```

**Метрики на шаг (best-guess для phase 2 implementation).** Где SQL изменяет rows (steps 1/2/4/7/8/9) — желательно через `connection.prepare("SELECT changes()").use { ... }` снять `rows affected` после execSQL и логировать как часть message. Где DDL (steps 3/5/6) — фиксировать `ok` / `failed` (DDL не возвращает count). Конкретный механизм счёта rows вне scope этого design — реализатор выбирает (либо логирует только step-name без count, MVP-минимум).

**Согласие с гайдами.**
- `logging.md` — «Запрещено android.util.Log напрямую; всё через LexemeLogger».
  **Возражение к собственному design'у:** правило `logging.md` нарушается. Альтернативы:
  1. Принять нарушение в migration (object без DI; alternative — top-level `var migrationLogger: LexemeLogger?` сетимый из app init — нарушает purity object'а ещё сильнее).
  2. Перевести Migration в `class` с ctor-параметром `logger: LexemeLogger` — серьёзный архитектурный рефактор `RoomDatabase.Builder.addMigrations(...)` cite, выходит за scope phase 2 infra.

  **Best-guess (consistent с инструкцией шага):** оставить `android.util.Log` с shared `LogTags.COMPONENT_CONSTRUCTOR`, отметить как **known violation** в комментарии («// IS481 phase 2: android.util.Log используется потому что Migration — object без DI; logger-инжект потребует архитектурного рефактора, см. backlog»). Если architect-ревьюер на iter 2 потребует — добавить в `docs/Backlog.md` отдельным пунктом «вытащить Migration в class с logger ctor» (per ЖЕЛЕЗНОЕ ПРАВИЛО архив-предложений).

- `naming.md` — использование `LogTags.COMPONENT_CONSTRUCTOR` константы из shared `:modules:core:logger` — соответствует convention; формат строки `###ComponentConstructor###` зафиксирован в Узле 1.

## log_messages

- iter 2 закрыл F031/F032: Узел 4 переключён на `LogTags.COMPONENT_CONSTRUCTOR` import из Узла 1 (хардкод `private const val TAG` удалён, фиктивное «circular concerns» обоснование снято — dep `:modules:core:logger` уже есть в `:core:core-db-impl/build.gradle.kts:40`).
- DAG обновлён: `Узел 4 depends: [1]`; alternative branch (duplication) убрана — single source of truth для implementation.

_model: claude-opus-4-7[1m]_
