---
status: done
---

# Summary — infra (IS481 component_constructor)

## Что сделано

Infra sub-flow для IS481 component_constructor завершён: подготовлена вся wiring-инфраструктура для двух новых screen-модулей (`components_manager` / `per_dictionary_components`) + одного widget-модуля (`component_widgets`) + расширены два host'а с drill-in переходами (SettingsTab → ComponentsManager; DictionaryAppBar shared widget → PerDictionaryComponents в трёх tab-host'ах Vocabulary/Quiz/Statistic). Реальная mate-логика / UseCase методы / UI откладываются на business_implement / ui_implement; на infra-уровне зафиксированы только interfaces, factories, DI graph, Navigator wiring и navigation routes.

### Новые модули (3 шт)

- **`:modules:widget:component_widgets`** — Tier 2 widget без DI/KSP (template-preview composables + `ComponentBlock` wrapper). gradle setup создан; dependencies: `:modules:core:theme`, `:modules:core:ui`, `:modules:domain:lexeme`, `:core:core-resources`. Реальные composables — на ui_implement.
- **`:modules:screen:components_manager`** — screen-модуль с TEA + Dagger (KSP). Создано: `build.gradle.kts`, `deps/ComponentsManagerUseCase` (placeholder interface), `ComponentsManagerNavigator` (back-only), placeholder `ComponentsManagerViewModel` (с inner `Factory`, AssistedInject ctor) + placeholder `ComponentsManagerScreen` composable (`Box { Text("TODO: UI in ui_implement") }`).
- **`:modules:screen:per_dictionary_components`** — структурно идентично components_manager. Placeholder ViewModel принимает `dictionaryId: Long` через AssistedFactory; Screen — placeholder.

### Изменённые модули

- **`:modules:screen:settingstab`** — расширен `SettingsNavigator` (`+ openComponentsManager()`), sealed `Msg` (`+ OpenComponentsManager : Msg`), sealed `SettingsNavigationEffect` (`+ OpenComponentsManager`), `SettingsTabReducer` (новая ветка `Msg.OpenComponentsManager → effect`), `SettingsNavigationEffectHandler` (mapping `OpenComponentsManager → navigator.openComponentsManager()`). build.gradle.kts получил `testImplementation` для junit + `:modules:core:mate` (parity с dictionaryappbar).
- **`:modules:widget:dictionaryappbar`** — расширен `DictionaryAppBarNavigator` (`+ openPerDictionaryComponents(dictionaryId: Long)`), sealed `Msg` (`+ OpenPerDictionaryComponents(dictionaryId: Long) : Msg` — payload, не чтение state), sealed `DictionaryAppBarNavigationEffect` (`+ OpenPerDictionaryComponents(dictionaryId: Long)`), `DictionaryAppBarReducer` + `DictionaryAppBarNavigationEffectHandler` (передача `dictionaryId` через payload).
- **`:modules:screen:main`** — расширены `CompositionRoot` interface (2 новых `*ScreenDep` + расширение 4 host-сигнатур), NavGraphBuilder ext'ы: `Settings.kt` (+ `COMPONENTS_MANAGER_ROUTE`), `Vocabulary.kt` (+ shared route `per_dict_components/{dictionaryId}` с `internal NavHostController.goToPerDictionaryComponents` extension), `Quiz.kt` (callsite через shared ext), `Statistic.kt` (новый файл — выделение из inline registration в MainScreen.kt для parity), `MainScreen.kt` (замена inline `composable(STATS)` на `statistic(...)`, удалён неиспользуемый import).
- **`app/`** — расширены `SettingsNavigatorImpl` (+4-я lambda `onOpenComponentsManager`) и `DictionaryAppBarNavigatorImpl` (+2-я lambda `onOpenPerDictionaryComponents`); созданы `ComponentsManagerNavigatorImpl` / `PerDictionaryComponentsNavigatorImpl` (back-only delegators); созданы 4 DI-класса (`ComponentsManagerUseCaseImpl` / `ComponentsManagerModule` / `PerDictionaryComponentsUseCaseImpl` / `PerDictionaryComponentsModule` — `@Binds`-pattern, parity с SettingsModule); расширены `AppComponent` (+2 factory-метода, +2 module entries в `AppModule.includes`), `CompositionRootImpl` (+2 factories в ctor, +2 `*ScreenDep` реализации, обновление 4 host-методов), `MainRouter.kt` (передача 2 новых factories в `CompositionRootImpl(...)`), `app/build.gradle.kts` (+3 `implementation(project(...))`), `settings.gradle.kts` (+3 include).

