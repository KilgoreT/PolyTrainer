# Релевантные гайды

## Архитектура

### docs/guides/mate-framework.md
Релевантен: экран формы словаря построен на TEA (Mate). Полный цикл State -> Message -> Reducer -> Effect -> EffectHandler.
Ключевое:
- `ReducerResult = Pair<State, Set<Effect>>`. Хелперы: `.state()`, `.effects()`
- Редьюсер чистый -- без suspend, без сайд-эффектов
- Эффекты -- sealed interfaces расширяющие `Effect`. Две категории: `DatasourceEffect` и `UiEffect`
- `initEffects` запускает первую загрузку данных (загрузка флагов, загрузка словаря при редактировании)
- `Msg.Empty` -- no-op фоллбэк в каждом хендлере
- Файловая структура: `logic/` (State, Message, Reducer, EffectHandlers), `deps/` (UseCase), `ui/` (ViewModel, Screen)

### docs/guides/navigation.md
Релевантен: форма словаря доступна через два route (DICTIONARY_SETUP, DICTIONARY_CREATE) с разным поведением.
Ключевое:
- Один контекст = один route. Разное поведение -> разные route, не параметры
- `DICTIONARY_SETUP` -- onboarding, без AppBar, `onClose = openMainScreen`
- `DICTIONARY_CREATE` -- из dropdown, с AppBar, `onClose = popBackStack`
- Nullable `onBackPress`: null = без AppBar (onboarding), не null = с AppBar
- Зависимости передаются через `MainUiDeps`, не напрямую из appComponent

### docs/guides/data-layer.md
Релевантен: форма вызывает `addDictionary`, `updateDictionary`, `setCurrentDictionary`, `getAllCountryFlags` через UseCase.
Ключевое:
- UseCase интерфейс в feature модуле, реализация в app модуле
- Три слоя маппинга: DB -> API -> Domain
- Все DB-операции через `withContext(Dispatchers.IO)` в эффект-хендлерах
- `CoreDbApi` -- единый контракт с вложенными API по доменам

## Mate (TEA State Management)

### docs/guides/state-and-extensions.md
Релевантен: стейт формы содержит поля ввода, выбранный флаг, фильтр, список флагов, enabled-состояние кнопки.
Ключевое:
- Иммутабельные data class с `@Stable`
- Дефолтные значения для всех полей
- Extension-функции для всех мутаций -- редьюсер никогда не вызывает `.copy()` напрямую на сложном стейте
- **Явные поля для каждого UI-элемента.** `saveButtonEnabled: Boolean` -- вычисляется в редьюсере, не в composable
- Нейминг расширений: глагол в начале -- `update`, `select`, `clear`, `show`, `hide`
- Расширения на корневом стейте, chain-friendly

### docs/guides/messages.md
Релевантен: форма генерирует сообщения от UI (ввод текста, выбор флага, фильтрация) и от эффектов (загрузка данных, результат сохранения).
Ключевое:
- Единый sealed interface `Msg`
- Действия пользователя: `Show*`/`Hide*`, `*TextChange`, `Save*`, `Add*`
- Результаты эффектов: `*Loaded`
- Toggle: явные on/off или булев параметр
- `Msg.Empty` -- no-op, обязателен
- Exhaustive `when` в редьюсере

### docs/guides/reducer-patterns.md
Релевантен: редьюсер формы обрабатывает ввод, выбор флага, фильтрацию, сохранение.
Ключевое:
- Предпочтительный стиль: inline цепочки расширений в `when`
- Паттерн "только стейт": `state.updateName(value) to setOf()`
- Паттерн "стейт + эффекты": цепочка + `to setOf(Effect)`
- Паттерн "условный стейт": if/else внутри `when`-ветки
- Логирование: prevState, message, newState, effects через `LexemeLogger`
- Зависимости редьюсера: только чистые (ResourceManager, Logger). Без UseCase, без корутин

### docs/guides/effect-handlers.md
Релевантен: эффекты загрузки флагов, сохранения/обновления словаря, установки текущего словаря, навигация, реактивные подписки.
Ключевое:
- `DatasourceEffectHandler` -- safe cast `effect as? DatasourceEffect`, `null -> Msg.Empty`, `.let(consumer)`
- `withContext(Dispatchers.IO)` для всех DB-операций
- Эффект + хендлер в одном файле
- `internal` visibility
- Без сложной логики в хендлерах -- только выполнить и конвертировать в сообщение

