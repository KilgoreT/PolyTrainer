# IS441. Задача 2 — Design Tree

**Input:**
- `02_tasks.md` — задача
- `02_solutions.md` — решение (два route, nullable onBackPress, переименования)
- `02_guides.md` — navigation, ui-patterns, project-architecture, code-style

---

## Граф

```yaml
# === Модуль: переименование createdictionary → dictionary ===

- id: 0
  file: settings.gradle.kts
  action: "~"
  depends: []

- id: 1
  file: modules/screen/createdictionary/ → modules/screen/dictionary/ (git mv)
  action: "~"
  depends: [0]

- id: 2
  file: modules/screen/dictionary/build.gradle.kts
  action: "~"
  depends: [1]

# === Файлы внутри модуля: обновление package declaration ===
# git mv переносит файлы, но package name остаётся старым.
# Каждый .kt файл нужно обновить: package me.apomazkin.createdictionary → me.apomazkin.dictionary

- id: 3
  file: modules/screen/dictionary/.../DictionaryScreen.kt (бывш. CreateDictionaryScreen.kt)
  action: "~"
  depends: [1, 9]

- id: 4
  file: modules/screen/dictionary/.../DictionaryViewModel.kt (бывш. CreateDictionaryViewModel.kt)
  action: "~"
  depends: [1]

- id: 5
  file: modules/screen/dictionary/.../logic/DictionaryReducer.kt (бывш. CreateDictionaryReducer.kt)
  action: "~"
  depends: [1]

- id: 6
  file: modules/screen/dictionary/.../logic/State.kt
  action: "~"
  depends: [1]

- id: 7
  file: modules/screen/dictionary/.../logic/Message.kt
  action: "~"
  depends: [1]

- id: 8
  file: modules/screen/dictionary/.../logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [1]

- id: 9
  file: modules/screen/dictionary/.../widget/DictionaryAppBar.kt
  action: "+"
  depends: [1]

- id: 10
  file: modules/screen/dictionary/.../DictionaryData.kt
  action: "~"
  depends: [1]

- id: 11
  file: modules/screen/dictionary/.../entity/PresetDictionaryUi.kt
  action: "~"
  depends: [1]

- id: 12
  file: modules/screen/dictionary/.../entity/DictionaryUpdateUi.kt
  action: "~"
  depends: [1]

- id: 13
  file: modules/screen/dictionary/.../widget/DictionaryPickerWidget.kt
  action: "~"
  depends: [1]

- id: 14
  file: modules/screen/dictionary/.../widget/DictionaryListWidget.kt
  action: "~"
  depends: [1]

- id: 15
  file: modules/screen/dictionary/.../widget/DictionaryItemWidget.kt
  action: "~"
  depends: [1]

- id: 16
  file: modules/screen/dictionary/.../widget/LoadingWidget.kt
  action: "~"
  depends: [1]

- id: 17
  file: modules/screen/dictionary/.../widget/ListHeaderWidget.kt
  action: "~"
  depends: [1]

# === app/build.gradle.kts — зависимость на модуль ===

- id: 32
  file: app/build.gradle.kts
  action: "~"
  depends: [0]

# === Навигация ===

- id: 18
  file: app/.../route/RootRouter.kt
  action: "~"
  depends: [1, 3]

- id: 19
  file: app/.../route/MainRouter.kt
  action: "~"
  depends: [18]

# === openAddDict → openDictionaryManagement ===

- id: 20
  file: modules/screen/main/.../MainUiDeps.kt
  action: "~"
  depends: []

- id: 21
  file: modules/screen/main/.../MainScreen.kt
  action: "~"
  depends: [19, 20]

- id: 22
  file: modules/screen/main/.../Vocabulary.kt
  action: "~"
  depends: [20]

- id: 23
  file: modules/screen/main/.../Quiz.kt
  action: "~"
  depends: [20]

- id: 24
  file: modules/screen/main/.../Settings.kt
  action: "~"
  depends: [20]

# === DI ===

- id: 25
  file: app/.../di/module/createDictionary/ → di/module/dictionary/ (git mv)
  action: "~"
  depends: [1]

- id: 26
  file: app/.../di/module/dictionary/DictionaryUseCaseImpl.kt (бывш. CreateDictionaryUseCaseImpl.kt)
  action: "~"
  depends: [25]

- id: 27
  file: app/.../di/module/dictionary/DictionaryModule.kt (бывш. CreateDictionaryModule.kt)
  action: "~"
  depends: [25]

- id: 28
  file: app/.../di/AppComponent.kt
  action: "~"
  depends: [27]

- id: 29
  file: app/.../uiDeps/MainUiDepsProvider.kt
  action: "~"
  depends: [20]

# === Виджеты с openAddDict ===

- id: 30
  file: modules/widget/dictionarypicker/.../DictDropDownWidget.kt
  action: "~"
  depends: [20]

- id: 31
  file: modules/widget/dictionaryappbar/.../DictionaryAppBar.kt
  action: "~"
  depends: [20]
```

---

## Детали

### #0 settings.gradle.kts [~]

`include(":modules:screen:createdictionary")` → `include(":modules:screen:dictionary")`

### #1 git mv модуля [~]

`git mv modules/screen/createdictionary modules/screen/dictionary`

### #2 build.gradle.kts [~]

`namespace = "me.apomazkin.createdictionary"` → `namespace = "me.apomazkin.dictionary"`

### #3 DictionaryScreen.kt [~]

- Переименование файла + класса: `CreateDictionaryScreen` → `DictionaryScreen`
- Package: `me.apomazkin.createdictionary` → `me.apomazkin.dictionary`
- Добавить `onBackPress: (() -> Unit)? = null`
- Заменить `Box` на `Scaffold` с условным topBar: `onBackPress?.let { DictionaryAppBar(it) }`

### #4 DictionaryViewModel.kt [~]

Переименование файла + класса + UseCase: `CreateDictionary*` → `Dictionary*`. Package update.

### #5 DictionaryReducer.kt [~]

Переименование файла + класса. Package update.

### #6-8 State, Message, DatasourceEffectHandler [~]

Package update. Содержимое не меняется.

### #9 DictionaryAppBar.kt [+]

Новый виджет. Паттерн `AboutAppBar`: TopAppBar + IconBoxed(ic_back) + Text(title).

### #10-17 Остальные файлы модуля [~]

Package update: `me.apomazkin.createdictionary` → `me.apomazkin.dictionary`. Содержимое не меняется.

### #18 RootRouter.kt [~]

- Удалить `CREATE_DICTIONARY` route
- Добавить `DICTIONARY_SETUP` и `DICTIONARY_MANAGEMENT`
- `openCreateDictionaryScreen()` → `openDictionarySetup()`
- `openAddDict` → `openDictionaryManagement`
- Import: `me.apomazkin.createdictionary.CreateDictionaryScreen` → `me.apomazkin.dictionary.DictionaryScreen`

### #19 MainRouter.kt [~]

`openAddDict` → `openDictionaryManagement`

### #20-24, 29-31 openAddDict → openDictionaryManagement [~]

Переименование параметра по всей цепочке.

### #25-27 DI [~]

git mv директории, переименование классов, package update.

### #28 AppComponent.kt [~]

`CreateDictionaryModule` → `DictionaryModule`, `getCreateDictionaryUseCase()` → `getDictionaryUseCase()`

---

## Статистика

| Действие | Кол-во |
|----------|--------|
| [+] создание | 1 (DictionaryAppBar) |
| [~] изменение | 32 |
| **Всего** | **33** |
