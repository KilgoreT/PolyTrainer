# Dagger DI — полный анализ графа

## Схема инициализации (App.onCreate)

```
App.onCreate()
│
├─ 1. LoggerComponent.create()
│     └── LexemeLogger
│
├─ 2. CoreDbComponent.init(context, logger)
│     └── RoomComponent.get(context, logger)
│           └── Database, WordDao, CoreDbApi.*
│
└─ 3. DaggerAppComponent.factory().create(context, logger, coreDbProvider)
       └── AppComponent (все UseCase, Prefs, Resources, Env)
```

## Компоненты

### 1. LoggerComponent

| | |
|---|---|
| Файл | `app/.../di/LoggerComponent.kt` |
| Scope | нет |
| Modules | LoggerModule |
| Dependencies | нет |
| Factory | `companion object { fun create() }` |
| Exposes | `getLogger(): LexemeLogger` |
| Создаётся | `App.onCreate()` — первым |

**Нюанс:** создан отдельно чтобы разорвать циклическую зависимость. Logger нужен RoomComponent, но раньше жил в AppComponent который зависит от RoomComponent.

### 2. RoomComponent

| | |
|---|---|
| Файл | `core/core-db-impl/.../di/RoomComponent.kt` |
| Scope | `@Singleton` |
| Modules | RoomModule, ApiModule |
| Dependencies | нет |
| @BindsInstance | `Context`, `LexemeLogger` |
| Implements | `CoreDbProvider` |
| Создаётся | `CoreDbComponent.init()` → `RoomComponent.get()` |

**Синглтон:** `lateinit var` + `synchronized` double-checked locking.

**Что провайдит:**
- `Database` — Room БД с миграциями 1→2...10→11 (@Singleton через RoomModule)
- `WordDao` — единственный DAO (40+ методов, god-object)
- Все `CoreDbApi.*` через inner classes `CoreDbApiImpl.*` (биндятся в ApiModule)

### 3. CoreDbComponent

| | |
|---|---|
| Файл | `core/core-db/.../di/CoreDbComponent.kt` |
| Scope | нет |
| Modules | нет |
| Dependencies | `CoreDbProvider` (от RoomComponent) |
| Implements | `CoreDbProvider` |
| Создаётся | `App.onCreate()` → `CoreDbComponent.init(context, logger)` |

**Прокси.** Не добавляет модулей — просто пробрасывает `CoreDbProvider` от RoomComponent.

**Два метода:**
- `init(context, logger)` — первый вызов, создаёт RoomComponent
- `get(context)` — последующие вызовы (уже инициализирован). Если не инициализирован — `IllegalStateException`

**Нюанс:** `RepositoryModule` в CoreInteractorComponent вызывает `CoreDbComponent.get(context)` — полагается что `init` уже был вызван в App.

### 4. AppComponent

| | |
|---|---|
| Файл | `app/.../di/AppComponent.kt` |
| Scope | `@Singleton` |
| Modules | AppModule (13 sub-модулей) |
| Dependencies | `CoreDbProvider` |
| @BindsInstance | `Context`, `LexemeLogger` |
| Implements | `AppProvider` (extends `CoreDbProvider`) |
| Создаётся | `App.onCreate()` — последним |

**AppModule includes:**

| Модуль | Тип | Биндит |
|--------|-----|--------|
| SplashModule | @Binds | SplashUseCaseImpl → SplashUseCase |
| DictionaryModule | @Binds | DictionaryUseCaseImpl → DictionaryUseCase |
| DictionaryTabModule | @Binds | DictionaryTabUseCaseImpl → DictionaryTabUseCase |
| WordCardModule | @Binds | WordCardUseCaseImpl → WordCardUseCase |
| QuizTabModule | @Binds | QuizTabUseCaseImpl → QuizTabUseCase |
| QuizChatModule | @Binds | QuizChatUseCaseImpl → QuizChatUseCase |
| StatisticModule | @Binds | StatisticUseCaseImpl → StatisticUseCase |
| SettingsModule | @Binds | SettingsTabUseCaseImpl → SettingsTabUseCase |
| DictionaryAppBarModule | @Binds | DictionaryAppBarUseCaseImpl → DictionaryAppBarUseCase |
| CountryProviderModule | @Provides @Singleton | CountryProviderImpl |
| PrefsProviderModule | @Provides @Singleton | PrefsProvider |
| ResourceModule | @Binds | ResourceManagerImpl → ResourceManager |
| EnvModule | @Binds | EnvParamsImpl → EnvParams |

**Exposed methods:**

```
getSplashUseCase()
getDictionaryUseCase()
getVocabularyUseCase()
getWordCardUseCase()
getQuizTabUseCase()
getQuizChatUseCase()
getStatisticUseCase()
getSettingsTabUseCase()
getDictionaryAppBarUseCase()
getResourceManager()
getPrefsProvider()
getEnvParams()
getLogger()
inject(MainActivity)
```

**Inner component:** `CoreDbDependenciesComponent` — используется как обёртка для передачи CoreDbProvider в factory.

### 5. CoreInteractorComponent

