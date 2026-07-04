# Infra walkthrough: IS481 component_constructor

Только факты о текущей инфраструктуре, без выводов и предложений. Все ссылки `file:line` — реальные строки на момент сбора.

## 1. Структура модулей и `settings.gradle.kts`

Файл реестра: `settings.gradle.kts:30-66`. Текущие включения, релевантные фиче (полный список):

- Screens (`modules/screen/*`):
  - `settings.gradle.kts:36` — `:modules:screen:splash`
  - `settings.gradle.kts:37` — `:modules:screen:dictionary`
  - `settings.gradle.kts:38` — `:modules:screen:main`
  - `settings.gradle.kts:39` — `:modules:screen:dictionaryTab`
  - `settings.gradle.kts:40` — `:modules:screen:wordcard`
  - `settings.gradle.kts:41` — `:modules:screen:quiztab`
  - `settings.gradle.kts:42` — `:modules:screen:stattab`
  - `settings.gradle.kts:43` — `:modules:screen:settingstab`
  - `settings.gradle.kts:44` — `:modules:screen:quiz:chat`
- Widgets (`modules/widget/*`):
  - `settings.gradle.kts:47` — `:modules:widget:dictionaryappbar`
  - `settings.gradle.kts:48` — `:modules:widget:dictionarypicker`
  - `settings.gradle.kts:49` — `:modules:widget:iconDropDowned`
  - `settings.gradle.kts:50` — `:modules:widget:chipPicker`
- Domain: `settings.gradle.kts:58` — `:modules:domain:lexeme` (единственный domain-модуль).
- Datasource: `settings.gradle.kts:52` — `:modules:datasource:prefs`.
- Library: `settings.gradle.kts:55` — `:modules:library:flags`.
- Core legacy: `settings.gradle.kts:61-64` — `:core:core-resources`, `:core:core-db-api`, `:core:core-db-impl`, `:core:core-db`.

**Аналог `screen` + `widget` пары для drill-in фичи:** `modules/screen/wordcard` (screen с TEA + Dagger) + `modules/widget/dictionaryappbar` (shared widget). Аналогичный паттерн для двух новых screen-модулей (`components_manager` + `per_dictionary_components`) и одного widget-модуля (`component_widgets`) — есть.

## 2. Gradle конвенция новых widget / screen модулей

### Widget без DI (Tier 2 примитив)

`modules/widget/iconDropDowned/build.gradle.kts:1-33` — минимальный widget:
- plugins: `com.android.library`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose` (без KSP).
- зависимости: `:modules:core:theme`, `:modules:core:ui` (`build.gradle.kts:27-28`).
- namespace: `me.apomazkin.icondropdowned` (`build.gradle.kts:8`).
- compileSdk = 35, minSdk = 23, targetSdk = 35 (`build.gradle.kts:9-13`).
- Java 17 + JVM target 17 (`build.gradle.kts:16-22`).

`modules/widget/chipPicker/build.gradle.kts:1-33` — идентичная структура; `modules/widget/dictionarypicker/build.gradle.kts:1-37` — добавлены compose lifecycle deps + dep на `:modules:widget:iconDropDowned`.

**Аналог для `component_widgets` (без DI, чисто UI-примитивы):** найден — `dictionarypicker` / `chipPicker`.

### Widget с DI (Mate ViewModel + Dagger)

`modules/widget/dictionaryappbar/build.gradle.kts:1-48` — widget с TEA + DI:
- plugins: + `com.google.devtools.ksp` (`build.gradle.kts:5`).
- зависимости: `:modules:core:di`, `:modules:core:mate`, `diLibs.dagger`, `ksp(diLibs.daggerCompiler)`, `javax.inject:javax.inject:1`, `:modules:widget:dictionarypicker`, `:modules:widget:iconDropDowned` (`build.gradle.kts:31-39`).
- compose lifecycle: `composeLibs.lifecycleViewmodelCompose`, `composeLibs.lifecycleRuntimeCompose` (`build.gradle.kts:41-42`).
- test: `junit:junit:4.13.2`, `:modules:core:mate` testImplementation (`build.gradle.kts:44-45`).

### Screen с TEA + Dagger

`modules/screen/wordcard/build.gradle.kts:1-46`:
- plugins: `com.android.library`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`, `com.google.devtools.ksp` (`build.gradle.kts:1-6`).
- зависимости: `:modules:core:di`, `:modules:core:mate`, `diLibs.dagger` + KSP, `:modules:core:theme`, `:modules:core:ui`, `:modules:core:tools`, `:modules:core:logger`, `:modules:domain:lexeme`, `:core:core-resources`, `:modules:widget:iconDropDowned`, compose lifecycle deps (`build.gradle.kts:28-41`).