### Принятые контракты с сигнатурами

- `Msg.OpenComponentsManager : Msg` (SettingsTab) + effect `SettingsNavigationEffect.OpenComponentsManager`.
- `Msg.OpenPerDictionaryComponents(dictionaryId: Long) : Msg` (DictionaryAppBar) + effect `DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId: Long)`.
- `SettingsNavigator.openComponentsManager()` extension.
- `DictionaryAppBarNavigator.openPerDictionaryComponents(dictionaryId: Long)` extension.
- `CompositionRoot.ComponentsManagerScreenDep(onBackPress: () -> Unit)`.
- `CompositionRoot.PerDictionaryComponentsScreenDep(dictionaryId: Long, onBackPress: () -> Unit)`.
- `SettingsTabScreenDep(onLangManagementClick, onAboutAppClick, onPrivacyPolicyClick, onComponentsManagerClick)` — добавлен 4-й lambda parameter.
- `VocabularyTabDep / QuizTabScreenDep / StatisticTabScreenDep(..., openPerDictionaryComponents: (Long) -> Unit)` — добавлен один параметр в каждый из трёх host-методов.
- `ComponentsManagerViewModel.Factory` / `PerDictionaryComponentsViewModel.Factory` (AssistedInject); per-dict factory принимает `dictionaryId: Long`.
- Shared nav route: `per_dict_components/{dictionaryId}` зарегистрирован один раз в `Vocabulary.kt`; `internal NavHostController.goToPerDictionaryComponents(dictionaryId: Long)` extension переиспользуется из `Quiz.kt` / `Statistic.kt`.

### Тесты

- **`SettingsTabReducerTest`** — НОВЫЙ файл (первый Reducer-тест в `:modules:screen:settingstab`). 1 case: `given default state, when Msg.OpenComponentsManager, then state unchanged AND effects = setOf(SettingsNavigationEffect.OpenComponentsManager)`. Стиль AAA + Given/When/Then, helper'ы `testReduce` / `assertEffects` из `:modules:core:mate.test`.
- **`DictionaryAppBarReducerTest`** — расширен 2 кейсами (7/8 по сквозной нумерации KDoc): (а) happy path с `dictionaryId=1L`, (б) sentinel `dictionaryId=0L` (документирует контракт «Reducer прокидывает payload as-is, не валидирует»). 6 существующих кейсов оставлены без модификации (regression).
- **Всего: 9 тестов pass, 0 fail, 0 errors** (1 новый SettingsTab + 6 regression + 2 новых DictionaryAppBar = 9).

### Что НЕ покрыто тестами и почему

- **NavigatorImpl** (existing modify + 2 new) — trivial lambda delegation (`override fun openX() = onOpenX()`), нечего проверять кроме compile. Прецедентов `*NavigatorImplTest` в проекте нет.
- **DI modules + AppComponent factories** — compile-time Dagger validation (missing binding / cycle / unsatisfied dep — все compile errors). Прецедентов `AppComponent*Test` в проекте нет.
- **CompositionRoot / CompositionRootImpl / MainRouter** — composable factory wiring, требует Compose runtime test'а — overhead не оправдан. Прецедентов нет.
- **NavGraphBuilder ext'ы (Settings/Vocabulary/Quiz/Statistic/MainScreen)** — integration-level, требует `NavHostController` в androidTest. Покрытие — manual checklist + smoke. Прецедентов нет.
- **Gradle / RoomModule** — build-time / data layer.
- **NavigationEffectHandler'ы** — minimal `when(effect)` ветки; convention в проекте такие handler'ы не покрывает.

## Ключевые решения

