# Spec: DI Graph — принципы и текущее состояние

## Принципы

### 1. Single Responsibility Component

Один компонент = одна ответственность. Не мешать DB + UI + Network в одном компоненте.

**Текущее состояние:** ⚠️ нарушается → ✅ после шага 1.

AppComponent сейчас наследует `AppProvider : CoreDbProvider` — экспонирует и UseCase'ы, и сырые DB API. Две ответственности: composition root + data layer provider.

После шага 1 (удаление AppProvider) — AppComponent только composition root. DB API доступен внутри графа через `dependencies`, но не экспонируется наружу.

---

### 2. No proxy components

Компонент без собственных модулей, который только пробрасывает зависимости — бесполезен.

**Текущее состояние:** ❌ нарушается, принято осознанно.

`CoreDbComponent` — proxy без модулей, пробрасывает `CoreDbProvider` от `RoomComponent`. Существует как архитектурный слой между `core-db-impl` (RoomComponent) и `app` (AppComponent). Удаление отвергнуто — пользователь хочет сохранить этот слой.

---

### 3. Unidirectional dependency

Зависимости идут в одну сторону: core → domain → presentation → app. Обратных нет.

**Текущее состояние:** ⚠️ нарушается → ✅ после шага 1.

`AppProvider : CoreDbProvider` — presentation layer (app) расширяет интерфейс data layer (core-db-api). Потребители AppComponent видят DB API напрямую. Обратная зависимость.

После шага 1 — AppComponent зависит от CoreDbProvider через `dependencies` (однонаправленно), но не экспонирует его. UseCase'ы получают DB API через constructor injection внутри графа.

---

### 4. Flat over deep

Плоский граф лучше глубокого.

**Текущее состояние:** ❌ нарушается, принято осознанно.

Цепочка инициализации — 4 уровня:
```
LoggerComponent → RoomComponent → CoreDbComponent → AppComponent
```

LoggerComponent существует для разрыва цикла (принцип 5). CoreDbComponent — proxy (принцип 2). Оба приняты. Глубина — следствие.

---

### 5. No circular workarounds

Если нужен отдельный компонент чтобы разорвать цикл — граф спроектирован неправильно.

**Текущее состояние:** ❌ нарушается, принято осознанно.

`LoggerComponent` существует только чтобы разорвать цикл: Logger нужен RoomComponent, но LoggerModule зависит от `BuildConfig` (app модуль). RoomComponent создаётся до AppComponent. Решение — отдельный LoggerComponent, создаётся первым.

Альтернативы (Logger в core модуле, Logger без Dagger) отвергнуты. LoggerComponent остаётся как Dagger-компонент.

---

### 6. Composition Root

App модуль — **composition root**. Единственное место где фичи встречаются и связываются.

**Текущее состояние:** ✅ соблюдается.

- Фичи не знают друг о друге — screen модули зависят только от своих UseCase интерфейсов
- App знает все фичи — AppModule включает все feature-модули
- `MainUiDepsProvider` — composition root для UI: связывает Dagger-зависимости с Compose-экранами через интерфейсы (MainUiDeps, DictionaryTabUiDeps и т.д.)
- Фича зависит от абстракции (интерфейс в своём модуле), не от реализации (виджет в чужом модуле)
- Рост composition root пропорционален количеству фичей — это нормально, это его ответственность

---

### 7. Constructor injection everywhere

Зависимости приходят через конструктор или @BindsInstance. Никакого service locator.

**Текущее состояние:** ❌ нарушается, принято осознанно.

`context.appComponent` — extension property в `App.kt`. Используется в RootRouter и MainRouter для получения зависимостей:

```kotlin
val context = LocalContext.current
MainScreen(mainUiDeps = MainUiDepsProvider(
    dictionaryTabUseCase = context.appComponent.getVocabularyUseCase(),
    ...
))
```

Это service locator паттерн. Но в Compose + Dagger без Hilt — единственный способ передать зависимости из Dagger в Compose navigation graph. Composable функции не имеют конструкторов, Dagger не может в них инжектить.

UseCase'ы внутри Dagger графа получают зависимости через constructor injection — это соблюдается. Нарушение только на границе Dagger → Compose.

---

### 8. Scoped where needed

@Singleton только для реально разделяемых объектов. UseCase — unscoped.

**Текущее состояние:** ⚠️ → ✅ после шага 2.

Что @Singleton (правильно):
- Database (RoomModule) — один инстанс БД
- PrefsProvider (PrefsProviderModule) — один DataStore
- CountryProvider (CountryProviderModule) — загрузка данных стран один раз

Что unscoped (правильно):
- Все UseCase'ы — stateless, создаются при запросе

Проблема: `CoreInteractorComponent` — `@Singleton` scope, но singleton обеспечен через nullable `var` без `synchronized`. Race condition. Шаг 2 удаляет его (мёртвый код).

---

### 9. Small modules (кроме composition root)

Модуль провайдит 1-3 вещи.

**Текущее состояние:** ✅ соблюдается.

Каждый feature-модуль маленький — один `@Binds`:
```kotlin
@Module
interface DictionaryTabModule {
    @Binds
    fun bindVocabularyUseCase(impl: DictionaryTabUseCaseImpl): DictionaryTabUseCase
}
```

AppModule — агрегатор, включает 13 feature-модулей. Это composition root (принцип 6), не god-module. Каждый включённый модуль — маленький.

---

### 10. Interface segregation (кроме composition root)

Provider interface экспонирует только то что нужно потребителю.

**Текущее состояние:** ⚠️ частично.

Нарушение: `CoreDbProvider` — 8 методов. Каждый UseCase видит все 8, хотя использует 1-3. Например `SplashUseCaseImpl` нуждается только в `dictionaryApi`, но через DI граф видит все 8 API.

Принято: сегрегация CoreDbProvider требует рефакторинга core-db-api, core-db-impl, всех UseCaseImpl. Отложено в бэклог.

Composition root (`MainUiDepsProvider` с 11 параметрами, `AppComponent` с 13 getter'ами) — не нарушение. Ему нужны все зависимости по определению.

---

## Сводка

| # | Принцип | Статус | Действие |
|---|---------|--------|----------|
| 1 | Single Responsibility | ⚠️ → ✅ | Шаг 1 фиксит |
| 2 | No proxy | ❌ | Принято |
| 3 | Unidirectional | ⚠️ → ✅ | Шаг 1 фиксит |
| 4 | Flat over deep | ❌ | Принято |
| 5 | No circular workarounds | ❌ | Принято |
| 6 | Composition Root | ✅ | Соблюдается |
| 7 | Constructor injection | ❌ | Принято (граница Dagger→Compose) |
| 8 | Scoped where needed | ⚠️ → ✅ | Шаг 2 фиксит |
| 9 | Small modules | ✅ | Соблюдается |
| 10 | Interface segregation | ⚠️ | CoreDbProvider — бэклог |
