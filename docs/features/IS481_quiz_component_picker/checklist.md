# Checklist

- [✅] User открывает chat → видит в меню новый пункт выбора компонента квиза с правильным состоянием (radio из `availableTypes`; `disabled+checked` если один тип; previous выбор восстановлен per-dictionary) [spec]
  - [✅] `LexemeSubmenuMenuItem` + `LexemeRadioMenuItem` primitives созданы (`modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/`)
  - [✅] `QuizComponentMenuItem` рендерит подменю, скрывает себя при пустом `availableTypes` (early `return` в composable)
  - [✅] `ComponentChoiceItem` маркирует selected ref через `type.toRef() == state.selectedRef`
  - [✅] `ActionsWidget` встраивает `QuizComponentMenuItem` между `MistakesMenuItem` и debug-блоком
  - [✅] `ItemsState.QuizComponent(availableTypes, selectedRef?)` + computed `isPickerEnabled = availableTypes.size > 1`
  - [✅] `LoadQuizComponentTypes` effect triggered из `Msg.PrepareToStart`; `QuizComponentTypesLoaded` обновляет state через `resolveSelection`
  - [✅] Default fallback на `available.first()` если restored ref не in list (после `isEmpty` guard)
  - [✅] Строки `chat_menu_item_quiz_component` + `chat_menu_item_component_translation` (+ ru) добавлены в `core-resources`
  - [✅] Лог: `###MATE### RunEffect: LoadQuizComponentTypes` при PrepareToStart (verified 2026-06-11 20:57:26 на Mi A1)
  - [✅] Визуальная проверка: меню открывается, radio-группа отображает все availableTypes (translation + Definition в dict 1), один пункт checked (default = translation) — verified user 2026-06-11
  - [✅] Визуальная проверка `availableTypes.size == 1` (dict без definition): state корректный (logcat 23:27:47.266 на Mi A1: `availableTypes=[ComponentType(TRANSLATION)]`, `restoredSelectedRef=null` → fallback `selectedRef=BuiltIn(TRANSLATION)`). UX-FIX verified 2026-06-11 23:27 user-confirmed «работает»: header submenu раскрывается (убран `enabled = isPickerEnabled` с `LexemeSubmenuMenuItem` в `QuizComponentMenuItem`); внутри radio показан selected + disabled (юзер видит что выбрано, не может переключить).
  - [↪] Cascaded popup вместо inline accordion — отложено в `docs/Backlog.md` (Tier 1 primitive `LexemeSubmenuMenuItem` остаётся accordion-style; миграция на cascaded popup — отдельная followup-задача с выбором cascade lib `me.saket.cascade:cascade-compose` / manual nested DropdownMenu).
- [✅] User меняет выбор компонента → выбор сохраняется в prefs per-dictionary [spec]
  - [✅] `Msg.SelectQuizComponent(ref)` → reducer emit'ит `SaveQuizPickerSelection(ref)`, state без изменений (`state to setOf(...)`)
  - [✅] `DatasourceEffect.SaveQuizPickerSelection` в handler → `useCase.setQuizPickerSelection(dictId, ref)` (`getCurrentDictionaryId()`; при null — no-op, emit `Msg.Empty`)
  - [✅] `QuizChatUseCaseImpl.setQuizPickerSelection` → `prefsProvider.setStringByRawKey(quizPickerPrefKey(dictionaryId), encodeRef(ref))`
  - [✅] `PrefsProvider.setStringByRawKey` (raw-string dynamic-key API через `stringPreferencesKey` + `dataStore.edit`)
  - [✅] `quizPickerPrefKey(dictionaryId)` в `:modules:datasource:prefs` — single source of truth
  - [✅] `QuizPickerFlowHandler` re-emit `Msg.QuizComponentTypesLoaded` после write (subscribe на `getStringFlowByRawKey`)
  - [✅] Лог: SaveQuizPickerSelection effect отработал (implied — после переключения logcat показал `QuizComponentTypesLoaded(restoredSelectedRef=UserDefined(name=Definition))` от FlowHandler re-emit на pref change)
  - [✅] Визуальная проверка: radio переключился (translation → Definition), state.selectedRef=UserDefined(name=Definition) в logcat — verified user 2026-06-11
