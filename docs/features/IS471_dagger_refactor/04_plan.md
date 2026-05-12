# План рефакторинга DI графа

## Шаг 1: Удалить AppProvider, убрать наследование CoreDbProvider

### Сейчас

```kotlin
// app/.../api/AppProvider.kt
interface AppProvider : CoreDbProvider
```

```kotlin
// AppComponent.kt
interface AppComponent : AppProvider { ... }
```

`AppProvider` наследует `CoreDbProvider` — 8 методов DB API (`getDictionaryApi()`, `getWordApi()`, `getQuizApi()` и т.д.). `AppComponent` реализует `AppProvider`, значит экспонирует наружу все DB API.

`AppProvider` нигде не используется как тип — все работают с `AppComponent` напрямую. Пустой маркер-интерфейс без смысла.

### Пример: DictionaryApi

`CoreDbApi.DictionaryApi` — интерфейс в `core-db-api`:

```kotlin
interface DictionaryApi {
    suspend fun addDictionary(name: String, numericCode: Int? = null): Long
    suspend fun getDictionary(numericCode: Int): DictionaryApiEntity?
    suspend fun getDictionaryById(id: Long): DictionaryApiEntity?
    suspend fun getDictionaryList(): List<DictionaryApiEntity>
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
}
```

Реализация — `CoreDbApiImpl.DictionaryApiImpl` в `core-db-impl`. Биндится через `ApiModule` в `RoomComponent`.

`CoreDbProvider` экспонирует:
```kotlin
interface CoreDbProvider {
    fun getDictionaryApi(): CoreDbApi.DictionaryApi
    fun getWordApi(): CoreDbApi.WordApi
    fun getTermApi(): CoreDbApi.TermApi
    fun getLexemeApi(): CoreDbApi.LexemeApi
    fun getQuizApi(): CoreDbApi.QuizApi
    fun getStatisticApi(): CoreDbApi.StatisticApi
    fun getDbInstance(): CoreDbApi.DbInstance
    fun getCoreDbApi(): CoreDbApi
}
```

UseCase пример — `DictionaryTabUseCaseImpl`:
```kotlin
class DictionaryTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,  // ← Dagger берёт из CoreDbProvider
    private val wordApi: CoreDbApi.WordApi,
    ...
)
```

Dagger видит что `AppComponent` зависит от `CoreDbProvider` (через `dependencies`), и автоматически резолвит `CoreDbApi.DictionaryApi` → `CoreDbProvider.getDictionaryApi()`. UseCase не знает откуда пришёл `dictionaryApi` — просто получает через конструктор.

### Как DB API доступен UseCase'ам

AppComponent объявлен с `dependencies = [CoreDbProvider::class]`:
```kotlin
@Component(
    modules = [AppModule::class],
    dependencies = [CoreDbProvider::class]  // ← DB API доступен ВНУТРИ графа
)
interface AppComponent : AppProvider
```

Dagger видит `CoreDbProvider` как источник зависимостей. Когда `DictionaryTabUseCaseImpl` запрашивает `dictionaryApi: CoreDbApi.DictionaryApi` через `@Inject constructor` — Dagger берёт его из `CoreDbProvider.getDictionaryApi()` автоматически.

Это работает **внутри** графа без экспонирования наружу.

### Стало

**Удалить файл:** `app/.../api/AppProvider.kt`

**AppComponent:**
```kotlin
@Component(
    modules = [AppModule::class],
    dependencies = [CoreDbProvider::class]
)
@Singleton
interface AppComponent {
    // DB API НЕ экспонируется наружу
    // UseCase'ы получают DB API через @Inject constructor внутри графа
    
    fun getSplashUseCase(): SplashUseCase
    fun getMainUiDeps(): MainUiDeps  // после шага 3
    fun getLogger(): LexemeLogger
    fun inject(mainActivity: MainActivity)
}
```

Снаружи (`context.appComponent`) — только UseCase'ы и logger. DB API доступен только внутри Dagger графа.

### Проверка

- `AppProvider` как тип нигде не используется — все работают с `AppComponent`
- DB API через `appComponent` никто не вызывает
- Удаление безопасно — компилятор подтвердит

**Риск:** нет.

---

