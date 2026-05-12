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
- `MainUiDepsProvider` — composition root для UI: связывает Dagger-зависимости с Compose-экранами через интерфейсы (MainUiDeps, DictionaryTabUiDeps и т.д.)
- Фича зависит от абстракции (интерфейс в своём модуле), не от реализации (виджет в чужом модуле)
- Рост composition root пропорционален количеству фичей — это нормально, это его ответственность

---

### 7. Constructor injection everywhere

Зависимости приходят через конструктор или @BindsInstance. Никакого service locator.

**Статус:** ❌ нарушается, принято осознанно.

`context.appComponent` — extension property в `App.kt`. Используется в RootRouter и MainRouter для получения зависимостей:

```kotlin
val context = LocalContext.current
MainScreen(mainUiDeps = MainUiDepsProvider(
    wordCardViewModelFactory = context.appComponent.getWordCardViewModelFactory(),
    envParams = context.appComponent.getEnvParams(),
    logger = context.appComponent.getLogger(),
))
```

В Compose + Dagger без Hilt — единственный способ передать зависимости из Dagger в Compose navigation graph. Composable функции не имеют конструкторов, Dagger не может в них инжектить.

UseCase'ы внутри Dagger графа получают зависимости через constructor injection — это соблюдается. Нарушение только на границе Dagger → Compose.

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

Каждый feature-модуль маленький — UseCase binding + ViewModel `@IntoMap`:
```kotlin
@Module
interface DictionaryTabModule {
    @Binds
    fun bindUseCase(impl: DictionaryTabUseCaseImpl): DictionaryTabUseCase

    @Binds
    @IntoMap
    @ViewModelKey(DictionaryTabViewModel::class)
    fun bindViewModel(vm: DictionaryTabViewModel): ViewModel
}
```

AppModule — агрегатор, включает feature-модули. Это composition root (принцип 6), не god-module. Каждый включённый модуль — маленький.

---

### 10. Interface segregation (кроме composition root)

Provider interface экспонирует только то что нужно потребителю.

**Статус:** ⚠️ частично.

Нарушение: `CoreDbProvider` — 8 методов. Каждый UseCase видит все 8, хотя использует 1-3.

Composition root (`MainUiDepsProvider`, `AppComponent`) — не нарушение. Ему нужны все зависимости по определению. MainUiDepsProvider — 3 параметра, AppComponent — 6 getter'ов (большинство ViewModel'ов биндится через `@IntoMap`, не через явные getter'ы).

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
