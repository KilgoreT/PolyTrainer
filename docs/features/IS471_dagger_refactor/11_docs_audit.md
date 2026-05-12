# Аудит документации после IS471

## TL;DR

После IS471 (Dagger DI рефакторинг + Navigator паттерн) часть гайдов и спеков отстала от кода. Найдено: 5 файлов **полностью устарели**, 7 **частично устарели**, 7 **актуальны**. Дополнительно обнаружено 7 файлов мёртвой DI инфраструктуры (`@IntoMap @ViewModelKey` поддержка, никем не используется).

**Все правки выполнены.** Build/tests/lint зелёные. См. раздел "Выполнено" ниже.

## Контекст изменений

| Изменение | Старое | Новое |
|---|---|---|
| Composition root | `MainUiDeps` / `MainUiDepsProvider` | `CompositionRoot` / `CompositionRootImpl` |
| ViewModel binding | `@Inject` + `@IntoMap @ViewModelKey` | `@AssistedInject` + `@AssistedFactory` |
| ViewModel в composable | `daggerViewModel()` | `viewModel(factory = viewModelFactory { factory.create(...) })` |
| Datasource handler | `MateEffectHandler<Msg, Effect>` + ручной `when (effect as? X)` | `MateTypedEffectHandler<Msg, E>` + `filter()` + `onEffect()` |
| Navigation handler | callback-based `NavigationEffectHandler(onBack, onExit)` | `MateNavigationEffectHandler<Msg>(navigator)` + `onScreenEffect()` |
| Базовый `NavigationEffect` | `Back` + `ExitApp` | только `Back` |
| ExitApp | `NavigationEffect.ExitApp` | per-screen `XxxNavigationEffect.ExitApp` |
| Навигация в composable | `onBackPress: () -> Unit` callback | `navigator: XxxNavigator` параметр |
| Навигация из state | флаги `closeScreen`, `exit` | `NavigationEffect.Back` через reducer |
| AppBar в табах | callback `openDictionaryCreate` | `factory + navigator` |
| `navigate(...)` | без флагов | `launchSingleTop = true` |

---

## Спеки `docs/features-spec/`

| Файл | Статус | Crit | Cosm |
|---|---|---:|---:|
| dagger-di-principles.md | ✅ актуален | 0 | 1 |
| navigation.md | ✅ актуален | 0 | 0 |
| webview-screen.md | ✅ актуален | 0 | 1 |
| flag-placeholder-widget.md | ✅ не затронут | 0 | 0 |
| logger.md | ✅ не затронут | 0 | 1 |
| dictionary-create.md | ⚠️ частично | 2 | 0 |
| dictionary-appbar.md | ⚠️ частично | 3 | 1 |
| dictionary-list.md | ❌ устарел (давний долг) | 4 | 0 |

### Критичное

**dictionary-list.md** — врёт системно (не от IS471, давний долг):
- L9-14: "три route — один экран" — `DICTIONARY_SETUP` ведёт на FormScreen, не на List
- L15: callbacks `onClose`/`onBackPress`/`onExit` — больше не существуют, заменены `ListNavigator`
- L32-34: путь `setCurrentDictionary` → `onClose()` — несуществующая логика List экрана
- L67-72: "AppBar через nullable `onBackPress`" — реально AppBar показывается по `state.dictionaries.isNotEmpty()`

**dictionary-appbar.md** — пропущена ключевая фича:
- L58-74: в таблицах Messages/Effects нет `Msg.OpenDictionaryCreate` и `DictionaryAppBarNavigationEffect.OpenDictionaryCreate` — главная навигационная фича виджета
- Не упомянуто, что widget — shared (back() — no-op), и Navigator через AssistedFactory

**dictionary-create.md** — мелкое:
- L12, L65-67: формулировка "callback handler" — устарела, сейчас `FormNavigationEffectHandler` → `FormNavigator.back()`
- Не упомянут вынос `FilterFlags` в отдельный `FlagFilterEffect`

---

## Гайды `docs/guides/`