## Шаг 2: Удалить CoreInteractorComponent (мёртвый код)

**Удалить:** модуль `core/core-interactor` целиком (CoreInteractorComponent, RepositoryModule, ScenarioModule, CoreInteractorApi).

Scenarios закомментированы. RepositoryModule — единственный рабочий модуль, просто берёт CoreDbApi из CoreDbComponent. Не используется в основном потоке.

**Риск:** проверить что никто не вызывает `CoreInteractorComponent.getOrInit()`.

---

## Шаг 3: MainUiDepsProvider через Dagger @Inject

### Что такое MainUiDeps

`MainUiDeps` — интерфейс в модуле `screen/main`. Определяет контракт: какие экраны доступны из главного экрана. Каждый метод — `@Composable` функция, которая рендерит экран с нужными зависимостями.

```kotlin
// modules/screen/main/.../MainUiDeps.kt
@Stable
interface MainUiDeps {
    @Composable fun VocabularyTabDep(openDictionaryCreate: () -> Unit, openWordCard: (Long) -> Unit)
    @Composable fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit)
    @Composable fun QuizTabScreenDep(openDictionaryCreate: () -> Unit, openChatQuiz: (String) -> Unit)
    @Composable fun ChatQuizScreenDep(onBackPress: () -> Unit)
    @Composable fun StatisticTabScreenDep(openDictionaryCreate: () -> Unit)
    @Composable fun SettingsTabScreenDep(onLangManagementClick: () -> Unit, onAboutAppClick: () -> Unit, onPrivacyPolicyClick: () -> Unit)
    @Composable fun AboutAppScreenDep(onBackPress: () -> Unit)
    @Composable fun WebViewScreenDep(pageKey: String, onBackPress: () -> Unit)
}
```

**Зачем нужен:** модуль `screen/main` не зависит от `app`. Он не знает про UseCase'ы, Dagger, конкретные экраны. Знает только интерфейс. Реализация (`MainUiDepsProvider`) живёт в `app` — там доступны все зависимости.

**Как используется:** `MainScreen` получает `MainUiDeps` как параметр и вызывает его методы в NavHost:
```kotlin
composable(TabPoint.VOCABULARY.route) {
    mainUiDeps.VocabularyTabDep(openDictionaryCreate, openWordCard)
}
```

### Что такое MainUiDepsProvider

