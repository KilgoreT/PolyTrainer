_model: claude-opus-4-7-1m

# Business walkthrough — IS481 Quiz Component Picker

Discovery в реальном коде. Карта существующих pattern'ов и точек интеграции для нового picker'а (radio-выбор компонента квиза + persistent per-dictionary).

## State.kt — структура `ItemsState`

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/State.kt`

```kotlin
data class ItemsState(
    val earliest: Earliest = Earliest(),
    val frequentMistakes: FrequentMistakes = FrequentMistakes(),
    val debug: Debug = Debug(),
) {
    data class Earliest(val isOn: Boolean = false)
    data class FrequentMistakes(val isOn: Boolean = false)
    data class Debug(val isOn: Boolean = false)
}
```

Pattern: nested `data class` для каждой toggle-фичи, поле `isOn: Boolean`. Дефолт через no-arg конструктор. Update через extension `updateMenu(...)` который `copy()` каждый nested по diff. — **Аналог найден.** Для picker'а — nested `QuizComponent(availableTypes: List<ComponentType> = emptyList(), selectedRef: ComponentTypeRef? = null)` ложится в pattern.

## Message.kt — `Msg.X` pattern для меню

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/Message.kt`

Existing menu-related Msg:
- `Msg.EarliestOn` / `Msg.EarliestOff` — `data object`, парные «вкл/выкл».
- `Msg.FrequentMistakesOn` / `Msg.FrequentMistakesOff`
- `Msg.DebugOn` / `Msg.DebugOff`
- `Msg.UpdateMenu(isEarliestOn, isFrequentMistakesOn, isDebugOn)` — **bulk-update**, прилетает от `AppBarFlowHandler` (combine трёх prefs flows).

Pattern: пара Msg-ов «On/Off» → effect (write to prefs) → flow notice → `UpdateMenu` → reducer обновляет State. **State обновляется НЕ напрямую из click'а, а через prefs flow → flow handler.**

Для picker'а — `Msg.SelectQuizComponent(ref: ComponentTypeRef)` (replace «On/Off», т.к. radio) + `Msg.QuizComponentTypesLoaded(types, restoredSelectedRef)`. **Аналог найден** (с modification: radio вместо boolean toggle).

## ChatReducer — handling

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/ChatReducer.kt:40-51`

```kotlin
is Msg.EarliestOn -> state to setOf(DatasourceEffect.EarliestOn)
is Msg.EarliestOff -> state to setOf(DatasourceEffect.EarliestOff)
// ...
is Msg.UpdateMenu -> state.updateMenu(...) to setOf()
```

Pattern: click Msg → State **не меняется**, только effect (write prefs). `Msg.UpdateMenu` (bulk-load от flow) — единственный путь модификации `ItemsState`. **Аналог найден.**

## DatasourceEffectHandler — effects + write to prefs

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/DatasourceEffectHandler.kt:49-72`

```kotlin
is DatasourceEffect.EarliestOn -> withContext(Dispatchers.IO) {
    prefsProvider.setBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN, true)
    Msg.Empty
}
```

Pattern: 6 effects (3 пары) — каждый делает `setBoolean(key, value)` + emit `Msg.Empty`. **Fetch on entry — нет.** State обновляется через flow handler (см. ниже). Для picker'а аналогично: `DatasourceEffect.PersistQuizComponent(ref)` → write → `Msg.Empty`. Для load `availableTypes` нужен **новый паттерн** — единоразовая загрузка на entry (см. ниже AppBarFlowHandler нужно расширить либо отдельный effect).

## AppBarFlowHandler — bulk-load из prefs flows

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/logic/AppBarFlowHandler.kt`

```kotlin
combine(
    prefsProvider.getBooleanFlow(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN),
    prefsProvider.getBooleanFlow(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN),
    prefsProvider.getBooleanFlow(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN),
) { earliest, frequentMistakes, debug ->
    Msg.UpdateMenu(...)
}.collectLatest { msg -> send(msg) }
```

Pattern: реактивная подписка на prefs → emit `UpdateMenu`. **Это ключевой паттерн для picker'а.** Однако: `availableTypes` берётся не из prefs, а из DB (per-dictionary). Значит нужен:
- одноразовый fetch `availableTypes` (через DatasourceEffect, на entry / при смене dictionaryId);
- prefs-flow для `selectedRef` (`Map<dictionaryId, ComponentTypeRef>` либо stable key) — **новый storage layer**, см. ниже.

**Подходящего combine flow для Map / JSON — нет, PrefsProvider не поддерживает.**

## QuizGameImpl — fetchData / toQuizItem / componentRefs

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt:176-228`

`fetchData()` (вызывается из `loadData()` на каждой quiz session):
1. `getCurrentDictionaryId()` (либо null → empty list).
2. `getQuizConfig(dictionaryId, "write")` (либо null → empty list).
3. `getRandomWriteQuizList(...)` → список `WriteQuiz`.
4. `.mapNotNull { it.toQuizItem(componentRefs = quizConfig.componentRefs, ...) }`.

