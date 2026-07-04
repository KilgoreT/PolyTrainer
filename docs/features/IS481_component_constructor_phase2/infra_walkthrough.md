# Infra walkthrough: IS481 component_constructor phase 2

Факты о реальной инфраструктуре проекта, релевантные phase 2: LogTags convention, logger module, widget module `:modules:widget:component_widgets`, DI-обвязка `flowDictionaries`, build/settings.

## 1. Logger module (`:modules:core:logger`)

JVM-only Kotlin модуль (без android plugin) — `build.gradle.kts:1-13`:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
}
```

Содержит только три файла в `src/main/java/me/apomazkin/logger/`:

- `LexemeLogger.kt:3-10` — interface с `log/d/i/w/e`, дефолтный `tag = "###LEXEME###"`:
  ```kotlin
  interface LexemeLogger {
      fun log(level: LogLevel = LogLevel.DEBUG, tag: String = "###LEXEME###", message: String)
      fun d(tag: String = "###LEXEME###", message: String) = log(LogLevel.DEBUG, tag, message)
      ...
  }
  ```
- `LogSink.kt:3-6` — interface (`minLevel`, `write(level, tag, message)`).
- `LogLevel.kt:3` — `enum class LogLevel { DEBUG, INFO, WARNING, ERROR }`.

**Факт:** в модуле `:modules:core:logger` нет файла `LogTags.kt`. Аналогов shared LogTags-объекта в этом модуле не существует.

## 2. LogTags convention (per-module)

14 файлов `LogTags.kt` найдено через `Grep "object LogTags" --glob "*.kt"`. Все живут в потребляющих модулях, package совпадает с module package. Содержимое — `object LogTags` с одной константой `const val X = "###XXX###"` либо короткая module-tag.

Найденные файлы и константы:

| Файл | Константа | Формат |
|---|---|---|
| `app/src/main/java/me/apomazkin/polytrainer/LogTags.kt:3-5` | `APP = "###APP###"` | feature-style |
| `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/LogTags.kt:3-5` | `DB = "###DB###"` | feature-style |
| `modules/core/ui/src/main/java/me/apomazkin/ui/LogTags.kt:3` | (object) | — |
| `modules/core/mate/src/main/java/me/apomazkin/mate/LogTags.kt:3` | (object) | — |
| `modules/widget/dictionaryappbar/.../LogTags.kt:3-5` | `APPBAR = "###APPBAR###"` | feature-style |
| `modules/screen/quiztab/.../LogTags.kt:3-5` | `QUIZ = "###QUIZ###"` | feature-style |
| `modules/screen/quiz/chat/.../LogTags.kt:3-5` | `CHAT = "###CHAT###"` | feature-style |
| `modules/screen/components_manager/.../LogTags.kt:10-12` | `COMPONENTS_MANAGER = "ComponentsManager"` | **БЕЗ `###` markers** |
| `modules/screen/per_dictionary_components/.../LogTags.kt:9-11` | `PER_DICT_COMPONENTS = "PerDictComponents"` | **БЕЗ `###` markers** |
| `modules/screen/wordcard/`, `dictionary/`, `settingstab/`, `stattab/`, `dictionaryTab/` | (объекты) | — |

**Факт-1 (асимметрия формата):** существующие screen-модули для IS481 (`components_manager` / `per_dictionary_components`) используют **module-tag без `###XXX###` marker** (`"ComponentsManager"` / `"PerDictComponents"`). Остальные модули (включая widget `dictionaryappbar`) используют `###СЛОВО###`. Гайд `logging.md` декларирует `###СЛОВО###` как стандарт, но компонентные модули в IS481 от него отклоняются (вероятно сознательно — для двойной оси: module-tag для debug + feature-tag для smoke).

**Факт-2 (нет shared LogTags):** в `:modules:core:logger` файла `LogTags.kt` нет. Все LogTags определены per-module.

## 3. Widget module `:modules:widget:component_widgets`

### 3.1. Текущее состояние

`modules/widget/component_widgets/build.gradle.kts:1-35`:

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.component_widgets"
    compileSdk = 35
    ...
}

