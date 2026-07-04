# infra_test.md — IS481 component_constructor

## Решение: тесты нужны

**Обоснование:** Из 37 узлов `infra_design_tree` единственная зона с unit-testable новым **поведением** — расширение существующих Reducer'ов (id 13 `SettingsTabReducer`, id 17 `DictionaryAppBarReducer`) новыми `Msg → NavigationEffect` ветками. Это чистые функции `(State, Msg) → (State, Set<Effect>)`, дешёвые для тестирования (`testReduce` helper из `:modules:core:mate`, без mock-инфраструктуры). Прецедент покрытия Reducer этой парой mate-узлов есть только частично — `DictionaryAppBarReducerTest` существует и покрывает 4 Msg-ветки; `SettingsTabReducerTest` **отсутствует**.

Остальная infra-работа — gradle setup, DI wiring, Navigator interface'ы + impl'ы (тривиальная lambda-делегация), NavGraphBuilder регистрация, RoomModule migration registration — либо glue без поведения, либо валидируется на build-time / runtime smoke (см. § «Не покрываем» ниже).

## Тестовые спеки

### 1. `SettingsTabReducerTest` — НОВЫЙ файл

**Файл:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/test/java/me/apomazkin/settingstab/logic/SettingsTabReducerTest.kt`

**Статус:** Создаётся впервые. В `:modules:screen:settingstab` сейчас НЕТ ни одного `*ReducerTest`. Покрытие будет минимально-достаточным: только новая ветка (фокус IS481), без back-fill всех существующих Msg.

**Build setup:** Проверить, что `modules/screen/settingstab/build.gradle.kts` уже содержит `testImplementation("junit:junit:4.13.2")` + `testImplementation(project(":modules:core:mate"))`. Если нет — добавить (parity с `dictionaryappbar/build.gradle.kts:44-45`).

**Прецедент структуры/стиля:** `DictionaryAppBarReducerTest.kt` — стиль AAA + Given/When/Then комментарии, `LexemeLogger` stub'ится anonymous object'ом, helper'ы из `:modules:core:mate.test`: `testReduce(state, msg)`, `result.state()`, `result.effects()`, `result.assertNoEffects(msg)`.

**Сценарий:**

- `given default state, when Msg.OpenComponentsManager dispatched, then assertEquals(initialState, result.state()) AND effects = setOf(SettingsNavigationEffect.OpenComponentsManager)`
  - Test name: `should emit OpenComponentsManager nav effect when Msg OpenComponentsManager received`
  - Используется `testReduce` + inline `assertEquals(initialState, result.state())` на state immutability + `assertEquals` на single-effect set.
  - Логика проверки эффекта: `result.assertEffects(setOf(SettingsNavigationEffect.OpenComponentsManager))`.
  - **Helper note:** `assertEffects(Set<EFFECT>)` для positive check (helper из `:modules:core:mate.test/MateTestHelper.kt`). `assertNoEffects()` — для отсутствия эффектов (тот же helper). Для state immutability — inline `assertEquals(initialState, result.state())` (отдельного helper'а нет; в `MateTestHelper.kt` нет `assertStateUnchanged` / `assertNoStateChange` / `assertSameState`, есть только generic `assertState(expected)`).

**Что НЕ покрываем в этом файле:** существующие `Msg.OpenLangManagement` / `Msg.OpenAboutApp` / `Msg.OpenWebView` / Export/Import / UiMsg ветки. Они не меняются в IS481 — back-fill вне scope этой фичи (отдельный backlog item если когда-нибудь захотим coverage parity).

### 2. `DictionaryAppBarReducerTest` — РАСШИРЕНИЕ существующего

**Файл:** `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/test/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducerTest.kt`

**Статус:** Существующий файл с 6 тест-кейсами; добавляем 2 новых теста к уже принятому стилю. KDoc в начале файла обновить — добавить новую секцию `=== Msg.OpenPerDictionaryComponents ===` в нумерованный список (после `Msg.DictMenu*`).

**Regression:** 6 существующих кейсов `DictionaryAppBarReducerTest` должны остаться green без модификации. Если падают — сигнал что расширение sealed `Msg`/`NavigationEffect` случайно влияет на reducer-логику других веток (нарушение Msg-isolation).

**Сценарии:**

- `given initial state with currentDict=dictEn (id=1), when Msg.OpenPerDictionaryComponents(dictionaryId=1L), then assertEquals(initialState, result.state()) AND result.assertEffects(setOf(DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId=1L)))`
  - Test name: `should emit OpenPerDictionaryComponents nav effect carrying dictionaryId when Msg OpenPerDictionaryComponents received`
  - Проверка: inline `assertEquals(initialState, result.state())` (state immutability — currentDict / availableDictList / isDropDownMenuOpen / isLoading все без изменений) + `result.assertEffects(setOf(DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId=1L)))` — single-effect set с правильным `dictionaryId` payload.

- `given initial state, when Msg.OpenPerDictionaryComponents(dictionaryId=0L), then result.assertEffects(setOf(DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId=0L))), assertEquals(initialState, result.state())`
  - Test name: `should pass dictionaryId payload as-is when Msg OpenPerDictionaryComponents received with sentinel value`
  - Обоснование: Документирует контракт `reducer не валидирует payload, прокидывает as-is`; защита от future `require(dictionaryId > 0)` рефакторинга. Reducer не должен зависеть от state (Msg несёт payload сам) — это документирует контракт «Reducer прозрачно прокидывает dictionaryId, не читая state.currentDict».

**Нумерация KDoc:** новые кейсы продолжают сквозную нумерацию — **7** и **8** (по convention существующего KDoc 1-6 в `DictionaryAppBarReducerTest.kt:19-29`).

**Helper note:** `assertEffects(Set<EFFECT>)` для positive check (helper из `:modules:core:mate.test/MateTestHelper.kt`). `assertNoEffects()` — для отсутствия эффектов (тот же helper). Для state immutability — inline `assertEquals(initialState, result.state())` (отдельного helper'а нет; в `MateTestHelper.kt` нет `assertStateUnchanged` / `assertNoStateChange` / `assertSameState`, есть только generic `assertState(expected)`).

### 3. `*NavigationEffectHandlerTest` — НЕ создаём

**Файл (не создаётся):** `modules/screen/settingstab/src/test/java/me/apomazkin/settingstab/SettingsNavigationEffectHandlerTest.kt` / `modules/widget/dictionaryappbar/src/test/.../DictionaryAppBarNavigationEffectHandlerTest.kt`

**Обоснование:** В проекте **нет ни одного `*NavigationEffectHandlerTest`** (verified Grep'ом: `*EffectHandlerTest.kt` findings — только `DatasourceEffectHandlerTest` в `wordcard` и `quiz/chat`, которые тестируют **бизнес-эффекты** через mock UseCase, а не nav-effect → navigator делегацию). Существующие NavigationEffectHandler'ы — это minimal `when(effect)` ветки, делегирующие в navigator-метод. Покрытие = дублирует код handler'а и при этом ничего не валидирует кроме того, что разработчик не забыл ветку (которая всё равно поймается компилятором через exhaustive `when` на sealed interface).

**Возражение к этой позиции (sanity check):** если в проекте появится дополнительная логика в `NavigationEffectHandler` (например, обёртывание navigator-вызова в `try-catch` для fail-soft, batching эффектов, или throttling) — тогда unit-test обязателен. На данной итерации новых handler'ов нет, расширения существующих — minimal ветка `is X.OpenY -> navigator.openY()`. Convention соблюдена.

## Не покрываем (с обоснованиями)

| Узел DAG (id) | Файл | Причина |
|---|---|---|
| id 1, 2, 3, 4, 36 | Gradle setup (`settings.gradle.kts`, новые `build.gradle.kts`, `app/build.gradle.kts`) | Build-time validation: модуль либо компилируется, либо нет. Unit-test невозможен. |
| id 5, 6 | UseCase interface placeholders (`ComponentsManagerUseCase` / `PerDictionaryComponentsUseCase`) | Пустые контракты на infra-уровне; реальные методы появятся на `business_design_tree`. Тестируется на business-слое. |
| id 7, 8, 9, 10 | Navigator interface'ы (новые `ComponentsManagerNavigator` / `PerDictionaryComponentsNavigator`; modify `SettingsNavigator` / `DictionaryAppBarNavigator`) | Interface — пустой контракт. Поведение проверяется в pair с handler/impl (которые мы тоже не покрываем — см. ниже). |
| id 19, 20, 21, 22 | `*NavigatorImpl` в `app/.../navigator/` (тривиальные lambda-делегаторы) | Тест дублирует код: `override fun openX() = onOpenX()` — нечего проверять кроме того, что разработчик не путает имена параметров. Поймает компилятор через сигнатуру interface. **Прецедент:** существующих `*NavigatorImpl*Test` в проекте нет. |
| id 23, 24, 25, 26, 27 | DI modules + AppComponent (новые `ComponentsManagerModule` / `PerDictionaryComponentsModule` / `ComponentsManagerUseCaseImpl` / `PerDictionaryComponentsUseCaseImpl` + AppComponent factories) | Dagger compile-time validation: missing binding / cycle / unsatisfied dep — все compile errors. Runtime instantiation проверяется через успешный build APK + smoke на устройстве. **Прецедент:** существующих `AppComponent*Test` в проекте нет. |
| id 28, 29, 37 | `CompositionRoot` interface / `CompositionRootImpl` / `MainRouter` | Composable factory wiring. Поведение — instantiate ViewModel + передать navigator/factory. Тест требует Compose runtime (`compose-test-junit4`) + `ViewModelStoreOwner` mocking — overhead не оправдан для простого wiring. **Прецедент:** существующих `CompositionRoot*Test` в проекте нет. |
| id 30, 31, 32, 33, 34 | NavGraphBuilder extensions (`Settings.kt`, `Vocabulary.kt`, `Quiz.kt`, `Statistic.kt`, `MainScreen.kt`) | Регистрация маршрутов + парсинг nav-arg'ов. Integration-level — требует `NavHostController` в Compose-runtime test'е (androidTest). На unit-уровне не тестируется. Покрытие — manual checklist (drill-in работает в каждом из 3 табов) + smoke. **Прецедент:** существующих `Navigation*Test` для NavGraphBuilder ext'ов в проекте нет. |
| id 35 | `RoomModule.kt` — регистрация `Migration_012_to_013` | Чистая регистрация в `.addMigrations(...)`. Само поведение миграции тестируется на data-слое (`migration_test` шаг — `MigrationTestHelper` + `runMigrationsAndValidate`). Здесь проверяется только compile + manual upgrade-path smoke (юзер с v12 → v13 без data loss). |
| id 11, 12, 15, 16 | Sealed `Msg` / `NavigationEffect` дополнения | Декларативное расширение sealed-иерархии — поведение тестируется в Reducer (id 13, 17), которые мы покрываем. |
| id 14, 18 | `SettingsNavigationEffectHandler` / `DictionaryAppBarNavigationEffectHandler` | См. секцию 3 выше — glue без логики, convention в проекте не покрывает handler'ы NavigationEffect'ов. |

## Что покрывается на ДРУГИХ test-шагах (cross-reference)

- **Domain тесты** (`modules/domain/lexeme/src/test/`): `TemplateValues` parser/serializer, `ComponentTemplate.fromKey` nullable contract — на `domain_test` / `business_test`.
- **Mapper тесты** (`core/core-db-impl/src/test/`): `parseTemplateValues(json, template)` golden fixtures (`fixtures/component_values/*.json`) — на `data_test` (см. aspect `mapper_golden_fixtures`).
- **Migration тесты** (`core/core-db-impl/src/androidTest/`): `Migration_012_to_013` через `MigrationTestHelper` + idempotency + edge-cases (malformed JSON, unknown template_key, null value, repeated apply) — на `migration_test`.
- **UseCase тесты** (business-слой): `createUserDefinedComponent` (cross-scope uniqueness invariant: aspects `soft_delete_unique_collision` + `userdefined_identity_invariant` — two-prong SELECT), `renameComponent` (atomic cascade с `quiz_configs.component_refs`), `softDeleteComponent` (prefs cleanup), `previewDeletionImpact` (impact aggregation) — на `business_test`.
- **Reducer тесты двух новых screen-модулей** (`ComponentsManagerReducer` / `PerDictionaryComponentsReducer`): State / Msg / Effect для list + create form + delete-confirm dialog — на `business_test` (после того как контракт зафиксируется в `business_design_tree`).

## Манипуляции с реальным кодом

Этот документ — **спецификация**, не реализация. Реальный код тестов будет написан на `infra_implement` шаге (либо отдельном test-шаге master-плана) согласно спекам выше. Файлы для создания/модификации:

- **CREATE:** `modules/screen/settingstab/src/test/java/me/apomazkin/settingstab/logic/SettingsTabReducerTest.kt` (1 test case).
- **MODIFY:** `modules/widget/dictionaryappbar/src/test/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducerTest.kt` (+2 test cases к существующим 6 + обновление KDoc-списка; существующие 6 кейсов не модифицируются — regression check).
- **VERIFY (build setup):** `modules/screen/settingstab/build.gradle.kts` содержит `testImplementation("junit:junit:4.13.2")` + `testImplementation(project(":modules:core:mate"))`. Если нет — добавить.

## Запуск

```bash
./scripts/cc-build.sh :modules:screen:settingstab:testDebugUnitTest
./scripts/cc-build.sh :modules:widget:dictionaryappbar:testDebugUnitTest
```

Либо общим прогоном:

```bash
./scripts/cc-build.sh testDebugUnitTest
```

## История ревью

- iter 2: applied F065 (payload-passthrough mandatory), F068 (regression check 6 cases), F070 (explicit assertEquals для state).
- iter 3: applied F072+F074 (Msg-name → OpenPerDictionaryComponents, scope + test), F073 (assertEffects helper), F075 (KDoc numbering 7/8).

_model: claude-opus-4-7[1m]_
