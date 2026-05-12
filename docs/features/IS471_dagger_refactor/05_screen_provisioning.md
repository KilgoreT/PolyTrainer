# Провайдинг экранов: текущий подход и решение

## Текущий подход: MainUiDeps + MainUiDepsProvider

### Что такое

`MainUiDeps` — интерфейс в `screen/main`. Контракт: какие экраны доступны.
`MainUiDepsProvider` — реализация в `app`. Composition root для UI: связывает Dagger-зависимости с Compose-экранами.

### Три уровня провайдинга

**Уровень 1: Tab экраны**
```
MainScreen(mainUiDeps)
  └── NavHost
      ├── mainUiDeps.VocabularyTabDep()      → DictionaryTabScreen
      ├── mainUiDeps.QuizTabScreenDep()      → QuizTabScreen
      ├── mainUiDeps.StatisticTabScreenDep() → StatisticTabScreen
      └── mainUiDeps.SettingsTabScreenDep()  → SettingsTabScreen
```

**Уровень 2: Вложенные экраны (siblings в NavHost)**
```
Vocabulary: VocabularyTabDep + WordCardScreenDep
Quiz:       QuizTabScreenDep + ChatQuizScreenDep
Settings:   SettingsTabScreenDep + AboutAppScreenDep + WebViewScreenDep
```

**Уровень 3: Вложенные UiDeps (AppBar)**

Три таба имеют одинаковый DictionaryAppBar. Для него — вложенные интерфейсы (DictionaryTabUiDeps, QuizTabUiDeps, StatisticUiDeps). MainUiDepsProvider создаёт их inline.

### Проблема

Composable функция — **курьер**. Принимает UseCase и logger чтобы передать в ViewModel Factory. Сама не использует:

```kotlin
@Composable
fun WordCardScreen(
    wordId: Long,                       // нужен экрану
    wordCardUseCase: WordCardUseCase,   // НЕ нужен — просто передаёт в Factory
    onBackPress: () -> Unit,            // нужен экрану
    viewModel: WordCardViewModel = viewModel(
        factory = WordCardViewModel.Factory(wordId, wordCardUseCase)
    ),
) { ... }
```

`wordCardUseCase` проходит путь: AppComponent → MainUiDepsProvider → WordCardScreenDep → WordCardScreen → Factory → ViewModel. Экран — труба.

---

## ОТКЛОНЁННЫЕ АЛЬТЕРНАТИВЫ

### ViewModel с прямым DI (@IntoMap)
Не поддерживает runtime параметры (wordId). По сути ручной Hilt.

### Гибрид @Inject Provider
Убирает ручную сборку MainUiDepsProvider, но composable всё ещё курьер. Не решает корневую проблему.

---

## Решение: AssistedInject

ViewModel получает DI-зависимости (UseCase, logger) из Dagger. Runtime параметры (wordId) — через `@Assisted`. Composable не знает про UseCase.

### Полный пример: WordCardScreen (с runtime параметром)

#### ViewModel — было:

```kotlin
class WordCardViewModel(
    wordId: Long,                       // runtime
    wordCardUseCase: WordCardUseCase,   // DI
) : ViewModel() {
    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(wordCardUseCase = wordCardUseCase),
            UiEffectHandler()
        )
    )

    // Ручная Factory — boilerplate
    class Factory(
        private val wordId: Long,
        private val wordCardUseCase: WordCardUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WordCardViewModel(wordId, wordCardUseCase) as T
        }
    }
}
```