dependencies {
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":core:core-resources"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
}
```

Источников в модуле **нет** (только `build.gradle.kts`). Модуль зарегистрирован в `settings.gradle.kts:59` и подключён в `app/build.gradle.kts:132` через `implementation(project("path" to ":modules:widget:component_widgets"))`.

### 3.2. Compose tooling dep

`:modules:core:ui/build.gradle.kts:32-33`:
```kotlin
api(composeLibs.uiToolingPreview)
debugApi(composeLibs.bundles.composePreview)
```

**Факт:** `:modules:core:ui` уже **api**-экспортирует `uiToolingPreview` и через `debugApi` подтягивает bundle `composePreview` (`composeLibs.versions.toml:25` → `["uiTooling", "uiToolingPreview"]`). Любой модуль с `implementation(":modules:core:ui")` уже получает preview-аннотации транзитивно. **Дополнительный `composeLibs.uiToolingPreview` в `:modules:widget:component_widgets` не требуется** — уже доступен через `:modules:core:ui`.

### 3.3. Аналог widget-модуля (`:modules:widget:dictionaryappbar`)

`modules/widget/dictionaryappbar/build.gradle.kts:1-49` — содержит полную обвязку для widget с собственным Mate-инстансом:
- `id("com.google.devtools.ksp")` + `diLibs.dagger` + `daggerCompiler` (DI).
- `:modules:core:di` + `:modules:core:mate` (Mate framework).
- Файлы (`Bash` list): `DictionaryAppBar.kt`, `DictionaryAppBarViewModel.kt`, `DictionaryAppBarNavigationEffect{Handler}.kt`, `DictionaryAppBarNavigator.kt`, `LogTags.kt`, директории `mate/`, `widget/`, `deps/`.

**Факт:** `dictionaryappbar` — пример widget модуля с **собственным Mate** + DI + LogTags. Phase 2 для `component_widgets` НЕ требует Mate (по решению из `02_scope.md` open Q3 — примитивы + callbacks, без mate-зависимостей), поэтому DI/KSP/Mate-deps добавлять НЕ нужно — текущий build.gradle.kts достаточен.

### 3.4. Существующие widget-дубликаты (cleanup target)

В `:modules:screen:components_manager/.../widget/` (8 файлов):
- `ComponentsEmptyStateWidget.kt`, `ComponentTemplateLabel.kt`, `CreateComponentDialog.kt`, `RenameComponentDialog.kt`, `CreateComponentFab.kt`, `DeleteComponentConfirmDialog.kt`, `UserDefinedRowWidget.kt`, `NameErrorLabel.kt`.

В `:modules:screen:per_dictionary_components/.../widget/` (8 файлов, дубликаты по именам, кроме row):
- те же + `PerDictRowWidget.kt` вместо `UserDefinedRowWidget.kt`.

Imports в `modules/screen/components_manager/.../ComponentsManagerScreen.kt:35-40`:
```kotlin
import me.apomazkin.components_manager.widget.ComponentsEmptyStateWidget
import me.apomazkin.components_manager.widget.CreateComponentDialog
import me.apomazkin.components_manager.widget.CreateComponentFab
import me.apomazkin.components_manager.widget.DeleteComponentConfirmDialog
import me.apomazkin.components_manager.widget.RenameComponentDialog
import me.apomazkin.components_manager.widget.UserDefinedRowWidget
```

**Факт:** screen-модули импортируют widgets из своего `widget/` package, не из shared `:modules:widget:component_widgets`. После выноса import paths поменяются на `me.apomazkin.component_widgets.*`.

## 4. DI-обвязка для `flowDictionaries`

### 4.1. Существующий паттерн (`flowDictionaryList`)

`core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt:65` — interface метод:
```kotlin
interface DictionaryApi {
    ...
    fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
}
```

`DictionaryApiEntity.kt:5-12`:
```kotlin
data class DictionaryApiEntity(
    val id: Long,
    val numericCode: Int?,
    val name: String,
    val addDate: Date,
    val changeDate: Date? = null,
    val deleteDate: Date? = null,
)
```

### 4.2. CoreDbApi resolved через CoreDbProvider

`core/core-db-api/.../CoreDbProvider.kt:3-12`:
```kotlin
interface CoreDbProvider {
    fun getCoreDbApi(): CoreDbApi
    fun getDbInstance(): CoreDbApi.DbInstance
    fun getDictionaryApi(): CoreDbApi.DictionaryApi
    fun getWordApi(): CoreDbApi.WordApi
    fun getTermApi(): CoreDbApi.TermApi
    fun getLexemeApi(): CoreDbApi.LexemeApi
    fun getQuizApi(): CoreDbApi.QuizApi
    fun getStatisticApi(): CoreDbApi.StatisticApi
}
```

`app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt:42-58`:
```kotlin
@Component(
    modules = [AppModule::class],
    dependencies = [CoreDbProvider::class]
)
@Singleton
interface AppComponent { ... }
```

**Факт:** `CoreDbApi.DictionaryApi` уже Dagger-injectable во всём app — `CoreDbProvider.getDictionaryApi()` exposed как component dependency. Никакой новой DI bindings для `dictionaryApi.flowDictionaryList()` не требуется.

### 4.3. Текущие use-сайты `flowDictionaryList`

`Grep "flowDictionaryList"` → 8 файлов, в т.ч.:
- `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImpl.kt:38-48` — map-обёртка:
  ```kotlin
  override fun flowDictionaryList(): Flow<List<DictionaryListItem>> {
      return dictionaryApi.flowDictionaryList().map { list ->
          list.map { entity -> DictionaryListItem(...) }
      }
  }
  ```
- `app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt:15`.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/splash/SplashUseCaseImpl.kt:10`.

