# IS493 / Э5 — План реализации: слова попадают в группы (пикер + чипы)

Основа: stage5_design_tree.md (D20–D23), решения В1–В4 (2026-08-23).
Конвенция reducer'ов: атомы StateAtoms/ReducerLogging, плоские Msg от
handler-мапперов, честный red прогоном.

## Коммит-нарезка

Один коммит на весь Э5 (по образцу Э3): domain + данные + groupstab-окна
+ wordcard-блок + тесты + доки. Промежуточные фазы — только зелёные.

## Фаза 1. Domain + данные: membership-мутации и окна групп

- [ ] Domain (`modules/domain/group/GroupOutcomes.kt`):
      `AddMembershipOutcome` (Added / AlreadyIn / GroupNotFound /
      WordNotFound), `RemoveMembershipOutcome` (Removed / NotFound).
      Юнитов domain нет (логики нет — чистые ADT).
- [ ] **androidTest СНАЧАЛА (red прогоном)** — `GroupMembershipTest`
      (новый файл рядом с GroupMutationsTest):
      - add: слово попадает в группу; wordGroups эмитит; slice-счётчик
        группы растёт;
      - add дубль → AlreadyIn, строк не прибавилось (составной PK);
      - add в мёртвую (soft-deleted) группу → GroupNotFound;
      - add в группу ЧУЖОГО словаря → GroupNotFound (D20.2);
      - add на несуществующее слово → WordNotFound;
      - remove: строка ушла, wordGroups переэмитил; повторный remove →
        NotFound (идемпотентно);
      - flowGroupWordsWindow: respects limit, переэмит при add/remove,
        id DESC; слово ЧУЖОЙ группы в окно НЕ попадает (ревью Test-4 —
        мутант без WHERE group_id);
      - wordGroups отдаёт ТОЛЬКО живые группы (после deleteGroup —
        группа исчезает из потока);
      - hard-delete слова (ревью Data-1): removeWordSuspend → каскад FK
        чистит word_groups (прямой счёт), окно/wordGroups/slice
        переэмитили без слова, счётчик группы упал;
      - deleteGroup под окном (ревью Data-5): flowGroupWordsWindow
        мёртвой группы переэмитил пустым БЕЗ промежуточного состояния.
- [ ] DAO (`GroupDao`/`WordDao`): `flowWordGroups(wordId)` (JOIN живые),
      `flowGroupWordsWindow(groupId, limit)`, `getLivingGroupByIdInDict
      (groupId, dictId)`, `insertWordGroup(OR IGNORE): Long` (−1 =
      AlreadyIn, ревью Data-4), `deleteWordGroup(wordId, groupId): Int`,
      `getWordDictionaryId(wordId): Long?` (ревью Data-2 — boolean не
      кормит JOIN-check).
- [ ] `CoreDbApi.GroupApi`: 4 новых метода (D20.1) + KDoc семантики.
- [ ] `GroupApiImpl`: транзакции §2.4 (`immediateTransaction`), порядок
      проверок D20.2 (слово → группа-в-словаре → insert), KDoc-инвариант
      «всё в одной транзакции, открытой до первого чтения» (ревью
      Data-3); `updated_at` группы НЕ трогается.

## Фаза 2. Groupstab: окна раскрытых групп

- [ ] **Юниты СНАЧАЛА (red прогоном)** — GroupsTabReducer/StateAtoms:
      - ToggleGroup: count>0 → окно CHUNK_SIZE + SetGroupWindow; count=0
        → «пусто» без подписки; повторный → закрытие + SetGroupWindow
        (null);
      - LoadMoreGroup: guard'ы (нет окна/грузится/всё показано) + шаг;
      - GroupWindowLoaded: контент целиком, спиннер, hasMore; guard
        «эмиссия догнала сворачивание» → no-op;
      - GroupWindowFailed: спиннер СВОЕГО окна off, окно живо; свёрнутая
        → no-op (ревью Test-3);
      - applyGroupCounts: рост под окном → компенсация; 0→N раскрытой →
        автооткрытие окна; N→0 под окном → закрытие в «пусто»; падение
        count → hasMore пересчитан, окно не тронуто (shrink, ревью
        Test-5); ПОРЯДОК: строго ДО applyGroups — тест на порядок
        (дельта от старого count, ревью Mate-3);
      - purgeDeadExpanded: мёртвая раскрытая группа → окно закрыто,
        эффект SetGroupWindow(null);
      - смена словаря: все окна групп закрыты.
      Сценарный — ДВЕ раскрытые группы (независимость окон, ревью
      Test-4): «Ещё» и рост count трогают только своё окно; затем
      группа удалена под раскрытием; snapshot-цепочка add→окно→shrink.
