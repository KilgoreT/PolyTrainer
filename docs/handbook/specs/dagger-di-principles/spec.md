# Spec: DI Graph — принципы и текущее состояние

## Принципы

### 1. Single Responsibility Component

Один компонент = одна ответственность. Не мешать DB + UI + Network в одном компоненте.

**Статус:** ✅ соблюдается.

AppComponent — composition root. DB API доступен UseCase'ам внутри графа через `dependencies`, но не экспонируется наружу.

---

### 2. No proxy components

Компонент без собственных модулей, который только пробрасывает зависимости — бесполезен.

**Статус:** ❌ нарушается, принято осознанно.

`CoreDbComponent` — proxy без модулей, пробрасывает `CoreDbProvider` от `RoomComponent`. Существует как архитектурный слой между `core-db-impl` (RoomComponent) и `app` (AppComponent).

---

### 3. Unidirectional dependency

Зависимости идут в одну сторону: core → domain → presentation → app. Обратных нет.

**Статус:** ✅ соблюдается.

AppComponent зависит от CoreDbProvider через `dependencies` (однонаправленно), но не экспонирует его. UseCase'ы получают DB API через constructor injection внутри графа.

---

### 4. Flat over deep

Плоский граф лучше глубокого.

**Статус:** ❌ нарушается, принято осознанно.

Цепочка инициализации — 4 уровня:
```
LoggerComponent → RoomComponent → CoreDbComponent → AppComponent
```

LoggerComponent существует для разрыва цикла (принцип 5). CoreDbComponent — proxy (принцип 2). Оба приняты. Глубина — следствие.

---

### 5. No circular workarounds

Если нужен отдельный компонент чтобы разорвать цикл — граф спроектирован неправильно.

**Статус:** ❌ нарушается, принято осознанно.

`LoggerComponent` существует только чтобы разорвать цикл: Logger нужен RoomComponent, но LoggerModule зависит от `BuildConfig` (app модуль). RoomComponent создаётся до AppComponent. Решение — отдельный LoggerComponent, создаётся первым.

---

### 6. Composition Root

App модуль — **composition root**. Единственное место где фичи встречаются и связываются.

**Статус:** ✅ соблюдается.

- Фичи не знают друг о друге — screen модули зависят только от своих UseCase интерфейсов
- App знает все фичи — AppModule включает все feature-модули
- `CompositionRootImpl` (реализация `CompositionRoot`) — composition root для UI: связывает Dagger-зависимости с Compose-экранами через ViewModel `AssistedFactory` и `XxxNavigator` интерфейсы
- `NavigatorImpl` классы в `app/.../navigator/` — composition root для навигации: знают про NavController + Compose Navigation, screen модули видят только Navigator интерфейс
- Фича зависит от абстракции (интерфейс в своём модуле), не от реализации (виджет в чужом модуле)
- Рост composition root пропорционален количеству фичей — это нормально, это его ответственность

---

### 7. Constructor injection everywhere

Зависимости приходят через конструктор или @BindsInstance. Никакого service locator.

**Статус:** ❌ нарушается, принято осознанно.

`context.appComponent` — extension property в `App.kt`. Используется в RootRouter и MainRouter для получения зависимостей:

```kotlin
val context = LocalContext.current
MainScreen(compositionRoot = CompositionRootImpl(
    wordCardViewModelFactory = context.appComponent.getWordCardViewModelFactory(),
    chatViewModelFactory = context.appComponent.getChatViewModelFactory(),
    appBarViewModelFactory = context.appComponent.getDictionaryAppBarViewModelFactory(),
    // ... + остальные Factory + envParams + logger
))
```

В Compose + Dagger без Hilt — единственный способ передать зависимости из Dagger в Compose navigation graph. Composable функции не имеют конструкторов, Dagger не может в них инжектить.

UseCase'ы и handlers внутри Dagger графа получают зависимости через constructor injection (`@Inject` / `@AssistedInject`) — это соблюдается. Нарушение только на границе Dagger → Compose.

