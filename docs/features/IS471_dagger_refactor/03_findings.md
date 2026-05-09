# Findings: DI Graph — нарушения и план рефакторинга

## Нарушения по принципам

### Принцип 1: Single Responsibility Component — НАРУШЕН
**AppComponent** смешивает 4 слоя: DB (через CoreDbProvider), Domain (9 UseCase), Infrastructure (Prefs, Resources, Env, Logger), Presentation (inject MainActivity).
**Фикс:** разделить на InfraComponent + FeatureComponents.
**Сложность:** HIGH

### Принцип 2: No Proxy Components — НАРУШЕН
1. **CoreDbComponent** — компонент без модулей, только пробрасывает CoreDbProvider от RoomComponent.
2. **CoreDbDependenciesComponent** — inner component в AppComponent, ещё одна обёртка.
**Фикс:** удалить оба, использовать RoomComponent напрямую.
**Сложность:** LOW

### Принцип 3: Unidirectional Dependency — НАРУШЕН
**AppProvider extends CoreDbProvider** — presentation layer расширяет data layer interface. Потребители AppProvider видят сырые DB API.
**Фикс:** AppProvider НЕ наследует CoreDbProvider.
**Сложность:** MEDIUM

### Принцип 4: Flat over Deep — НАРУШЕН
Цепочка: LoggerComponent → CoreDbComponent → CoreDbDependenciesComponent → AppComponent (4 уровня).
**Фикс:** после удаления proxy — 2 уровня: RoomComponent → AppComponent.
**Сложность:** LOW (решается попутно с принципом 2)

### Принцип 5: No Circular Workarounds — НАРУШЕН
**LoggerComponent** существует только чтобы разорвать цикл AppComponent → RoomComponent → AppComponent.
**Фикс:** Logger создаётся напрямую без Dagger (если нет DI-зависимостей). Или как часть InfraComponent.
**Сложность:** LOW

### Принцип 6: Component per Feature — НАРУШЕН
**AppModule** включает 13 модулей — по одному на каждую фичу. Добавление фичи = изменение AppModule + AppComponent + MainUiDepsProvider.
**Фикс:** feature subcomponents. Каждая фича провайдит свой UseCase.
**Сложность:** HIGH

### Принцип 7: Constructor Injection Everywhere — НАРУШЕН
1. **Service Locator** — `context.appComponent` в RootRouter (4 вызова) и MainRouter (11 вызовов).
2. **Static singletons** — CoreDbComponent.get(), RoomComponent.get() — companion object с lateinit.
3. **CoreInteractorComponent** — nullable var БЕЗ synchronized (race condition).
**Фикс:** router'ы получают зависимости через DI, убрать static singletons.
**Сложность:** MEDIUM

### Принцип 8: Scoped Where Needed — ЧАСТИЧНО НАРУШЕН
1. LoggerComponent — нет scope (хотя вызывается один раз).
2. CoreDbComponent — singleton через companion object, не через Dagger scope.
3. CoreInteractorComponent — @Singleton scope, но race condition в getOrInit().
**Фикс:** убрать ручные singletons, использовать Dagger scope.
**Сложность:** LOW

### Принцип 9: Small Modules — НАРУШЕН
**AppModule** — god-module с 13 includes.
**Фикс:** разнести по feature subcomponents (решается вместе с принципом 6).
**Сложность:** MEDIUM

### Принцип 10: Interface Segregation — НАРУШЕН
1. **CoreDbProvider** — 8 методов. SplashUseCase нуждается в 1, получает 8.
2. **AppComponent** — 13 expose-методов.
3. **MainUiDepsProvider** — 11 параметров, god-object.
**Фикс:** сегрегация CoreDbProvider на мелкие интерфейсы, feature components.
**Сложность:** HIGH

---

## Целевая архитектура

```
App.onCreate()
│
├── Logger = LexemeLogger(...) // без Dagger
│
├── RoomComponent(ctx, logger) → сегрегированные DbProviders
│     └── Database, DAO, *DbApi
│
├── InfraComponent(ctx, logger, dbProviders)
│     └── PrefsProvider, ResourceManager, EnvParams, CountryProvider
│
└── AppComponent(infraComponent)
      ├── SplashFeatureComponent → SplashUseCase
      ├── DictionaryFeatureComponent → DictionaryUseCase, DictionaryTabUseCase
      ├── QuizFeatureComponent → QuizTabUseCase, QuizChatUseCase
      ├── StatisticFeatureComponent → StatisticUseCase
      ├── SettingsFeatureComponent → SettingsTabUseCase
      └── WordCardFeatureComponent → WordCardUseCase
```

Нет: LoggerComponent, CoreDbComponent, CoreDbDependenciesComponent, CoreInteractorComponent.
Нет: context.appComponent service locator.
Нет: AppProvider : CoreDbProvider наследование.

---

## Порядок рефакторинга

### Фаза 1: Очистка (LOW, отдельные PR'ы)

| Шаг | Что | Scope | Сложность |
|-----|-----|-------|-----------|
| 1 | Удалить CoreInteractorComponent (мёртвый код) | core-interactor | LOW |
| 2 | Удалить CoreDbComponent (proxy) | core-db, app | LOW |
| 3 | Удалить CoreDbDependenciesComponent (proxy) | app | LOW |
| 4 | Удалить LoggerComponent, создавать Logger напрямую | app | LOW |
| 5 | Убрать AppProvider : CoreDbProvider наследование | app | LOW-MEDIUM |

### Фаза 2: Реструктуризация (HIGH, один feature branch)

| Шаг | Что | Scope | Сложность |
|-----|-----|-------|-----------|
| 6 | Сегрегация CoreDbProvider | core-db-api, core-db-impl, app | HIGH |
| 7 | Feature components (subcomponents) | app, все screen modules | HIGH |
| 8 | Убрать service locator | app routing | MEDIUM |

**Рекомендация:** Фаза 1 — шаги 1-5, каждый отдельный PR. Фаза 2 — один бриф, один branch, несколько коммитов. Шаги 6-8 взаимосвязаны.

---

## Сводка

| # | Принцип | Статус | Фаза |
|---|---------|--------|------|
| 1 | Single Responsibility Component | НАРУШЕН | 2 |
| 2 | No Proxy Components | НАРУШЕН | 1 |
| 3 | Unidirectional Dependency | НАРУШЕН | 1 |
| 4 | Flat over Deep | НАРУШЕН | 1 |
| 5 | No Circular Workarounds | НАРУШЕН | 1 |
| 6 | Component per Feature | НАРУШЕН | 2 |
| 7 | Constructor Injection | НАРУШЕН | 2 |
| 8 | Scoped Where Needed | ЧАСТИЧНО | 1 |
| 9 | Small Modules | НАРУШЕН | 2 |
| 10 | Interface Segregation | НАРУШЕН | 2 |
