# Findings: ревью плана EffectHandlers через Dagger (08_)

Четыре ревью: Architect, Dagger DI, Compose/TEA, Migration. Findings отфильтрованы — противоречащие задумке (modules/navigation, Hilt, перенос Impl в screen) убраны.

---

## КРИТИЧНЫЕ (блокеры реализации)

### 1. KSP processor не упомянут в плане для screen модулей
**Статус:** ✅ обработано — в этап 2 добавлен sub-step с подключением KSP + Dagger в build.gradle.kts.

### 2. @IntoMap биндинги мигрируемых ViewModel'ей не удалены
**Статус:** ✅ обработано — в этап 2 (шаг 6) добавлено удаление `@IntoMap @ViewModelKey` биндинга из DI модуля.

### 3. NavController lifecycle: VM может держать мёртвый NavController
**Статус:** ✅ обработано — в план добавлена секция "Контракт NavController". В Lexeme NavController пересоздаётся только вместе с ViewModel (configuration change, process death, nested NavHost) — парные. Зафиксировано требование Navigator stateless.

### 4. NavigationEffect должен быть sealed для exhaustive when
**Статус:** ✅ обработано — в базовый класс добавлен дженерик `E : NavigationEffect`. Per-screen эффект — `sealed interface` в своём модуле, exhaustive `when` работает через дженерик. `NavigationEffect` остаётся обычным interface (sealed нельзя — наследники в screen модулях).

### 5. emptyMsg + consumer(emptyMsg) — убрать из базового класса
**Статус:** ✅ обработано — `emptyMsg` и `consumer(emptyMsg)` удалены из базового класса. Конвенция в `effect-handlers.md` обновлена: "Consumer вызывается только при полезном msg".

### 6. final override + hardcoded поведение — лишить гибкости
**Статус:** ✅ обработано — `consumer(emptyMsg)` убран (finding 5). `final` на `runEffect` оставлен — это правильно (filter+dispatch — обязанность базового). `consumer` в `onScreenEffect` не передаём — не нужно в 99% случаев.

### 7. ExitApp недоделан — комментарий-заглушка
**Статус:** ✅ обработано — `ExitApp` убран из базового `NavigationEffect`. Добавляется в per-screen sealed effect только для root экранов (Splash, DictionaryList). Per-screen Navigator получает `fun exit()`, `NavigatorImpl` принимает `onExit: () -> Unit` callback. Activity извлекается через `LocalActivity.current` в navigation graph.

### 8. MainScreen "composition нескольких NavController'ов" — не решение
**Статус:** ✅ обработано — в план добавлена секция "Cross-graph навигация". Tab NavigatorImpl принимает `tabsNavController` + callbacks для root операций. MainScreen не знает про rootNavController — только про callbacks (сохраняется текущий паттерн).

### 9. DictionaryAppBar shared widget — решение должно быть до миграции
**Статус:** ✅ обработано — один `DictionaryAppBarNavigator` + один `DictionaryAppBarNavigatorImpl` (поведение во всех 3 табах идентично). В MainScreen создаётся один инстанс через `remember`, передаётся во все табы.

### 10. Conditional onBackPress — миграция бизнес-логики
**Статус:** ✅ обработано — в этап 3 добавлен пункт 7: conditional навигация переезжает в reducer (`Msg.RequestBack` → проверка state → разный effect). Помечено как миграция бизнес-логики.

### 11. BackHandler не упомянут в плане
**Статус:** ✅ обработано — в этап 3 (пункт 5) добавлено: `BackHandler { viewModel.accept(Msg.RequestBack) }`. Системная кнопка идёт через reducer.

### 12. Табы отсутствуют в плане
**Статус:** ✅ обработано — в таблицу порядка добавлены 4 таба (VocabularyTab, QuizTab, StatisticTab, SettingsTab) с зависимостями.

### 13. Порядок миграции: AppBar/табы должны быть ДО MainScreen
**Статус:** ✅ обработано — порядок переписан: root экраны (1-3) → nested (4-5) → AppBar (6) → табы (7-10) → MainScreen (11). Обоснование добавлено в план.

---

## ВАЖНЫЕ

### 14. AssistedFactory getters в AppComponent не упомянуты
**Статус:** ✅ обработано — в этап 2 (шаг 7) добавлено: "Добавить getter `getXxxViewModelFactory(): XxxViewModel.Factory` в `AppComponent`".

### 15. Тесты не упомянуты в этапе 4 (Cleanup)
**Статус:** ✅ обработано — в этап 4 добавлен пункт 4: моки NavigationEffectHandler заменить на моки Navigator, тесты ViewModel с factories, reducer-тесты для conditional навигации.

### 16. Stability NavigatorImpl — нужен remember
**Статус:** ✅ обработано — в плане Navigation graph оборачивает `NavigatorImpl(navController)` в `remember(navController)`.

