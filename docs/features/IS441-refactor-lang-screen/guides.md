# IS441. Релевантные гайды

---

## Релевантные гайды

### docs/guides/project-architecture.md
Релевантен: задача затрагивает все слои — от Room до UI. Нужно понимать граф зависимостей.
Ключевое:
- Feature модули зависят от core, не наоборот
- `core-db-api` — контракты, `core-db-impl` — реализация, `app/di/module/` — UseCase реализации
- DI: единый AppComponent, UseCase интерфейс в feature, реализация в app

### docs/guides/data-layer.md
Релевантен: переименование затрагивает Room entity, DAO, API контракты, маппинг entity, DataStore.
Ключевое:
- Три слоя маппинга: DB → API → Domain, каждый через extension-функцию
- UseCase интерфейс в feature-модуле, реализация в app-модуле
- `CoreDbApi.LangApi` → станет `CoreDbApi.DictionaryApi`
- FK с CASCADE delete на всех связях — при переименовании таблицы FK нужно пересоздать

### docs/guides/testing-migrations.md
Релевантен: задача требует Room-миграции #11.
Ключевое:
- `BaseMigration` — три фазы: onCreate → afterCreateCheck → afterMigrationCheck
- На каждую версию таблицы — свой `Schemable` object (WriteQuizV11, WordV11, DictionaryV11)
- `Schemable.data()` — тестовые данные, `getFromDatabase()` — чтение через cursor, `checkMatcher` — сравнение полей
- Schemable для старой версии использует актуальный entity class — при переименовании полей сломаются старые Schemable (известная проблема, см. backlog)

### docs/guides/state-and-extensions.md
Релевантен: DictUiEntity и PresetLangUi — стейт-модели, которые изменятся.
Ключевое:
- Явные поля для каждого UI-элемента, не вычислять в composable
- Extension-функции в State.kt, не в Reducer.kt
- `@Immutable` / `@Stable` аннотации обязательны

### docs/guides/effect-handlers.md
Релевантен: DatasourceEffectHandler в createdictionary, dictionaryappbar, dictionaryTab — все используют `langApi`.
Ключевое:
- `null -> Msg.Empty` — фоллбэк в каждом хендлере
- `withContext(Dispatchers.IO)` для data-операций
- FlowHandler для подписок (dictionaryappbar наблюдает за текущим словарём)

### docs/guides/messages.md
Релевантен: переименование затронет Message в createdictionary (SaveLang → SaveDictionary и т.д.).
Ключевое:
- Sealed interface `Msg`, exhaustive when
- `data object` для действий без данных, `data class` для действий с параметрами

### docs/guides/navigation.md
Релевантен: route `CREATE_DICTIONARY` — точка входа, используется из трёх мест.
Ключевое:
- RootRouter: splash → CREATE_DICTIONARY → MAIN_ROUTER
- Settings: onLangManagementClick → openAddDict → CREATE_DICTIONARY
- DictionaryAppBar → DictDropDownWidget → openAddDict
- Зависимости через MainUiDeps, не напрямую из appComponent

### docs/guides/code-style.md
Релевантен: массовое переименование ~68 файлов.
Ключевое:
- Максимальная длина строки: 120 символов
- Код и комментарии — английский
- Git: ветка `IS441-...`, коммит `IS441. <описание>.`

### docs/guides/viewmodel-wiring.md
Релевантен: CreateDictionaryViewModel и DictionaryAppBarViewModel используют langApi.
Ключевое:
- Mate инициализация: initState, initEffects, reducer, effectHandlerSet
- Factory pattern для ViewModelProvider

---

## Нерелевантные гайды

| Гайд | Почему не нужен |
|------|----------------|
| mate-framework.md | Фреймворк не меняется, только данные внутри него |
| reducer-patterns.md | Редьюсеры не меняют логику, только имена параметров |
| testing-extensions.md | Extension-тесты не затронуты переименованием |
| testing-reducers.md | Reducer-тесты обновятся, но паттерн тот же |
| ui-patterns.md | UI-слой минимально затронут |
| theme-and-resources.md | Тема и ресурсы не меняются |
| tools-utils.md | Утилиты не затронуты |
