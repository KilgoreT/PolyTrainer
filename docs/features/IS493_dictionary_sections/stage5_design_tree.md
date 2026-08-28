# IS493 / Э5 — Design tree

Слова попадают в группы: пикер + чипы в карточке слова, живой контент
раскрытых групп на вкладке «Группы».

Основа: rollout_stages.md (редакция 2026-08-23 — вложенности НЕТ, группы
корневые листья), architecture.md, прецеденты Э2 (живое окно) и Э3
(шторки, транзакции, reducer-конвенция StateAtoms/ReducerLogging).

## Решения пользователя (В1–В4, 2026-08-23)

- **В1. Пикер = модальная шторка** со списком групп словаря и чекбоксами
  (M3 ModalBottomSheet, прецедент Э3). «Все» в пикере нет (виртуальная).
  Снекбар под шторкой не виден (итог Э3) — обратная связь об ошибках
  решается внутри шторки (D22.3).
- **В2. Контент раскрытой группы = живое окно** — та же механика, что у
  «Все» (предикатный LIMIT-запрос, «Ещё» += CHUNK_SIZE, компенсация
  вытеснения), окно НА КАЖДУЮ раскрытую группу.
- **В3. Запись membership — сразу по галочке** (галочка = факт), keyed
  in-flight: на время записи дизейблится только тапнутая галочка.
- **В4. Тап по чипу группы в карточке = открыть пикер** (чип —
  индикатор; мутации только через пикер).

## D20. Данные: membership-мутации + окна групп

### D20.1 GroupApi — новые методы

```kotlin
/** Живые группы слова (для чипов и предзаполнения пикера). */
fun wordGroups(wordId: Long): Flow<List<GroupApiEntity>>

/** Живое окно слов группы: membership ∩ words, id DESC LIMIT. */
fun flowGroupWordsWindow(groupId: Long, limit: Int): Flow<List<TermApiEntity>>

suspend fun addWordToGroup(wordId: Long, groupId: Long): AddMembershipOutcome
suspend fun removeWordFromGroup(wordId: Long, groupId: Long): RemoveMembershipOutcome
```

- `wordGroups` наблюдает `word_groups ⋈ dictionary_groups` (только живые
  группы), сортировка НЕ в SQL — Collator на слое handler'а (прецедент А8).
- `flowGroupWordsWindow` — зеркало `flowTermsWindow` Э2 с membership-JOIN:
  `SELECT w.* FROM words w JOIN word_groups wg ON wg.word_id = w.id
   WHERE wg.group_id = :groupId ORDER BY w.id DESC LIMIT :limit`.
  Room-инвалидация по обеим таблицам — вставки/снятия переэмичиваются сами.

### D20.2 Транзакции мутаций (§2.4: read → validate → write)

- `addWordToGroup`, порядок проверок (ревью Data-2):
  1. `getWordDictionaryId(wordId): Long?` — null → `WordNotFound`
     (слова удаляются HARD, у words нет removed_at);
  2. `getLivingGroupByIdInDict(groupId, wordDictId)` — null →
     `GroupNotFound` (мёртвая ИЛИ чужого словаря);
  3. `insertWordGroup(...): Long` (`OR IGNORE`) — `-1` → `AlreadyIn`
     (составной PK), иначе `Added` (ревью Data-4: Unit не различает).
  **ИНВАРИАНТ (ревью Data-3):** все три шага строго внутри ОДНОЙ
  `immediateTransaction`, открытой ДО первого чтения — `OR IGNORE` гасит
  только PK-конфликт, FK-violation (слово/группа умерли между check и
  insert) был бы crash'ем; сериализация с deleteGroup/deleteWord на
  writer-коннекте исключает гонку. Зафиксировать в KDoc Impl.
- Outcomes: `Added`/`AlreadyIn` — успех (UI не различает);
  `GroupNotFound`/`WordNotFound` — тихие (галочку/чипы поправит живая
  подписка wordGroups — единственный источник правды; карточка при
  внешнем удалении слова остаётся stale-открытой — существующее
  поведение экрана, живая подписка на слово — Backlog, не Э5).
- `removeWordFromGroup`: `DELETE` по составному ключу; rowcount 0 →
  `NotFound` = идемпотентный успех (снимать нечего — цель достигнута).
- Мутации словаря групп НЕ трогают — `updated_at` группы не меняется
  (membership — отдельная сущность).

### D20.3 Domain outcomes