#### ViewModel — стало:

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted private val wordId: Long,             // runtime — @Assisted
    private val wordCardUseCase: WordCardUseCase,   // DI — Dagger инжектит
) : ViewModel() {
    private val stateHolder = Mate(
        initState = WordCardState(),
        initEffects = setOf(DatasourceEffect.LoadWord(wordId)),
        reducer = WordCardReducer(),
        effectHandlerSet = setOf(
            DatasourceEffectHandler(wordCardUseCase = wordCardUseCase),
            UiEffectHandler()
        )
    )

    // Dagger генерирует реализацию автоматически
    @AssistedFactory
    interface Factory {
        fun create(wordId: Long): WordCardViewModel
    }
}
```

#### Composable — было:

```kotlin
@Composable
fun WordCardScreen(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,       // ← курьер
    onBackPress: () -> Unit,
    viewModel: WordCardViewModel = viewModel(
        factory = WordCardViewModel.Factory(wordId, wordCardUseCase)
    ),
) { ... }
```

#### Composable — стало:

```kotlin
@Composable
fun WordCardScreen(
    wordId: Long,
    onBackPress: () -> Unit,
    factory: WordCardViewModel.Factory,     // ← AssistedFactory из Dagger
    viewModel: WordCardViewModel = viewModel(
        factory = viewModelFactory { factory.create(wordId) }
    ),
) { ... }
```

`wordCardUseCase` исчез. Factory приходит из Dagger, создаёт ViewModel с runtime `wordId`.

#### MainUiDepsProvider — было:

```kotlin
@Composable
override fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit) {
    WordCardScreen(
        wordId = wordId,
        wordCardUseCase = wordCardUseCase,     // ← UseCase
        onBackPress = onBackPress,
    )
}
```

#### MainUiDepsProvider — стало:

```kotlin
@Composable
override fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit) {
    WordCardScreen(
        wordId = wordId,
        onBackPress = onBackPress,
        factory = wordCardViewModelFactory,     // ← Factory
    )
}
```

### Полный пример: DictionaryTabScreen (без runtime параметров)

Не все ViewModel'ы имеют runtime параметры. DictionaryTabViewModel принимает только UseCase и logger — оба из DI. Тут `@AssistedInject` избыточен. Обычный `@Inject` + `@IntoMap`.

#### ViewModel — стало:

```kotlin
class DictionaryTabViewModel @Inject constructor(
    private val useCase: DictionaryTabUseCase,   // DI
    private val logger: LexemeLogger,             // DI
) : ViewModel() {
    // ... Mate init ...
    // Factory не нужна — Dagger создаёт через @IntoMap
}
```

Dagger Module:
```kotlin
@Module
interface DictionaryTabViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(DictionaryTabViewModel::class)
    fun bind(vm: DictionaryTabViewModel): ViewModel
}
```

#### Composable — стало:

```kotlin
@Composable
fun DictionaryTabScreen(
    dictionaryTabUiDeps: DictionaryTabUiDeps,     // AppBar (UI callback)
    openWordCard: (wordId: Long) -> Unit,         // навигация
    viewModel: DictionaryTabViewModel = daggerViewModel(),  // из общей DaggerViewModelFactory
) { ... }
```

UseCase и logger исчезли из параметров. Не нужна индивидуальная Factory — общий `daggerViewModel()` helper.

#### Helper — daggerViewModel:

```kotlin
@Composable
inline fun <reified T : ViewModel> daggerViewModel(): T {
    val factory = (LocalContext.current.applicationContext as App).appComponent.viewModelFactory()
    return viewModel(factory = factory)
}
```

`DaggerViewModelFactory` — одна общая factory для всех `@Inject` ViewModel'ов:

```kotlin
class DaggerViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creators[modelClass]?.get() as T
            ?: throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
