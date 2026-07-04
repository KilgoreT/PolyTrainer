# Dagger DI — архитектура

## Граф компонентов

```
LoggerComponent [нет зависимостей, создаётся первым]
    │ modules: LoggerModule
    │ provides: LexemeLogger
    │
    ↓ LexemeLogger (@BindsInstance)
    │
RoomComponent [@Singleton]
    │ modules: RoomModule, ApiModule
    │ @BindsInstance: Context, LexemeLogger
    │ implements: CoreDbProvider
    │ provides: Database, WordDao, CoreDbApi, все *Api
    │
    ↓ CoreDbProvider
    │
CoreDbComponent [без scope]
    │ dependencies: CoreDbProvider
    │ implements: CoreDbProvider (прокси)
    │ init(context, logger) — первый вызов
    │ get(context) — последующие вызовы
    │
    ↓ CoreDbProvider
    │
AppComponent [@Singleton]
    │ modules: AppModule (13 sub-модулей)
    │ dependencies: CoreDbProvider
    │ @BindsInstance: Context, LexemeLogger
    │ provides: UseCase'ы, AssistedFactory всех ViewModel,
    │           EnvParams, Logger
    │ ViewModel'ы — через @AssistedInject + AssistedFactory (single pattern)
```

## Порядок создания (App.onCreate)

```kotlin
// 1. LoggerComponent — без зависимостей, создаётся первым
val logger = LoggerComponent.create().getLogger()

// 2. CoreDbComponent.init — создаёт RoomComponent с logger
CoreDbComponent.init(this, logger)

// 3. AppComponent — получает CoreDbProvider + logger
DaggerAppComponent.factory().create(
    appContext = this,
    logger = logger,
    coreDbProvider = DaggerAppComponent_CoreDbDependenciesComponent
        .builder()
        .coreDbProvider(CoreDbComponent.init(this, logger))
        .build(),
)
```

LoggerComponent решает циклическую зависимость: logger создаётся до RoomComponent и AppComponent, передаётся обоим через `@BindsInstance`.

## Компоненты — детали

### RoomComponent

**Файл:** `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/RoomComponent.kt`

```kotlin
@Singleton
@Component(modules = [RoomModule::class, ApiModule::class])
interface RoomComponent : CoreDbProvider {
    @Component.Factory
    interface RoomComponentFactory {
        fun create(@BindsInstance context: Context): RoomComponent
    }
}
```

Синглтон через `companion object` с double-checked locking.

### CoreDbComponent

**Файл:** `core/core-db/src/main/java/me/apomazkin/core_db/di/CoreDbComponent.kt`

Прокси-обёртка. `dependencies = [CoreDbProvider::class]`. Не добавляет модулей — просто пробрасывает CoreDbProvider.

### AppComponent

**Файл:** `app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt`

```kotlin
@Singleton
@Component(
    modules = [AppModule::class],
    dependencies = [CoreDbProvider::class]
)
interface AppComponent {
```

AppModule включает 13 модулей. LoggerModule вынесен в LoggerComponent. AppComponent не наследует AppProvider/CoreDbProvider — DB API не экспонируется наружу.

## Модули

### RoomModule (core-db-impl)

| Provides | Тип | Scope |
|----------|-----|-------|
| `provideDatabase(context)` | Database | @Singleton |
| `provideWordDao(db)` | WordDao | — |

### ApiModule (core-db-impl)

@Binds интерфейс. Биндит все `CoreDbApiImpl.*` → `CoreDbApi.*`:
- CoreDbApiImpl → CoreDbApi
- DbInstanceImpl → DbInstance
- DictionaryApiImpl → DictionaryApi
- WordApiImpl → WordApi
- TermApiImpl → TermApi
- LexemeApiImpl → LexemeApi
- QuizApiImpl → QuizApi
- StatisticApiImpl → StatisticApi

### LoggerModule (app, используется в LoggerComponent)

| Provides | Тип |
|----------|-----|
| `provideSinks()` | List\<LogSink\> |
| `provideLogger(sinks)` | LexemeLogger |

Вынесен из AppModule в отдельный LoggerComponent — создаётся первым, logger передаётся через @BindsInstance в RoomComponent и AppComponent.

## @Inject constructor в core-db-impl

Все inner-классы `CoreDbApiImpl` получают `wordDao: WordDao` через @Inject:

| Класс | Зависимости |
|-------|------------|
| CoreDbApiImpl | wordDao, logger |
| DbInstanceImpl | db: Database |
| DictionaryApiImpl | wordDao |
| TermApiImpl | wordDao |
| WordApiImpl | wordDao |
| LexemeApiImpl | wordDao |
| QuizApiImpl | wordDao, logger |
| StatisticApiImpl | wordDao |

## ViewModel инфраструктура (core/di)

Модуль `modules/core/di` содержит helper для AssistedFactory ViewModel'ей.

### Файлы

```
modules/core/di/
└── ViewModelHelper.kt    — viewModelFactory { ... } helper для AssistedInject ViewModel
```

`viewModelFactory { ... }` оборачивает `ViewModelProvider.Factory` вокруг `AssistedFactory.create(...)`. Используется в composable.

## Паттерн ViewModel: @AssistedInject + @AssistedFactory

**Все** ViewModel в проекте инжектятся через `@AssistedInject`. Даже если runtime аргументов нет — `Navigator` приходит через `@Assisted`, остальные зависимости через обычный constructor injection.

**ViewModel:**
```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(wordId: Long, navigator: WordCardNavigator): WordCardViewModel
    }
}
```

**Module (в app)** — только `@Binds` для UseCase, биндинг ViewModel НЕ нужен:
```kotlin
@Module
interface WordCardModule {
    @Binds
    fun bindUseCase(impl: WordCardUseCaseImpl): WordCardUseCase
}
```

**AppComponent** экспонирует Factory:
```kotlin
interface AppComponent {
    fun getWordCardViewModelFactory(): WordCardViewModel.Factory
}
```

**build.gradle.kts (screen модуль):**
```kotlin
plugins {
    id("com.google.devtools.ksp")  // ← нужен KSP для @AssistedFactory
}

dependencies {
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
}
```

**Composable:**
```kotlin
@Composable
fun WordCardScreen(
    wordId: Long,
    factory: WordCardViewModel.Factory,
    navigator: WordCardNavigator,
    viewModel: WordCardViewModel = viewModel(
        key = "wordCard_$wordId",
        factory = viewModelFactory { factory.create(wordId, navigator) }
    ),
) { ... }
```

**CompositionRootImpl** — собирает Factory + Navigator и создаёт screen:
```kotlin
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    // ...
) : CompositionRoot {
    @Composable
    override fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit) {
        val navigator = remember(onBackPress) { WordCardNavigatorImpl(onBackPress) }
        WordCardScreen(
            wordId = wordId,
            factory = wordCardViewModelFactory,
            navigator = navigator,
        )
    }
}
```

## Конвенции

1. Каждый core-модуль имеет свой Component + Provider interface
2. @Singleton на уровне Component, не Module
3. @BindsInstance для Context и конфигурационных зависимостей
4. @Binds для interface → impl биндинга (ApiModule)
5. @Provides для создания объектов (RoomModule, LoggerModule)
6. Component.Factory вместо Builder
7. Double-checked locking в companion object для синглтонов
8. **ViewModel — `@AssistedInject` + `@AssistedFactory`** для всех экранов
9. **AppComponent экспонирует `getXxxViewModelFactory()`** для каждой ViewModel
10. KSP processor нужен в каждом screen модуле с @AssistedFactory (handlers + ViewModel)
