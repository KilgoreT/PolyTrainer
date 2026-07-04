# Infra design tree: IS481 component_constructor

DAG-граф infrastructure-файлов для двух новых screen-модулей (`components_manager` / `per_dictionary_components`) + одного widget-модуля (`component_widgets`) + расширения существующих host'ов (`SettingsTab` drill-in + `DictionaryAppBar` shared 3-host drill-in) + регистрации миграции M12→M13 в `RoomModule`.

## Часть 1: Граф

```yaml
# ============================================================
# Tier 0: Gradle setup (новые модули)
# ============================================================
- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/settings.gradle.kts
  action: "~"
  depends: []

- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/component_widgets/build.gradle.kts
  action: "+"
  depends: [1]

- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/build.gradle.kts
  action: "+"
  depends: [1]

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/build.gradle.kts
  action: "+"
  depends: [1]

- id: 36
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/build.gradle.kts
  action: "~"
  depends: [2, 3, 4]

# ============================================================
# Tier 1: UseCase interface (deps/) в screen-модулях
# ============================================================
- id: 5
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt
  action: "+"
  depends: [3]

- id: 6
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt
  action: "+"
  depends: [4]

# ============================================================
# Tier 2: Navigator interfaces в screen-модулях
# ============================================================
- id: 7
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerNavigator.kt
  action: "+"
  depends: [3]

- id: 8
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/PerDictionaryComponentsNavigator.kt
  action: "+"
  depends: [4]

# ============================================================
# Tier 3: Existing screen-host navigator interfaces — расширение
# ============================================================
- id: 9
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigator.kt
  action: "~"
  depends: []

- id: 10
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigator.kt
  action: "~"
  depends: []

# ============================================================
# Tier 4: Mate-wiring SettingsTab (Msg / NavigationEffect / Reducer / Handler)
# ============================================================
- id: 11
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/Message.kt
  action: "~"
  depends: []

- id: 12
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffect.kt
  action: "~"
  depends: []

- id: 13
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/SettingsTabReducer.kt
  action: "~"
  depends: [11, 12]

- id: 14
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffectHandler.kt
  action: "~"
  depends: [9, 12]

# ============================================================
# Tier 5: Mate-wiring DictionaryAppBar (Msg / NavigationEffect / Reducer / Handler)
# ============================================================
- id: 15
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt
  action: "~"
  depends: []

- id: 16
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigationEffect.kt
  action: "~"
  depends: []

- id: 17
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducer.kt
  action: "~"
  depends: [15, 16]

- id: 18
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBarNavigationEffectHandler.kt
  action: "~"
  depends: [10, 16]

# ============================================================
# Tier 6: Navigator impl в app/
# ============================================================
- id: 19
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/SettingsNavigatorImpl.kt
  action: "~"
  depends: [9]

- id: 20
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/DictionaryAppBarNavigatorImpl.kt
  action: "~"
  depends: [10]

- id: 21
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/ComponentsManagerNavigatorImpl.kt
  action: "+"
  depends: [7, 36]

- id: 22
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/PerDictionaryComponentsNavigatorImpl.kt
  action: "+"
  depends: [8, 36]

# ============================================================
# Tier 7: DI modules (binds UseCase impl) в app/
# ============================================================
- id: 23
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt
  action: "+"
  depends: [5, 36]

- id: 24
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerModule.kt
  action: "+"
  depends: [5, 23]

- id: 25
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt
  action: "+"
  depends: [6, 36]

- id: 26
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsModule.kt
  action: "+"
  depends: [6, 25]

# ============================================================
# Tier 8: AppComponent (factory methods + module includes)
# ============================================================
- id: 27
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt
  action: "~"
  depends: [24, 26, 36]

# ============================================================
# Tier 9: CompositionRoot interface + impl
# ============================================================
- id: 28
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/CompositionRoot.kt
  action: "~"
  depends: []

- id: 29
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/uiDeps/CompositionRootImpl.kt
  action: "~"
  depends: [19, 20, 21, 22, 27, 28, 36]

- id: 37
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/route/MainRouter.kt
  action: "~"
  depends: [27, 29]

# ============================================================
# Tier 10: Navigation route registration в modules/screen/main/
# ============================================================
- id: 30
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt
  action: "~"
  depends: [28]

- id: 31
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Vocabulary.kt
  action: "~"
  depends: [28]

- id: 32
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Quiz.kt
  action: "~"
  depends: [28, 31]

- id: 33
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/Statistic.kt
  action: "+"
  depends: [28, 31]

- id: 34
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/MainScreen.kt
  action: "~"
  depends: [33]

# ============================================================
# Tier 11: RoomModule — регистрация миграции M12→M13
# ============================================================
- id: 35
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt
  action: "~"
  depends: []
```

## Часть 2: Детали изменений

### Tier 0: Gradle setup

#### [~] `settings.gradle.kts` (id 1)

Было:
```kotlin
//Features
include(":modules:screen:splash")
...
include(":modules:screen:settingstab")
include(":modules:screen:quiz:chat")

//Widget
include(":modules:widget:dictionaryappbar")
include(":modules:widget:dictionarypicker")
include(":modules:widget:iconDropDowned")
include(":modules:widget:chipPicker")
```

Стало (добавить три include после соответствующих секций):
```kotlin
//Features
...
include(":modules:screen:quiz:chat")
include(":modules:screen:components_manager")
include(":modules:screen:per_dictionary_components")

//Widget
...
include(":modules:widget:chipPicker")
include(":modules:widget:component_widgets")
```

