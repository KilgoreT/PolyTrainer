# infra_implement.md — IS481 component_constructor

## Создано

### Gradle setup (Tier 0)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/component_widgets/build.gradle.kts`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/build.gradle.kts`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/build.gradle.kts`

### Placeholder UseCase interfaces (Tier 1)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt`

### Navigator interfaces (Tier 2)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerNavigator.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsNavigator.kt`

### Placeholder ViewModel + Screen для замыкания DI/compose wiring (вне явного DAG, см. §Нетривиальные решения)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerViewModel.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsViewModel.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsScreen.kt`

### NavigatorImpl в app/ (Tier 6 — new)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/ComponentsManagerNavigatorImpl.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/PerDictionaryComponentsNavigatorImpl.kt`

### DI modules (Tier 7)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerModule.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt`
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsModule.kt`

### Navigation host extension (Tier 10 — new)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Statistic.kt`

### Tests (Шаг 2)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/test/java/me/apomazkin/settingstab/logic/SettingsTabReducerTest.kt`

## Изменено

### Gradle setup
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/settings.gradle.kts` — добавлены 3 include для новых модулей.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/build.gradle.kts` — добавлены 3 `implementation(project(...))`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/build.gradle.kts` — добавлен `testImplementation` для junit + `:modules:core:mate` (parity с dictionaryappbar).

### Existing navigator interfaces (Tier 3)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigator.kt` — добавлен `openComponentsManager()`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigator.kt` — добавлен `openPerDictionaryComponents(dictionaryId: Long)`.

### Mate-wiring SettingsTab (Tier 4)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/Message.kt` — добавлен `Msg.OpenComponentsManager`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffect.kt` — добавлен `SettingsNavigationEffect.OpenComponentsManager`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/SettingsTabReducer.kt` — branch для `Msg.OpenComponentsManager`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffectHandler.kt` — case → `navigator.openComponentsManager()`.

### Mate-wiring DictionaryAppBar (Tier 5)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt` — добавлен `Msg.OpenPerDictionaryComponents(dictionaryId)`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigationEffect.kt` — добавлен `OpenPerDictionaryComponents(dictionaryId)`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducer.kt` — branch для нового Msg.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigationEffectHandler.kt` — case → `navigator.openPerDictionaryComponents(...)`.

### Existing NavigatorImpl (Tier 6 — modify)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/SettingsNavigatorImpl.kt` — +4-я lambda `onOpenComponentsManager`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/DictionaryAppBarNavigatorImpl.kt` — +2-я lambda `onOpenPerDictionaryComponents`.

### AppComponent (Tier 8)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt` — 2 новых factory-метода + 2 module entries в `AppModule.includes` + соответствующие импорты.

### CompositionRoot (Tier 9)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/CompositionRoot.kt` — 2 новых `*ScreenDep` + расширение 4 host-сигнатур (`VocabularyTabDep` / `QuizTabScreenDep` / `StatisticTabScreenDep` / `SettingsTabScreenDep`).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/uiDeps/CompositionRootImpl.kt` — реализация 2 новых *ScreenDep, расширение ctor (+2 factories), wiring 4 host-методов.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/route/MainRouter.kt` — передача 2 новых factories в `CompositionRootImpl(...)`.

### Navigation hosts (Tier 10 — modify)
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt` — drill-in `COMPONENTS_MANAGER_ROUTE` + передача `onComponentsManagerClick`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Vocabulary.kt` — drill-in `per_dict_components/{dictionaryId}` + `internal NavHostController.goToPerDictionaryComponents`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Quiz.kt` — передача `openPerDictionaryComponents` через shared `goToPerDictionaryComponents`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/MainScreen.kt` — замена inline `composable(STATS)` на вызов `statistic(...)` + удалён неиспользуемый import `composable`.