```

#### Аннотация ViewModelKey:

```kotlin
@MapKey
@Target(AnnotationTarget.FUNCTION)
annotation class ViewModelKey(val value: KClass<out ViewModel>)
```

### Два паттерна

| Когда | Паттерн | Factory | Composable получает VM |
|-------|---------|---------|----------------------|
| Без runtime параметров | `@Inject` + `@IntoMap` | Общая `DaggerViewModelFactory` | `daggerViewModel()` |
| С runtime параметрами | `@AssistedInject` + `@AssistedFactory` | Индивидуальная `VM.Factory` | `viewModelFactory { factory.create(id) }` |

Каждый паттерн по делу. Не нужно натягивать `@AssistedInject` на ViewModel без runtime параметров.

### UiDeps (AppBar) — тоже становится тоньше

DictionaryAppBar используется в трёх табах. С AssistedInject — UiDeps передаёт Factory вместо UseCase+logger:

**Было:**
```kotlin
dictionaryTabUiDeps = object : DictionaryTabUiDeps {
    @Composable
    override fun AppBar(titleResId: Int) = DictionaryAppBar(
        titleResId = titleResId,
        logger = logger,
        dictionaryAppBarUseCase = dictionaryAppBarUseCase,
        openDictionaryCreate = openDictionaryCreate,
    )
}
```

**Стало:**
```kotlin
dictionaryTabUiDeps = object : DictionaryTabUiDeps {
    @Composable
    override fun AppBar(titleResId: Int) = DictionaryAppBar(
        titleResId = titleResId,
        openDictionaryCreate = openDictionaryCreate,
        factory = dictionaryAppBarViewModelFactory,
    )
}
```

UiDeps интерфейс и MainUiDepsProvider **остаются** — composition root. Но провайдер тоньше.

### MainUiDepsProvider конструктор

**Было:**
```kotlin
class MainUiDepsProvider(
    private val wordCardUseCase: WordCardUseCase,
    private val dictionaryTabUseCase: DictionaryTabUseCase,
    // ... ещё 9 UseCase'ов/зависимостей
) : MainUiDeps
```

**Стало:**
```kotlin
class MainUiDepsProvider @Inject constructor(
    // Для ViewModel'ов с runtime параметрами — индивидуальные Factory
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    private val chatViewModelFactory: ChatViewModel.Factory,
    // Для UiDeps (AppBar) — тоже AssistedFactory (есть runtime callback)
    private val dictionaryAppBarViewModelFactory: DictionaryAppBarViewModel.Factory,
    // Общие зависимости для навигации/UI
    private val envParams: EnvParams,
    private val logger: LexemeLogger,
) : MainUiDeps
```

ViewModel'ы без runtime (`DictionaryTabViewModel`, `QuizTabViewModel`, `StatisticViewModel`, `SettingsTabViewModel`) — composable берёт их через `daggerViewModel()` напрямую. MainUiDepsProvider про них не знает.

AppComponent экспонирует:
```kotlin
fun getMainUiDeps(): MainUiDeps
fun viewModelFactory(): DaggerViewModelFactory  // для daggerViewModel()
```

### Helper

```kotlin
fun viewModelFactory(creator: () -> ViewModel): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }
}
```

### Что меняется в screen модулях

Screen модуль получает зависимость на Dagger:
```kotlin
// build.gradle.kts
implementation("com.google.dagger:dagger:2.x")
ksp("com.google.dagger:dagger-compiler:2.x")
```

Нужно для `@AssistedInject`, `@Assisted`, `@AssistedFactory`.

### Lazy инициализация

ViewModel создаётся лениво — `viewModel(factory = ...)` вызывается только при рендере composable. Factory — легковесный интерфейс. Создание Factory не создаёт ViewModel.

### Плюсы

- Composable не курьер — не принимает UseCase/logger
- Runtime параметры работают (`@Assisted wordId`)
- Lazy — ViewModel создаётся при рендере
- Factory генерируется Dagger'ом — нет ручного boilerplate
- MainUiDepsProvider тоньше — Factory вместо UseCase'ов
- Единый паттерн для всех ViewModel'ов
- Типобезопасность

### Минусы

- Screen модули получают зависимость на Dagger (ksp processor)
- MainUiDepsProvider всё ещё нужен для UiDeps (AppBar) и навигационных callback'ов
- Миграция всех ViewModel'ов — объём работы

### Итого

| Что | Было | Стало |
|-----|------|-------|
| Composable параметры | UseCase + logger + callbacks | Factory + callbacks |
| ViewModel конструктор | обычный | @AssistedInject |
| Factory | ручная (boilerplate) | @AssistedFactory (генерируется) |
| MainUiDepsProvider | передаёт UseCase'ы | передаёт Factory |
| Screen модуль → Dagger | нет | да (ksp) |
| MainRouter | 11 строк | 1 строка |

---

## Findings из ревью

### 1. Неполная Dagger-конфигурация

Для `@Inject` ViewModel'ов нужна полная цепочка:

**ViewModelKey аннотация** (один раз, в core или app):
```kotlin
@MapKey
@Target(AnnotationTarget.FUNCTION)
annotation class ViewModelKey(val value: KClass<out ViewModel>)
```

**Module для каждого @Inject ViewModel** (в app):
```kotlin
@Module
interface DictionaryTabViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(DictionaryTabViewModel::class)
    fun bind(vm: DictionaryTabViewModel): ViewModel
}
```

Добавить все ViewModel-модули в `AppModule.includes`.

**DaggerViewModelFactory** (в app, `@Inject constructor` — Dagger провайдит автоматически):
```kotlin
class DaggerViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creators[modelClass]?.get() as T
            ?: throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
```

**CompositionRootImpl (MainUiDepsProvider)** — `@Binds` в Module:
```kotlin
@Module
interface CompositionRootModule {
    @Binds
    fun bind(impl: CompositionRootImpl): CompositionRoot
}
```

**@AssistedFactory** — регистрировать в Module НЕ нужно. Dagger биндит автоматически.

### 2. ViewModel key для runtime параметров

Для ViewModel'ов с runtime параметрами — добавлять `key` в `viewModel()`:

```kotlin
viewModel: WordCardViewModel = viewModel(
    key = "wordCard_$wordId",
    factory = viewModelFactory { factory.create(wordId) }
)
```

Без key: если два WordCard с разными wordId в одном NavBackStackEntry — вернётся старый ViewModel.

### 3. Постепенная миграция

Паттерны совместимы. CompositionRootImpl может содержать смесь UseCase'ов (старые) и Factory (новые).

Порядок:
1. Инфраструктура: DaggerViewModelFactory, ViewModelKey, daggerViewModel() helper
2. Один простой экран (`DictionaryTabScreen`) → `@Inject` + `@IntoMap`
3. Один экран с runtime (`WordCardScreen`) → `@AssistedInject` + `@AssistedFactory`
4. Остальные по одному

Каждый шаг — рабочее приложение.