#### [+] `modules/widget/component_widgets/build.gradle.kts` (id 2)

Назначение: Tier 2 widget без DI (template-preview composables + `ComponentBlock` wrapper). Аналог — `modules/widget/dictionarypicker/build.gradle.kts`.

Сигнатура:
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "me.apomazkin.component_widgets"
    compileSdk = 35
    defaultConfig { minSdk = 23; targetSdk = 35 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }
}

dependencies {
    implementation(project(":modules:core:theme"))
    implementation(project(":modules:core:ui"))
    implementation(project(":modules:domain:lexeme"))
    implementation(project(":core:core-resources"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)
}
```

Зависимость на `:modules:domain:lexeme` нужна для доступа к `ComponentTemplate` / `Primitive` / `Field` (template-resolver wiring).

#### [+] `modules/screen/components_manager/build.gradle.kts` (id 3)

Назначение: screen-модуль c TEA + Dagger (KSP). Аналог — `modules/screen/wordcard/build.gradle.kts`.

Сигнатура (структурно идентична wordcard; различия — namespace и дополнительная зависимость на `:modules:widget:component_widgets`):
```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "me.apomazkin.components_manager"
    compileSdk = 35
    defaultConfig { minSdk = 23; targetSdk = 35 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }
}

dependencies {
    implementation(project(":modules:core:di"))
    implementation(project(":modules:core:mate"))
    implementation(diLibs.dagger)
    ksp(diLibs.daggerCompiler)
    implementation(project(":modules:core:theme"))
    implementation(project(":modules:core:ui"))
    implementation(project(":modules:core:tools"))
    implementation(project(":modules:core:logger"))
    implementation(project(":modules:domain:lexeme"))
    implementation(project(":core:core-resources"))
    implementation(project(":modules:widget:component_widgets"))

    implementation(composeLibs.lifecycleViewmodelCompose)
    implementation(composeLibs.lifecycleRuntimeCompose)

    testImplementation("junit:junit:4.13.2")
    testImplementation(project(":modules:core:mate"))
}
```

#### [+] `modules/screen/per_dictionary_components/build.gradle.kts` (id 4)

Структурно идентично `components_manager/build.gradle.kts` (id 3); различие — `namespace = "me.apomazkin.per_dictionary_components"`. Тот же набор dependencies.

#### [~] `app/build.gradle.kts` (id 36)

Назначение: `app/` ссылается в `AppComponent` / `CompositionRootImpl` на ViewModel'ы новых screen-модулей и template-preview composables widget'а — без явных `implementation(project(...))` модули не доступны на compile-classpath.

Добавить три `implementation(project(...))` к dependencies:
```kotlin
implementation(project(":modules:screen:components_manager"))
implementation(project(":modules:screen:per_dictionary_components"))
implementation(project(":modules:widget:component_widgets"))
```

Точное расположение строк зависит от существующих секций dependencies в `app/build.gradle.kts` — добавлять рядом с `:modules:screen:settingstab` / другими screen module entries.

### Tier 1: UseCase interfaces (deps/)

#### [+] `ComponentsManagerUseCase.kt` (id 5)

Назначение: deps-контракт screen-модуля (аналог `SettingsTabUseCase.kt`). Точные методы — на `business_design_tree`. Здесь — placeholder interface, чтобы `ComponentsManagerModule.@Binds` имел target.

```kotlin
package me.apomazkin.components_manager.deps

interface ComponentsManagerUseCase {
    // методы (createUserDefinedComponent / renameComponent / softDeleteComponent /
    // previewDeletionImpact / flowUserDefinedTypes / componentsAcrossAllDictionaries)
    // — финализируются на business_contract / business_design_tree
}
```

#### [+] `PerDictionaryComponentsUseCase.kt` (id 6)

Аналогично, для per-dict screen-модуля. Placeholder interface. Точные методы — на `business_design_tree` (per-dict UseCase возможно reuse'ит часть `ComponentsManagerUseCase` методов либо имеет свой scoped subset).

### Tier 2: Navigator interfaces в новых screen-модулях

#### [+] `ComponentsManagerNavigator.kt` (id 7)

Назначение: navigator контракт для `ComponentsManagerScreen` (аналог `WordCardNavigator` — back-only пока, drill-in внутри экрана не required в MVP).

```kotlin
package me.apomazkin.components_manager

import me.apomazkin.mate.Navigator

interface ComponentsManagerNavigator : Navigator {
    // back() из Navigator — единственный метод в MVP
}
```

#### [+] `PerDictionaryComponentsNavigator.kt` (id 8)

Аналогично, для per-dict screen-модуля:
```kotlin
package me.apomazkin.per_dictionary_components

import me.apomazkin.mate.Navigator

