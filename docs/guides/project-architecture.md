# Архитектура проекта

## Граф модулей

```
app (application)
 |
 +-- modules/core/mate          -- TEA фреймворк состояний
 +-- modules/core/theme         -- Material3 тема (AppTheme, цвета, типографика)
 +-- modules/core/ui            -- Общие composable виджеты (~40+ компонентов)
 +-- modules/core/tools         -- Kotlin утилиты для коллекций
 |
 +-- modules/screen/splash          -- Splash (простой ViewModel, без TEA)
 +-- modules/screen/createdictionary -- Создание словаря (TEA)
 +-- modules/screen/main            -- Контейнер табов (Navigation, без TEA)
 +-- modules/screen/dictionaryTab   -- Список слов (TEA)
 +-- modules/screen/wordcard        -- Карточка слова (TEA, reference с тестами)
 +-- modules/screen/quiztab         -- Выбор квиза (TEA)
 +-- modules/screen/quiz/chat       -- Чат-квиз (TEA, reference для редьюсера)
 +-- modules/screen/stattab         -- Статистика (TEA)
 +-- modules/screen/settingstab     -- Настройки (TEA)
 |
 +-- modules/widget/dictionaryappbar    -- AppBar с TEA
 +-- modules/widget/dictionarypicker    -- Выбор словаря
 +-- modules/widget/chipPicker          -- Выбор чипов
 +-- modules/widget/iconDropDowned      -- Dropdown иконка
 |
 +-- modules/datasource/prefs   -- DataStore preferences
 +-- modules/library/flags      -- Флаги стран
 |
 +-- core/core-db               -- DB API интерфейсы (legacy)
 +-- core/core-db-api           -- DB API контракты
 +-- core/core-db-impl          -- Room реализация
 +-- core/core-resources        -- Общие ресурсы
```

## Правила зависимостей

1. **Feature модули зависят от core модулей.** Никогда наоборот.
2. **Core модули не зависят друг от друга** (кроме: `ui` зависит от `theme`).
3. **Screen модули не зависят от других screen модулей.** Связь через навигацию.
4. **Widget модули** могут иметь собственные Mate инстансы (например, `dictionaryappbar`).
5. **Screen модули не зависят от Compose Navigation.** Видят интерфейс `XxxNavigator : Navigator` из `core/mate`, реализация `XxxNavigatorImpl` живёт в `app/.../navigator/`.
6. **Legacy core модули** (`core/core-*`) постепенно заменяются.

## Слои

```
UI Layer (Screen.kt)                                    Navigator (XxxNavigator)
    |                                                       ^
    | collectAsStateWithLifecycle()                         |
    | accept(Msg)                                           |
    v                                                       |
State Management Layer (ViewModel + Mate + Reducer)         |
    |                                                       |
    | Effects (DatasourceEffect / UiEffect /                |
    |          NavigationEffect)                            |
    v                                                       |
Effect Layer (handlers)                                     |
    |  MateTypedEffectHandler / MateFlowHandler  --------> NavigatorImpl (app/.../navigator/)
    |  MateNavigationEffectHandler ----------------------> NavController
    | UseCase вызовы
    v
Domain Layer (UseCase интерфейсы)
    |
    | Реализация
    v
Data Layer (Room, DataStore, Prefs)
```

Базовые классы handlers в `core/mate`:
- `MateTypedEffectHandler<Msg, E>` — автоматическая фильтрация чужих эффектов через `filter()`.
- `MateNavigationEffectHandler<Msg>(navigator)` — наследует typed handler, обрабатывает `NavigationEffect.Back` через `Navigator.back()`, делегирует специфичные в `onScreenEffect()`.
- `MateFlowHandler` — для долгоживущих подписок на Flow.

## Dependency Injection

**Dagger 2** с единственным `AppComponent` (Singleton scope).

### AppComponent

```kotlin
@Singleton
@Component(
    dependencies = [CoreDbProvider::class],
    modules = [AppModule::class],
)
interface AppComponent {
    fun getSplashUseCase(): SplashUseCase
    fun getDictionaryUseCase(): DictionaryUseCase

    // Factory каждой ViewModel — Dagger генерирует из @AssistedFactory
    fun getDictionaryFormViewModelFactory(): DictionaryFormViewModel.Factory
    fun getDictionaryListViewModelFactory(): DictionaryListViewModel.Factory
    fun getWordCardViewModelFactory(): WordCardViewModel.Factory
    fun getChatViewModelFactory(): ChatViewModel.Factory
    fun getDictionaryAppBarViewModelFactory(): DictionaryAppBarViewModel.Factory
    fun getDictionaryTabViewModelFactory(): DictionaryTabViewModel.Factory
    fun getQuizTabViewModelFactory(): QuizTabViewModel.Factory
    fun getStatisticViewModelFactory(): StatisticViewModel.Factory
    fun getSettingsTabViewModelFactory(): SettingsTabViewModel.Factory

    fun getLogger(): LexemeLogger
}
```

### Composition root для UI

`CompositionRootImpl` (в app модуле) — единственное место, где Factory из AppComponent встречаются с навигационными callbacks из RootRouter:

```kotlin
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    // ... остальные Factory + envParams + logger
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

Composable получает `factory + navigator` параметрами — никогда не лезет в `appComponent` напрямую.

### DI модуль фичи

```kotlin
@Module
interface QuizChatModule {
    @Binds
    fun bindQuizChatUseCase(impl: QuizChatUseCaseImpl): QuizChatUseCase
}
```

ViewModel биндинга в модуле фичи **нет** — `@AssistedInject` + `@AssistedFactory` создаёт Factory автоматически, getter на AppComponent её экспонирует.

### ViewModel — @AssistedInject

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    @AssistedFactory
    interface Factory {
        fun create(wordId: Long, navigator: WordCardNavigator): WordCardViewModel
    }
}
```

`@Assisted` для runtime аргументов (Navigator, id), остальные зависимости — обычный constructor injection через Dagger.

## Сборка

- **Version catalogs** в `deps/*.versions.toml` (не `gradle/libs.versions.toml`)
- **Build варианты:** LOCAL, CI_DEV, CI_PROD с разными keystore
- **CI/CD:** GitHub Actions на ветках `IS*` и `MT*`: Lint → Tests → Build APK
- **Convention plugin** в `build-logic/` (частично используется)

## Ключевые технические решения

| Решение | Обоснование |
|---------|-------------|
| Кастомный TEA (Mate) vs MVVM | Явные эффекты, чистые редьюсеры, трассируемый стейт |
| Dagger 2 vs Hilt | Проект старше Hilt, миграция не приоритетна |
| Модуль на каждый экран | Чёткая изоляция, независимая компиляция |
| @Stable vs @Immutable | Более гибко для вложенных ссылок |
| `@AssistedInject` + `@AssistedFactory` для всех ViewModel | Constructor injection без Hilt, Navigator/runtime аргументы через `@Assisted` |
| Sealed interface для Msg | Exhaustive when, явный каталог сообщений |
| Navigator интерфейс в screen, Impl в app | Screen не зависит от Compose Navigation, легко мокать в тестах |
| `CompositionRoot` / `CompositionRootImpl` | Единая точка связки Dagger → Compose: фабрики ViewModel встречаются с навигационными callbacks |
