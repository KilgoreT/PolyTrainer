# Спецификация: группы (подсловари) словаря

Канон фичи IS493 **по факту реализации v1** (2026-08-28). Покрывает домен,
данные и логику двух экранных поверхностей: вкладка «Группы» таба словаря и
блок групп карточки слова. UI-раскладка — [ui.md](ui.md).

Историческая спека-проект `docs/features/IS493_dictionary_sections/spec.md`
устарела (описывала дерево подгрупп) и помечена superseded.

---

## 1. Концепция

**Группа** — структурная единица словаря (подсловарь): выделяет часть слов
словаря. Это НЕ тег: группа — способ структурировать словарь, а не метка.
Слово может состоять в нескольких группах и всегда остаётся в главном
словаре (группа ссылается на слово, не владеет им).

**В v1 вложенности НЕТ** (решение 2026-08-23): группы — плоский список
корневых узлов, каждая группа хранит слова. «Папки» (контейнеры групп) —
отдельная будущая фича; текущая оставляет задел в схеме (`parent_group_id`,
`kind`) и в домене (рекурсия `buildDisplayTree`, outcome-ветки
`ParentNotFound`/`ParentHasWords`) — заделы не развиваются и не удаляются,
их судьбу решает фича папок.

**Закон трёх пластов.** Каждая колонка БД принадлежит ровно одному пласту:
структура (группы, membership) / контент (слова, лексемы, значения, схема
компонентов) / пользовательское состояние (прогресс, статистика,
audit-даты). Публикуются пласты 1–2; пласт 3 — никогда. Группы — пласт 1.

**Группы ≠ компоненты.** Компоненты (IS486) живут на **лексеме**, группы —
на **слове**. Модели не пересекаются.

## 2. Термины

- **Группа** (`Group`) — живая строка `dictionary_groups`; в UI и коде —
  только «группа» («раздел» — синоним в обсуждениях).
- **Membership** — факт «слово состоит в группе» (строка `word_groups`).
- **Живая группа** — `removed_at IS NULL`. Все чтения и мутации работают
  только с живыми группами.
- **«Все»** (`AllWords`) — единственная виртуальная группа (НЕ строка БД):
  первая на вкладке, содержит ВСЕ слова словаря (включая разложенные по
  группам). Имя — ресурс UI (`group_all_title`), локализуемо;
  kebab/действий нет.
- **DisplayTree** — модель отображения вкладки: «Все» + отсортированные
  группы со счётчиками.
- **Окно** — живой предикатный запрос первых `limit` слов узла
  (`LIMIT`, id DESC); «Ещё» расширяет `limit`. Paging 3 не используется.

## 3. Доменная модель (`modules/domain/group`)

Модуль pure-JVM, zero-deps; типы границы (`MembershipEntry`, `GroupNode`)
не пускают Api-entity data-слоя в screen-модули.

### Имя группы

- `normalizeGroupName = trim + NFC` (композиция диакритики: разложенное
  «й» → U+0439). Хранится и сравнивается ТОЛЬКО нормализованная форма;
  `NameCheck.Valid.normalizedName` — единственный источник имени для
  записи. Та же функция — для будущего импорта публикации.
- `validateGroupName(raw, livingSiblingNames, reservedNames, locale)` —
  чистая, вызывается ИЗ транзакции data-слоя. Сравнение —
  case-insensitive по locale (единый `Locale.getDefault()` вызывающей
  стороны).
- **Self-exclusion при rename:** вызывающий обязан исключить саму
  переименуемую группу из сиблингов — смена регистра собственного имени
  легальна.
- **Резерв имён:** имена группы «Все» ВСЕХ локалей ресурса («Все», «All»,
  регистронезависимо). Набор инжектится параметром (domain zero-deps);
  app собирает его из ресурсов обеих локалей и передаёт конструкторно в
  data-слой (`RoomComponent.factory`).
- UNIQUE-индекса в БД нет (soft-delete мешает): уникальность живых
  имён — только валидацией в транзакции.

### DisplayTree