interface PerDictionaryComponentsNavigator : Navigator {
    // back() из Navigator — единственный метод в MVP
}
```

### Tier 3: Existing navigator interfaces — расширение

#### [~] `SettingsNavigator.kt` (id 9)

Было:
```kotlin
interface SettingsNavigator : Navigator {
    fun openLangManagement()
    fun openAboutApp()
    fun openWebView(pageKey: String)
}
```

Стало (добавить один метод):
```kotlin
interface SettingsNavigator : Navigator {
    fun openLangManagement()
    fun openAboutApp()
    fun openWebView(pageKey: String)
    fun openComponentsManager()
}
```

#### [~] `DictionaryAppBarNavigator.kt` (id 10)

Было:
```kotlin
interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
}
```

Стало (добавить один метод с arg):
```kotlin
interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
    fun openPerDictionaryComponents(dictionaryId: Long)
}
```

### Tier 4: Mate-wiring SettingsTab

#### [~] `Message.kt` (settingstab) (id 11)

Было — sealed `Msg` уже содержит `OpenLangManagement`, `OpenAboutApp`, `OpenWebView`.

Стало — добавить:
```kotlin
data object OpenComponentsManager : Msg
```

#### [~] `SettingsNavigationEffect.kt` (id 12)

Было — sealed `SettingsNavigationEffect` содержит `OpenLangManagement`, `OpenAboutApp`, `OpenWebView`.

Стало — добавить:
```kotlin
data object OpenComponentsManager : SettingsNavigationEffect
```

#### [~] `SettingsTabReducer.kt` (id 13)

Было — в `reduce(...) when (message)` — три ветки для существующих Open*.

Стало — добавить ветку:
```kotlin
is Msg.OpenComponentsManager -> state to setOf(
    SettingsNavigationEffect.OpenComponentsManager
)
```

#### [~] `SettingsNavigationEffectHandler.kt` (id 14)

Было — `when (effect)` маппит три effect → три метода navigator'а.

Стало — добавить ветку:
```kotlin
is SettingsNavigationEffect.OpenComponentsManager -> settingsNavigator.openComponentsManager()
```

### Tier 5: Mate-wiring DictionaryAppBar

#### [~] `Message.kt` (dictionaryappbar.mate) (id 15)

Было — sealed `Msg` содержит `AvailableDict`, `CurrentDict`, `ChangeDict`, `DictMenuOn`, `DictMenuOff`, `OpenDictionaryCreate`, `Empty`.

Стало — добавить:
```kotlin
data class OpenPerDictionaryComponents(val dictionaryId: Long) : Msg
```

Note: payload `dictionaryId` нужен, т.к. UseCase / Reducer хранит `currentDict.id` в state — извлекать его в Msg явно (а не читать state внутри ui-onClick'а) — соответствует mate-framework convention (`docs/guides/mate-framework.md`).

#### [~] `DictionaryAppBarNavigationEffect.kt` (id 16)

Было — sealed `DictionaryAppBarNavigationEffect` содержит `OpenDictionaryCreate`.

Стало — добавить:
```kotlin
data class OpenPerDictionaryComponents(val dictionaryId: Long) : DictionaryAppBarNavigationEffect
```

#### [~] `DictionaryAppBarReducer.kt` (id 17)

Было — `is Msg.OpenDictionaryCreate -> state to setOf(DictionaryAppBarNavigationEffect.OpenDictionaryCreate)`.

Стало — добавить ветку:
```kotlin
is Msg.OpenPerDictionaryComponents -> state to setOf(
    DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(message.dictionaryId)
)
```

#### [~] `DictionaryAppBarNavigationEffectHandler.kt` (id 18)

Стало — добавить ветку:
```kotlin
is DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents ->
    barNavigator.openPerDictionaryComponents(effect.dictionaryId)
```

### Tier 6: Navigator impl в `app/`

#### [~] `SettingsNavigatorImpl.kt` (id 19)

Было — ctor 3 lambda's; override 3 метода + `back()`.

Стало — ctor 4 lambda's; override 4 метода:
```kotlin
class SettingsNavigatorImpl(
    private val onOpenLangManagement: () -> Unit,
    private val onOpenAboutApp: () -> Unit,
    private val onOpenWebView: (String) -> Unit,
    private val onOpenComponentsManager: () -> Unit,
) : SettingsNavigator {
    override fun back() { /* таб остаётся открытым */ }
    override fun openLangManagement() = onOpenLangManagement()
    override fun openAboutApp() = onOpenAboutApp()
    override fun openWebView(pageKey: String) = onOpenWebView(pageKey)
    override fun openComponentsManager() = onOpenComponentsManager()
}
```

#### [~] `DictionaryAppBarNavigatorImpl.kt` (id 20)

Было — ctor 1 lambda; override 1 метод + `back()`.

Стало — ctor 2 lambda's; override 2 метода:
```kotlin
class DictionaryAppBarNavigatorImpl(
    private val onOpenDictionaryCreate: () -> Unit,
    private val onOpenPerDictionaryComponents: (dictionaryId: Long) -> Unit,
) : DictionaryAppBarNavigator {
    override fun back() { /* shared widget — back делегируется хост-экраном */ }
    override fun openDictionaryCreate() = onOpenDictionaryCreate()
    override fun openPerDictionaryComponents(dictionaryId: Long) =
        onOpenPerDictionaryComponents(dictionaryId)
}
```

Note: `DictionaryAppBarNavigatorImpl` создаётся через `remember { ... }` в **трёх** местах `CompositionRootImpl` (`VocabularyTabDep` / `QuizTabScreenDep` / `StatisticTabScreenDep`) — каждый host передаёт свою lambda для `onOpenPerDictionaryComponents` (F043/F048).

#### [+] `ComponentsManagerNavigatorImpl.kt` (id 21)

Назначение: minimal back-only navigator для `ComponentsManagerScreen`. Аналог — `WordCardNavigatorImpl`.

```kotlin
package me.apomazkin.polytrainer.navigator

import me.apomazkin.components_manager.ComponentsManagerNavigator