`MainUiDepsProvider` — реализация `MainUiDeps` в модуле `app`. Принимает 11 зависимостей (UseCase'ы, logger, prefs, resources). Каждый `@Composable` метод создаёт конкретный экран с нужными зависимостями:

```kotlin
// app/.../uiDeps/MainUiDepsProvider.kt
@Composable
override fun VocabularyTabDep(...) {
    DictionaryTabScreen(
        dictionaryTabUseCase = dictionaryTabUseCase,  // из конструктора
        logger = logger,                                // из конструктора
        dictionaryTabUiDeps = object : DictionaryTabUiDeps {
            @Composable
            override fun AppBar(titleResId: Int) = DictionaryAppBar(...)
        },
    )
}
```

Также создаёт вложенные UiDeps (DictionaryTabUiDeps, QuizTabUiDeps, StatisticUiDeps) — интерфейсы для AppBar, который одинаковый на трёх табах (словарь, квизы, статистика).

### Проблема сейчас

MainRouter **вручную** создаёт MainUiDepsProvider, доставая 11 зависимостей из appComponent:

```kotlin
// MainRouter.kt
MainScreen(
    mainUiDeps = MainUiDepsProvider(
        dictionaryTabUseCase = context.appComponent.getVocabularyUseCase(),
        wordCardUseCase = context.appComponent.getWordCardUseCase(),
        quizTabUseCase = context.appComponent.getQuizTabUseCase(),
        quizChatUseCase = context.appComponent.getQuizChatUseCase(),
        statisticUseCase = context.appComponent.getStatisticUseCase(),
        settingsTabUseCase = context.appComponent.getSettingsTabUseCase(),
        dictionaryAppBarUseCase = context.appComponent.getDictionaryAppBarUseCase(),
        prefsProvider = context.appComponent.getPrefsProvider(),
        resourceManager = context.appComponent.getResourceManager(),
        envParams = context.appComponent.getEnvParams(),
        logger = context.appComponent.getLogger(),
    )
)
```

Каждая новая фича = +1 параметр в MainRouter, +1 getter в AppComponent, +1 строка тут.

**Решение:** сделать MainUiDepsProvider инжектируемым через Dagger.

**MainUiDepsProvider.kt** — добавить `@Inject constructor`:
```kotlin
@Stable
class MainUiDepsProvider @Inject constructor(
    private val dictionaryTabUseCase: DictionaryTabUseCase,
    private val wordCardUseCase: WordCardUseCase,
    private val quizTabUseCase: QuizTabUseCase,
    private val quizChatUseCase: QuizChatUseCase,
    private val statisticUseCase: StatisticUseCase,
    private val dictionaryAppBarUseCase: DictionaryAppBarUseCase,
    private val settingsTabUseCase: SettingsTabUseCase,
    private val resourceManager: ResourceManager,
    private val prefsProvider: PrefsProvider,
    private val envParams: EnvParams,
    private val logger: LexemeLogger,
) : MainUiDeps
```

**Новый модуль** — `MainUiDepsModule.kt`:
```kotlin
@Module
interface MainUiDepsModule {
    @Binds
    fun bind(impl: MainUiDepsProvider): MainUiDeps
}
```

Добавить `MainUiDepsModule` в `AppModule.includes`.

**AppComponent** — один getter вместо 11:
```kotlin
fun getMainUiDeps(): MainUiDeps
```

Удалить из AppComponent: `getVocabularyUseCase()`, `getWordCardUseCase()`, `getQuizTabUseCase()`, `getQuizChatUseCase()`, `getStatisticUseCase()`, `getSettingsTabUseCase()`, `getDictionaryAppBarUseCase()`, `getResourceManager()`, `getPrefsProvider()`, `getEnvParams()`. Оставить `getLogger()` (используется в RootRouter).

**MainRouter** — один вызов:
```kotlin
MainScreen(mainUiDeps = context.appComponent.getMainUiDeps())
```

Вместо 11 строк — одна.

**Добавление новой фичи:**
1. Добавить параметр в MainUiDepsProvider — Dagger заинжектит автоматически
2. Добавить метод в MainUiDeps
3. Реализовать в MainUiDepsProvider

MainRouter НЕ трогаем. AppComponent НЕ трогаем.

**Риск:** LOW. `@Composable` на методах интерфейса — не проблема для Dagger (он инжектит класс, не вызывает методы).

---

## Шаг 4: Переименовать MainUiDeps → CompositionRoot

**Файлы:**

| Файл | Изменение |
|------|-----------|
| `modules/screen/main/.../MainUiDeps.kt` | Переименовать интерфейс → `CompositionRoot`, файл → `CompositionRoot.kt` |
| `app/.../uiDeps/MainUiDepsProvider.kt` | Переименовать класс → `CompositionRootImpl`, файл → `CompositionRootImpl.kt` |
| `modules/screen/main/.../MainScreen.kt` | `mainUiDeps` → `compositionRoot` |
| `modules/screen/main/.../Vocabulary.kt` | `mainUiDeps` → `compositionRoot` |
| `modules/screen/main/.../Quiz.kt` | `mainUiDeps` → `compositionRoot` |
| `modules/screen/main/.../Settings.kt` | `mainUiDeps` → `compositionRoot` |
| `app/.../route/MainRouter.kt` | `mainUiDeps` → `compositionRoot` |

Механическое переименование. Отражает архитектурную роль — это composition root, не просто "UI dependencies".

**Риск:** нет. Rename refactoring, компилятор поймает все пропущенные места.

---

## Бэклог (отдельные задачи)

### Сегрегация CoreDbProvider
CoreDbProvider 8 методов → мелкие интерфейсы. HIGH, затрагивает core-db-api, core-db-impl, все UseCaseImpl.

### Убрать service locator
`context.appComponent` в RootRouter → через параметры или CompositionLocal. MEDIUM.

---

## Порядок

| Шаг | Сложность | Зависимости |
|-----|-----------|-------------|
| 1 | LOW | нет |
| 2 | LOW | нет |
| 3 | LOW | нет |

Шаги 1-3 — делаем сейчас.