`buildDisplayTree(slice, groups, comparator)` — чистая сборка:

- «Все» = distinct `wordId` среза с сохранением порядка первого вхождения
  (глобальный порядок даёт slice — id DESC, как на вкладке «Слова»);
  дедуп обязателен — слово в двух группах даёт две строки среза.
- Группы: сиблинги отсортированы `comparator` (locale-aware `Collator`
  инжектится UI-слоем); `directWords` — distinct по группе;
  `subtreeWordCount` — distinct по поддереву (в v1 = directWords.size).
- **Combine-skew drop:** membership с `groupId`, отсутствующим во входных
  группах, отбрасывается — слово остаётся в «Все»; transient до
  следующей эмиссии.
- **Fail-soft:** дерево строится только от корней — узел с битым
  parent-ref не попадает в обход (пропуск с поддеревом).

### Outcomes

Typed sealed-исходы каждой мутации — `GroupOutcomes.kt`:

| Операция | Исходы |
|---|---|
| create | `Success(groupId)` \| `EmptyName` \| `DuplicateSibling` \| `ReservedName` \| `ParentNotFound`* \| `ParentHasWords`* |
| rename | `Success` \| `EmptyName` \| `DuplicateSibling` \| `ReservedName` \| `NotFound` |
| delete | `Success` \| `NotFound` |
| deleteGroupWithWords | `Success(deletedWords)` \| `NotFound` |
| addWordToGroup | `Added` \| `AlreadyIn` \| `GroupNotFound` \| `WordNotFound` |
| removeWordFromGroup | `Removed` \| `NotFound` (идемпотентный успех) |

\* — недостижимые в v1 заделы под папки. `NotFound` везде = отсутствует ∪
soft-deleted. NotFound-исходы membership тихие: галочку/чипы поправляет
живая подписка.

## 4. Инварианты

1. **Единство словаря:** слово и группа в membership — из одного словаря
   (проверка `getWordDictionaryId` + `getLivingGroupByIdInDict` в
   транзакции add).
2. **Плоскость v1:** все группы корневые (`parent_group_id IS NULL`),
   `kind = 'GROUP'` у всех строк; вложенность не создаёт ни одна операция.
3. **Имя:** непустое; уникально среди ЖИВЫХ групп словаря
   (case-insensitive, locale-aware); не входит в резерв «Все» всех
   локалей; хранится нормализованным (trim+NFC).
4. **Liveness membership:** вставка — только на живую группу словаря
   слова; все read'ы `word_groups` JOIN'ят живые группы.
5. **Транзакционная граница:** каждая мутация = чтение актуального
   состояния → валидация → запись, целиком одной
   `useWriterConnection { immediateTransaction }` в `CoreDbApiImpl`
   (deferred не защищает — read-фаза без write-lock). Use case — тонкая
   делегация без валидаций. Конкурентность тестами не гоняется —
   покрыт последовательный путь (осознанное ограничение).
6. **«Все» не в БД:** виртуальна, действий не имеет, имя — ресурс UI.
7. **Слово всегда в словаре:** membership не влияет на `words`; удаление
   группы (без галки) слова не трогает.

## 5. API данных (`CoreDbApi.GroupApi`)

Мутации (suspend, транзакции §4.5):

- `createGroup(dictionaryId, name, parentId = null)` — v1 только корневые.
- `renameGroup(groupId, name)` — self-exclusion.
- `deleteGroup(groupId)` — soft-delete группы + hard-delete её
  membership'ов.
- `deleteGroupWithWords(groupId, chunkSize = 999)` — ДЕСТРУКТИВ, см. §6;
  `chunkSize` — только для тестов границы чанка (bind-лимит SQLite).
- `addWordToGroup(wordId, groupId)` — порядок проверок: слово →
  группа-в-словаре-слова → `INSERT OR IGNORE` (результат −1 = AlreadyIn).
- `removeWordFromGroup(wordId, groupId)` — rowcount 0 = NotFound.

Чтения (живые Flow, Room-инвалидация):