**Факт:** все консьюмеры инжектят `CoreDbApi.DictionaryApi` через ctor (`private val dictionaryApi: CoreDbApi.DictionaryApi`) и вызывают `flowDictionaryList()` напрямую либо с маппингом в domain-тип. Для phase 2 `02_scope.md:194` уже зафиксировано: «UseCaseImpl делегирует на `dictionaryApi.flowDictionaryList()` без mapping (F026)» — возвращает `Flow<List<DictionaryApiEntity>>` как есть.

### 4.4. ComponentsManagerUseCaseImpl (current ctor)

`app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt:42-46`:
```kotlin
class ComponentsManagerUseCaseImpl @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) : ComponentsManagerUseCase { ... }
```

**Факт:** для `flowDictionaries()` нужно добавить новый ctor-параметр `private val dictionaryApi: CoreDbApi.DictionaryApi` — Dagger сам резолвит через `CoreDbProvider.getDictionaryApi()`. Новых `@Provides` / `@Binds` не нужно.

### 4.5. ComponentsManagerModule (current)

`app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerModule.kt:7-12`:
```kotlin
@Module
interface ComponentsManagerModule {
    @Binds
    fun bindComponentsManagerUseCase(impl: ComponentsManagerUseCaseImpl): ComponentsManagerUseCase
}
```

`PerDictionaryComponentsModule.kt:7-14` — симметрично, один `@Binds`.

**Факт:** оба DI-модуля — только `@Binds` UseCase. Никакой `@Provides flowDictionaries` не требуется — `dictionaryApi` подтянется через ctor inject UseCaseImpl.

## 5. FlowHandler pattern (DictionariesFlowHandler — parity)

### 5.1. Существующий `AllUserDefinedTypesFlowHandler`

`modules/screen/components_manager/.../mate/AllUserDefinedTypesFlowHandler.kt:22-59`:

```kotlin
class AllUserDefinedTypesFlowHandler @Inject constructor(
    private val useCase: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        if (effect is DatasourceEffect.LoadAllUserDefinedTypes) {
            val s = scope ?: return
            unsubscribe()
            subscribe(s, send ?: consumer)
        }
    }

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
        job = scope.launch {
            useCase.flowAllUserDefinedTypes()
                .catch { e -> ... send(Msg.TypesLoadFailed(e)) }
                .collectLatest { snapshot -> send(Msg.TypesLoaded(snapshot)) }
        }
    }
}
```

### 5.2. Wiring в ViewModel

`modules/screen/components_manager/.../ComponentsManagerViewModel.kt:29-48`:
```kotlin
class ComponentsManagerViewModel @AssistedInject constructor(
    @Assisted navigator: ComponentsManagerNavigator,
    datasourceHandler: DatasourceEffectHandler,
    flowHandler: AllUserDefinedTypesFlowHandler,
    ...
) {
    private val stateHolder = Mate(
        initState = ComponentsManagerScreenState(isLoading = true),
        initEffects = emptySet(),
        coroutineScope = viewModelScope,
        reducer = ComponentsManagerReducer(),
        effectHandlerSet = setOf(datasourceHandler, flowHandler, uiHandler, ...),
    )
}
```

