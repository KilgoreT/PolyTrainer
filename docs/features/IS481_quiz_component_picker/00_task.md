## Задача

Под-задача внутри IS481. Добавить в chat-меню (`ActionsWidget`, рядом с `EarliestReviewedMenuItem` / `MistakesMenuItem` / `DebugMenuItem`) новый пункт выбора **компонента квиза** — из доступных в текущем словаре `ComponentType` (built-in либо user-defined). Квиз отдаёт только выбранный компонент.

**Зачем.** Сейчас `QuizGameImpl.fetchData` использует `QuizConfig.componentRefs` (например `[BuiltIn(translation), UserDefined("Definition")]`) и `toQuizItem` собирает все доступные компоненты лексемы. Пользователь хочет в session-time выбирать какой именно компонент тренирует — переводы, определения, либо любой будущий тип.

**Принципиально.** Picker **обобщённый**: работает на любой набор `ComponentType` из `getComponentTypes(dictionaryId)`. Не хардкодит конкретные имена. Сейчас набор типов в фиче маленький (translation built-in + опционально Definition user-defined per AGG-1 + IS481 migration), но архитектура должна расширяться без переделок:
- (a) Codebase добавляет новые built-in типы (например, transcription, pronunciation, IPA, …) — picker автоматически их подхватывает.
- (b) User-defined типы создаются пользователем (configurator UI из backlog «Quiz config UX») — picker отображает их в меню по факту наличия в словаре.

## Контекст

### UI / поведение

- Новый MenuItem в dropdown чата (между `MistakesMenuItem` и debug-block).
- Раскрывается в подменю / radio-группу. **Выбор взаимоисключающий** (radio): один компонент = текущая тренировочная категория.
- UI динамически рендерит `availableTypes` (список `ComponentType` текущего словаря). Без хардкода под имена/ключи.
- **Edge case — один тип в словаре:** пункт checked + disabled (нечего альтернативного выбирать).
- **Edge case — несколько типов:** все enabled, один выбран. Default = первый по `position` (translation built-in обычно первый).
- Display label:
  - built-in (`systemKey != null`) — локализованная строка ресурса по `systemKey`.
  - user-defined — `ComponentType.name` как есть.

### Quiz logic

- Quiz session использует **только выбранный** компонент:
  - `QuizGameImpl.fetchData` фильтрует `quizConfig.componentRefs` до single-element list `[selectedRef]` перед передачей в `toQuizItem(...)`.
  - `toQuizItem` уже умеет graceful skip (lexema без выбранного компонента — null).
- Изменение выбора применяется к следующему `loadData()` (фактически — к следующей quiz session). Текущий `quizList` доигрывает с прежним mode (lifecycle не ломаем).

### Persistence

- Выбор **persistent per-dictionary**. Сохраняется между визитами в чат. Восстанавливается при возврате в словарь.
- Storage mechanism — design sub-flow. Кандидаты:
  - (a) Prefs (`Map<dictionaryId, ComponentTypeRef>` сериализованная). Default — не ломает семантику `quiz_configs.component_refs`.
  - (b) Update `quiz_configs.component_refs` для текущего словаря (single-element list). Меняет семантику config'а.
- Если сохранённый `selectedRef` недоступен в текущем `availableTypes` (тип удалён) → fallback на default.

### State

- В `ItemsState` поле `quizComponent` (nested):
  - `availableTypes: List<ComponentType>` — компоненты словаря из `getComponentTypes(dictionaryId)`.
  - `selectedRef: ComponentTypeRef?` — radio-выбор (null до load).
- Используются существующие domain типы из `modules/domain/lexeme`: `ComponentTypeRef` (sealed BuiltIn/UserDefined) для identity selection, `ComponentType` (data class) для UI display. **Новые enum/sealed не вводятся.**
- TODO future: при переходе на multi-select — рефактор `selectedRef → Map<ComponentTypeRef, Boolean>` с invariant'ом «при radio mode ровно один true».

### Что меняется (layers)

| Слой | Что |
|---|---|
| UI (`widget/appbar/`) | Новый `QuizComponentMenuItem.kt` (динамический radio по `availableTypes`). Integration в `ActionsWidget.kt`. |
| Mate (`logic/`) | `ItemsState.quizComponent`, `Msg.SelectQuizComponent(ref)`, `Msg.QuizComponentTypesLoaded(types, restored)`, reducer + datasource effect handler. |
| Quiz logic | `QuizGameImpl.fetchData` фильтрует componentRefs по `selectedRef`. |
| UseCase | `QuizChatUseCase` расширяется методами `getAvailableTypes(dictionaryId)` + read/write persistent selection. |
| Storage | Prefs (default) либо `quiz_configs` mapper (если квиз-конфиг подход). |
| Resources | `strings.xml` — заголовок MenuItem + built-in компонентов локализация. |

### Что НЕ делать

- Не менять DB schema / migration.
- Не трогать `Earliest` / `FrequentMistakes` / `Debug` toggle'ы.
- Не хардкодить конкретные типы (translation/Definition) в логике picker'а. Только обобщённая работа через `ComponentType` / `ComponentTypeRef`.

### Acceptance

- В chat menu новый пункт «Компонент квиза» (или подходящее имя).
- Подменю динамически показывает `availableTypes` текущего словаря с правильным enabled-state.
- Если один тип — пункт checked + disabled.
- Квиз отдаёт только выбранный компонент.
- Выбор persistent per-dictionary, восстанавливается при возврате.

_model: claude-opus-4-7[1m]_