---

### 8. Scoped where needed

@Singleton только для реально разделяемых объектов. UseCase — unscoped.

**Статус:** ✅ соблюдается.

@Singleton:
- Database (RoomModule) — один инстанс БД
- PrefsProvider (PrefsProviderModule) — один DataStore
- CountryProvider (CountryProviderModule) — загрузка данных стран один раз

Unscoped:
- Все UseCase'ы — stateless, создаются при запросе
- ViewModel'ы — lifecycle управляется ViewModelStore, не Dagger scope

---

### 9. Small modules (кроме composition root)

Модуль провайдит 1-3 вещи.

**Статус:** ✅ соблюдается.

Каждый feature-модуль маленький — только `@Binds` для UseCase реализации. ViewModel биндится через `AssistedFactory`, который Dagger генерирует из `@AssistedInject constructor` — отдельный модуль для этого не нужен.

```kotlin
@Module
interface DictionaryTabModule {
    @Binds
    fun bindUseCase(impl: DictionaryTabUseCaseImpl): DictionaryTabUseCase
}
```

Factory ViewModel'и экспонируется через AppComponent getter:

```kotlin
fun getDictionaryTabViewModelFactory(): DictionaryTabViewModel.Factory
```

AppModule — агрегатор, включает feature-модули. Это composition root (принцип 6), не god-module. Каждый включённый модуль — маленький.

---

### 10. Interface segregation (кроме composition root)

Provider interface экспонирует только то что нужно потребителю.

**Статус:** ⚠️ частично.

Нарушение: `CoreDbProvider` — 8 методов. Каждый UseCase видит все 8, хотя использует 1-3.

Composition root (`CompositionRootImpl`, `AppComponent`) — не нарушение. Ему нужны все зависимости по определению. `CompositionRootImpl` принимает 7 `XxxViewModel.Factory` + envParams + logger. AppComponent экспонирует Factory каждой `@AssistedInject` ViewModel через явный getter — single point of truth для composition root.

---

### 11. Assisted injection для runtime параметров

ViewModel'и с runtime зависимостями (Navigator, wordId, editingId) используют `@AssistedInject` + `AssistedFactory`. Это позволяет Dagger построить большую часть графа автоматически, а runtime значения передавать в момент создания.

**Статус:** ✅ соблюдается.

Шаблон:

```kotlin
class XxxViewModel @AssistedInject constructor(
    @Assisted navigator: XxxNavigator,                       // runtime
    @Assisted wordId: Long,                                   // runtime
    datasourceHandler: XxxDatasourceEffectHandler,            // из графа
    navHandlerFactory: XxxNavigationEffectHandler.Factory,    // из графа
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navigator: XxxNavigator, wordId: Long): XxxViewModel
    }
}
```

Та же схема для `XxxNavigationEffectHandler` — Navigator приходит через `@Assisted`, остальное через `@Inject`.

Composable получает `factory: XxxViewModel.Factory` + runtime значения и создаёт ViewModel через helper:

```kotlin
viewModel: XxxViewModel = viewModel(
    factory = viewModelFactory { factory.create(navigator, wordId) },
)
```

Подробнее — `docs/handbook/guides/navigation.md`, `docs/handbook/guides/effect-handlers.md`.

---

## Сводка

| # | Принцип | Статус |
|---|---------|--------|
| 1 | Single Responsibility | ✅ |
| 2 | No proxy | ❌ принято |
| 3 | Unidirectional | ✅ |
| 4 | Flat over deep | ❌ принято |
| 5 | No circular workarounds | ❌ принято |
| 6 | Composition Root | ✅ |
| 7 | Constructor injection | ❌ принято (Dagger→Compose) |
| 8 | Scoped where needed | ✅ |
| 9 | Small modules | ✅ |
| 10 | Interface segregation | ⚠️ CoreDbProvider — бэклог |
| 11 | Assisted injection | ✅ |