`toQuizItem` (line 447-520) резолвит **первый matched** `componentRef` через `firstNotNullOfOrNull`. Если есть `selectedRef` → надо передать `componentRefs = listOf(selectedRef)` (single-element). Логика resolution не меняется.

**Mode передачи `selectedRef`:** не через DI / не через PrefsProvider напрямую. Чище — через `QuizChatUseCase.getQuizConfig(...)` который сам подставит selection, **либо** через расширение `QuizGameImpl` поле, **либо** добавить параметр в `loadData(selectedRef)`. Design sub-flow обсудит. Точка изменения — одна строка fetchData (line 223): `componentRefs = listOf(selectedRef) ?: quizConfig.componentRefs`.

`loadData()` lifecycle: `clearData()` → `fetchData()` → `addQuizData()`. На каждый `LoadQuiz` (start + continue) пересборка списка. **Per-question re-fetch отсутствует** — mid-session change не применяется (по scope).

## QuizChatUseCase — interface

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt`

Существующие методы:
- `getCurrentDictionaryId(): Long?` — current dictionary из prefs + fallback to first.
- `updateWriteQuiz(entity): Int`
- `getRandomWriteQuizList(limit, maxGrade, dictionaryId): List<WriteQuiz>`
- `getQuizConfig(dictionaryId, quizMode): QuizConfig?` — pre-fetched config на quiz session.

Implementation (`app/.../QuizChatUseCaseImpl.kt`) — собирает данные через `CoreDbApi.DictionaryApi`, `QuizApi`, `LexemeApi`, `PrefsProvider`.

`CoreDbApi.LexemeApi.getComponentTypes(dictionaryId): List<ComponentTypeApiEntity>` **уже существует** (core-db-api/CoreDbApi.kt:140). Маппер на domain `ComponentType` тоже есть (используется в WordCardUseCase).

Для picker'а добавить:
- `suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType>` — простой wrapper на `lexemeApi.getComponentTypes` + map to domain.
- read/write picker selection — **подписать на тип хранилища.**

## PrefsProvider — limitations + что нужно добавить

Файл: `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt`

API сегодня: `getInt/setInt`, `getLong/setLong`, `getBoolean/setBoolean` (+ Flow версии). **String и сложные типы — НЕТ.** `PrefKey` enum — 4 hard-coded ключа.

**Проверено grep'ом:** `stringPreferencesKey` / `kotlinx.serialization` / `Json.encodeToString` в проекте **отсутствуют**. JSON-сериализации сложных значений нет нигде. Зависимости (datastoreLibs.preferences) минимальные.

**Аналог для persistent `Map<dictionaryId, ComponentTypeRef>` — НЕ найден.** Storage decision требует одного из:
1. **Per-dictionary `Long` PrefKey + enum-кодирование** — добавить `getString/setString` + строковый ключ типа `"quiz_picker_dict_${id}"`. Серializация `ComponentTypeRef` через простой `"builtin:translation"` / `"user:Definition"`.
2. **Single JSON-blob под одним ключом** — добавить kotlinx-serialization + `Map<Long, String>` (по dictId → encoded ref).
3. **Через `quiz_configs.component_refs` в БД** — записывать selected ref как single-element list. Меняет семантику `quizConfig.componentRefs` («whitelist» → «текущий выбор»). По scope зафиксировано «не трогаем quiz_configs», поэтому отбрасывается.

Аналога точно под Map persistence — нет. **Минимальный путь:** добавить в PrefsProvider `getString/setString` + `getStringFlow`, новый dynamic `PrefKey` (поскольку enum hard-coded — придётся либо превратить в `data class` со value, либо добавить расширение через сырое имя ключа).

**Edge case:** dynamic ключи (per-dictionary) ломают `PrefKey` enum. Нужно либо вынести в `data class PrefKey(val value: String)`, либо добавить overload методов `getString(rawKey: String)`. Это инфра-вопрос; должно быть отмечено в design sub-flow.

## Domain types — `modules/domain/lexeme/`

Все типы уже есть:
- `ComponentTypeRef` (sealed, value class): `BuiltIn(key: BuiltInComponent)` / `UserDefined(name: String)`. **Stable identity** для serialization (см. сериализационная стратегия выше).
- `ComponentType` (data class): `id`, `systemKey: BuiltInComponent?`, `dictionaryId: Long?`, `name: String?`, `template`, `position`, `removeDate`. Используется для UI display.
- `BuiltInComponent` enum: только `TRANSLATION("translation")` после AGG-1 миграции. `fromKey` companion для парсинга.
- `QuizConfig(dictionaryId, quizMode, componentRefs: List<ComponentTypeRef>)`.

**Новые domain типы — не нужны.** `ComponentTypeRef` готов как persistence-identity, `ComponentType` готов как UI-display.

## UI — MenuItem / radio

Файл: `modules/widget/iconDropDowned/src/main/java/me/apomazkin/icondropdowned/MenuItem.kt`

Существуют variants: `MenuItemWithIcon`, `MenuItemTextOnly`, `MenuItemWithCheckbox`. **Radio-вариант отсутствует.** `RadioButton` Composable нигде в проекте не используется (grep).

Для picker'а варианты:
1. Добавить `MenuItemWithRadio` (новый sealed-вариант + `MenuItem.withRadio(isSelected, title, onClick)`) — расширение существующего dropdown widget'а.
2. Submenu approach (nested `DropdownMenu`) — material3 не имеет нативного nested submenu, придётся писать через дополнительный `expanded` state. Сложнее.
3. Просто несколько `MenuItem.text(...)` с tick-mark icon при selected — простой и без новых widget'ов, но менее идиоматично.

**Аналог radio в dropdown — не найден.** Минимальный путь — вариант 1 (новый `MenuItemWithRadio` в общем виджете).

## ActionsWidget — integration point

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/widget/appbar/ActionsWidget.kt`

