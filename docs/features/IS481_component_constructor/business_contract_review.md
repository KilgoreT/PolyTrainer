# Review: business_contract

## Findings

### 1. `flowAllUserDefinedTypes()` возвращает `Pair<...>` — нарушение собственного Open question #3

**О чём:** Интерфейс `ComponentsManagerUseCase.flowAllUserDefinedTypes(): Flow<Pair<List<ComponentType>, ComponentUsage>>` (контракт § UseCase, строки 514).

**Почему проблема:** в Open Q #3 контракт сам фиксирует решение «контракт использует dedicated data class (`UserDefinedTypesSnapshot`, `PerDictionarySnapshot`, `ComponentUsage`) — устраняет неоднозначность `.first/.second` в reducer». Парный метод `PerDictionaryComponentsUseCase.flowComponentsForDictionary` уже возвращает `Flow<PerDictionarySnapshot>` (dedicated). Aggregated-метод асимметричен: возвращает Pair вместо dedicated `UserDefinedTypesSnapshot`. Reducer тогда будет читать `.first/.second`, чего сам же контракт запрещает. Также data-layer `flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesSnapshot>` уже использует именно dedicated класс — domain-метод его теряет.

**Что предложить:** Поменять подпись на `fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>` и добавить data class `UserDefinedTypesSnapshot(val types: List<ComponentType>, val usage: ComponentUsage)` в `components_manager.logic` (либо в `modules/domain/lexeme/` рядом с `PerDictionarySnapshot`). `Msg.TypesLoaded` тогда принимает `snapshot: UserDefinedTypesSnapshot` (или один аргумент вместо двух).

---

### 2. `CreateComponentOutcome.Success(type: ComponentTypeApiEntity)` несовместим со `Scope.PerDictionaries(N>1)` — N rows = N entities

**О чём:** Data-layer outcome `CreateComponentOutcome.Success(val type: ComponentTypeApiEntity)` (контракт § Data-layer shape, строка 702) и зеркальный domain `CreateOutcome.Success(val type: ComponentType)` (строка 462).

**Почему проблема:** walkthrough § 9 / ui_placement.md / scope_analysis aspect `userdefined_identity_invariant` фиксируют: при `Scope.PerDictionaries(listOf(d1, d2, d3))` API создаёт **N независимых rows в `component_types`** (по одной per dictionary_id). Поведение прямо сформулировано в KDoc `createUserDefinedComponent` контракта: «для `Scope.PerDictionaries(N)` создаётся N rows» (строка 522). Но `Success` несёт ровно **один** `ComponentTypeApiEntity` — выбрать какой row из N произвольно нельзя без потери информации. Reducer после успешного create не сможет получить все созданные entities (например для optimistic refresh / undo). При scope = `Global` или одного `PerDictionaries(listOf(x))` это работает, при multi — теряет данные.

**Что предложить:** Поменять `Success(type)` → `Success(types: List<ComponentType>)` (и зеркально на data-layer `Success(types: List<ComponentTypeApiEntity>)`). При `scope=Global` или одного per-dict — список длины 1, при multi-scope — N. Альтернатива: разделить scope в API — `createGlobal(...)` + `createPerDictionaries(list)` с разными outcome shapes; но это раздувает контракт.

---

### 3. Reducer-таблица упоминает `Msg.CreateResult(NameTaken(scope))` — variant `NameTaken` не существует в sealed `CreateOutcome`

**О чём:** § Msg / Reducer-реакции таблица, строка 238: «`CreateResult(NameTaken(scope))` → `isCreating=false, createDialog.copy(nameError=ScopeCollision)`».

**Почему проблема:** sealed `CreateOutcome` определяет варианты `Success` / `NameEmpty` / `SameScopeCollision` / `CrossScopeCollision` / `Failure`. Variant'а `NameTaken(scope)` нет. Сходно `nameError=ScopeCollision` — в `NameError` нет варианта `ScopeCollision`, есть `SameScopeCollision` и `CrossScopeCollision`. Reducer-эскиз ссылается на несуществующие имена → читатель контракта вынужден догадываться, маппинг State↔Msg ломается.

**Что предложить:** Привести таблицу к фактическим именам sealed-вариантов: две строки `CreateResult(SameScopeCollision)` → `nameError = NameError.SameScopeCollision` и `CreateResult(CrossScopeCollision)` → `nameError = NameError.CrossScopeCollision`. Или одна строка с обобщением `CreateResult(out: SameScopeCollision|CrossScopeCollision)` + комментарий про маппинг.

---

### 4. `PerDictionaryComponentsUseCase.flowComponentsForDictionary` — stale KDoc возвращает «pair», а тип уже `Flow<PerDictionarySnapshot>`

**О чём:** строка 586: `@return pair: (types, valueCountByType, dictionaryName)`, реальный return — `Flow<PerDictionarySnapshot>` (строка 590).

**Почему проблема:** не блокирующее само по себе (содержательно signature правильная — dedicated class), но KDoc прямо противоречит подписи. На business_design_tree / business_implement разраб увидит «pair» и пойдёт искать `.first/.second`. Хвост старого решения до Open Q #3 фиксации.

**Что предложить:** Заменить `@return pair: ...` на `@return [PerDictionarySnapshot] — типы + valueCountByType + dictionaryName`.

---

## Verdict

verdict: changes_requested
