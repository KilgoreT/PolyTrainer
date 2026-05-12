# План реализации шага 3: AssistedInject + @IntoMap для ViewModel'ов

## Этап 1: Инфраструктура

Один PR. Не трогает экраны — только добавляет механизм.

### Создать файлы:

**`modules/core/di/.../ViewModelKey.kt`**
```kotlin
@MapKey
@Target(AnnotationTarget.FUNCTION)
annotation class ViewModelKey(val value: KClass<out ViewModel>)
```

**`modules/core/di/.../DaggerViewModelFactory.kt`**
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

**`modules/core/di/.../ViewModelHelper.kt`**
```kotlin
fun viewModelFactory(creator: () -> ViewModel): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }
}

@Composable
inline fun <reified T : ViewModel> daggerViewModel(): T {
    val factory = (LocalContext.current.applicationContext as App).appComponent.viewModelFactory()
    return viewModel(factory = factory)
}
```

### Изменить:

**`AppComponent`** — добавить:
```kotlin
fun viewModelFactory(): DaggerViewModelFactory
```

### Проверка:

Lint, тесты, build. Приложение работает без изменений — инфраструктура добавлена, но не используется.

---

## Этап 2: Миграция простого экрана (DictionaryTabScreen)

Один PR. Первая проверка паттерна `@Inject` + `@IntoMap`.

### ViewModel'ы без runtime параметров:

| ViewModel | Модуль | UseCase | Logger |
|-----------|--------|---------|--------|
| DictionaryTabViewModel | dictionaryTab | DictionaryTabUseCase | да |
| QuizTabViewModel | quiztab | QuizTabUseCase | да |
| StatisticViewModel | stattab | StatisticUseCase | да |
| SettingsTabViewModel | settingstab | SettingsTabUseCase | да |

Начинаем с `DictionaryTabViewModel`.

### Изменить:

**`DictionaryTabViewModel.kt`** (screen модуль):
```kotlin
// Было:
class DictionaryTabViewModel(
    dictionaryTabUseCase: DictionaryTabUseCase,
    logger: LexemeLogger,
) : ViewModel() {
    class Factory(...) : ViewModelProvider.Factory { ... }
}

// Стало:
class DictionaryTabViewModel @Inject constructor(
    dictionaryTabUseCase: DictionaryTabUseCase,
    logger: LexemeLogger,
) : ViewModel() {
    // Factory удалена
}
```

**`dictionaryTab/build.gradle.kts`** — добавить:
```kotlin
implementation("com.google.dagger:dagger:2.x")  // для @Inject
```
KSP processor НЕ нужен — `@Inject` не требует кодогенерации в screen модуле.

**`app/.../di/module/dictionarytab/DictionaryTabViewModelModule.kt`** — создать:
```kotlin
@Module
interface DictionaryTabViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(DictionaryTabViewModel::class)
    fun bind(vm: DictionaryTabViewModel): ViewModel
}
```

**`AppModule`** — добавить `DictionaryTabViewModelModule::class` в includes.

**`VocabularyTabScreen.kt`** (screen модуль):
```kotlin
// Было:
@Composable
fun DictionaryTabScreen(
    dictionaryTabUseCase: DictionaryTabUseCase,
    dictionaryTabUiDeps: DictionaryTabUiDeps,
    logger: LexemeLogger,
    viewModel: DictionaryTabViewModel = viewModel(
        factory = DictionaryTabViewModel.Factory(dictionaryTabUseCase, logger)
    ),
    openWordCard: (wordId: Long) -> Unit,
)

// Стало:
@Composable
fun DictionaryTabScreen(
    dictionaryTabUiDeps: DictionaryTabUiDeps,
    openWordCard: (wordId: Long) -> Unit,
    viewModel: DictionaryTabViewModel = daggerViewModel(),
)
```

Параметры `dictionaryTabUseCase` и `logger` — удалены.

**`CompositionRootImpl (MainUiDepsProvider)`** — упростить VocabularyTabDep:
```kotlin
// Было:
DictionaryTabScreen(
    dictionaryTabUseCase = dictionaryTabUseCase,
    logger = logger,
    dictionaryTabUiDeps = ...,
    openWordCard = openWordCard,
)

// Стало:
DictionaryTabScreen(
    dictionaryTabUiDeps = ...,
    openWordCard = openWordCard,
)
```

Убрать `dictionaryTabUseCase` из конструктора CompositionRootImpl (если больше нигде не используется).