**Факт:** `MateFlowHandler` (`:modules:core:mate/.../MateFlowHandler.kt:6`) — standard pattern. Mate framework `subscribe()` вызывает на init (`Mate.kt:50,55` — `filterIsInstance<MateFlowHandler>`). Новый `DictionariesFlowHandler` будет zero-ceremony: ctor с `dictionaryApi` (или через UseCase делегирование), `subscribe()` → `dictionaryApi.flowDictionaryList().collectLatest { send(Msg.DictionariesLoaded(it)) }`, добавление в `effectHandlerSet` ViewModel.

### 5.3. QuizPickerFlowHandler (другой пример subscribe-only handler)

`modules/screen/quiz/chat/.../logic/QuizPickerFlowHandler.kt:27-50` — простейшая subscribe-only форма (без `runEffect` re-subscribe), служит вторым reference pattern.

## 6. Migration / QuizConfigDao logger context

### 6.1. Migration_012_to_013 — без логов

`core/core-db-impl/.../room/migrations/Migration_012_to_013.kt:1-175` — **logger полностью отсутствует** (нет `LexemeLogger` импорта, нет логов в `migrate()` или per-step функциях). 9 атомарных шагов уже структурно выделены в private методы (`renameComponentTypesRemoveDate` / `addComponentTypesNewColumns` / ... / `rewriteImageJson`) и вызываются последовательно в `migrateImpl()`.

**Факт:** инфра для логирования в migration отсутствует — нужно добавить `LexemeLogger` параметр (или фабричный паттерн), потому что `Migration` — `object`, не Dagger-injectable. Аналогов «инжектируемой миграции» в проекте нет.

