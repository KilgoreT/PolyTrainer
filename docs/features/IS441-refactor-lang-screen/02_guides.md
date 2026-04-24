# IS441. Задача 2 — Релевантные гайды

## Релевантные гайды

### docs/guides/navigation.md
Релевантен: два новых route (DICTIONARY_SETUP, DICTIONARY_MANAGEMENT), переименование callback openAddDict → openDictionaryManagement по всей цепочке навигации.
Ключевое:
- Route через enum: `RootPoint(val route: String)`
- Навигационные функции — private extensions на NavController
- Зависимости через MainUiDeps
- `popBackStack()` для возврата, `popUpTo` для onboarding

### docs/guides/ui-patterns.md
Релевантен: добавление Scaffold с условным AppBar, новый виджет DictionaryAppBar.
Ключевое:
- AppBar — всегда отдельный виджет, никогда inline TopAppBar
- Nullable `onBackPress: (() -> Unit)?` → `onBackPress?.let { MyAppBar(it) }` для условного AppBar
- Три уровня виджетов: core/ui (везде), modules/widget (несколько экранов), screen/widget (один экран)
- DictionaryAppBar — экранный виджет (используется только в dictionary screen) → `screen/dictionary/widget/`
- Паттерн 1:1 с AboutAppBar: TopAppBar + IconBoxed(ic_back) + Text(title, LexemeStyle.H5)

### docs/guides/project-architecture.md
Релевантен: переименование модуля createdictionary → dictionary.
Ключевое:
- settings.gradle.kts — include модуля
- namespace в build.gradle.kts
- Зависимости через `project(":modules:screen:...")`

### docs/guides/code-style.md
Релевантен: переименование классов, файлов, callback'ов.
Ключевое:
- Файлы: Screen → `*Screen.kt`, ViewModel → `*ViewModel.kt`, Reducer → `*Reducer.kt`
- Git: ветка `IS441-...`, коммит `IS441. <описание>.`

## Нерелевантные

Остальные гайды не затронуты — задача не касается TEA-логики, тестов, data layer, темы.