- [✅] User запускает следующую quiz session → квиз отдаёт только выбранный компонент (через filter `componentRefs` по `selectedRef`) [spec]
  - [✅] `QuizGameImpl.fetchData` вызывает `quizChatUseCase.getQuizPickerSelection(dictionaryId)` после `getQuizConfig`
  - [✅] `effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs`
  - [✅] `null` → fallback на `quizConfig.componentRefs` (preserves семантику до первого выбора)
  - [✅] `QuizChatUseCase` iface расширен 3 методами (`getAvailableTypes` / `getQuizPickerSelection` / `setQuizPickerSelection`)
  - [✅] Визуальная проверка: при выбранном `UserDefined("Definition")` квиз отдаёт определение; после switch'а на `BuiltIn(TRANSLATION)` — квиз отдаёт перевод. Verified user 2026-06-11 20:58 (logcat: LoadQuiz effect → `Переведи следующее слово` chat message).
- [✅] User возвращается в словарь → previous выбор восстановлен из prefs [spec]
  - [✅] Per-dictionary ключ `quiz_picker_dict_<id>` гарантирует изоляцию между словарями
  - [✅] Initial load через `LoadQuizComponentTypes` effect → emit `QuizComponentTypesLoaded` с restored ref
  - [✅] Encoding: `builtin:<key>` / `user:<name>`; decode через `substringAfter(':')` (поддержка `:` в name, unicode, пустой строки); unknown built-in / иной prefix → null
  - [✅] `ComponentType.toRef()` extension в `modules/domain/lexeme`
  - [✅] DataStore persistence (PrefsProvider unchanged для cold start survival)
  - [✅] Cross-dictionary isolation verified 2026-06-11: dict 1 (`quiz_picker_dict_1 = "user:Definition"`) → switch на dict 2 (`quiz_picker_dict_2` отсутствует → restoredSelectedRef=null → fallback BuiltIn(TRANSLATION)). Префы изолированы per-dictionary. Logcat подтверждает разные state.
  - [✅] Визуальная проверка: cold start (force-stop + relaunch) — выбор UserDefined("Definition") восстановлен из prefs. Verified 2026-06-11 20:57: PID 9037 → 11610 (fresh process), `LoadQuizComponentTypes → QuizComponentTypesLoaded(restoredSelectedRef=UserDefined(name=Definition))` → state.selectedRef = UserDefined(name=Definition).

## Ручное тестирование

### Сценарий 1. Открытие chat — видимость и состояние picker'а
1. Открыть приложение, перейти в словарь с `availableTypes.size > 1` (built-in translation + хотя бы один UserDefined).
2. Открыть quiz chat этого словаря.
3. Logcat: должен появиться `###MATE### RunEffect: LoadQuizComponentTypes` (срабатывает на `Msg.PrepareToStart` в reducer).
4. Тапнуть кнопку меню в appbar.
5. Ожидание: между пунктом «Mistakes» и debug-блоком виден заголовок «Quiz component» (RU: «Компонент квиза») с раскрывающимся chevron'ом. Тап по нему раскрывает inline radio-группу со всеми `availableTypes` (порядок — по `position` из БД).
6. Ожидание: один из radio-пунктов checked — либо ранее сохранённый, либо default = `availableTypes.first().toRef()` (если в prefs ничего нет или restored не в списке).
7. Перейти в словарь, у которого `availableTypes.size == 1`, открыть chat → меню.
8. Ожидание: пункт «Quiz component» виден, в подменю — единственный radio checked, но **disabled** (`isPickerEnabled = false` → клики не работают).
9. Перейти в словарь, у которого `availableTypes.isEmpty()` (если такой есть).
10. Ожидание: пункт «Quiz component» **полностью скрыт** (composable `return` на пустом списке).

### Сценарий 2. Смена выбора и persist per-dictionary
1. Словарь с `availableTypes.size > 1` → chat → меню → раскрыть «Quiz component».
2. Тапнуть radio-пункт, отличный от текущего checked.
3. Logcat: `###MATE### RunEffect: SaveQuizPickerSelection(ref=...)` (ref — выбранный `ComponentTypeRef`).
4. Logcat: следом `###MATE### RunEffect: ...` от `LoadQuizComponentTypes` НЕ срабатывает на каждый клик (только `PrepareToStart`); обновление приходит через `QuizPickerFlowHandler` re-emit — в reducer log: `Reduce ---message---: QuizComponentTypesLoaded(...)`.
5. Ожидание: radio мгновенно переключился на новый пункт.
6. Закрыть chat (back), открыть chat снова → меню → раскрыть «Quiz component».
7. Ожидание: checked именно тот пункт, что выбран на шаге 2 (восстановление из prefs через `getStringByRawKey(quizPickerPrefKey(dictId))`).