class ComponentsManagerNavigatorImpl(
    private val onBack: () -> Unit,
) : ComponentsManagerNavigator {
    override fun back() = onBack()
}
```

#### [+] `PerDictionaryComponentsNavigatorImpl.kt` (id 22)

Аналогично:
```kotlin
package me.apomazkin.polytrainer.navigator

import me.apomazkin.per_dictionary_components.PerDictionaryComponentsNavigator

class PerDictionaryComponentsNavigatorImpl(
    private val onBack: () -> Unit,
) : PerDictionaryComponentsNavigator {
    override fun back() = onBack()
}
```

### Tier 7: DI modules в `app/.../di/module/`

#### [+] `ComponentsManagerUseCaseImpl.kt` (id 23)

Назначение: impl `ComponentsManagerUseCase` с инжекшеном `CoreDbApi.LexemeApi` (либо нового `ComponentApi`, см. Open question в `02_scope.md`). Точные методы — на `business_design_tree`. На infra-уровне — `@Inject constructor(...)` + stub-методы.

```kotlin
package me.apomazkin.polytrainer.di.module.componentsmanager

import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import javax.inject.Inject

class ComponentsManagerUseCaseImpl @Inject constructor(
    // dependencies injected from core-db-api / domain — финализация на business_design_tree
) : ComponentsManagerUseCase {
    // implementations — business_design_tree
}
```

#### [+] `ComponentsManagerModule.kt` (id 24)

Аналог `SettingsModule.kt`:
```kotlin
package me.apomazkin.polytrainer.di.module.componentsmanager

import dagger.Binds
import dagger.Module
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase

@Module
interface ComponentsManagerModule {
    @Binds
    fun bindComponentsManagerUseCase(impl: ComponentsManagerUseCaseImpl): ComponentsManagerUseCase
}
```

#### [+] `PerDictionaryComponentsUseCaseImpl.kt` (id 25)

Аналогично id 23, для per-dict экрана.

#### [+] `PerDictionaryComponentsModule.kt` (id 26)

Аналогично id 24:
```kotlin
package me.apomazkin.polytrainer.di.module.perdictionarycomponents

import dagger.Binds
import dagger.Module
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase

@Module
interface PerDictionaryComponentsModule {
    @Binds
    fun bindPerDictionaryComponentsUseCase(
        impl: PerDictionaryComponentsUseCaseImpl
    ): PerDictionaryComponentsUseCase
}
```

### Tier 8: AppComponent

#### [~] `AppComponent.kt` (id 27)

**Изменение 1** — добавить два factory-метода (per-screen ViewModel factories):
```kotlin
fun getComponentsManagerViewModelFactory(): ComponentsManagerViewModel.Factory
fun getPerDictionaryComponentsViewModelFactory(): PerDictionaryComponentsViewModel.Factory
```

Имена `ComponentsManagerViewModel` / `PerDictionaryComponentsViewModel` — конкретные классы будут созданы в `business_design_tree` внутри соответствующих screen-модулей.

**Изменение 2** — добавить два модуля в `AppModule.includes`:
```kotlin
@Module(
    includes = [
        ...,
        SettingsModule::class,
        DictionaryAppBarModule::class,
        ComponentsManagerModule::class,
        PerDictionaryComponentsModule::class,
        ...,
    ]
)
interface AppModule
```

**Изменение 3** — добавить import'ы соответствующих ViewModel классов и DI module'ей.

### Tier 9: CompositionRoot

#### [~] `CompositionRoot.kt` (id 28)

Было — 8 `*Dep` методов: `VocabularyTabDep`, `WordCardScreenDep`, `QuizTabScreenDep`, `ChatQuizScreenDep`, `StatisticTabScreenDep`, `SettingsTabScreenDep`, `AboutAppScreenDep`, `WebViewScreenDep`.

**Изменение 1** — добавить два новых `*Dep` метода:
```kotlin
@Composable
fun ComponentsManagerScreenDep(
    onBackPress: () -> Unit,
)

@Composable
fun PerDictionaryComponentsScreenDep(
    dictionaryId: Long,
    onBackPress: () -> Unit,
)
```

**Изменение 2** — расширить сигнатуры трёх существующих host'-методов — добавляется параметр `openPerDictionaryComponents`, поскольку drill-in `openPerDictionaryComponents` живёт **внутри tab-host** (`DictionaryAppBar` shared widget), а не на уровне `MainScreen`. Лямбда поступает из NavGraphBuilder ext'ов (`Vocabulary.kt` / `Quiz.kt` / `Statistic.kt`) через тот же механизм что и `openDictionaryCreate`:

```kotlin
@Composable
fun VocabularyTabDep(
    openDictionaryCreate: () -> Unit,
    openWordCard: (wordId: Long) -> Unit,
    openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
)

@Composable
fun QuizTabScreenDep(
    openDictionaryCreate: () -> Unit,
    openChatQuiz: (quizType: String) -> Unit,
    openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
)