### 17. Связь с Backlog `MateTypedEffectHandler` задачей
**Статус:** ✅ обработано — этап 2 переименован "Миграция DatasourceEffectHandler на @Inject + MateTypedEffectHandler". Совмещаем две миграции в один PR per экран. Добавлены особые случаи из аудита (пустые DatasourceEffect, crash в chat, FilterFlags конфликт).

### 18. Базовый `MateNavigationEffectHandler` должен наследовать `MateTypedEffectHandler`
**Статус:** ✅ обработано — `MateNavigationEffectHandler` теперь наследует `MateTypedEffectHandler<Msg, NavigationEffect>`. Фильтрация и `return` без consumer — в базовом TypedEffectHandler. Единый паттерн.

### 19. Раздел "Что в итоге" противоречит документу
**Статус:** ✅ обработано — секция переписана. Убраны упоминания modules/navigation, ViewModel/Composable с NavController. Соответствует телу документа.

### 20. Существующие AssistedInject (WordCardViewModel) — двойная правка
**Статус:** ✅ обработано — в этап 2 добавлена секция про уже мигрированные AssistedInject ViewModel'и. Этапы 2 и 3 — две независимые правки одного файла, отдельные PR.

### 21. Этап 4 (Cleanup) упустил пункты
**Статус:** ✅ обработано — в этап 4 добавлены: удаление старых NavigationEffectHandler (пункт 2), `launchSingleTop = true` в NavigatorImpl (пункт 5).

---

## ЗАМЕЧАНИЯ

### 22. Scoping policy для @Inject handlers
**Статус:** ✅ обработано — в план добавлена секция "Правила и заметки" / Scoping handlers: handlers всегда unscoped.

### 23. NavigatorImpl без тестов — смещение ответственности
**Статус:** ✅ обработано — правило "NavigatorImpl без логики, одна строка на метод" в секции "Правила и заметки".

### 24. 7 точек изменения на новый экран
**Статус:** ✅ обработано — упомянуто в секции "Правила и заметки" как сознательная плата за чистоту.

### 25. Правило для новых экранов во время миграции
**Статус:** ✅ обработано — exit-criteria для этапа 1 в секции "Правила и заметки": после завершения этапа 1 новые экраны только по новому паттерну.

### 26. Splash openMain — наследование ради наследования
**Статус:** ✅ обработано — в секции "Правила и заметки": Splash переопределяет `back()` как no-op или exit. ExitApp обязательный в Splash sealed effect.

### 27. Process death / state restore
**Статус:** ✅ обработано — задокументировано в секции "Правила и заметки": Navigator пересоздаётся парно с ViewModel (см. Контракт NavController).

---

## Сводная таблица

| # | Проблема | Источник | Уровень |
|---|----------|----------|---------|
| 1 | KSP не упомянут в screen модулях | Dagger | КРИТ |
| 2 | @IntoMap биндинги ViewModel'ей не удалены | Dagger | КРИТ |
| 3 | NavController lifecycle — VM мёртвая ссылка | Architect, Compose | КРИТ |
| 4 | NavigationEffect должен быть sealed | Architect, Compose | КРИТ |
| 5 | emptyMsg + consumer(emptyMsg) убрать | Architect, Compose | КРИТ |
| 6 | final + hardcoded — лишает гибкости | Compose | КРИТ |
| 7 | ExitApp недоделан (Activity callback) | Architect, Compose | КРИТ |
| 8 | MainScreen "composition нескольких" — TODO | Architect, Migration | КРИТ |
| 9 | DictionaryAppBar shared widget — решение до миграции | Migration | КРИТ |
| 10 | Conditional onBackPress — миграция бизнес-логики | Compose | КРИТ |
| 11 | BackHandler не упомянут | Compose | КРИТ |
| 12 | Табы отсутствуют в плане | Migration | КРИТ |
| 13 | Порядок: AppBar/tabs ДО MainScreen | Migration | КРИТ |
| 14 | AssistedFactory getters в AppComponent | Dagger | ВАЖН |
| 15 | Тесты не упомянуты в Cleanup | Migration | ВАЖН |
| 16 | Stability NavigatorImpl — нужен remember | Compose | ВАЖН |
| 17 | Связь с Backlog MateTypedEffectHandler | Migration | ВАЖН |
| 18 | Унифицировать с MateTypedEffectHandler | Architect | ВАЖН |
| 19 | "Что в итоге" противоречит документу | Architect | ВАЖН |
| 20 | Существующие AssistedInject — двойная правка | Migration | ВАЖН |
| 21 | Cleanup упустил: старые handlers, launchSingleTop | Migration | ВАЖН |
| 22 | Scoping policy для @Inject handlers | Dagger | замечание |
| 23 | NavigatorImpl без тестов | Architect | замечание |
| 24 | 7 точек изменения на screen | Architect | замечание |
| 25 | Правило для новых экранов | Migration | замечание |
| 26 | Splash openMain — наследование ради наследования | Migration | замечание |
| 27 | Process death / state restore | Architect | замечание |