| Файл | Статус | Crit | Cosm |
|---|---|---:|---:|
| effect-handlers.md | ✅ актуален | 0 | 2 |
| navigation.md | ✅ актуален | 1 | 2 |
| testing-migrations.md | ✅ не затронут | 0 | 0 |
| messages.md | ⚠️ частично | 1 | 1 |
| reducer-patterns.md | ⚠️ частично | 2 | 1 |
| state-and-extensions.md | ⚠️ частично | 2 | 1 |
| testing-reducers.md | ⚠️ частично | 1 | 2 |
| mate-framework.md | ⚠️ устарел | 3 | 2 |
| testing-extensions.md | ⚠️ устарел | 1 | 1 |
| dagger-di.md | ❌ устарел | 5 | 1 |
| ui-patterns.md | ❌ устарел | 3 | 3 |
| project-architecture.md | ❌ устарел | 4 | 2 |
| viewmodel-wiring.md | ❌ полностью устарел | 6 | — |

### viewmodel-wiring.md — переписать с нуля

- L11-42: ручной `ViewModelProvider.Factory(wordId, useCase)` без Dagger
- L142-156: `MateEffectHandler<Msg, Effect>` со старым `null -> Msg.Empty`
- L190-218: рецепт "новый экран" не упоминает AssistedFactory, AppComponent getter, CompositionRootImpl, Navigator

### dagger-di.md — мёртвый раздел

- L168-204: целый раздел "@Inject + @IntoMap" — паттерн не используется ни одной VM
- L189-191: `@Binds @IntoMap @ViewModelKey(...)` биндинг — удалён из всех модулей
- L194-202: `daggerViewModel()` — заменён на `viewModelFactory { factory.create(...) }`
- L280-297: таблица "Что выбрать?" и конвенции 8-9 — врут

### project-architecture.md — общая архитектура отстала

- L31-35: упомянут `core/core-interactor` — удалён в `e229d6d`
- L104-110: "доступ из composable через `context.appComponent.getXxxUseCase()`" — устарело
- L124-139: ручной `WordCardViewModel.Factory` — устарело
- Нет упоминаний: `CompositionRoot`, Navigator, `MateTypedEffectHandler`, `MateNavigationEffectHandler`, AssistedInject

### ui-patterns.md — учит мёртвым паттернам

- L94-99: `LaunchedEffect(state.closeScreen) { if (state.closeScreen) onBackPress() }` — прямо учит делать то, что IS471 удалила
- L11-27: UseCase в сигнатуре composable + onBack callback
- L141-154: `TopBarWidget(state, onBackPress, sendMsg)` — устарело

### mate-framework.md — не догнал Mate

- L54-64: не упоминает `MateTypedEffectHandler`, `MateNavigationEffectHandler`, `Navigator`, `NavigationEffect`
- L143-159: пример `ChatViewModel` без `@AssistedInject`
- L171: категории эффектов — только `DatasourceEffect` + `UiEffect`, пропущена `NavigationEffect`

### state-and-extensions.md — внутреннее противоречие

- L9: принцип "State без навигационных флагов" — верный
- L18: пример с `val closeScreen: Boolean = false` — нарушает L9 в том же файле
- L75: `fun ChatScreenState.exit() = copy(exit = true)` — поле удалено

### reducer-patterns.md — антипаттерн как пример

- L96-108 (Паттерн 5): `UserAction.EXIT -> emptySet()` + `state.exit()` — антипаттерн
- L115-126: пример WordCardReducer упоминает `closeScreen`
- Нет паттерна conditional `ExitApp` vs `Back` в reducer

### messages.md

- L40: `CloseScreen`, `Exit` в таблице категорий — устаревшие имена, теперь `RequestBack`, `OpenXxx(...)`
- Нет паттерна `Msg.RequestBack` для BackHandler

### testing-extensions.md / testing-reducers.md

- testing-extensions L51-57: пример с `exit = false` + проверки иммутабельности `exit` — поле удалено, не компилируется
- testing-reducers L43-216: ни одного примера тестирования `NavigationEffect.Back` / `XxxNavigationEffect.OpenYyy` через `assertSingleEffect`
- testing-reducers L216-223: в "Что тестировать" пропущена категория навигационных эффектов

### navigation.md (guide) — минор

- L55, L67: параметр `openAddDict` в сигнатуре MainScreen — реально `openDictionaryCreate` + `openDictionaryList`

---

## Архитектурный долг: мёртвая DI инфраструктура

После IS471 ни одна ViewModel не использует `@IntoMap @ViewModelKey`. Map биндингов **пустая**. На ней висят 7 файлов мёртвого кода:

