# Verification

## Rules

Этот файл — живой документ. Правила для всех шагов, получающих его как input:

1. Корневые пункты (`- [ ]` на верхнем уровне) создаются ТОЛЬКО на этом шаге, на основе спеки.
2. Остальные шаги ДОПОЛНЯЮТ подпунктами (вложенные `  - [ ]`), но НЕ добавляют корневые.
3. Если сценарий можно привязать к конкретному корневому пункту (тот же user action, другое условие/результат) — это подпункт. Если сценарий ортогонален всем корневым (другой actor, другой trigger) — ОСТАНОВИСЬ.
   Не дописывай ничего в этот файл. Верни в своём output секцию `## Error`:
   `## Error`
   `Обнаружен сценарий: [описание]. Отсутствует в спеке. Требуется согласование.`
4. Подпункты не затирают и не изменяют существующие записи — только дополняют.
5. Корневой пункт = верифицируемый сценарий из спеки. Для UI — действие пользователя (тап, ввод, свайп, навигация). Для бизнес-логики без UI — бизнес-правило. Системные события (ошибка, загрузка, lifecycle) — подпункты к сценарию, который их спровоцировал. Подпункт = уточнение поведения (edge case, state change, UI detail).
6. Каждый подпункт помечен тегом шага-источника: `[step_name]`.
7. Два уровня вложенности: корневой и подпункт. Третий уровень запрещён.
8. Чекбоксы создаются в состоянии `[ ]`. Переключение в `[x]` — только при ручной верификации.
9. Перед добавлением подпункта проверь — нет ли подпункта с твоим тегом и тем же смыслом. Дубликаты не добавляй.

## Checklist

- ✅ Пользователь открывает форму создания словаря (через "Новый словарь" или route DICTIONARY_CREATE) → отображается пустая форма с placeholder вместо флага, пустым полем названия, пустым полем фильтрации, полным grid флагов и кнопкой "Создать" (неактивна)
  - ✅ [contract_state] State defaults: editingDictionaryId=null, name="", flagFilter="", flags=empty, selectedFlag=null, saveButtonEnabled=false
  - ✅ [contract_effect_msg] FlowHandler подписывается при старте → UseCase загружает флаги → FlagsUpdated(all)
  - ✅ [usecase] getAllCountryFlags() вызывается один раз при инициализации, обогащает каждый CountryFlagItem языками через countryProvider.getLanguagesForCountry(numericCode)
  - ✅ [usecase] flagsFlow() при пустом filterQuery эмитит полный список allFlags
  - ✅ [design_tree] FlagFilterFlowHandler.subscribe() подписан на flagsFlow(), эмитит FlagsUpdated при каждом emit
- ✅ Пользователь открывает форму редактирования словаря (тап на элемент в списке) → форма предзаполнена: название из словаря, флаг выбран по numericCode (или placeholder если null), кнопка "Сохранить" активна
  - ✅ [contract_state] State после prefillForEdit: editingDictionaryId=id, name=dictName, selectedFlag=matchedFlag, saveButtonEnabled=true
  - ✅ [contract_effect_msg] Цепочка: FlagsUpdated → Reducer → Effect.LoadDictionary(id) → DictionaryLoaded → prefillForEdit
  - ✅ [usecase] getDictionary(id) загружает словарь по ID, возвращает (id, name, numericCode) для prefillForEdit
  - ✅ [design_tree] initEffects при editingDictionaryId!=null содержит LoadDictionary(id), DictionaryFormEffectHandler вызывает getDictionary + findFlag → DictionaryLoaded
  - ✅ [test] prefillForEdit с пустым name → saveButtonEnabled=false (edge case редактирования)
- ✅ Пользователь вводит название словаря → кнопка "Создать"/"Сохранить" становится активной при `isNotBlank()`, неактивной при пустом/пробельном значении
  - ✅ [contract_state] Extension updateName(value) обновляет name и saveButtonEnabled=value.isNotBlank()
  - ✅ [contract_ui_msg] Msg.NameChanged(value) → updateName(value), без effects
- ✅ Пользователь тапает на флаг в grid → флаг выбран, отображается рядом с полем названия (ImageFlagWidget 48dp), в grid обведён BorderStroke
  - ✅ [contract_state] Extension selectFlag(flag) устанавливает selectedFlag
  - ✅ [contract_ui_msg] Msg.SelectFlag(item) при item!=selectedFlag → selectFlag(item)
  - ✅ [test] Msg.SelectFlag реализует toggle-семантику: тап на выбранный → deselectFlag, тап на другой → selectFlag/switch
- ✅ Пользователь тапает на уже выбранный флаг → выбор снимается, рядом с полем названия возвращается placeholder
  - ✅ [contract_state] Extension deselectFlag() сбрасывает selectedFlag=null
  - ✅ [contract_ui_msg] Msg.SelectFlag(item) при item==selectedFlag → deselectFlag() (toggle)
- ✅ Пользователь вводит текст в поле фильтрации флагов → grid фильтруется с debounce ~300ms по названию страны и языкам (case-insensitive substring), при пустом поле показаны все флаги
  - ✅ [contract_state] Extension updateFlagFilter(query) обновляет flagFilter
  - ✅ [contract_ui_msg] Msg.FlagFilterChanged(query) → updateFlagFilter(query) + Effect.FilterFlags(query)
  - ✅ [contract_effect_msg] Effect.FilterFlags → FlowHandler.runEffect → useCase.updateFilter(query) → Flow emit → FlagsUpdated
  - ✅ [test] Msg.FlagFilterChanged порождает Effect.FilterFlags(query), state обновляет только flagFilter
  - ✅ [usecase] updateFilter(query) эмитит в filterQuery MutableStateFlow, flagsFlow() применяет debounce ~300ms и фильтрует allFlags по countryName и languages (case-insensitive substring)
- ✅ Пользователь нажимает "Создать" (режим создания) → словарь создаётся в Room (INSERT), устанавливается текущим (setCurrentDictionary), форма закрывается
  - ✅ [contract_ui_msg] Msg.Save при editingDictionaryId==null → Effect.SaveDictionary(name, numericCode)
  - ✅ [contract_effect_msg] SaveDictionary → addDictionary + setCurrentDictionary → DictionarySaved → Effect.Close
  - ✅ [usecase] addDictionary(name, numericCode) создаёт словарь, setCurrentDictionary(id) устанавливает текущим — два последовательных вызова UseCase
  - ✅ [design_tree] DictionarySaved → Reducer порождает Effect.Close → NavigationEffectHandler вызывает onClose
  - ✅ [test] Msg.DictionarySaved порождает Effect.Close (не needClose в State), state не меняется
- ✅ Пользователь нажимает "Сохранить" (режим редактирования) → словарь обновляется в Room (UPDATE), форма закрывается
  - ✅ [contract_ui_msg] Msg.Save при editingDictionaryId!=null → Effect.UpdateDictionary(id, name, numericCode)
  - ✅ [contract_effect_msg] UpdateDictionary → updateDictionary → DictionarySaved → Effect.Close
  - ✅ [usecase] updateDictionary(id, name, numericCode) обновляет словарь в Room
- ✅ Пользователь нажимает "Назад" (back press) → форма закрывается без сохранения, без диалога подтверждения
  - ✅ [contract_ui_msg] Msg.Back → Effect.Back
  - ✅ [contract_effect_msg] Effect.Back → NavigationEffectHandler → onBackPress() → Msg.Empty
  - ✅ [design_tree] NavigationEffectHandler получает onBackPress через конструктор ViewModel, вызывает onBackPress?.invoke()
  - ✅ [test] Msg.Back порождает Effect.Back, state не меняется