`modules/domain/group/GroupOutcomes.kt` +:

```kotlin
sealed interface AddMembershipOutcome { Added; AlreadyIn; GroupNotFound; WordNotFound }
sealed interface RemoveMembershipOutcome { Removed; NotFound }
```

Валидаций имён нет — мутации по id.

## D21. Groupstab: окна раскрытых групп

### D21.1 State — окно на группу

`expandedGroups: Set<Long>` заменяется картой:

```kotlin
/** Окна раскрытых групп; ключа нет — группа свёрнута. */
val expandedGroupWindows: Map<Long, GroupWindowState> = emptyMap()

data class GroupWindowState(
    val window: Int,
    val loadedWords: List<TermUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
)
```

- «Раскрыта» = ключ в карте (explicit state, прежний Set уходит).
- `hasMore` — против счётчика группы из `GroupUiItem.count` (живой slice).
- Пустая группа (count=0): раскрытие БЕЗ окна — `GroupWindowState(window=0,
  isLoading=false)`, заглушка «пусто» как в Э3, подписка не открывается
  (зеркало T-5а «Все»).
- **Переходы «пусто ↔ окно» под раскрытием (ревью, 4 агента):** живой
  count меняется под раскрытой группой — окна обязаны реагировать:
  0→N — окно автооткрывается (`SetGroupWindow(id, CHUNK_SIZE)` — юзер
  добавил слово из карточки и смотрит на вкладку); N→0 — окно
  закрывается в заглушку «пусто» (`SetGroupWindow(id, null)`); рост при
  открытом окне — компенсация вытеснения; всегда — пересчёт hasMore.
  Всё это — атом `applyGroupCounts` (D21.2).

### D21.2 Атомы (StateAtoms, продолжение конвенции)

Примерный набор: `openGroupWindow(groupId, limit)` (+эффект
`SetGroupWindow`), `closeGroupWindow(groupId)` (+эффект `SetGroupWindow
(null)`), `widenGroupWindowBy(groupId, step)`, `applyGroupWindowWords
(groupId, words)`, `hideGroupWindowLoading(groupId)`,
`recalcGroupHasMore(groupId)`, `expandGroupEmpty(groupId)`,
`purgeDeadExpanded` — расширяется: закрытие окон мёртвых групп обязано
отдавать эффекты `SetGroupWindow(id, null)` на каждую умершую.

**`applyGroupCounts(counts: Map<Long, Int>)`** — один шаг «окна
раскрытых групп отреагировали на свежие счётчики slice» (D21.1:
автооткрытие 0→N / закрытие N→0 / компенсация роста / hasMore; эффекты
`SetGroupWindow` рождаются в атоме). **ПОРЯДОК ЗНАЧИМ (ревью Mate-3):
в цепочке SliceLoaded — строго ДО `applyGroups`**: прежний count живёт
в `state.groups`, который `applyGroups` перезаписывает — после него
дельта всегда 0 (у «Все» иначе: прежний count в `allNode`). Тест на
порядок обязателен.

### D21.3 Msg / эффекты

```kotlin
ToggleGroup(groupId)          // теперь: открыть/закрыть ОКНО группы
LoadMoreGroup(groupId)
GroupWindowLoaded(groupId, words)
GroupWindowFailed(groupId)
GroupsEffect.SetGroupWindow(groupId, limit: Int?)  // null — погасить
```

### D21.4 Handler — динамические подписки окон

`MutableStateFlow<Map<Long, Int>>` (groupId → limit);
**`merge`, НЕ `combine` (ревью Mate-1):** combine гейтит все окна за
самым медленным, теряет идентичность эмитента и умирает целиком от
ошибки одного окна. Схема:

```kotlin
flatMapLatest { map ->
    if (map.isEmpty()) emptyFlow()
    else merge(map.map { (id, limit) ->
        flowGroupWordsWindow(id, limit)
            .map<_, Msg> { Msg.GroupWindowLoaded(id, it) }
            .catch { emit(Msg.GroupWindowFailed(id)) }
    })
}
```

Изменение карты пересоздаёт merge — хвостовые эмиссии уже закрытых
окон гасит guard reducer'а («id нет в карте → no-op»). Смена словаря —
карта очищается. Лог: `window(group=5): limit=10 loaded=7`.
Room инвалидирует по ТАБЛИЦАМ: insert в word_groups переэмитит окна
ВСЕХ раскрытых групп — это benign-поведение, фиксируется в
лог-контракте ручников (D23).