- [ ] State: `expandedGroups: Set` → `expandedGroupWindows: Map<Long,
      GroupWindowState>` (D21.1); атомы D21.2 с logStep; UI-читатели
      (`GroupsTabScreen`, заглушка «пусто») — на карту.
- [ ] Msg/Effects: D21.3; handler: динамический MERGE окон (D21.4 —
      per-flow map+catch, НЕ combine) + лог `window(group=N)`.

## Фаза 3. Wordcard: иконка + чипы + пикер

- [ ] **Юниты СНАЧАЛА (red прогоном)** — новые ветки WordCardReducer
      (через reduce(), NoopLogger):
      - OpenGroupPicker/Dismiss; ToggleGroupMembership: направление по
        wordGroupIds, in-flight guard (спам), эффект Add/Remove;
      - WordGroupsLoaded/DictGroupsLoaded — сортированные списки в
        state; гонка порядка: чипы появляются после прихода ОБЕИХ
        подписок (ревью Test-1);
      - MembershipDone/Failed — снятие in-flight; wordGroupIds НЕ
        меняется мутационными Msg (только подпиской — D22.3);
      - Done ПОСЛЕ DismissGroupPicker → inFlight чистится (ревью
        Mate-6);
      - guard экрана: OpenGroupPicker/ToggleGroupMembership гейтятся
        isGuardedByPending, Done/Failed/Loaded — нет (ревью Mate-5);
      - flush-on-back: Back при летящей membership-записи → выход
        откладывается до Done/Failed (ревью UX-1/Arch-1, контракт А11).
      Сценарный: открыл пикер → тап галочки → in-flight → Done →
      подписка переэмитила членства → чип появился; снятие галочки →
      чип ушёл (remove-направление, ревью Test-5).
- [ ] `wordcard/mate/GroupBlockAtoms : ReducerLogging` (tag WORDCARD),
      `GroupsBlockState` в WordCardState; `WordCardReducer` наследует
      GroupBlockAtoms, новые ветки — цепочки атомов; inFlight — в
      hasInFlightCommits (exit-guard).
- [ ] Handler: `GroupBlockFlowHandler` (отдельный MateFlowHandler,
      прецедент AvailableComponentTypesFlowHandler, UNDISPATCHED) +
      триггер-эффект `SubscribeGroupBlock` одной строкой из ветки
      WordLoaded; подписки wordGroups/groupTree (Collator, лог с
      ID-СПИСКАМИ — ревью Test-1); мутации через `toMembershipMsg`-
      маппер (плоские Msg), event-логи `membership add/remove: …
      outcome=…`.
- [ ] UI: иконка групп в TopBarWidget (actions, перед kebab); блок чипов
      под WordFieldWidget (FlowRow, derived; ОТЛИЧИМЫ от чипов
      компонентов — без trailing-иконки, maxLines=1+ellipsis, ревью
      UX-5/6); `GroupPickerBottomSheetWidget` (ModalBottomSheet +
      LazyColumn с maxHeight, Checkbox; пустой список — «Групп пока
      нет»); строки в core-resources (обе локали).
- [ ] `WordCardUseCase` + app-Impl: 4 метода, возвращают domain-типы
      (`GroupNode`/outcomes, НЕ GroupApiEntity — ревью Arch-2);
      `wordcard/build.gradle` + `:modules:domain:group`.

## Фаза 4. Верификация + мануал + док-синк

- [ ] Тесты последовательно: domain:group (`test`), groupstab, wordcard,
      app; assembleDebug; :app:lintDebug.
- [ ] androidTest на девайсе: GroupMembershipTest + полный регресс
      core-db-impl (был 102/102).
- [ ] Мутационная проверка: ≥5 мутаций (направление toggle, in-flight
      guard, JOIN-check словаря, OR IGNORE→Long, keying merge окон —
      ревью Test-4, порядок applyGroupCounts/applyGroups).
- [ ] `stage5_manual_test.md` — ручники с ЛОГ-КОНТРАКТОМ (D23):
      таблица «событие → строка лога» в шапке; в каждом ТК блок `ЛОГ`
      с ПОЛНЫМ ожидаемым набором строк, включая benign-переэмиссии
      окон соседних групп (Room инвалидирует по таблицам) и легальные
      no-op; критерий провала — конкретный blacklist
      (MembershipFailed/GroupWindowFailed/FATAL + расхождение с блоком
      ЛОГ), НЕ «всё неожиданное» (ревью Test-2).
- [ ] Док-синк: rollout Э5 «Внутри» (add/remove вместо setMembership —
      ревью Arch-4), architecture §2.5+§GroupApi (тот же синк),
      project-architecture (если конвенция уточнится).

## Вне скоупа Э5

- Создание группы из пикера (Э7), импакт удаления (Э6).
- Снятие группы с вкладки «Группы», фильтры, бейджи в «Словах» (v1 out).
- Миграция старых веток wordcard-reducer'а на атомы.