### 6.2. CoreDbApiImpl уже имеет logger

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:51,58,144`:
```kotlin
import me.apomazkin.logger.LexemeLogger
...
private val logger: LexemeLogger,  // line 58
...
private val logger: LexemeLogger,  // line 144 — inner class scope
```

`core-db-impl/build.gradle.kts:40`:
```kotlin
implementation(project("path" to ":modules:core:logger"))
```

**Факт:** `:core:core-db-impl` уже имеет dep на `:modules:core:logger` и `CoreDbApiImpl` уже инжектит `LexemeLogger`. Для добавления feature-tag логов в orchestration `editComponentType` (внутри `LexemeApiImpl` inner class) — инфра готова, нужно только использовать.

### 6.3. QuizConfigDao — DAO без logger

`core/core-db-impl/.../room/dao/QuizConfigDao.kt:49` — `suspend fun updateComponentRefs(id: Long, newRefs: String)` — Room `@Dao` interface, **logger inject невозможен** (Room генерирует impl). Логирование `updateComponentRefs` (before/after refs count из `02_scope.md` § migration_logging) должно жить **в LexemeApiImpl orchestration call site** (где DAO method вызывается из `cascadeRenameInQuizConfigs` `CoreDbApiImpl.kt:567-579` и `softDeleteComponentType` `:626-635`), не в самом DAO.

**Факт:** «логи в `QuizConfigDao.updateComponentRefs`» из `02_scope.md` буквально невозможны (Room DAO). Логирование должно быть в caller-сайте — `LexemeApiImpl.cascadeRenameInQuizConfigs()` / `softDeleteComponentType()` блоках, обёртывая `quizConfigDao.updateComponentRefs(...)` вызовы. Это уточнение для `infra_design_tree`.

## 7. Module registration в settings/app

`settings.gradle.kts:34-59` (выдержки):
```kotlin
include(":modules:core:logger")
...
include(":modules:screen:components_manager")
include(":modules:screen:per_dictionary_components")
...
include(":modules:widget:component_widgets")
```

`app/build.gradle.kts:127-132`:
```kotlin
implementation(project("path" to ":modules:screen:components_manager"))
implementation(project("path" to ":modules:screen:per_dictionary_components"))
...
implementation(project("path" to ":modules:widget:component_widgets"))
```

**Факт:** все три модуля уже зарегистрированы в `settings.gradle.kts` и подключены в `app`. Никаких изменений в `settings.gradle.kts` или `app/build.gradle.kts` deps для phase 2 не требуется.

## 8. Build deps screen-модулей (для добавления `component_widgets` import)

`modules/screen/components_manager/build.gradle.kts:27-46`:
```kotlin
dependencies {
    implementation(project("path" to ":modules:core:di"))
    implementation(project("path" to ":modules:core:mate"))
    ...
    implementation(project("path" to ":modules:core:theme"))
    implementation(project("path" to ":modules:core:ui"))
    implementation(project("path" to ":modules:core:logger"))
    implementation(project("path" to ":modules:core:tools"))
    implementation(project("path" to ":modules:domain:lexeme"))
    implementation(project("path" to ":core:core-resources"))
    ...
}
```

`per_dictionary_components/build.gradle.kts:27-46` — симметрично.

**Факт:** ни один screen-модуль НЕ имеет deps на `:modules:widget:component_widgets`. Для использования shared widgets из screen-модулей нужно **добавить** `implementation(project("path" to ":modules:widget:component_widgets"))` в build.gradle.kts обоих screen-модулей.

## Вердикт

Аналог: **частично найден**.

- **Logger module** — существует, JVM-only, без LogTags файла; новый shared `:modules:core:logger/LogTags.kt` создаётся первым в этом модуле (аналогов нет).
- **Per-module LogTags** — 14 файлов, две convention'ы: `###XXX###` (12 файлов) и module-tag без markers (`components_manager`, `per_dictionary_components`). Аналог нового файла-overlay'a `:modules:core:logger/LogTags.kt` с feature-tag для cross-module use — отсутствует, шаблон создаётся впервые.
- **Widget module `:modules:widget:component_widgets`** — build.gradle.kts уже создан, deps на `theme/ui/lexeme/core-resources`, transitively получает compose tooling через `:modules:core:ui`. Источников нет, наполнение начинается с нуля. Аналог widget-модуля **с собственным Mate** — `:modules:widget:dictionaryappbar` (но phase 2 Mate не нужен).
- **DI flowDictionaries** — `CoreDbApi.DictionaryApi.flowDictionaryList()` уже существует (`CoreDbApi.kt:65`), `DictionaryApi` exposed через `CoreDbProvider.getDictionaryApi()` (`CoreDbProvider.kt:6`), Dagger ресолвит через `@Component(dependencies = [CoreDbProvider::class])` в `AppComponent.kt:42-58`. Никаких новых `@Provides`/`@Binds` не нужно — достаточно добавить ctor-параметр в UseCaseImpl. Аналог зрелый, copy-paste из `DictionaryUseCaseImpl.kt:38-48`.
- **FlowHandler pattern** — `AllUserDefinedTypesFlowHandler` (`modules/screen/components_manager/.../mate/AllUserDefinedTypesFlowHandler.kt:22-59`) — точный template для нового `DictionariesFlowHandler` (subscribe + send Msg pattern). Аналог 1:1.
- **Migration logger** — Migration_012_to_013 **без логов и без logger-инжекта**. Аналогов «migration с logger» в проекте нет — паттерн создаётся впервые (потребует либо ctor-параметр у Migration object, либо top-level `var migrationLogger: LexemeLogger?` сетимый из app init).
- **QuizConfigDao логи** — буквально в DAO невозможно (Room generates impl). Caller-side logging в `LexemeApiImpl.cascadeRenameInQuizConfigs` — соответствует существующему паттерну (logger уже инжектен в CoreDbApiImpl).

Все инфра-затраты phase 2 — небольшие точечные правки внутри существующей структуры, **кроме** migration logger (новый pattern) и shared LogTags файла (новое место).

## log_messages

- infra_walkthrough собран: 5 факт-блоков (logger / LogTags / widget module / DI / FlowHandler) + 2 dataflow-блока (migration logger / QuizConfigDao caller-side) на основе grep + Read 22 файлов.
- 2 уточнения для design_tree: (1) Migration_012_to_013 не имеет logger-инжекта — pattern создаётся впервые; (2) "логи в QuizConfigDao.updateComponentRefs" из 02_scope.md невыполнимы буквально, реализуется в caller-side `LexemeApiImpl`.
- Подтверждено: compose-tooling уже транзитивно доступен из `:modules:core:ui` (debugApi composePreview bundle); явный dep в `component_widgets/build.gradle.kts` не нужен.

_model: claude-opus-4-7[1m]_
