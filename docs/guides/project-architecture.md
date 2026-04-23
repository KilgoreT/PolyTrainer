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
 +-- core/core-interactor       -- Доменные интеракторы
 +-- core/core-resources        -- Общие ресурсы
```

## Правила зависимостей

1. **Feature модули зависят от core модулей.** Никогда наоборот.
2. **Core модули не зависят друг от друга** (кроме: `ui` зависит от `theme`).
3. **Screen модули не зависят от других screen модулей.** Связь через навигацию.
4. **Widget модули** могут иметь собственные Mate инстансы (например, `dictionaryappbar`).
5. **Legacy core модули** (`core/core-*`) постепенно заменяются.

## Слои

```
UI Layer (Screen.kt)
    |
    | collectAsStateWithLifecycle()
    | accept(Msg)
    v
State Management Layer (ViewModel + Mate + Reducer)
    |
    | Effects
    v
Effect Layer (EffectHandlers)
    |
    | UseCase вызовы
    v
Domain Layer (UseCase интерфейсы)
    |
    | Реализация
    v
Data Layer (Room, DataStore, Prefs)
```

## Dependency Injection

**Dagger 2** с единственным `AppComponent` (Singleton scope).

### AppComponent

```kotlin
@Singleton
@Component(
    dependencies = [CoreDbProvider::class],
    modules = [
        SplashModule::class,
        CreateDictionaryModule::class,
        VocabularyModule::class,
        WordCardModule::class,
        QuizTabModule::class,
        QuizChatModule::class,
        StatisticModule::class,
        SettingsModule::class,
        DictionaryAppBarModule::class,
    ]
)
interface AppComponent {
    fun getSplashUseCase(): SplashUseCase
    fun getWordCardUseCase(): WordCardUseCase
    fun getQuizChatUseCase(): QuizChatUseCase
    fun getResourceManager(): ResourceManager
    fun getPrefsProvider(): PrefsProvider
    fun getLogger(): LexemeLogger
}
```

### Доступ из composable

```kotlin
// Extension-функция
fun Context.appComponent: AppComponent =
    (applicationContext as App).appComponent

// В параметрах экрана
val useCase = context.appComponent.getWordCardUseCase()
```

### DI модуль фичи

```kotlin
@Module
interface QuizChatModule {
    @Binds
    fun bindQuizChatUseCase(impl: QuizChatUseCaseImpl): QuizChatUseCase
}
```

### ViewModel Factory

```kotlin
class WordCardViewModel(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

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
| Factory для ViewModels | Позволяет constructor injection без Hilt |
| Sealed interface для Msg | Exhaustive when, явный каталог сообщений |
