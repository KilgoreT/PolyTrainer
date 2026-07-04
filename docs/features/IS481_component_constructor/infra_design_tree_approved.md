# Approved findings — infra_design_tree.md, iteration 4

3 critical findings (F062, F063, F064). Все — пропущенные depends в DAG, ведущие к intermediate compile-broken между шагами.

## F062 [critical] — id 29 не зависит от id 19, id 20

id 29 (`CompositionRootImpl.kt`) расширяет ctor-вызовы:
- `SettingsNavigatorImpl(onOpenLangManagement, onOpenAboutApp, onOpenWebView, onOpenComponentsManager)` — 4-й параметр добавляется в id 19 (modify of `SettingsNavigatorImpl.kt` ctor).
- `DictionaryAppBarNavigatorImpl(onOpenDictionaryCreate, onOpenPerDictionaryComponents)` — 2-й параметр добавляется в id 20 (modify of `DictionaryAppBarNavigatorImpl.kt` ctor).

Без id 19 и id 20 ctor-аргументы в id 29 ссылаются на несуществующие параметры конструктора → compile fail.

**Что исправить:** в id 29 — `depends: [21, 22, 27, 28]` → `depends: [19, 20, 21, 22, 27, 28]`.

## F063 [critical] — id 32 и id 33 не зависят от id 31

id 32 (`Quiz.kt`) и id 33 (`Statistic.kt`) в теле вызывают `navController.goToPerDictionaryComponents(dictId)`. Эта extension-функция объявлена `internal` в id 31 (`Vocabulary.kt`). Без неё id 32 и id 33 не компилируются.

**Что исправить:**
- id 32: `depends: [28]` → `depends: [28, 31]`.
- id 33: `depends: [28]` → `depends: [28, 31]`.

## F064 [critical] — узлы `app/` не зависят от id 36 (gradle wiring)

id 36 (`app/build.gradle.kts`) добавляет `implementation(project(":modules:screen:components_manager"))`, `implementation(project(":modules:screen:per_dictionary_components"))`, `implementation(project(":modules:widget:component_widgets"))`. Без этого узлы `app/`, импортирующие типы из этих модулей, не имеют их на compile-classpath.

Затронуты:
- id 21 (`ComponentsManagerNavigatorImpl.kt`) — импортирует `ComponentsManagerNavigator`.
- id 22 (`PerDictionaryComponentsNavigatorImpl.kt`) — импортирует `PerDictionaryComponentsNavigator`.
- id 23 (`ComponentsManagerScreenModule.kt`) — импортирует `ComponentsManagerUseCase`.
- id 25 (`PerDictionaryComponentsScreenModule.kt`) — импортирует `PerDictionaryComponentsUseCase`.
- id 27 (`AppComponent.kt`) — импортирует ViewModel factories.
- id 29 (`CompositionRootImpl.kt`) — импортирует ViewModels + Screen composables.

Семантика DAG = порядок шагов: если id 36 пойдёт после узла-consumer'а — intermediate compile broken между шагами. id 36 должен предшествовать.

**Что исправить:** добавить id 36 в `depends` узлов 21, 22, 23, 25, 27, 29.

Опционально (для явности) — id 24 и 26 (DI Components, зависят от id 23/25), но транзитивная зависимость через id 23/25 уже даёт корректный порядок.