#### NavigationEffectHandler (навигация через Effect)
Закрытие экрана по результату действия -- side-effect, НЕ флаг в State.
- Убирает навигационные флаги (`needClose`, `closeScreen`) из State
- Вся навигация: Msg → Reducer → NavigationEffect → handler вызывает `onClose()`
- UI не вызывает навигационные callback'и напрямую. `BackHandler { viewModel.accept(Msg.BackPressed) }`
- Reducer контролирует логику -- может решить не закрывать (показать диалог)

#### FlowHandler с runEffect (subscribe + эффекты в одном handler)
FlowHandler может одновременно подписываться на Flow И обрабатывать одноразовые эффекты:
- `subscribe()` -- подписан на `useCase.flagsFlow()`, при каждом emit → Message
- `runEffect()` -- принимает `Effect.FilterFlags(query)`, вызывает `useCase.updateFilter(query)`
- UseCase внутри: `updateFilter()` пишет в MutableStateFlow → Flow пересчитывает → emit → FlowHandler → Message
- Один FlowHandler = одна подписка (один `job`)
- `collectLatest` если новый emit должен отменять обработку предыдущего

#### Цепочка эффектов (гарантированный порядок)
Когда два эффекта зависят друг от друга -- НЕ запускать одновременно через initEffects (Set неупорядочен):
- initEffects: только первый эффект `setOf(Effect.LoadA)`
- LoadA → Handler → Msg.ALoaded → Reducer → `setOf(Effect.LoadB)` → второй запускается
- Пример: загрузка флагов → FlagsUpdated → Reducer порождает LoadDictionary (поиск флага по numericCode в списке)
- Без цепочки -- race condition: LoadB может завершиться раньше LoadA

### docs/guides/viewmodel-wiring.md
Релевантен: создание ViewModel для формы словаря с Factory паттерном.
Ключевое:
- ViewModel реализует `MateStateHolder<State, Msg>`
- Factory принимает зависимости (UseCase, editingDictionaryId)
- Screen composable создает ViewModel через `viewModel(factory = ...)`
- DI: UseCase биндится через Dagger Module, добавляется в AppComponent

## Тестирование

### docs/guides/testing-extensions.md
Релевантен: тесты extension-функций стейта формы (updateName, selectFlag, clearFilter и т.д.).
Ключевое:
- Файлы: `*ExtTest.kt` в папке `ext/`
- Каждый тест: основная функциональность + проверка иммутабельности ВСЕХ остальных полей
- Нумерованная документация кейсов в doc-комментарии класса
- Порядок: Boundary -> Standard -> Edge
- Имена: `should [ожидание] when [условие]`
- Формат описания: таблица "до -> после" + варианты входных данных + сигнатуры

### docs/guides/testing-reducers.md
Релевантен: тесты обработки сообщений формы (ввод текста, выбор флага, сохранение, загрузка данных).
Ключевое:
- `testReduce()` -- один message на начальный стейт
- `testScenario()` -- последовательность сообщений
- Хелперы: `assertNoEffects()`, `assertSingleEffect<T>()`, `assertEffects(set)`
- Один тест-класс на группу сообщений
- Всегда проверять и стейт И эффекты
- Проверки иммутабельности для не затронутых полей

## UI

### docs/guides/ui-patterns.md
Релевантен: экран формы с TextField, LazyVerticalGrid, кнопкой, AppBar.
Ключевое:
- Двухуровневый паттерн: публичная (DI + ViewModel) + внутренняя (stateless)
- `sendMessage: (Msg) -> Unit` -- единственный callback для UI-событий
- AppBar -- всегда отдельный виджет, nullable `onBackPress` для условного показа
- `LaunchedEffect` для закрытия экрана по флагу стейта
- Горизонтальный padding: 16.dp, spacing: 8.dp
- Фокус и клавиатура: `FocusRequester`, `imePadding()`

## Стиль и конвенции

### docs/guides/code-style.md
Релевантен: общие правила форматирования и именования.
Ключевое:
- Макс. длина строки: 120 символов
- Пакеты: `me.apomazkin.<module>.logic`, `.deps`, `.ui`, `.entity`
- Файлы: `State.kt`, `Message.kt`, `*Reducer.kt`, `*EffectHandler.kt`, `*Screen.kt`, `*Widget.kt`
- Коммиты: `IS441. <описание на английском>.`
- Extension chain -- каждый вызов на новой строке
- **Лаконичный нейминг:** короткое имя лучше длинного при равной ясности. `FlagsUpdated` > `FilteredFlagsLoaded`, `flags` > `filteredFlagsList`. При ревью проверять: можно ли переименовать короче?

_model: claude-opus-4-6[1m]_