### D21.5 Счётчики групп

НИЧЕГО не делаем: `membershipSlice` уже наблюдает `word_groups`,
`buildDisplayTree` уже считает `subtreeWordCount` (= слова листа) —
счётчики оживают в момент первого `addWordToGroup`. «Все» продолжает
показывать все слова словаря (D21-инвариант: слово в группе остаётся в
«Все»).

## D22. Wordcard: иконка, чипы, пикер

### D22.1 Встраивание в существующий экран

- Иконка групп — `TopBarWidget.actions` ПЕРЕД kebab (ic из
  core-resources; выбрать существующую или добавить).
- Чипы — блок под `WordFieldWidget` (FlowRow, по алфавиту Collator,
  wrap; пустой список — блока нет). Тап по чипу → `OpenGroupPicker`.
  **НЕ переиспользовать `SubentityChip` as-is (ревью UX-5):** на экране
  уже есть чипы компонентов с `ic_add` = действие-мутация; групповые
  чипы — индикаторы (В4), обязаны отличаться: без trailing-иконки
  действия. Текст чипа: `maxLines=1 + ellipsis` (имена групп —
  произвольные юзер-строки, ревью UX-6).
- Пикер — `GroupPickerBottomSheetWidget`: M3 ModalBottomSheet,
  **LazyColumn с ограничением высоты (ревью UX-4** — 20+ групп,
  landscape**)**, строка = имя (`maxLines=1+ellipsis`) + `Checkbox`;
  группы по алфавиту; пустой словарь групп → текст-заглушка «Групп пока
  нет» (создание — Э7).

### D22.2 State/Msg/атомы wordcard — по конвенции

Wordcard-reducer НЕ мигрирует целиком (words-принцип «по мере правок»):
добавляется `wordcard/mate/GroupBlockAtoms : ReducerLogging(logger,
tag=###WORDCARD###)` ТОЛЬКО с групповыми атомами; `WordCardReducer`
наследует его, НОВЫЕ ветки — цепочки атомов, старые не трогаются.

State-блок:

```kotlin
data class GroupsBlockState(
    val dictGroups: List<GroupUi> = emptyList(),   // группы словаря (пикер)
    val wordGroupIds: Set<Long> = emptySet(),      // членства слова
    val isPickerOpen: Boolean = false,
    val inFlight: Set<Long> = emptySet(),          // keyed in-flight (В3)
)
```

Чипы — derived: `dictGroups.filter { it.id in wordGroupIds }` (алфавит
уже в dictGroups). Msg: `OpenGroupPicker` / `DismissGroupPicker` /
`ToggleGroupMembership(groupId)` / `WordGroupsLoaded` /
`DictGroupsLoaded` / `MembershipDone(groupId)` /
`MembershipFailed(groupId)`.

- `GroupUi(id, name)` — свой лёгкий UI-тип wordcard; `WordCardUseCase`
  отдаёт domain-`GroupNode` (НЕ `GroupApiEntity` — граница слоёв,
  ревью Arch-2; прецедент GroupsTabUseCase), маппинг Api→domain в
  app-Impl, domain→GroupUi — handler wordcard. `wordcard/build.gradle`
  получает `implementation(:modules:domain:group)`.
- **Глобальный guard экрана (ревью Mate-5):** `OpenGroupPicker` и
  `ToggleGroupMembership` — в true-список `isGuardedByPending`
  (пикер не открывается над умирающей карточкой); `MembershipDone/
  Failed/WordGroupsLoaded/DictGroupsLoaded` — НЕ гейтятся (иначе
  теряется снятие in-flight).
- **Flush-on-back (ревью UX-1/Arch-1, контракт А11):**
  `GroupsBlockState.inFlight` включается в `hasInFlightCommits`
  (exit-guard карточки) — Back при летящей membership-записи ждёт её
  завершения, запись не теряется. Юнит обязателен.

### D22.3 Поток мутации и гонки

- `ToggleGroupMembership`: guard `groupId in inFlight` → no-op (спам);
  членство по CURRENT `wordGroupIds` определяет направление
  (add/remove); атомы `markMembershipInFlight(groupId)` + эффект.