@Composable
fun StatisticTabScreenDep(
    openDictionaryCreate: () -> Unit,
    openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
)
```

**Изменение 3** — расширить сигнатуру `SettingsTabScreenDep` — добавить параметр `onComponentsManagerClick: () -> Unit` (по аналогии с host-методами Vocabulary/Quiz/Statistic, которые получают `openPerDictionaryComponents`). Лямбда поступает из NavGraphBuilder ext'а `Settings.kt` через тот же механизм что и `onLangManagementClick` / `onAboutAppClick` / `onPrivacyPolicyClick`:

```kotlin
@Composable
fun SettingsTabScreenDep(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onComponentsManagerClick: () -> Unit,
)
```

Это изменение interface'а парно с «Изменение 3» в `CompositionRootImpl.kt` (id 29) — обе сигнатуры обязаны совпадать.

#### [~] `CompositionRootImpl.kt` (id 29)

**Изменение 1** — расширить ctor добавлением двух новых ViewModel factories:
```kotlin
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    ...,
    private val settingsTabViewModelFactory: SettingsTabViewModel.Factory,
    private val componentsManagerViewModelFactory: ComponentsManagerViewModel.Factory,
    private val perDictionaryComponentsViewModelFactory: PerDictionaryComponentsViewModel.Factory,
    private val envParams: EnvParams,
    private val logger: LexemeLogger,
) : CompositionRoot { ... }
```

**Изменение 2** — обновить `VocabularyTabDep` / `QuizTabScreenDep` / `StatisticTabScreenDep`:
- расширить ctor `DictionaryAppBarNavigatorImpl` — добавить вторую лямбду `onOpenPerDictionaryComponents = openPerDictionaryComponents`;
- расширить сигнатуру самого `*TabDep` метода (новый параметр `openPerDictionaryComponents: (Long) -> Unit`).

Пример для `VocabularyTabDep`:
```kotlin
@Composable
override fun VocabularyTabDep(
    openDictionaryCreate: () -> Unit,
    openWordCard: (wordId: Long) -> Unit,
    openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
) {
    val appBarNavigator = remember(openDictionaryCreate, openPerDictionaryComponents) {
        DictionaryAppBarNavigatorImpl(
            onOpenDictionaryCreate = openDictionaryCreate,
            onOpenPerDictionaryComponents = openPerDictionaryComponents,
        )
    }
    ...
}
```

**Изменение 3** — обновить `SettingsTabScreenDep`:
- расширить ctor `SettingsNavigatorImpl` — добавить четвёртую лямбду `onOpenComponentsManager`;
- расширить сигнатуру `SettingsTabScreenDep` — добавить параметр `onComponentsManagerClick: () -> Unit`.

```kotlin
@Composable
override fun SettingsTabScreenDep(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onComponentsManagerClick: () -> Unit,
) {
    val navigator = remember(onLangManagementClick, onAboutAppClick, onPrivacyPolicyClick, onComponentsManagerClick) {
        SettingsNavigatorImpl(
            onOpenLangManagement = onLangManagementClick,
            onOpenAboutApp = onAboutAppClick,
            onOpenWebView = { ... },
            onOpenComponentsManager = onComponentsManagerClick,
        )
    }
    SettingsTabScreen(...)
}
```

**Изменение 4** — реализовать `ComponentsManagerScreenDep`:
```kotlin
@Composable
override fun ComponentsManagerScreenDep(
    onBackPress: () -> Unit,
) {
    val navigator = remember(onBackPress) {
        ComponentsManagerNavigatorImpl(onBack = onBackPress)
    }
    ComponentsManagerScreen(
        factory = componentsManagerViewModelFactory,
        navigator = navigator,
    )
}
```

**Изменение 5** — реализовать `PerDictionaryComponentsScreenDep`:
```kotlin
@Composable
override fun PerDictionaryComponentsScreenDep(
    dictionaryId: Long,
    onBackPress: () -> Unit,
) {
    val navigator = remember(onBackPress) {
        PerDictionaryComponentsNavigatorImpl(onBack = onBackPress)
    }
    PerDictionaryComponentsScreen(
        dictionaryId = dictionaryId,
        factory = perDictionaryComponentsViewModelFactory,
        navigator = navigator,
    )
}
```

Note (per F042): сигнатура `*Screen(...)` composable'ов финализируется в `ui_design_tree` шаге. На infra-уровне фиксируется только то, что они принимают factory + navigator (+ `dictionaryId` для per-dict).

**Изменение 6** — обновить wiring в `MainRouter.kt` — выделено в отдельный узел графа (id 37, см. ниже).

#### [~] `MainRouter.kt` (id 37)

Внутри `MainRouter` создаётся `CompositionRootImpl(...)` с передачей factories из `AppComponent` (текущий wiring: 7 factories + envParams + logger). После расширения ctor `CompositionRootImpl` (id 29) и добавления двух новых factory-методов в `AppComponent` (id 27) — `MainRouter.kt` compile-broken без обновления.

**Изменение** — добавить два новых factory parameters в ctor-call `CompositionRootImpl(...)`:

Было:
```kotlin
CompositionRootImpl(
    wordCardViewModelFactory = context.appComponent.getWordCardViewModelFactory(),
    chatViewModelFactory = context.appComponent.getChatViewModelFactory(),
    appBarViewModelFactory = context.appComponent.getDictionaryAppBarViewModelFactory(),
    dictionaryTabViewModelFactory = context.appComponent.getDictionaryTabViewModelFactory(),
    quizTabViewModelFactory = context.appComponent.getQuizTabViewModelFactory(),
    statisticViewModelFactory = context.appComponent.getStatisticViewModelFactory(),
    settingsTabViewModelFactory = context.appComponent.getSettingsTabViewModelFactory(),
    envParams = context.appComponent.getEnvParams(),
    logger = context.appComponent.getLogger(),
)
```

Стало:
```kotlin
CompositionRootImpl(
    wordCardViewModelFactory = context.appComponent.getWordCardViewModelFactory(),
    chatViewModelFactory = context.appComponent.getChatViewModelFactory(),
    appBarViewModelFactory = context.appComponent.getDictionaryAppBarViewModelFactory(),
    dictionaryTabViewModelFactory = context.appComponent.getDictionaryTabViewModelFactory(),
    quizTabViewModelFactory = context.appComponent.getQuizTabViewModelFactory(),
    statisticViewModelFactory = context.appComponent.getStatisticViewModelFactory(),
    settingsTabViewModelFactory = context.appComponent.getSettingsTabViewModelFactory(),
    componentsManagerViewModelFactory = context.appComponent.getComponentsManagerViewModelFactory(),
    perDictionaryComponentsViewModelFactory = context.appComponent.getPerDictionaryComponentsViewModelFactory(),
    envParams = context.appComponent.getEnvParams(),
    logger = context.appComponent.getLogger(),
)
```

Без этого изменения `MainRouter.kt` не компилируется после расширения ctor `CompositionRootImpl`.

### Tier 10: Navigation route registration в `modules/screen/main/`

#### [~] `Settings.kt` (id 30)

Было — `settings(navController, compositionRoot, openDictionaryList)` регистрирует:
- `composable(TabPoint.SETTINGS.route)` → `SettingsTabScreenDep(...)` (3 callback'а).
- `composable(ABOUT_APP_ROUTE)` → `AboutAppScreenDep(...)`.
- `composable(WEBVIEW_ROUTE)` → `WebViewScreenDep(...)`.

Стало (добавление `COMPONENTS_MANAGER_ROUTE` + расширение Tab-composable передачей `onComponentsManagerClick`):
```kotlin
private const val ABOUT_APP_ROUTE = "about_app"
private const val WEBVIEW_ROUTE = "webview/{pageKey}"
private const val COMPONENTS_MANAGER_ROUTE = "components_manager"