### Проверка:

Lint, тесты, build, запуск на устройстве — открыть таб словарей.

---

## Этап 3: Миграция экрана с runtime параметром (WordCardScreen)

Один PR. Проверка паттерна `@AssistedInject` + `@AssistedFactory`.

### ViewModel'ы с runtime параметрами:

| ViewModel | Модуль | Runtime params | DI deps |
|-----------|--------|---------------|---------|
| WordCardViewModel | wordcard | wordId: Long | WordCardUseCase |
| ChatViewModel | quiz/chat | — сложный (4 deps) | QuizChatUseCase, ResourceManager, PrefsProvider, Logger |
| DictionaryAppBarViewModel | widget/dictionaryappbar | — | DictionaryAppBarUseCase, Logger |

Начинаем с `WordCardViewModel` — чистый пример (1 runtime, 1 DI).

### Изменить:

**`WordCardViewModel.kt`** (screen модуль):
```kotlin
// Было:
class WordCardViewModel(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
) : ViewModel() {
    class Factory(...) : ViewModelProvider.Factory { ... }
}

// Стало:
class WordCardViewModel @AssistedInject constructor(
    @Assisted private val wordId: Long,
    private val wordCardUseCase: WordCardUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(wordId: Long): WordCardViewModel
    }
}
```

**`wordcard/build.gradle.kts`** — добавить:
```kotlin
id("com.google.devtools.ksp")  // в plugins
implementation("com.google.dagger:dagger:2.x")
ksp("com.google.dagger:dagger-compiler:2.x")  // KSP нужен для @AssistedFactory
```

**`WordCardScreen.kt`**:
```kotlin
// Было:
@Composable
fun WordCardScreen(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
    onBackPress: () -> Unit,
    viewModel: WordCardViewModel = viewModel(
        factory = WordCardViewModel.Factory(wordId, wordCardUseCase)
    ),
)

// Стало:
@Composable
fun WordCardScreen(
    wordId: Long,
    onBackPress: () -> Unit,
    factory: WordCardViewModel.Factory,
    viewModel: WordCardViewModel = viewModel(
        key = "wordCard_$wordId",
        factory = viewModelFactory { factory.create(wordId) }
    ),
)
```

**`CompositionRootImpl`** — изменить конструктор и WordCardScreenDep:
```kotlin
// Конструктор: wordCardUseCase → wordCardViewModelFactory
private val wordCardViewModelFactory: WordCardViewModel.Factory

// WordCardScreenDep:
WordCardScreen(
    wordId = wordId,
    onBackPress = onBackPress,
    factory = wordCardViewModelFactory,
)
```

**Module не нужен** — `@AssistedFactory` биндится Dagger'ом автоматически.

### Проверка:

Lint, тесты, build, запуск — открыть карточку слова.

---

## Этап 4: Миграция остальных экранов

По одному PR на экран. Порядок:

### @Inject + @IntoMap (без runtime):
1. QuizTabViewModel
2. StatisticViewModel
3. SettingsTabViewModel

### @AssistedInject + @AssistedFactory (с runtime):
4. ChatViewModel (сложный — 4 DI deps)
5. DictionaryAppBarViewModel (widget, не screen)

### Каждый PR:
1. ViewModel: добавить @Inject/@AssistedInject, удалить ручную Factory
2. Screen: убрать UseCase/logger из параметров
3. CompositionRootImpl: убрать UseCase из конструктора, добавить Factory если AssistedInject
4. build.gradle.kts: добавить Dagger зависимость (+ KSP для AssistedInject)
5. Lint, тесты, build

---

## Этап 5: Cleanup

Один финальный PR.

1. Удалить неиспользуемые getter'ы из AppComponent (UseCase'ы которые больше не нужны снаружи)
2. Убрать неиспользуемые UseCase'ы из конструктора CompositionRootImpl
3. Обновить гайды (dagger-di.md, ui-patterns.md)

---

## Сводка

| Этап | Что | PR |
|------|-----|-----|
| 1 | Инфраструктура (Factory, Key, Helper) | 1 |
| 2 | DictionaryTabScreen (@Inject) | 1 |
| 3 | WordCardScreen (@AssistedInject) | 1 |
| 4 | Остальные 5 экранов | 5 |
| 5 | Cleanup | 1 |
| **Итого** | | **9 PR** |

Каждый PR — рабочее приложение. Можно остановиться на любом этапе.