| | |
|---|---|
| Файл | `core/core-interactor/.../di/CoreInteractorComponent.kt` |
| Scope | `@Singleton` |
| Modules | RepositoryModule, ScenarioModule |
| @BindsInstance | `Context` |
| Implements | `CoreInteractorApi` |
| Создаётся | лениво через `getOrInit(context)` |

**Нюанс:** синглтон через nullable `var instance` без synchronized — **не потокобезопасный**.

**Практически не используется** — Scenarios закомментированы. RepositoryModule провайдит CoreDbApi через `CoreDbComponent.get(context)`.

## CoreDbApiImpl — inner classes

Все inner classes в `CoreDbApiImpl` — отдельные `@Inject` классы, биндятся через ApiModule:

| Класс | @Inject зависимости | Биндится в |
|-------|---------------------|-----------|
| CoreDbApiImpl | wordDao, logger | CoreDbApi |
| DbInstanceImpl | db: Database | CoreDbApi.DbInstance |
| DictionaryApiImpl | wordDao | CoreDbApi.DictionaryApi |
| TermApiImpl | wordDao | CoreDbApi.TermApi |
| WordApiImpl | wordDao | CoreDbApi.WordApi |
| LexemeApiImpl | wordDao | CoreDbApi.LexemeApi |
| QuizApiImpl | wordDao, logger | CoreDbApi.QuizApi |
| StatisticApiImpl | wordDao | CoreDbApi.StatisticApi |

**Нюанс:** все зависят от единственного `WordDao` — god-object с 40+ методами.

## UseCaseImpl — зависимости

Все живут в `app/di/module/` — каждый в своём пакете:

| UseCase | Зависимости |
|---------|------------|
| SplashUseCaseImpl | dictionaryApi |
| DictionaryUseCaseImpl | dictionaryApi, countryProvider, prefsProvider |
| DictionaryTabUseCaseImpl | dictionaryApi, wordApi, termApi, prefsProvider, countryProvider |
| WordCardUseCaseImpl | wordApi, dictionaryApi, termApi, lexemeApi, quizApi, prefsProvider |
| QuizTabUseCaseImpl | dictionaryApi, prefsProvider, countryProvider |
| QuizChatUseCaseImpl | dictionaryApi, quizApi, prefsProvider |
| StatisticUseCaseImpl | dictionaryApi, statisticDbApi, prefsProvider |
| SettingsTabUseCaseImpl | context, dbInstance |
| DictionaryAppBarUseCaseImpl | dictionaryApi, prefsProvider, countryProvider |

**Паттерн:** почти все зависят от `dictionaryApi` + `prefsProvider`. `countryProvider` у 4 из 9.

## Service Locator — точки использования

Extension property в App.kt:
```kotlin
val Context.appComponent: AppComponent
    get() = (applicationContext as App).appComponent
```

**Вызывается в:**

| Файл | Что берёт |
|------|-----------|
| RootRouter.kt | getSplashUseCase(), getDictionaryUseCase() |
| MainRouter.kt | все 11 getters для MainUiDepsProvider |

**MainUiDepsProvider** — создаётся в MainRouter вручную из 11 полей appComponent:

```kotlin
MainUiDepsProvider(
    dictionaryTabUseCase = appComponent.getVocabularyUseCase(),
    wordCardUseCase = appComponent.getWordCardUseCase(),
    quizTabUseCase = appComponent.getQuizTabUseCase(),
    quizChatUseCase = appComponent.getQuizChatUseCase(),
    statisticUseCase = appComponent.getStatisticUseCase(),
    dictionaryAppBarUseCase = appComponent.getDictionaryAppBarUseCase(),
    settingsTabUseCase = appComponent.getSettingsTabUseCase(),
    resourceManager = appComponent.getResourceManager(),
    prefsProvider = appComponent.getPrefsProvider(),
    envParams = appComponent.getEnvParams(),
    logger = appComponent.getLogger(),
)
```

## DataStore / Prefs

- `PrefsProvider(context)` — обёртка над `DataStore<Preferences>`
- DataStore name: `"lexemePrefStore"`
- Создаётся как `@Singleton` в PrefsProviderModule
- Используется в 9 из 9 UseCaseImpl (кроме SettingsTab)

## Проблемные зоны

1. **CoreDbComponent — прокси без смысла.** Не добавляет модулей, только пробрасывает CoreDbProvider. Можно было бы передать RoomComponent напрямую.

2. **init/get на CoreDbComponent.** Двойной API (`init` + `get`) — хрупкий контракт. Если `get` вызван до `init` — RuntimeException.

3. **CoreInteractorComponent — мёртвый код.** Scenarios закомментированы. RepositoryModule единственный рабочий модуль — и он просто берёт CoreDbApi из CoreDbComponent.

4. **WordDao — 40+ методов.** Все inner classes CoreDbApiImpl зависят от одного DAO. Нет разделения по ответственности.

5. **AppModule — 13 модулей.** App знает про все фичи. Нет feature-level DI.

6. **MainUiDepsProvider — god-object.** 11 параметров, растёт с каждой фичей. Собирается вручную в MainRouter через service locator.

7. **LoggerComponent — костыль.** Существует только чтобы разорвать цикл Logger → RoomComponent → AppComponent. Если бы DI граф был плоским — не нужен.