fun NavGraphBuilder.settings(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
    openDictionaryList: () -> Unit,
) {
    composable(TabPoint.SETTINGS.route) {
        compositionRoot.SettingsTabScreenDep(
            onLangManagementClick = openDictionaryList,
            onAboutAppClick = { navController.goToAboutApp() },
            onPrivacyPolicyClick = { navController.goToWebView("privacy_policy") },
            onComponentsManagerClick = { navController.goToComponentsManager() },
        )
    }
    composable(ABOUT_APP_ROUTE) { ... }
    composable(WEBVIEW_ROUTE, arguments = ...) { ... }

    composable(route = COMPONENTS_MANAGER_ROUTE) {
        compositionRoot.ComponentsManagerScreenDep(
            onBackPress = { navController.backPress() },
        )
    }
}

private fun NavHostController.goToComponentsManager() {
    navigate(route = COMPONENTS_MANAGER_ROUTE) { launchSingleTop = true }
}
```

#### [~] `Vocabulary.kt` (id 31)

Per-dict экран должен быть достижим из **трёх** табов (Vocabulary / Quiz / Statistic). Решение: **регистрировать route один раз в одном файле, ссылаться из всех трёх tab-host'ов через `navController.navigate(...)`**.

**Решение** — регистрировать route per-dict экрана в `Vocabulary.kt` (как «канонический» tab-graph, потому что в нём уже зарегистрирован самый длинный набор маршрутов и базовая семантика «работа с лексемами»). Из `Quiz.kt` / `Statistic.kt` навигация идёт через **тот же NavHostController** через прямой `navigate("$PER_DICT_COMPONENTS_ROUTE/$dictId")`.

Изменение `Vocabulary.kt`:
```kotlin
private const val WORD_CARD_ROUTE = "wordCard"
private const val WORD_ID_ARG = "wordId"
private const val PER_DICT_COMPONENTS_ROUTE = "per_dict_components"
private const val PER_DICT_COMPONENTS_DICT_ID_ARG = "dictionaryId"

fun NavGraphBuilder.vocabulary(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
    openDictionaryCreate: () -> Unit,
) {
    composable(TabPoint.VOCABULARY.route) {
        compositionRoot.VocabularyTabDep(
            openDictionaryCreate = openDictionaryCreate,
            openWordCard = { navController.goToWordCard(it) },
            openPerDictionaryComponents = { dictId -> navController.goToPerDictionaryComponents(dictId) },
        )
    }
    composable(
        route = "$WORD_CARD_ROUTE/{$WORD_ID_ARG}",
        arguments = listOf(navArgument(WORD_ID_ARG) { type = NavType.LongType })
    ) { ... }
    composable(
        route = "$PER_DICT_COMPONENTS_ROUTE/{$PER_DICT_COMPONENTS_DICT_ID_ARG}",
        arguments = listOf(navArgument(PER_DICT_COMPONENTS_DICT_ID_ARG) { type = NavType.LongType })
    ) { navBackStackEntry ->
        val dictId: Long = navBackStackEntry.arguments?.getLong(PER_DICT_COMPONENTS_DICT_ID_ARG)
            ?: throw IllegalArgumentException("Unknown dictionaryId")
        compositionRoot.PerDictionaryComponentsScreenDep(
            dictionaryId = dictId,
            onBackPress = { navController.backPress() },
        )
    }
}