- **id 35 `RoomModule.kt` — отложен**. design_tree (Tier 11) требует `.addMigrations(Migration_012_to_013)`, но сам `Migration_012_to_013.kt` создаётся на `data_implement` шаге. Добавление imports неимпортируемого имени сломает compile. Решение зафиксировано в `infra_implement.md § Известные TODO`. **Следствие**: до выполнения data_implement upgrade M12→M13 уйдёт по `fallbackToDestructiveMigration` → data loss; run app пока опасен.
- **Placeholder ViewModel + Screen** в новых screen-модулях создаются на infra-уровне как минимально необходимое для замыкания compile-graph (AppComponent factory методы + `ComponentsManagerScreen(factory, navigator)` call в CompositionRootImpl). Реальная mate-логика — на business_implement; реальный UI — на ui_implement. В placeholder VM'ях — targeted `@Suppress("unused")` + TODO для business_implement.
- **Sealed Msg/Effect расширение** — `data object` для no-payload (`Msg.OpenComponentsManager` / `SettingsNavigationEffect.OpenComponentsManager`), `data class` для payload (`Msg.OpenPerDictionaryComponents(dictionaryId)` / `DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId)`) — соблюдает convention существующих sealed (`Msg.OpenWebView(pageKey)` имеет ту же форму).
- **Payload в Msg вместо чтения state** — `Msg.OpenPerDictionaryComponents(dictionaryId)` несёт `dictionaryId` явно в payload (а не Reducer читает `state.currentDict.id`). Соответствует mate-framework convention; защищает от race с переключением currentDict.
- **Shared nav route в Vocabulary.kt** — per-dict экран достижим из 3 табов (Vocabulary/Quiz/Statistic). Решение: регистрировать composable один раз в `Vocabulary.kt` (как «канонический» tab-graph), вызывать `navController.navigate(...)` из всех трёх через `internal fun NavHostController.goToPerDictionaryComponents(dictionaryId)`. Альтернатива (регистрация в трёх местах) — duplication + конфликт routes в одном NavHost.
- **`Statistic.kt` выделен в отдельный NavGraphBuilder ext** — parity с Vocabulary/Quiz/Settings. До IS481 Statistic-tab регистрировался inline через `composable(TabPoint.STATS.route)` в `MainScreen.kt`. Альтернатива (оставить inline + расширить сигнатуру inline) — захламляет MainScreen.kt и нарушает parity.
- **DI Module pattern: `@Binds` без `@Subcomponent`** — в инструкции конструктора было упоминание `@Subcomponent`, но design_tree указывает на `@Binds`-Module-only pattern (convention SettingsModule.kt). Реализовано по design_tree.

## Что осталось вне scope этого sub-flow

- **M12→M13 миграция БД** (data_implement) — создание `Migration_012_to_013.kt` + регистрация в RoomModule. Прямая зависимость; без неё app может потерять данные.
- **TemplateValues sealed + ComponentApi** (business_implement) — domain-типы для template payload + DAO/repository слой.
- **Реальный UI** (ui_implement): `ComponentsManagerScreen` (list + create form + delete-confirm dialog), `PerDictionaryComponentsScreen` (per-dict list), `ComponentsManageWidget` (новый item в SettingsTab), icon-button «молоток» в `DictionaryAppBar` (видим при `currentDict != null`), template-preview composables в `:modules:widget:component_widgets`.
- **Реальная business логика VM** (business_implement) — mate-flow Reducer/Effect/UiMsg для двух новых screen-модулей; реальные методы `ComponentsManagerUseCase` (`createUserDefinedComponent`, `renameComponent`, `softDeleteComponent`, `previewDeletionImpact`, `flowUserDefinedTypes`, `componentsAcrossAllDictionaries`) и `PerDictionaryComponentsUseCase`.
- **Reducer тесты двух новых screen-модулей** — на business_test (после фиксации mate-контракта).

## Артефакты

- [`infra_walkthrough.md`](infra_walkthrough.md) — реальные file:line маршруты DI/gradle/navigation для всех затронутых host'ов.
- [`infra_design_tree.md`](infra_design_tree.md) — DAG 37 узлов, 11 tiers, 8 closed findings (F051-F064), 6 rejected. Циклов нет.
- [`infra_test.md`](infra_test.md) — спека 2 тестов (новый `SettingsTabReducerTest` + расширение `DictionaryAppBarReducerTest`), 11 closed findings (F065-F075), 4 rejected.
- [`infra_implement.md`](infra_implement.md) — реализация (17 create + 17 modify, 9/9 тестов pass), 2 closed findings (F076, F077), 0 rejected.

_model: claude-opus-4-7[1m]_