### Tests
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/test/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducerTest.kt` — добавлены 2 кейса (7/8) + KDoc-секция, import `assertEffects` + `DictionaryAppBarNavigationEffect`. 6 существующих кейсов не изменены (regression-check passed).

## Тесты

Запущено:
- `./scripts/cc-build.sh :modules:screen:settingstab:testDebugUnitTest --tests "*SettingsTabReducerTest*"` → BUILD SUCCESSFUL. Результат XML: `tests="1" skipped="0" failures="0" errors="0"`.
- `./scripts/cc-build.sh :modules:widget:dictionaryappbar:testDebugUnitTest --tests "*DictionaryAppBarReducerTest*"` → BUILD SUCCESSFUL. Результат XML: `tests="8" skipped="0" failures="0" errors="0"` (6 regression + 2 new).

**Pass: 9. Fail: 0. Errors: 0.**

## Нетривиальные решения

1. **Placeholder ViewModel + Screen в новых screen-модулях** (`ComponentsManagerViewModel.kt` / `ComponentsManagerScreen.kt` / `PerDictionaryComponentsViewModel.kt` / `PerDictionaryComponentsScreen.kt`).
   - design_tree фиксирует `AppComponent.getComponentsManagerViewModelFactory()` (id 27) и вызов `ComponentsManagerScreen(factory, navigator)` в `CompositionRootImpl.ComponentsManagerScreenDep` (id 29 «Изменение 4»). Эти референсы требуют существования классов `ComponentsManagerViewModel` (с inner `Factory`) и composable `ComponentsManagerScreen`. Без них AppComponent / CompositionRootImpl / MainRouter не компилируются.
   - design_tree это явно не перечисляет как отдельный узел графа — фактически они должны были бы появиться в business_design_tree / business_implement / ui_implement. На infra-уровне создан **минимальный placeholder** (ViewModel — пустой `@AssistedInject` ctor с одним AssistedFactory; Screen — placeholder `Box { Text("TODO: UI in ui_implement") }`).
   - Это соответствует note из design_tree: «сигнатура `*Screen(...)` composable'ов финализируется в `ui_design_tree` шаге. На infra-уровне фиксируется только то, что они принимают factory + navigator (+ `dictionaryId` для per-dict)».

2. **id 35 `RoomModule.kt` — НЕ изменялся**.
   - design_tree (Tier 11, id 35) требует регистрацию `Migration_012_to_013` в `.addMigrations(...)`. Но сам `Migration_012_to_013.kt` создаётся на `data_implement` шаге (см. § Затронутые файлы → Data layer). Добавление imports неимпортируемого имени сломает compile.
   - Промпт явно допускает skip: «id 35: RoomModule.kt — modify (если требуется по design_tree; обычно — нет, M12→M13 в data layer)». Решение — отложить до data_implement.

3. **DI Module pattern** — в инструкции конструктора было упоминание «`@Subcomponent`», но design_tree (главный источник) указывает на `@Binds`-Module-only pattern (convention SettingsModule.kt). Реализовано по design_tree — без `@Subcomponent`. Подключение модулей — `AppModule.includes`.

## Известные TODO (для последующих слоёв)

- **ComponentsManagerUseCase / PerDictionaryComponentsUseCase** — interfaces пустые (placeholder), имплементации `*UseCaseImpl` с пустым `@Inject constructor()`. Реальные методы (`createUserDefinedComponent`, `renameComponent`, `softDeleteComponent`, `previewDeletionImpact`, `flowUserDefinedTypes`, `componentsAcrossAllDictionaries`) — на `business_design_tree` / `business_implement`.
- **ComponentsManagerViewModel / PerDictionaryComponentsViewModel** — placeholder ViewModel'и без mate-wiring (нет State / Msg / Effect / Reducer / EffectHandler). Реальный mate-flow — на `business_implement`.
- **ComponentsManagerScreen / PerDictionaryComponentsScreen** — placeholder composable'ы с `Text("TODO: UI in ui_implement")`. Реальный UI (list + create form + delete-confirm dialog) — на `ui_implement`.
- **RoomModule.kt: `.addMigrations(Migration_012_to_013)`** — будет добавлено на `data_implement` шаге одновременно с созданием `Migration_012_to_013.kt`. Без этого upgrade M12→M13 уйдёт по `fallbackToDestructiveMigration` → data loss.
- **`ComponentsManageWidget` в SettingsTab** — UI-уровневое расширение `SettingsTabScreen.kt` (новый item в `LangManageWidget`-секции). Реализуется на `ui_implement`.
- **Icon-button «молоток» в `DictionaryAppBar`** — UI-уровневое расширение `DictionaryAppBar.kt` (видим при `currentDict != null`). Реализуется на `ui_implement`.

_model: claude-opus-4-7[1m]_

## История ревью

### iter 2 (2026-06-16): F076 + F077 fixed

- **F076**: удалено 5 unused project-deps из каждого gradle файла:
  - `modules/screen/components_manager/build.gradle.kts` — убраны `:modules:domain:lexeme`, `:modules:widget:component_widgets`, `:modules:core:tools`, `:modules:core:logger`, `:core:core-resources`.
  - `modules/screen/per_dictionary_components/build.gradle.kts` — те же 5 deps убраны.
- **F077**: убран `@Suppress("UnusedPrivateMember") val vm = viewModel` костыль из `ComponentsManagerScreen.kt` и `PerDictionaryComponentsScreen.kt`. ViewModel default-параметр продолжает инстанцировать factory, ссылка в теле не нужна (Kotlin не считает default-параметр unused). В placeholder VM'ях — targeted `@Suppress("unused")` с TODO-комментарием для business_implement.
- Тесты: 9/9 pass (повтор).