internal fun NavHostController.goToPerDictionaryComponents(dictionaryId: Long) {
    navigate(route = "per_dict_components/$dictionaryId") { launchSingleTop = true }
}
```

`goToPerDictionaryComponents` оставить `internal` (не `private`) чтобы переиспользовать из `Quiz.kt` / `Statistic.kt` в том же модуле `:modules:screen:main`.

#### [~] `Quiz.kt` (id 32)

Добавить третий параметр `openPerDictionaryComponents` в `compositionRoot.QuizTabScreenDep(...)`:
```kotlin
composable(TabPoint.QUIZ.route) {
    compositionRoot.QuizTabScreenDep(
        openDictionaryCreate = openDictionaryCreate,
        openChatQuiz = { navController.goToQuiz(it) },
        openPerDictionaryComponents = { dictId -> navController.goToPerDictionaryComponents(dictId) },
    )
}
```

Route per-dict-components **не** регистрируется здесь — используется shared route, зарегистрированный в `Vocabulary.kt`.

#### [+] `Statistic.kt` (id 33)

Назначение: новый NavGraphBuilder extension для Statistic tab. Сейчас Statistic-tab регистрируется inline через `composable(TabPoint.STATS.route)` в `MainScreen.kt:52-54`. Выделить в отдельный файл для parity с Vocabulary/Quiz/Settings и для возможности передать новый параметр `openPerDictionaryComponents` без захламления `MainScreen.kt`.

```kotlin
package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

fun NavGraphBuilder.statistic(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
    openDictionaryCreate: () -> Unit,
) {
    composable(TabPoint.STATS.route) {
        compositionRoot.StatisticTabScreenDep(
            openDictionaryCreate = openDictionaryCreate,
            openPerDictionaryComponents = { dictId -> navController.goToPerDictionaryComponents(dictId) },
        )
    }
}
```

#### [~] `MainScreen.kt` (id 34)

Было — `composable(TabPoint.STATS.route) { compositionRoot.StatisticTabScreenDep(openDictionaryCreate = openDictionaryCreate) }` inline.

Стало — заменить inline-registration на вызов нового NavGraphBuilder ext'а `statistic(...)`:
```kotlin
NavHost(...) {
    vocabulary(navController, compositionRoot, openDictionaryCreate)
    quiz(navController, compositionRoot, openDictionaryCreate)
    statistic(navController, compositionRoot, openDictionaryCreate)
    settings(navController, compositionRoot, openDictionaryList)
}
```

### Tier 11: RoomModule

#### [~] `RoomModule.kt` (id 35)

Было (`RoomModule.kt:50`):
```kotlin
.addMigrations(Migration_011_to_012)
```

Стало:
```kotlin
.addMigrations(Migration_011_to_012, Migration_012_to_013)
```

+ добавить import:
```kotlin
import me.apomazkin.core_db_impl.room.migrations.Migration_012_to_013
```

Note (F040): сама миграция `Migration_012_to_013.kt` создаётся на `data_design_tree` шаге; здесь регистрируется только её подключение к Room builder. **Без этого изменения upgrade 12→13 уйдёт по `fallbackToDestructiveMigration` → data loss.**

Также обновить docstring о текущей схеме: `v12 → v13`. Текущий комментарий «Текущая схема — v12 (IS481 main)» меняется на «Текущая схема — v13 (IS481 component_constructor)».

---

## Аудит

### 1. Покрытие § Затронутые файлы → Infrastructure из `02_scope.md`

`02_scope.md` фиксирует следующие infra/wiring-файлы:

| Файл из scope | Узел графа | Статус |
|---|---|---|
| `app/build.gradle.kts` | id 36 | ok |
| `modules/screen/components_manager/build.gradle.kts` | id 3 | ok |
| `modules/widget/component_widgets/build.gradle.kts` | id 2 | ok |
| `settings.gradle.kts` | id 1 | ok |
| `app/.../di/AppComponent.kt` | id 27 | ok |
| `app/.../di/module/...` (новые DI modules) | id 23, 24, 25, 26 | ok |
| `app/.../navigator/SettingsNavigatorImpl.kt` | id 19 | ok |
| `app/.../navigator/DictionaryAppBarNavigatorImpl.kt` | id 20 | ok |
| `modules/screen/main/CompositionRoot.kt` | id 28 | ok |
| `app/.../uiDeps/CompositionRootImpl.kt` | id 29 | ok |
| `modules/screen/main/Settings.kt` | id 30 | ok |
| `modules/screen/main/Vocabulary.kt` либо `Quiz.kt` | id 31, 32 | ok (per-dict route в `Vocabulary.kt`, callsite в `Quiz.kt`) |
| `core/core-db-impl/.../di/module/RoomModule.kt` | id 35 | ok |
| `SettingsTab.Msg` (`Message.kt`) | id 11 | ok |
| `SettingsNavigationEffect.kt` | id 12 | ok |
| `SettingsTabReducer.kt` | id 13 | ok |
| `SettingsNavigationEffectHandler.kt` | id 14 | ok |
| `DictionaryAppBar.mate.Msg` (`Message.kt`) | id 15 | ok |
| `DictionaryAppBarNavigationEffect.kt` | id 16 | ok |
| `DictionaryAppBarReducer.kt` | id 17 | ok |
| `DictionaryAppBarNavigationEffectHandler.kt` | id 18 | ok |

**Дополнения, не явно перечисленные в scope, но требуемые для замыкания DAG'а:**

- `Statistic.kt` (id 33) — НОВЫЙ файл; не упомянут в scope (там только `Vocabulary.kt`/`Quiz.kt`), но необходим для parity с Vocabulary/Quiz при per-dict-host wiring (Statistic — третий host `DictionaryAppBar` per F048). Альтернатива — оставить inline registration в `MainScreen.kt` + расширить inline сигнатуру `StatisticTabScreenDep`. Выбран явный extension-файл для parity.
- `MainScreen.kt` (id 34) — изменение от выноса Statistic в extension.
- Navigator interface'ы новых screen-модулей (`ComponentsManagerNavigator.kt` / `PerDictionaryComponentsNavigator.kt`, id 7/8) — упомянуты в scope как «новые `*NavigatorImpl` в `app/navigator/`», но interface часть в screen-module не явно зафиксирована. Convention — `DictionaryAppBarNavigator.kt` живёт в widget-модуле, `SettingsNavigator.kt` живёт в screen-модуле. Аналогично новые navigator interfaces размещаются в новых screen-модулях.
- UseCase interface placeholders в `deps/` (id 5, 6) — упомянуто в scope как `Mate wiring` + `UseCase методы`; placeholder interfaces в `deps/` нужны для `@Binds` в DI module.

### 2. Циклы / порядок зависимостей

- Tier 0 (gradle) — id 1 — лист (без deps); id 2/3/4 зависят от id 1; id 36 (`app/build.gradle.kts`) зависит от id 3/4 (screen-module gradle setup должен существовать до того как `app/` на них зависит).
- Tier 1-2 (UseCase / Navigator interfaces в screen-модулях) — зависят только от gradle setup своего модуля.
- Tier 3 (existing navigator interfaces) — листья (без `depends`); расширение public contract.
- Tier 4-5 (mate wiring) — внутренние зависимости (Msg/Effect → Reducer/Handler).
- Tier 6 (navigator impls в `app/`) — зависят от navigator interfaces (T3 для existing, T2 для новых).
- Tier 7 (DI modules) — зависят от UseCase interfaces (T1).
- Tier 8 (AppComponent) — зависит от DI modules (T7), потому что `AppModule.includes` ссылается на конкретные модули.
- Tier 9 (CompositionRoot + impl + MainRouter) — `CompositionRoot.kt` (id 28) и `CompositionRootImpl.kt` (id 29) зависят от `AppComponent` (T8) + navigator impls (T6); `MainRouter.kt` (id 37) — лист Tier 9, зависит от `AppComponent` (id 27) и `CompositionRootImpl.kt` (id 29).
- Tier 10 (navigation routes) — зависит от `CompositionRoot` interface (T9 для сигнатур `*ScreenDep`).
- Tier 11 (RoomModule) — independent leaf (только регистрация миграции).

**Циклов нет.** Каждый узел реализуется только когда зависимости готовы.

### 3. Verify existing files (Read/Glob)

Проверено (см. Bash `ls` + Read'ы выше):

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/settings.gradle.kts` — существует.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/main/src/main/java/me/apomazkin/main/{CompositionRoot.kt, MainScreen.kt, Settings.kt, Vocabulary.kt, Quiz.kt}` — существуют. Файла `Statistic.kt` **нет** — будет создан (id 33).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/uiDeps/CompositionRootImpl.kt` — существует.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/route/MainRouter.kt` — существует (id 37).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt` — существует.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/navigator/{SettingsNavigatorImpl.kt, DictionaryAppBarNavigatorImpl.kt}` — существуют. `ComponentsManagerNavigatorImpl.kt` / `PerDictionaryComponentsNavigatorImpl.kt` **отсутствуют** — будут созданы (id 21, 22).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/{settingstab/SettingsModule.kt, widget/DictionaryAppBarModule.kt, wordCard/WordCardModule.kt}` — существуют (аналоги). Папки `componentsmanager/` / `perdictionarycomponents/` **отсутствуют** — будут созданы (id 23-26).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/{SettingsNavigator.kt, SettingsNavigationEffect.kt, SettingsNavigationEffectHandler.kt, logic/Message.kt, logic/SettingsTabReducer.kt, deps/SettingsTabUseCase.kt}` — существуют.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/{DictionaryAppBarNavigator.kt, DictionaryAppBarNavigationEffect.kt, DictionaryAppBarNavigationEffectHandler.kt, mate/Message.kt, mate/DictionaryAppBarReducer.kt}` — существуют.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt` — существует, текущий `.addMigrations(Migration_011_to_012)` подтверждён на `RoomModule.kt:50`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/build.gradle.kts` — существует (id 36).