`modules/screen/settingstab/build.gradle.kts:1-41` — analogous screen module с DI (drill-in `AboutAppScreen` живёт прямо здесь, без отдельного gradle модуля): plugins + KSP + Dagger + core/di + core/mate + core/theme + core/ui + core/resources + composeLibs lifecycle/activity.

**Аналог для `components_manager` / `per_dictionary_components` screen-модулей:** найден — `wordcard` / `settingstab`.

## 3. DI wiring

### `AppComponent`

`app/.../di/AppComponent.kt:38-92`:
- `@Component(modules = [AppModule::class], dependencies = [CoreDbProvider::class])` + `@Singleton` (`AppComponent.kt:38-42`).
- Factory создаётся через `@Component.Factory` с `@BindsInstance appContext` + `@BindsInstance logger` + `coreDbProvider` (`AppComponent.kt:47-54`).
- Per-screen getters для ViewModel factory (паттерн `getXxxViewModelFactory(): XxxViewModel.Factory`):
  - `AppComponent.kt:57` — `getSplashViewModelFactory()`
  - `AppComponent.kt:61` — `getWordCardViewModelFactory()`
  - `AppComponent.kt:62` — `getChatViewModelFactory()`
  - `AppComponent.kt:63` — `getDictionaryAppBarViewModelFactory()`
  - `AppComponent.kt:64` — `getDictionaryTabViewModelFactory()`
  - `AppComponent.kt:65` — `getQuizTabViewModelFactory()`
  - `AppComponent.kt:66` — `getStatisticViewModelFactory()`
  - `AppComponent.kt:67` — `getSettingsTabViewModelFactory()`

`AppModule` includes (`AppComponent.kt:76-90`):
- `SplashModule`, `DictionaryModule`, `DictionaryTabModule`, `WordCardModule`, `QuizTabModule`, `QuizChatModule`, `StatisticModule`, `SettingsModule`, `DictionaryAppBarModule`, `CountryProviderModule`, `PrefsProviderModule`, `ResourceModule`, `EnvModule`.

### Per-screen DI modules

Паттерн (одно-binding interface module):
- `app/.../di/module/settingstab/SettingsModule.kt:1-13` — `@Module interface SettingsModule` + `@Binds fun bindSettingsTabUseCase(impl: SettingsTabUseCaseImpl): SettingsTabUseCase`.
- `app/.../di/module/widget/DictionaryAppBarModule.kt:1-13` — то же для widget'а.
- `app/.../di/module/wordCard/WordCardModule.kt:1-12` — то же для wordcard.

Текущая структура `app/src/main/java/me/apomazkin/polytrainer/di/module/`:
```
dictionary/  dictionarytab/  EnvModule.kt  flags/  LoggerModule.kt  prefs/
quizchat/  quiztab/  ResourceModule.kt  settingstab/  splash/  statistictab/
widget/  wordCard/
```