Сейчас порядок: `EarliestReviewedMenuItem` → `MistakesMenuItem` → (debug-only) divider + `DebugMenuItem`. Новый `QuizComponentMenuItem` ложится после `MistakesMenuItem` и до debug-блока. Если submenu — composable принимает `availableTypes`, `selectedRef`, `onSelect`.

## Что нужно решить в design sub-flow

1. **Storage mechanism для persistent map** (фиксировано «PREFS» по scope, но subform):
   - per-dictionary String preferences key + строковый encoding `ComponentTypeRef`;
   - либо single JSON-blob + kotlinx-serialization (новая dependency).
2. **`PrefKey` enum vs dynamic key.** Если per-dictionary keys — enum не подходит, нужен переход к `data class` либо raw-string API.
3. **Передача `selectedRef` в `QuizGameImpl.fetchData`.** Чище — через UseCase (`getQuizConfig` сам подставит) либо отдельный метод `getPickerSelection(dictionaryId)`. Передача через DI `PrefsProvider` напрямую в `QuizGameImpl` — нарушает existing pattern (UseCase — единая точка для chat-данных).
4. **Radio UI:** новый `MenuItemWithRadio` в общем widget'е vs локальный composable в quiz/chat.
5. **Restore-on-entry flow:** одноразовый `DatasourceEffect.LoadPickerTypes` (fires once on chat init) vs расширение `AppBarFlowHandler` с дополнительным flow. Текущая архитектура — flow handler работает только с prefs-Boolean; loading domain data (`getComponentTypes`) не делает.

## Вердикт

**Аналог найден частично:**

- `ItemsState` + nested toggle state — pattern **найден**, picker ложится 1-в-1 (с modification radio→single value).
- Msg pair + reducer + DatasourceEffect + prefs-write — pattern **найден**, для radio вместо boolean — тривиальное расширение.
- `AppBarFlowHandler` combine prefs flows — pattern **найден**, для String/Map prefs — **частичный** (нужен `getStringFlow` API в PrefsProvider).
- Fetch domain data на entry (для `availableTypes`) — **не найден прямой аналог**: текущий flow handler работает только с prefs, fetch on entry в chat сейчас идёт через `DatasourceEffect.PrepareToStart` который только меняет state без data load. Нужен новый одноразовый effect.
- `toQuizItem` интеграция — pattern **найден**, точка изменения одна (`componentRefs` argument в fetchData).
- `CoreDbApi.LexemeApi.getComponentTypes` — **уже существует**, новых data-методов не нужно.
- Domain types (`ComponentTypeRef`, `ComponentType`) — **готовы**, новых не нужно.

**Аналог НЕ найден:**

- **Persistent `Map<dictionaryId, X>` в PrefsProvider** — нет ни одного примера. JSON-серializация / `stringPreferencesKey` отсутствуют по всему репо. `PrefKey` enum статичен, dynamic keys не предусмотрены. **Главный gap.** Design sub-flow обязан выбрать stratagy (per-dict String key vs JSON-blob) + extend PrefsProvider.
- **Radio MenuItem в dropdown** — нет ни одного `RadioButton` / radio-pattern в существующих widget'ах. Новый `MenuItemWithRadio` либо custom composable.
- **Domain-data fetch on screen entry в этом chat-экране** — текущий flow «load в quiz session start» через `LoadQuiz` effect. Для picker'а данные нужны **до** start (показать меню до старта). Нужен новый одноразовый `DatasourceEffect.LoadAvailableTypes` (fires из init effects либо при первом open menu).

**Обоснование частичности:** core EventBus/MVI каркас (Mate + reducer + effect handler + flow handler) — полностью переиспользуется без модификаций. Главные gaps — в storage layer (PrefsProvider без String/Map) и UI widget'е (нет radio в MenuItem). Оба gap решаются локально в рамках фичи (1 файл PrefsProvider + 1 новый MenuItem-вариант), без архитектурных изменений. Design sub-flow должен зафиксировать storage strategy (per-dict key vs JSON-blob) и owner для radio widget'а (общий iconDropDowned vs локальный quiz/chat).

## log_messages

- Discovery: MVI каркас (State/Msg/Reducer/EffectHandler/FlowHandler) полностью pattern-matched, picker ложится в существующий слой.
- Gap #1: PrefsProvider не имеет String/JSON layer и dynamic-keys — persistent Map нужен новый API.
- Gap #2: radio в dropdown MenuItem отсутствует; fetch domain-data on entry — нет существующего эффекта аналога.