Все упомянутые existing файлы реально присутствуют в репозитории.

---

## log_messages

- iter 2: закрыл 3 approved minor — F051 (MainRouter.kt → id 37 Tier 9, depends [27, 29]), F052 (app/build.gradle.kts → id 36 в Tier 0), F053 (удалил self-contradictory абзац в Tier 9 описании host-методов)
- iter 3: закрыл F058 (interface/impl divergence) — в id 28 добавил «Изменение 3»: `SettingsTabScreenDep` получает параметр `onComponentsManagerClick: () -> Unit`; теперь interface (id 28) и impl (id 29) сигнатуры согласованы
- iter 4: закрыл F061 (approved minor) — id 36 (`app/build.gradle.kts`) `depends: [3, 4]` → `[2, 3, 4]` (симметрия: `app/` также ссылается на `:modules:widget:component_widgets`)
- iter 5: applied F062 (id 29 +19/20), F063 (id 32, 33 +31), F064 (id 21, 22, 23, 25, 27, 29 +36)
- DAG из 37 узлов: 12 [+] новых файлов, 15 [~] modify, 0 [-]; циклов нет
- per-dict экран — единая регистрация route в `Vocabulary.kt`, callsite navigate(...) из `Quiz.kt`/`Statistic.kt` через shared `NavHostController`

_model: claude-opus-4-7[1m]_