UseCase-API контракт лежит в screen/widget module под `deps/`:
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/deps/SettingsTabUseCase.kt:1-7` — interface.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt` — interface.
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/deps/` — widget deps.

Impl живёт в `app/.../di/module/<feature>/XxxUseCaseImpl.kt`.

**Аналог для `ComponentsManagerModule` / `PerDictionaryComponentsModule` + use case Impl:** найден — `SettingsModule` + `SettingsTabUseCaseImpl`, `WordCardModule` + `WordCardUseCaseImpl`.

## 4. `CompositionRoot` interface и реализация

### Interface

`modules/screen/main/src/main/java/me/apomazkin/main/CompositionRoot.kt:1-53` — `@Stable interface CompositionRoot`. Текущий набор `*Dep` методов:
- `CompositionRoot.kt:8-12` — `VocabularyTabDep(openDictionaryCreate, openWordCard)`.
- `CompositionRoot.kt:14-18` — `WordCardScreenDep(wordId, onBackPress)` — пример drill-in с arg.
- `CompositionRoot.kt:20-24` — `QuizTabScreenDep(openDictionaryCreate, openChatQuiz)`.
- `CompositionRoot.kt:26-29` — `ChatQuizScreenDep(onBackPress)`.
- `CompositionRoot.kt:31-34` — `StatisticTabScreenDep(openDictionaryCreate)`.
- `CompositionRoot.kt:36-41` — `SettingsTabScreenDep(onLangManagementClick, onAboutAppClick, onPrivacyPolicyClick)`.
- `CompositionRoot.kt:43-46` — `AboutAppScreenDep(onBackPress)` — простой drill-in без arg.
- `CompositionRoot.kt:48-52` — `WebViewScreenDep(pageKey, onBackPress)` — drill-in с string arg.

**Конвенция подтверждена** (см. F042 в `02_scope.md`): каждый screen имеет собственный `*ScreenDep` метод. Прецеденты для `ComponentsManagerScreenDep` (без arg) — `AboutAppScreenDep`. Прецедент для `PerDictionaryComponentsScreenDep(dictionaryId: Long)` — `WordCardScreenDep(wordId: Long, ...)`.

### Implementation

`app/.../uiDeps/CompositionRootImpl.kt:40-50` — class signature принимает 7 ViewModel factories + `EnvParams` + `LexemeLogger`.

`CompositionRootImpl.kt:52-74` — `VocabularyTabDep`: создаёт `DictionaryAppBarNavigatorImpl` (line 56-58) + `VocabularyNavigatorImpl` (line 59-61), передаёт factory + nav в `DictionaryTabScreen`. Inline-объект `DictionaryTabUiDeps` (line 65-71) — содержит `AppBar(titleResId)` composable, который проксирует на `DictionaryAppBar`.

`CompositionRootImpl.kt:89-112` — `QuizTabScreenDep`: тот же паттерн, второй экземпляр `DictionaryAppBarNavigatorImpl` (line 94-96).

`CompositionRootImpl.kt:125-145` — `StatisticTabScreenDep`: третий экземпляр `DictionaryAppBarNavigatorImpl` (line 129-131). **Подтверждено F048: `DictionaryAppBar` инстанцируется в 3 host'ах** (`VocabularyTabDep` / `QuizTabScreenDep` / `StatisticTabScreenDep`).

`CompositionRootImpl.kt:147-167` — `SettingsTabScreenDep`: `SettingsNavigatorImpl` (line 154-162) с тремя lambda's (`onOpenLangManagement`, `onOpenAboutApp`, `onOpenWebView`).

`CompositionRootImpl.kt:169-177` — `AboutAppScreenDep`: простой случай, нет navigator'а, только `onBackPress`.

`CompositionRootImpl.kt:179-195` — `WebViewScreenDep(pageKey, onBackPress)`: parsing arg → enum `WebPage.fromKey()` (`CompositionRootImpl.kt:184`).

### `MainRouter` wiring

`app/.../route/MainRouter.kt:24-41` — `CompositionRootImpl` инстанцируется один раз внутри `composable(MainPoint.MAIN.route)`, передаётся в `MainScreen(compositionRoot = ...)`.

## 5. Navigation pattern (NavGraphBuilder extension)

### Per-tab NavGraph

`modules/screen/main/src/main/java/me/apomazkin/main/MainScreen.kt:14-19` — `enum class TabPoint(route)` с 4 routes: `VOCABULARY`, `QUIZ`, `STATS`, `SETTINGS`.

`MainScreen.kt:36-61` — `NavHost(startDestination = TabPoint.VOCABULARY.route)`. Каждая tab делегируется в свою NavGraphBuilder extension:
- `MainScreen.kt:42-46` — `vocabulary(navController, compositionRoot, openDictionaryCreate)`.
- `MainScreen.kt:47-51` — `quiz(navController, compositionRoot, openDictionaryCreate)`.
- `MainScreen.kt:52-54` — `composable(TabPoint.STATS.route)` напрямую (без extension, т.к. только tab без drill-in).
- `MainScreen.kt:56-60` — `settings(navController, compositionRoot, openDictionaryList)`.

### Vocabulary tab — drill-in с arg

`modules/screen/main/src/main/java/me/apomazkin/main/Vocabulary.kt:1-44`:
- константа route: `Vocabulary.kt:9-10` — `WORD_CARD_ROUTE = "wordCard"`, `WORD_ID_ARG = "wordId"`.
- tab composable: `Vocabulary.kt:17-22` — `composable(TabPoint.VOCABULARY.route)` → `compositionRoot.VocabularyTabDep(...)` с `openWordCard = { navController.goToWordCard(it) }`.
- drill-in composable: `Vocabulary.kt:24-36` — `composable("$WORD_CARD_ROUTE/{$WORD_ID_ARG}", arguments = listOf(navArgument(WORD_ID_ARG) { type = NavType.LongType }))` → парсит `wordId` через `navBackStackEntry.arguments?.getLong(...)` → `compositionRoot.WordCardScreenDep(wordId, onBackPress)`.
- navigate helper: `Vocabulary.kt:39-43` — `private fun NavHostController.goToWordCard(wordId)` → `navigate("$WORD_CARD_ROUTE/$wordId") { launchSingleTop = true }`.

### Settings tab — drill-in без arg + с string arg

`modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt:1-64`:
- route constants: `Settings.kt:9-10` — `ABOUT_APP_ROUTE = "about_app"`, `WEBVIEW_ROUTE = "webview/{pageKey}"`.
- function signature: `Settings.kt:12-16` — `fun NavGraphBuilder.settings(navController, compositionRoot, openDictionaryList)`.
- tab composable: `Settings.kt:18-24` — `composable(TabPoint.SETTINGS.route)` → `SettingsTabScreenDep(onLangManagementClick = openDictionaryList, onAboutAppClick = { navController.goToAboutApp() }, onPrivacyPolicyClick = { navController.goToWebView("privacy_policy") })`.
- AboutApp drill-in: `Settings.kt:26-32` — `composable(ABOUT_APP_ROUTE)` → `AboutAppScreenDep(onBackPress = { navController.backPress() })`.
- WebView drill-in с arg: `Settings.kt:34-45` — `composable(WEBVIEW_ROUTE, arguments = listOf(navArgument("pageKey") { type = NavType.StringType }))` → парсит `pageKey` → `WebViewScreenDep(...)`.
- navigate helpers: `Settings.kt:49-58` — `goToAboutApp()`, `goToWebView(pageKey)` (приватные extension'ы).
- `Settings.kt:61-63` — `backPress() = popBackStack()`.

### Quiz tab — pattern parity

`modules/screen/main/src/main/java/me/apomazkin/main/Quiz.kt:1-49` — аналогичная структура: tab + drill-in `QUIZ_ROUTE/{QUIZ_ROUTE_ARG}` (string arg).

**`navigation/Constants.kt` и `NavEntity.kt` НЕ используются для регистрации routes**:
- `app/.../navigation/Constants.kt:1-10` — только `LOG_TAG`, `ROOT_POINT`, `UNKNOWN_POINT`, `NON_LOG_DEEP_LINK` (логирование).
- `app/.../navigation/NavEntity.kt:1-8` — data class для NavLogEntity (логирование маршрутов).
- `app/.../MainActivity.kt:32-46` — `setContent { ... RootRouter(navController, onExitApp) }` — нет `composable(...)` в MainActivity.

**Подтверждено F041:** реальная регистрация drill-in маршрутов лежит в `modules/screen/main/{Settings,Vocabulary,Quiz}.kt`.

**Аналог для `ComponentsManagerScreen` (без arg) drill-in из Settings tab:** найден — `AboutAppScreen` (`Settings.kt:26-32`).
**Аналог для `PerDictionaryComponentsScreen(dictId)` drill-in из Vocabulary/Quiz tab:** найден — `WordCard` (`Vocabulary.kt:24-36`).

## 6. Root navigation

`app/.../route/RootRouter.kt:20-28` — `enum class RootPoint(route)`: `SPLASH`, `DICTIONARY_SETUP`, `DICTIONARY_CREATE?editId={editId}`, `DICTIONARY_LIST`, `MAIN_ROUTER`. Эти routes регистрируются прямо в `RootRouter(navController)` через `composable(RootPoint.X.route)` (`RootRouter.kt:48-98`). Drill-in screen-модулей фичи лежит **внутри MainScreen NavHost**, а не на root-уровне — `RootRouter.kt:99-111` делегирует через `mainRouter(...)` NavGraphBuilder ext.

`app/.../route/MainRouter.kt:11-13` — `enum class MainPoint(route)`: `MAIN`. Single composable `MAIN` инкапсулирует CompositionRoot и MainScreen.

## 7. Navigator interfaces / Impl

Конвенция: на каждый screen-модуль / widget с навигацией — `XxxNavigator: Navigator` (в screen-module) + `XxxNavigatorImpl(...)` в `app/.../navigator/`.

### Examples

`app/.../navigator/SettingsNavigatorImpl.kt:1-18`:
- импортирует `me.apomazkin.settingstab.SettingsNavigator` (line 3) — interface live в screen-module.
- ctor params (line 5-8): три lambda — `onOpenLangManagement`, `onOpenAboutApp`, `onOpenWebView`.
- `back() { ... // таб остаётся открытым }` (line 10-12).

`modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigator.kt:1-9` — `interface SettingsNavigator : Navigator { openLangManagement(); openAboutApp(); openWebView(pageKey) }`.

`app/.../navigator/DictionaryAppBarNavigatorImpl.kt:1-13` — `class DictionaryAppBarNavigatorImpl(onOpenDictionaryCreate)` — текущий single-method navigator (понадобится расширение для `openPerDictionaryComponents`).

`app/.../navigator/WordCardNavigatorImpl.kt:1-9` — minimal `class WordCardNavigatorImpl(onBack)` — пример простого drill-in.

`app/.../navigator/VocabularyNavigatorImpl.kt:1-13` — `class VocabularyNavigatorImpl(onOpenWordCard: (Long) -> Unit)` — пример с arg-callback.

Текущий список `*Impl` в `app/.../navigator/` (полный):
```
ChatNavigatorImpl.kt
DictionaryAppBarNavigatorImpl.kt
FormNavigatorImpl.kt
ListNavigatorImpl.kt
QuizTabNavigatorImpl.kt
SettingsNavigatorImpl.kt
SplashNavigatorImpl.kt
StatisticNavigatorImpl.kt
VocabularyNavigatorImpl.kt
WordCardNavigatorImpl.kt
```

**Аналог для `ComponentsManagerNavigatorImpl` / `PerDictionaryComponentsNavigatorImpl`:** найден — `WordCardNavigatorImpl` (back-only) + `VocabularyNavigatorImpl` (callback с arg).

## 8. Mate framework wiring (Msg / Reducer / NavigationEffect / Handler)

Прецедент wiring nav-effect в screen-module (`settingstab`):

- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/Message.kt:6-17` — `sealed interface Msg { data object OpenLangManagement; data object OpenAboutApp; data class OpenWebView(pageKey); ... }`.
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffect.kt:5-9` — `sealed interface SettingsNavigationEffect : NavigationEffect { data object OpenLangManagement; data object OpenAboutApp; data class OpenWebView(pageKey) }`.
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/SettingsTabReducer.kt:20-32` — reducer mapping `Msg.OpenX → SettingsNavigationEffect.OpenX`.
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffectHandler.kt:1-26` — `@AssistedInject` handler, в `onScreenEffect(effect)` `when(effect)` маппит `SettingsNavigationEffect → settingsNavigator.openX()`. `@AssistedFactory interface Factory { fun create(navigator: SettingsNavigator): SettingsNavigationEffectHandler }`.

Аналогичный pattern в widget'е:
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt:7-33` — `sealed interface Msg { ... OpenDictionaryCreate ... }`.
- `modules/widget/dictionaryappbar/.../DictionaryAppBarNavigationEffect.kt:5-7` — `sealed interface DictionaryAppBarNavigationEffect : NavigationEffect { data object OpenDictionaryCreate }`.
- `modules/widget/dictionaryappbar/.../mate/DictionaryAppBarReducer.kt:35-37` — `is Msg.OpenDictionaryCreate -> state to setOf(DictionaryAppBarNavigationEffect.OpenDictionaryCreate)`.
- `modules/widget/dictionaryappbar/.../DictionaryAppBarNavigationEffectHandler.kt:10-23` — handler.

**Аналог wiring `Msg.OpenComponentsManager` / `Msg.OpenComponentConstructor` + соответствующих NavigationEffect variants + handler branches:** найден (см. F045 / F048).

## 9. `LangManageWidget` + `AboutAppScreen` precedent (drill-in из Settings tab)

### Widget в Settings list

`modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/items/LangManageWidget.kt:1-26`:
- `SettingsItemWidget(iconRes, titleRes, showNextIcon = true, onClick)` — base widget (line 12-17).
- Используется в `SettingsTabScreen.kt:92` — `LangManageWidget(onClick = { sendMessage(Msg.OpenLangManagement) })`.

Список items в `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/items/`:
```
AboutAppWidget.kt  AppShareWidget.kt  base/  ExportDataWidget.kt
FeedBackWidget.kt  ImportDataWidget.kt  LangManageWidget.kt
PrivacyPolicyWidget.kt  RateWidget.kt
```

**Аналог `ComponentsManageWidget` для Settings list:** найден — `LangManageWidget` (рядом, тот же pattern).

### AboutAppScreen — простой drill-in без отдельного gradle module

`modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/AboutAppScreen.kt:1-75` — composable в том же `settingstab` модуле, БЕЗ собственного ViewModel / DI (только `appVersion` + `onBackPress`).

**ВАЖНО:** `AboutAppScreen` живёт **внутри** `:modules:screen:settingstab` — не выделен в отдельный модуль. Прецедент того, что drill-in экран может жить в parent screen-module.

## 10. `DictionaryAppBar` shared widget — host wiring

`modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBar.kt:43-77` — public composable `DictionaryAppBar(titleResId, state, sendMessage)`.

Текущий `actions` slot (`DictionaryAppBar.kt:52-74`):
- `state.isLoading == true` — `CircularProgressIndicator`.
- иначе — `DictDropDownWidget` (текущий «единственный» action).

**Подтверждено F043/F048:** `DictionaryAppBar` инстанцируется в 3 host'ах через `CompositionRootImpl`:
- `CompositionRootImpl.kt:67-71` — `VocabularyTabDep` (Vocabulary tab).
- `CompositionRootImpl.kt:105-109` — `QuizTabScreenDep` (Quiz tab).
- `CompositionRootImpl.kt:138-142` — `StatisticTabScreenDep` (Statistic tab).

Каждый host передаёт свой `DictionaryAppBarNavigatorImpl` (factory-style). Параметры single navigator interface (`DictionaryAppBarNavigator.kt:5-7`): `openDictionaryCreate()` (один метод сейчас).

## 11. `core/ui` widgets (Tier 1)

`modules/core/ui/src/main/java/me/apomazkin/ui/` — generic primitives:
```
btn/  dialog/  dropdown/  FlagPlaceholderWidget.kt  IconBoxed.kt
ImageBgGradWidget.kt  ImageFlagWidget.kt  ImageRoundedWidget.kt
ImageTitledWidget.kt  input/  LexemeBottomSheet.kt  lifecycle/
LogTags.kt  preview/  resource/  SystemBarsWidget.kt  text/  unused/
```

`core/ui` НЕ содержит domain-specific composables (нет «component»/«template» виджетов). Прецедент: `LexemeBottomSheet.kt` — название отсылает к domain, но это generic bottom-sheet primitive.

## 12. Resources

`core/core-resources/` — общий resources-модуль:
- Icon ресурс `R.drawable.ic_logo_splash` упомянут в `AboutAppScreen.kt:49` (`me.apomazkin.core_resources.R.drawable.ic_logo_splash`).
- `R.string.settings_section_privacy_policy` (`CompositionRootImpl.kt:202`).

Конвенция: drawable / string / dimens идут в `:core:core-resources` (общий для всех модулей).

## 13. Что НЕ найдено как аналог

- **Generic «template-driven UI» виджет** — `ComponentBlock(name, content)` wrapper аналога нет. `core/ui` не содержит, `wordcard/widget/lexeme/` содержит `LexemeCard.kt` / `LexemeMeaningField.kt` (вокруг которых строится UI лексемы), но они не template-driven — wired статично на translation/definition fields. См. `wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeMeaningField.kt` (передаётся `labelRes` + `state`).
- **Многоэкранный (list + form + dialog) screen-модуль для CRUD сущности** — `settingstab` содержит import/export workflow с диалогом, `dictionary` (`:modules:screen:dictionary`) содержит form-экран + list-экран (`FormNavigatorImpl` / `ListNavigatorImpl` в `app/navigator/`). Эта пара — частичный прецедент: list + form в одном модуле, но без soft-delete confirmation dialog с impact-preview. `wordcard` содержит `ConfirmDeleteLexemeWidget` (`wordcard/src/main/java/me/apomazkin/wordcard/widget/ConfirmDeleteLexemeWidget.kt`) — аналог confirmation dialog без impact-preview.
- **Soft-delete preview-of-impact widget** — нет. Создаётся в первый раз для IS481.

## 14. CI / job

Из `CLAUDE.md`: «CI pipeline (GitHub Actions): Lint → Unit Tests → Build APK. Triggers on branches `IS*` and `MT*`.»

CI не требует расширения для добавления screen-модуля — `:modules:screen:components_manager:testDebugUnitTest` подхватится автоматически по conventional task `testDebugUnitTest`.

## Вердикт

Аналог: **найден** для всех ключевых infra-требований.

- Gradle setup нового widget-модуля (без DI / с DI) — есть прецеденты `iconDropDowned` / `chipPicker` / `dictionarypicker` (без DI) и `dictionaryappbar` (с DI).
- Gradle setup нового screen-модуля (TEA + Dagger) — `wordcard` / `settingstab`.
- DI wiring (`AppComponent` getter + `XxxModule.kt` + `XxxUseCaseImpl.kt` + `XxxUseCase` interface в `deps/`) — `SettingsModule` / `WordCardModule`.
- `CompositionRoot.*ScreenDep` метод — `AboutAppScreenDep` (без arg) и `WordCardScreenDep(wordId)` (с arg).
- `*NavigatorImpl` в `app/navigator/` — `SettingsNavigatorImpl` (multi-method) и `WordCardNavigatorImpl` (back-only) и `VocabularyNavigatorImpl(callback)`.
- Регистрация drill-in routes в `modules/screen/main/{Settings,Vocabulary}.kt` (НЕ в `MainActivity` / `Constants.kt` / `NavEntity.kt`).
- Расширение Mate (Msg / NavigationEffect / Reducer / EffectHandler) — `settingstab` + `dictionaryappbar`.
- Widget в Settings list — `LangManageWidget`.
- Shared widget host wiring в 3 tab'ах — `DictionaryAppBar` через `CompositionRootImpl`.

Не найдено как полный аналог (создаётся впервые):
- template-driven UI wrapper `ComponentBlock(name, content)` — generic-варианта нет ни в `core/ui`, ни в widget'ах.
- soft-delete confirmation dialog с impact-preview (count of values / affected dictionaries / quiz configs / prefs).
- CRUD-screen с aggregated cross-dictionary view (`ComponentsManagerScreen`).

Эти три блока — net-new UI, но не требуют новой infra (укладываются в существующие конвенции screen + widget модулей).

_model: claude-opus-4-7[1m]_

## log_messages

- собраны факты по: `settings.gradle.kts`, `AppComponent`, `CompositionRoot[Impl]`, `MainScreen` / `Settings.kt` / `Vocabulary.kt` / `Quiz.kt`, `*NavigatorImpl`, widget gradle conventions, mate wiring (Msg/Effect/Handler), `LangManageWidget` / `AboutAppScreen`, `DictionaryAppBar` 3-host wiring;
- аналог найден для всех ключевых infra-точек фичи; net-new — только `ComponentBlock` wrapper, deletion-impact preview dialog, aggregated cross-dict view;
- подтверждены F040-F048 фактами `file:line`.
