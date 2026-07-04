# Scope analysis: IS481 Quiz Component Picker

## Резюме

Под-задача IS481. В chat-меню добавить новый пункт-селектор «компонент квиза» — radio-выбор одного из `ComponentType` текущего словаря. Выбор **persistent per-dictionary**, восстанавливается при возврате в словарь. Выбор переопределяет filter в `QuizGameImpl.fetchData` перед `toQuizItem(...)`. Storage mechanism (prefs vs `quiz_configs`) — design sub-flow. Edge case: словарь с одним типом → пункт disabled + checked. Существующие domain типы (`ComponentTypeRef`, `ComponentType`) используются — без нового enum.

## Принятые ответы по open questions (финальные, согласованы с user)

1. **Persistent vs transient** — **PERSISTENT, per-dictionary**. Выбор сохраняется между сессиями. Логичная единица хранения — per dictionary (потому что `availableTypes` per-dictionary). При возврате в словарь восстанавливается прошлый выбор; при первом визите либо если сохранённый `selectedRef` недоступен в текущем `availableTypes` (тип удалён) → fallback на default (первый по `position`). **Storage mechanism — open для design sub-flow:** либо prefs (`Map<dictionaryId, ComponentTypeRef>` сериализованная), либо update `quiz_configs.component_refs` для текущего словаря (single-element list). См. trade-offs ниже.

2. **State location** — **`selectedRef: ComponentTypeRef`** через nested `quizComponent` поле в `ItemsState`. Использовать **существующий** domain тип `ComponentTypeRef` (sealed: `BuiltIn(key)` / `UserDefined(name)`), не вводить новый enum. Структура:
   - `quizComponent.availableTypes: List<ComponentType>` — компоненты текущего словаря (`getComponentTypes(dictionaryId)`).
   - `quizComponent.selectedRef: ComponentTypeRef` — текущий радио-выбор. Дефолт = первый из `availableTypes` (по `position`).

   **НЕ Map<X, Boolean> сейчас** (хотя multi-select в roadmap бэклогом). Map оправдан когда multi придёт; пока radio — `selectedRef` проще. Когда multi-select — рефактор `selectedRef → Map`, тогда же ввести invariant «при radio mode ровно один true». **TODO для будущего рефактора:** при переходе на Map потребуется invariant enforcement (reducer всегда set один true). Сейчас invariant не нужен — типом single ссылка выражена.

3. **Mid-session change** — **применить к следующей `loadData()` (фактически к следующей quiz session)**. Текущая архитектура `QuizGameImpl.loadData()` строит `quizList` один раз. Менять mode внутри активного списка не пытаемся. Возможно изменение на per-question re-fetch — потом, по факту UX.

4. **Dictionary switch — невалидный кейс.** В UI чата НЕТ способа сменить словарь mid-session (verified: `widget/` чата без dictionary switcher, словарь берётся из `getCurrentDictionaryId()` prefs, меняется только на dictionary tab вне чата). Закрывается через Q1 — при возврате в словарь после смены восстанавливается persistent selection per-dictionary.

(Вариант «only definition в словаре» — не возникает per AGG-1, translation built-in доступен всем словарям после IS481 migration.)

## Затронутые слои

- **Infrastructure** — нет — без DI graph / mate / logger / navigation / theme изменений. Новый MenuItem composable и Msg тривиально добавляются в существующий граф.
- **Business logic** — да — новые `Msg.SelectQuizComponent(ref: ComponentTypeRef)` + `Msg.QuizComponentTypesLoaded(types, restored)`; nested `quizComponent` поле в `ItemsState` (`availableTypes: List<ComponentType>` + `selectedRef: ComponentTypeRef?`); reducer + DatasourceEffectHandler; фильтрация `quizConfig.componentRefs` по `selectedRef` в `QuizGameImpl.fetchData` перед `toQuizItem`; новый contract-метод в `QuizChatUseCase` — `getAvailableTypes(dictionaryId): List<ComponentType>` (использует `CoreDbApi.LexemeApi.getComponentTypes`) + read/write persistent picker selection через PrefsProvider extension. Domain types существующие: `ComponentTypeRef` / `ComponentType` из `modules/domain/lexeme` — новые типы не вводятся.
- **UI** — да — новый `QuizComponentMenuItem.kt` (либо подменю с двумя пунктами) в `widget/appbar/menu/`, integration в `ActionsWidget.kt` между `MistakesMenuItem` и debug-block. Возможно потребуется string resources.
- **Data** — нет — DB schema / migration / persistent storage не трогаем. Existing `CoreDbApi.LexemeApi.getComponentTypes(dictionaryId)` уже есть, новые read-only вызовы не требуют изменений data слоя.

## Аспекты