| Файл | Что делает | Кем используется |
|---|---|---|
| `core/di/ViewModelKey.kt` | annotation | никем |
| `core/di/DaggerViewModelFactory.kt` | factory из пустой Map | никем |
| `core/di/ViewModelFactoryProvider.kt` | контракт | никем |
| `core/di/daggerViewModel()` helper | resolve через провайдер | никем в screen модулях |
| `App.kt: ViewModelFactoryProvider` | имплементация | никем |
| `AppComponent.viewModelFactory()` | getter | никем |
| `ViewModelModule.@Multibinds viewModels()` | пустая Map | DaggerViewModelFactory (пустую) |

**Что делать:** отдельной задачей удалить целиком. Backlog → `## ВекторныйПиздеж`.

---

## Противоречия между гайдами

1. **dagger-di.md vs viewmodel-wiring.md** — один описывает "два паттерна Dagger", другой — ручной Factory без Dagger. Реальный код — только AssistedFactory. Оба врут.
2. **mate-framework.md vs effect-handlers.md** — mate-framework не упоминает typed/navigation базы, effect-handlers строит на них всё.
3. **viewmodel-wiring.md Шаг 3 vs effect-handlers.md** — учит старому `MateEffectHandler` с `null -> Msg.Empty`, effect-handlers это явно объявляет удалённым.
4. **state-and-extensions.md** — L9 декларирует принцип, L18+L75 его нарушают.

---

## Выполнено

### Код — удалена мёртвая `@IntoMap` инфраструктура

Удалены файлы (никем не используются после IS471):
- `modules/core/di/ViewModelKey.kt`
- `modules/core/di/DaggerViewModelFactory.kt`
- `modules/core/di/ViewModelFactoryProvider.kt` (включая `daggerViewModel()` helper)
- `app/.../di/module/ViewModelModule.kt` (`@Multibinds` для пустой Map)

Очищено:
- `App.kt` — убрана реализация `ViewModelFactoryProvider`
- `AppComponent.kt` — убран `fun viewModelFactory()` getter
- `AppModule.kt` — убран `ViewModelModule` из `includes`

`ViewModelHelper.kt` (`viewModelFactory { ... }`) — оставлен, используется screen модулями.

Build/tests/lint после удаления — зелёные.

### Документация — все правки сделаны

**Гайды:**
1. `viewmodel-wiring.md` — переписан с нуля под AssistedInject + Navigator
2. `ui-patterns.md` — удалён `LaunchedEffect(state.closeScreen)`, обновлены сигнатуры экранов и AppBar
3. `dagger-di.md` — выпилен раздел "@Inject + @IntoMap", обновлены граф и конвенции
4. `project-architecture.md` — добавлены CompositionRoot, Navigator, MateTyped/Navigation базы, обновлена схема слоёв
5. `mate-framework.md` — добавлены `MateTypedEffectHandler`, `MateNavigationEffectHandler`, `Navigator`, обновлён пример ViewModel
6. `state-and-extensions.md` — удалён `closeScreen` из примера, удалён `exit()` extension, добавлен явный запрет навигационных флагов
7. `reducer-patterns.md` — переписан Паттерн 5 (UserAction.EXIT через NavigationEffect.Back), добавлен Паттерн 6 (conditional ExitApp vs Back)
8. `messages.md` — заменены `CloseScreen/Exit` на `RequestBack/OpenXxx`, добавлен раздел про навигационные сообщения
9. `testing-reducers.md` — добавлен раздел "Тестирование навигационных эффектов" с примерами, обновлены "Что НЕ тестировать"
10. `testing-extensions.md` — удалён `exit = false` из примера
11. `navigation.md` (guide) — фикс сигнатуры MainScreen

**Спеки:**
12. `dictionary-appbar.md` — добавлены `Msg.OpenDictionaryCreate`, `DictionaryAppBarNavigationEffect.OpenDictionaryCreate`, раздел про Navigator, попутно исправлены битые UTF-символы
13. `dictionary-create.md` — освежён под `FormNavigator`/`MateNavigationEffectHandler`, добавлен раздел про FlagFilterEffect
14. `dictionary-list.md` — переписан целиком: убрана давняя ложь про "три route — один экран", описана текущая архитектура с conditional навигацией

### Итог

| Категория | Файлов до | Файлов после правок |
|---|---|---|
| Гайды устарели/частично | 10 | 0 |
| Спеки устарели/частично | 3 | 0 |
| Мёртвый код | 7 файлов / артефактов | удалено |