### Сценарий 3. Применение выбора к следующей quiz session
1. Словарь с `availableTypes` ≥ 2 типа (например `BuiltIn(TRANSLATION)` + `UserDefined("Definition")`).
2. Chat → меню → picker → выбрать `UserDefined("Definition")` → закрыть меню.
3. Запустить новую quiz session (отправить триггер старта квиза в chat).
4. Logcat: при `fetchData` в `QuizGameImpl` — никаких `###QuizPicker###` логов (отсутствуют в production коде), но через `LogTags.CHAT` могут проходить связанные сообщения от других этапов. Проверять поведение, не лог.
5. Ожидание: квиз отдаёт только items с `componentRef == UserDefined("Definition")` (через `effectiveRefs = listOf(selectedRef)` → `toQuizItem(componentRefs = effectiveRefs)`); items с `BuiltIn(TRANSLATION)` не появляются в этой сессии.
6. Закончить session, в picker сменить выбор на `BuiltIn(TRANSLATION)`, запустить ещё одну session.
7. Ожидание: квиз отдаёт только translation items.
8. Ожидание (граничный): если `getQuizPickerSelection` вернёт `null` (например очистка prefs) — fallback на `quizConfig.componentRefs` (полный список).

### Сценарий 4. Восстановление выбора при возврате в словарь и cold start
1. Словарь A: chat → picker → выбрать `UserDefined("Definition")` → закрыть chat.
2. Перейти в словарь B через dictionary tab → chat → picker → выбрать `BuiltIn(TRANSLATION)` → закрыть chat.
3. Вернуться в словарь A → chat → меню → picker.
4. Ожидание: в словаре A checked `UserDefined("Definition")` (изоляция per-dict через ключ `quiz_picker_dict_<A.id>`, не путается с `quiz_picker_dict_<B.id>`).
5. Вернуться в словарь B → chat → меню → picker.
6. Ожидание: в словаре B checked `BuiltIn(TRANSLATION)`.
7. **Cold start**: kill процесс (swipe из recents) → перезапустить приложение → словарь A → chat → picker.
8. Logcat при open chat: `###MATE### RunEffect: LoadQuizComponentTypes` (фаза `PrepareToStart`).
9. Ожидание: выбор `UserDefined("Definition")` сохранился (DataStore переживает kill).
10. Дополнительно (encoding edge cases — если получится подсунуть в prefs руками через ADB / debug build):
    - `builtin:translation` → decode в `BuiltIn(TRANSLATION)`.
    - `user:My:Custom:Component` → decode в `UserDefined("My:Custom:Component")` (через `substringAfter(':')` сохраняет внутренние двоеточия).
    - `garbage` / `USER:foo` / `builtin:unknown_key` → decode null → fallback на `available.first()`.

## Примечание о логах

В production коде **отсутствует** тег `###QuizPicker###`, описанный в спецификации `checklist_init` и в `business_summary.md` (раздел «Лог-точки и tag»). Реально доступны:
- `###MATE### RunEffect: <Effect>` — пишется из `MateTypedEffectHandler` для всех Effect'ов (включая `LoadQuizComponentTypes` / `SaveQuizPickerSelection`).
- `###MATE### Reduce ---message---: <Msg>` / `Reduce --newState--: <State>` / `Reduce --toEffect--: <Effect>` — из `ChatReducer`.
- `###CHAT### fetchData: no current dictionary (null id)` / `fetchData: no quiz config ...` — из `QuizGameImpl.fetchData` при null guards.

Если spec'д тег `###QuizPicker###` нужен для отладки — добавить вызовы `logger.d(tag = "###QuizPicker###", ...)` в `DatasourceEffectHandler` (load/save outcomes), `QuizPickerFlowHandler` (subscribe/emit), `QuizChatUseCaseImpl` (decode failure → null). Сейчас же ручная проверка опирается на существующие `###MATE###` / `###CHAT###` сообщения.

_model: claude-opus-4-7[1m]_