- `membershipSlice(dictionaryId)` — id-срез одним запросом; наблюдает
  ТРИ таблицы (`words`, `dictionary_groups`, `word_groups`); слово вне
  живых групп — `(wordId, null)`; порядок id DESC.
- `groupTree(dictionaryId)` — живые группы словаря.
- `wordGroups(wordId)` — живые группы слова (чипы/пикер карточки);
  наблюдает `word_groups` + `dictionary_groups` — rename/delete группы
  переэмичивает; сортировка Collator'ом на вызывающей стороне.
- `flowGroupWordsWindow(groupId, limit)` — живое окно слов группы
  (membership ∩ words, id DESC LIMIT), зеркало `flowTermsWindow` «Все».

Резерв имён и locale — конструкторная инъекция impl, в сигнатурах их нет.

## 6. Удаление и каскады

- **Обычное удаление группы:** одна транзакция — soft-delete строки
  (`removed_at`) + hard-delete её `word_groups`. Слова НЕ тронуты.
- **Деструктив `deleteGroupWithWords`:** одна транзакция —
  liveness-check группы → `wordIdsOfGroup` → по чанкам `chunkSize`:
  явная чистка legacy-`samples` (таблица БЕЗ FK на words!) →
  hard-delete слов (`DELETE FROM words WHERE id IN …`; FK CASCADE чистит
  лексемы, значения, квиз-статистику и membership ВСЕХ групп) → удаление
  самой группы. `Success.deletedWords` — факт на момент транзакции
  (может отличаться от цифры конфирма при гонке — TOCTOU принят).
- **Удаление слова:** FK CASCADE чистит `word_groups` — без кода.
- **Удаление словаря:** FK CASCADE сносит группы (включая self-FK) и
  membership.
- Soft-deleted строки `dictionary_groups` копятся — принято (низкая
  кардинальность, вакуум не нужен).

## 7. Хранение

```
dictionary_groups
  id                PK autoincrement
  dictionary_id     FK → dictionaries.id  CASCADE, NOT NULL, INDEX
  parent_group_id   FK → dictionary_groups.id  CASCADE, NULL, INDEX
                    -- v1: всегда NULL; задел фичи папок
  name              TEXT NOT NULL         -- нормализованное (trim+NFC)
  kind              TEXT NOT NULL DEFAULT 'GROUP'
                    -- задел фичи папок: тип узла ЯВНЫЙ, чтобы потом не
                    -- угадывать тип пустых узлов по живым данным
  created_at / updated_at  NOT NULL       -- updated_at трогает только rename
  removed_at        NULL                  -- soft-delete (audit, не undo)

word_groups
  word_id           FK → words.id             CASCADE, NOT NULL
  group_id          FK → dictionary_groups.id CASCADE, NOT NULL, INDEX
  created_at        NOT NULL
  PRIMARY KEY (word_id, group_id)           -- первая не-autoincrement PK
```

- Поля `position` НЕТ: порядок групп — алфавитный, locale-aware Collator
  в Kotlin (не SQLite NOCASE — кириллица). Rename может передвинуть
  группу в списке — принятая цена.
- `word_groups`: ровно один индекс `index_word_groups_group_id` — PK
  покрывает префикс `word_id`, «симметричный» индекс Room не
  экспортирует.
- `words` не меняется.

## 8. Миграция 12→13

`Migration_012_to_013` — только DDL, обе таблицы создаются сразу в
финальной форме и пустыми. **Одна миграция на всю фичу**: между этапами
релизов не было, версия не растёт (концепт-правка 2026-08-06; `kind`
добавлен правкой этой же миграции). DDL выверен против экспорта
`13.json`; `IF NOT EXISTS` defensive; идемпотентна по построению.
Тесты: изолированный + chained 11→13 + каскады + `kind`-дефолт
(`MigrationFrom12to13`).

## 9. Вкладка «Группы» (`modules/screen/groupstab`)