- Handler: `runCatching { addWordToGroup/removeWordFromGroup }` →
  outcome → плоские Msg через маппер (`toMembershipMsg`, прецедент
  MutationMappers): `Added/AlreadyIn/Removed/NotFound` →
  `MembershipDone(groupId)`; `GroupNotFound/WordNotFound` → ТОЖЕ
  `MembershipDone` (галочку/чип поправит живая подписка wordGroups —
  единственный источник правды); исключение → `MembershipFailed(groupId)`.
- `MembershipDone/Failed` → `clearMembershipInFlight(groupId)`.
  Оптимистичного локального торгла НЕТ: `wordGroupIds` меняет ТОЛЬКО
  подписка `wordGroups` (галочка перещёлкивается по факту БД — при
  живом Flow это <100мс; убирает весь класс рассинхронов).
- `MembershipFailed` — снятие in-flight; отдельного UI-алерта нет
  (галочка осталась в фактическом состоянии БД — честность подписки);
  лог ошибки — контракт ручника.
- **In-flight НЕ зависит от пикера (ревью Mate-6):** атомы
  `markMembershipInFlight`/`clearMembershipInFlight` — БЕЗ guard'а на
  `isPickerOpen`; `DismissGroupPicker` поле `inFlight` не трогает
  (запись долетает, чипы поправит подписка). Иначе `MembershipDone`
  после dismiss оставляет группу вечно задизейбленной. Юнит: «Done
  после dismiss чистит inFlight».

### D22.4 Подписки wordcard

**Место рождения подписок (ревью Mate-2):** существующий handler
wordcard — `MateTypedEffectHandler` (one-shot), а wordId/dictionaryId
на старте движка неизвестны. Прецедент в самом модуле —
`AvailableComponentTypesFlowHandler`: отдельный `MateFlowHandler`
(`GroupBlockFlowHandler`) + триггер-эффект `SubscribeGroupBlock(wordId,
dictionaryId)`, который добавляет ЕДИНСТВЕННУЮ строку в существующую
ветку `WordLoaded` (это разрешённое касание старого кода);
`CoroutineStart.UNDISPATCHED` против потери первой эмиссии.

Подписки внутри:
- `wordGroups(wordId)` → Collator-сортировка → `WordGroupsLoaded`;
- `groupTree(dictionaryId)` → Collator → `DictGroupsLoaded` (пикер живой:
  группа, созданная на вкладке параллельно, появляется под открытым
  пикером).
Логи подписок — с ID-СПИСКАМИ (ревью Test-1): `wordGroups: word=3
ids=[5,7]`, `dictGroups: count=4 ids=[5,7,9,12]` — рассинхрон derived-
чипов выводим из лога. Юнит на гонку порядка: «WordGroupsLoaded до
DictGroupsLoaded → чипы появляются после прихода dictGroups».

## D23. Лог-контракт Э5 (ручники «100% видеть неожиданное»)

- groupstab: `###GROUPS###` — атомы окон групп (`openGroupWindow |
  group=5 limit=10`), handler-эмиссии (`window(group=5): loaded=7`),
  slice-счётчики.
- wordcard: `###WORDCARD###` — `Reduce ---message---` +
  `Reduce ---step---` новых атомов (ReducerLogging), handler:
  `membership add: word=3 group=5 outcome=Added`, подписки
  `wordGroups: word=3 groups=2`.
- Ручники строятся так, что КАЖДЫЙ шаг юзера имеет ожидаемую сигнатуру
  в логе; в каждом ТК — явный блок `ЛОГ` с ПОЛНЫМ ожидаемым набором
  строк, включая benign-строки (Room инвалидирует по таблицам — окна
  ВСЕХ раскрытых групп переэмичиваются на любой insert в word_groups;
  легальные `no-op` guard'ов) — ревью Test-2.
- Критерий провала — конкретный blacklist, а не «всё неожиданное»:
  `MembershipFailed`, `GroupWindowFailed`, `…Failed`-outcomes handler'а,
  `FATAL EXCEPTION`, плюс расхождение фактических строк с блоком `ЛОГ`
  кейса.

## Риски

- Динамический combine окон групп (D21.4) — самая сложная часть; гонка
  «эмиссия окна догнала сворачивание» гасится guard'ом reducer'а
  (зеркало WindowLoaded Э2).
- Рост числа живых подписок: N раскрытых групп = N окон; приемлемо
  (юзер физически держит раскрытыми единицы).
- Кол-во веток wordcard-reducer'а растёт — новые строго по конвенции,
  старые не трогаем (риск регресса карточки минимизирован).