- `public_contract_change` — `QuizChatUseCase` расширяется методами: получение `availableTypes` для словаря (через `getComponentTypes`), read/write picker selection (storage mechanism TBD). App-side impl обновляется.
- `persistent_per_dictionary` — выбор сохраняется между сессиями, ключ хранения — `dictionaryId`. **Storage decision — PREFS** (`Map<dictionaryId, ComponentTypeRef>` сериализованная через PrefsProvider). Это **зафиксировано** — не trogаем `quiz_configs.component_refs` (остаётся «whitelist для квиза» для будущего configurator из backlog). `data_touched: false`. Альтернативный quiz_configs route — backlog в случае необходимости.
- `runtime_config_override` — pre-fetched `QuizConfig.componentRefs` переопределяется на session level фильтром по `selectedRef`. Аналог runtime feature flag поверх statically-configured домен-объекта.
- `ui_availability_from_data` — enabled-state UI зависит от `availableTypes` (что в текущем словаре). Loading availability info на entry в chat screen.
- `existing_domain_types` — используются `ComponentTypeRef` (selection identity) + `ComponentType` (UI display) из `modules/domain/lexeme`. Новые enum/sealed не вводятся.

## Затронутые файлы

- `modules/screen/quiz/chat/.../widget/appbar/ActionsWidget.kt` — integration нового MenuItem между `MistakesMenuItem` и debug-block.
- `modules/screen/quiz/chat/.../widget/appbar/menu/QuizComponentMenuItem.kt` — **новый** composable: submenu/radio-group по `availableTypes` (динамический список, не хардкод). Display label: для `BuiltIn(TRANSLATION)` — string resource, для `UserDefined(name)` — `name` как есть. Если `availableTypes.size == 1` — пункт checked + disabled.
- `modules/screen/quiz/chat/.../logic/State.kt` — `ItemsState` + nested `quizComponent` поле: `availableTypes: List<ComponentType>` + `selectedRef: ComponentTypeRef?` (null до load).
- `modules/screen/quiz/chat/.../logic/Message.kt` — `Msg.SelectQuizComponent(ref: ComponentTypeRef)` + `Msg.QuizComponentTypesLoaded(types: List<ComponentType>, restoredSelectedRef: ComponentTypeRef?)`.
- `modules/screen/quiz/chat/.../logic/ChatReducer.kt` — handling новых Msg. На `QuizComponentTypesLoaded` — выставить `availableTypes` + восстановить `selectedRef` либо default. На `SelectQuizComponent` — обновить `selectedRef` + persist (write effect).
- `modules/screen/quiz/chat/.../logic/DatasourceEffectHandler.kt` — fetch `availableTypes` + restore persistent `selectedRef` на entry в chat.
- `modules/screen/quiz/chat/.../quiz/QuizGameImpl.kt` — `fetchData()` фильтрует `quizConfig.componentRefs` по `selectedRef` (single-element list) перед `toQuizItem(...)`. Передача `selectedRef` — design sub-flow (вероятно через DI / PrefsProvider).
- `modules/screen/quiz/chat/.../quiz/QuizGame.kt` — возможно расширение API (TBD design sub-flow).
- `modules/screen/quiz/chat/.../deps/QuizChatUseCase.kt` — `suspend fun getAvailableTypes(dictionaryId): List<ComponentType>` (через `CoreDbApi.LexemeApi.getComponentTypes`); + read/write persistent picker selection (signature TBD по storage decision).
- `app/.../QuizChatUseCaseImpl.kt` — impl новых методов.
- **`modules/datasource/prefs/`** — расширить PrefsProvider методами для read/write `Map<dictionaryId, ComponentTypeRef>` (serializable, JSON layer). Storage = PREFS (зафиксировано).
- `modules/screen/quiz/chat/src/main/res/values/strings.xml` — строки menu item title + label для built-in translation.

## Релевантные спеки и гайды

- `docs/features-spec/lexeme-domain.md` — `QuizConfig` / `ComponentTypeRef` / `ComponentType` (built-in vs user-defined). Понимание `systemKey == "translation"` (BuiltIn) vs `name == "Definition"` (UserDefined).
- `docs/features-spec/dagger-di-principles.md` — для добавления нового метода в `QuizChatUseCase` (контракт + impl в app модуле).
- Спека `quiz-chat.md` в `docs/features-spec/` **отсутствует** (нет в `features-spec/README.md`). Новая спека в рамках этой фичи не создаётся — `feature_has_ui_contract: true` означает локальный контракт-блок в business sub-flow, без записи в `features-spec/`.

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | нет | DI / mate / logger / navigation / theme не трогаются. |
| Business | да | Msg/State/Reducer/UseCase contract + impl + filtering logic в QuizGameImpl. Главный sub-flow фичи. |
| UI | да | Новый MenuItem composable + integration в ActionsWidget. Enabled/disabled state + radio-выбор. |
| Data | нет | DB schema / migration / persistent storage не меняются. Existing `getComponentTypes(dictionaryId)` используется read-only. |

## context

```yaml
infra_touched: false
business_touched: true
ui_touched: true
data_touched: false
needs_tests: true
needs_migration_tests: false
feature_has_ui_contract: true
spec_filename: null
```