Живёт в host-модуле `vocabulary` (TabRow «Слова | Группы»); host резолвит
`dictionaryId` и раздаёт вкладкам слотом (`DictionarySlot`), FAB
контекстный (на «Группах» — создать группу), ActionMode — только у
«Слов». Reducer — по конвенции атомарных экстеншнов
(`project-architecture.md` §Reducer-конвенция): Msg → видимая цепочка
атомов `StateAtoms`, каждый атом логируется.

### State (`GroupsTabState`)

- `isLoading` / `hasNoDictionary` / `dictionaryId`;
- `allNode: AllNodeState?` — «Все»: count, isExpanded, окно
  (`window`/`loadedWords`/`isWindowLoading`/`hasMore`);
- `groups` (отсортированы из DisplayTree) и `visibleGroups` (под
  live-фильтром шторки создания);
- `sheet: GroupSheetState?` — шторка create/rename: `mode`, `input`,
  inline-`error` (EMPTY/DUPLICATE/RESERVED), `isSubmitting`;
- `errorSnackbar` — ТОЛЬКО путь гонки dismiss (шторка уже закрыта);
- `confirmDelete: ConfirmDeleteState?` — `groupId`, галка `deleteWords`,
  `countdownLeft` (пауза осмысления);
- `openMenuGroupId` — kebab;
- `expandedGroupWindows: Map<Long, GroupWindowState>` — окна раскрытых
  групп; ключ в карте = группа раскрыта; `window == 0` — раскрыта
  пустой (заглушка, подписки нет).

### Ключевые правила reducer'а

- **Окна per-группа:** подписка окон — merge (НЕ combine) отдельных
  Flow c per-flow catch; эффекты `SetGroupWindow(groupId, limit?)` /
  `ClearGroupWindows` (смена словаря). Окно «Все» — отдельный
  `SetWindow(limit?)`. Шаг окна `CHUNK_SIZE = 10`.
- **`applyGroupCounts` идёт ДО `applyGroups`** в цепочке `SliceLoaded`
  (дельта считается от старого count): автооткрытие окна 0→N
  (CHUNK_SIZE), закрытие N→0, компенсация роста, пересчёт hasMore.
  Полная цепочка: `hideLoading → applyGroupCounts → applyGroups →
  purgeDeadExpanded → closeConfirmForDeadGroup → refreshVisibleGroups →
  widenWindowForGrowth → applyAllCount`.
- **Мутации имени:** шторка шлёт единый `SubmitSheet`; intent → effect
  маппит `GroupSheetState.toMutationEffect` (mode в state); outcome →
  ПЛОСКИЙ Msg маппит handler ДО отправки (`toMutationMsg`):
  `MutationApplied` / `MutationRejected(error)` / `MutationIgnored`;
  исключение — `GroupMutationFailed`. Reducer доменные sealed'ы не
  разворачивает.
- **Конфирм удаления (единый диалог):** guard'ы — `ConfirmDelete` при
  `countdownLeft > 0` → no-op (reducer не доверяет UI); галка при
  count=0 игнорируется (обычный delete); конфирм мёртвой группы
  закрывает slice (`closeConfirmForDeadGroup`). Галка →
  `StartDeleteCountdown(DELETE_COUNTDOWN_SEC = 5)`; тики
  (`DeleteCountdownTick`) шлёт effect handler (не LaunchedEffect);
  снятие галки / dismiss / смена словаря → `CancelDeleteCountdown`.
  Исходы удалений state не трогают — список обновляет живая подписка.
- Ошибки подписок (`SliceLoadFailed`/`WindowLoadFailed`/
  `GroupWindowFailed`) гасят спиннеры, подписки живы.

## 10. Блок групп карточки слова (`modules/screen/wordCard`)

`GroupsBlockState` внутри `WordCardState`; атомы — `GroupBlockAtoms :
ReducerLogging` (тег WORDCARD), reducer и тесты наследуют.

