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
    │ modules: AppModule (15 sub-модулей, без LoggerModule)
    │ dependencies: CoreDbProvider
    │ @BindsInstance: Context, LexemeLogger
    │ implements: AppProvider
    │ provides: все UseCase, ViewModel factories, Prefs, Resources
    │
    ↓ appComponent
    │
CoreInteractorComponent [@Singleton, ленивый]
    │ modules: RepositoryModule, ScenarioModule
    │ @BindsInstance: Context
    │ implements: CoreInteractorApi
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
interface AppComponent : AppProvider, CoreDbProvider
```

AppModule включает 15 модулей: SplashModule, DictionaryModule, PrefsProviderModule и т.д. LoggerModule вынесен в LoggerComponent.

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

### RepositoryModule (core-interactor)

| Provides | Тип | Scope |
|----------|-----|-------|
| `provideCoreDbApi(context)` | CoreDbApi | @Singleton |

Получает через `CoreDbComponent.get(context).getCoreDbApi()`.

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
| QuizApiImpl | wordDao |
| StatisticApiImpl | wordDao |

## Доступ к зависимостям из UI

```kotlin
// В Composable (Service Locator паттерн)
val appComponent = (context as App).appComponent
val logger = appComponent.getLogger()

// Через MainUiDepsProvider (инжектится в конструктор)
class MainUiDepsProvider(
    private val logger: LexemeLogger,
    private val settingsTabUseCase: SettingsTabUseCase,
    // ...
)
```

## Конвенции

1. Каждый core-модуль имеет свой Component + Provider interface
2. @Singleton на уровне Component, не Module
3. @BindsInstance для Context и конфигурационных зависимостей
4. @Binds для interface → impl биндинга (ApiModule)
5. @Provides для создания объектов (RoomModule, LoggerModule)
6. Component.Factory вместо Builder
7. Double-checked locking в companion object для синглтонов