- `dictGroups` — живые группы словаря (список пикера, Collator
  handler'а); `wordGroupIds` — членства слова; `isPickerOpen`;
  `inFlight: Set<Long>` — keyed in-flight записи.
- **Чипы — derived:** `dictGroups ∩ wordGroupIds` (порядок словарного
  списка). Показываются ВНУТРИ карточки слова, под строкой
  «слово + дата | флаг».
- **`wordGroupIds` меняет ТОЛЬКО подписка** `wordGroups(wordId)`:
  оптимистичных правок нет, галочка/чип — всегда факт БД (ошибка записи
  откатывается сама).
- **Подписки** (`GroupBlockFlowHandler`) стартуют по
  `SubscribeGroupBlock` из ветки `WordLoaded` (прецедент
  AvailableComponentTypesFlowHandler).
- **Toggle:** тап по галочке → `markMembershipInFlight` + эффект
  мутации (направление знает ветка); исход → `MembershipDone(groupId)`
  (ВСЕ исходы, включая NotFound — тихие) → `clearMembershipInFlight`
  БЕЗ guard'а на пикер (Done после dismiss обязан чистить).
- **Flush-on-back:** in-flight membership участвует в
  `hasInFlightCommits` — выход из карточки ждёт запись;
  `isGuardedByPending` гейтит `OpenGroupPicker`/`Toggle`.
- Пикер: плоский список групп словаря с чекбоксами, без «Все»;
  создание группы из пикера — вне v1 (backlog).

## 11. Логирование

Конвенция `ReducerLogging` (`modules/core/mate`): фичевые теги —
`###GROUPS###` (вкладка) и тег WORDCARD (карточка); под одним тегом —
`Reduce ---message---`, `Reduce ---step---: атом | k=v`, `no-op | reason`
и event-логи handler'ов (effects/outcomes/slice/window counts) — весь
флоу читается одним grep. Словарь маркеров и анти-инварианты — в
`docs/features/IS493_dictionary_sections/stage5_manual_test.md`.

## 12. Публикация (семантика зафиксирована, фича позже)

- **П1:** импорт — всегда в текущий словарь; каталог — изнутри словаря,
  фильтр по языку.
- **П2:** совпадение текста слова (trim + case-insensitive, Kotlin
  locale-aware) — сигнал для единого preview-ревью (per-word
  включить/исключить); отказ = слова из пакета нет нигде.
- **П3:** импортированное слово — всегда новая строка `words` (дубли
  легальны); импорт — чисто аддитивная вставка; reuse/склейки нет.
- **Пакет:** группы (имена, без порядка) + membership + слова + лексемы +
  значения + подмножество схемы компонентов (builtin — по `system_key`
  типов И опций) + пара языков «изучаемый → перевод». Audit-колонки,
  «Все», user-state — не входят.
- **Чек-лист v1:** не вводить уникальность текста слова; уникальность
  имён групп — только валидацией; самодостаточность словаря;
  идентичность builtin — system_key; «Все» не в БД; язык словаря не
  терять; прогресс в пласте 3; UUID/provenance не вводить.

## 13. Вне скоупа v1

- **Папки (контейнеры групп) и любая вложенность** — отдельная будущая
  фича (решений по форме нет; заделы см. §1, §7).
- Создание группы из пикера карточки (бывший Э7 — Backlog).
- Живая подписка карточки на слово (Backlog).
- Тренировка по группе; reparent; фильтр по группе на «Словах»; снятие
  слова с группы на вкладке «Группы»; бейджи групп в списке «Слова»;
  публикация целиком (П4–П6).

## 14. Ссылки

- Код: `modules/domain/group`, `core/core-db-api` (`GroupApi`),
  `core/core-db-impl` (`GroupApiImpl`, `GroupDao`,
  `Migration_012_to_013`), `modules/screen/groupstab`,
  `modules/screen/wordCard` (`mate/GroupsBlock.kt`),
  `modules/core/mate/ReducerLogging.kt`.
- Конвенция reducer'ов: `docs/handbook/guides/project-architecture.md`
  §Reducer-конвенция.
- Ручники и словарь логов:
  `docs/features/IS493_dictionary_sections/stage5_manual_test.md`.
- История решений фичи: `docs/features/IS493_dictionary_sections/`
  (brief, architecture, rollout_stages, stage*-артефакты).
