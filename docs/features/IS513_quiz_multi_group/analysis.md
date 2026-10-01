# IS513 | Анализ: несколько групп для тренировки в квиз-чате

Бриф: [brief.md](brief.md) (Д1–Д7), спека: [quiz-group-filter](../../handbook/specs/quiz-group-filter/spec.md).
Код — master `cd24a2a8`.

## 1. Что есть (по слоям)

| Слой | Сейчас |
|---|---|
| Домен `modules/domain/quiz` | `QuizGroupState(options, selectedGroupId: Long?)`; воронка `resolveQuizGroupState(groupCounts, dictionaryWordCount, persistedGroupId: Long?)` — пометка `isEligible` по порогу и резолв: выбор вне пригодных → `null` («Все»). |
| Pref | `quizGroupPrefKey(type, dict)` = `quiz_group_<тип>_dict_<id>`, значение — один `groupId` строкой, нет ключа = «Все». |
| Store (app) | `QuizGroupSelectionStore.getValidatedSelection(): Long?` (воронка + лог `fallback=none/garbage/dead/below_threshold`), `setSelection(groupId: Long?)` (null стирает ключ), `flowSelection(): Flow<Long?>`. |
| Выборка | `QuizApi.getWriteQuizIds/getEarliestWriteQuizList/getFrequentMistakesWriteQuizList(…, groupId: Long?, coreTypeIds)`; SQL `(:groupId IS NULL OR EXISTS (… wg.group_id = :groupId))`. Резолв группы — внутри `QuizChatUseCaseImpl.getRandomWriteQuizList` через store. |
| Таб «Квизы» | State `selectedGroupId: Long?`; `Msg.PickGroup(quizType, groupId)` → оптимистично + `PersistGroupSelection(groupId)`; подписка `GroupOptions` → `GroupOptionsLoaded(…, selectedGroupId, selectionInvalidated)`, при `selectionInvalidated` редьюсер стирает pref. Сортировка групп — `Collator` в саб-хендлере. |
| Пикер (UI) | `QuizGroupPickerWidget(selectedTitle, selectedGroupId, items, enabled, onPick: (Long?) -> Unit)`: одиночный выбор, клик по пункту закрывает меню, выбранный — жирным. `selectedTitle` считается в `QuizTabScreen` (имя по id или «Все»). |
| Чат | Сабтайтл аппбара: `QuizChatUseCase.getSelectedQuizGroupName(dict): String?` → `Msg.QuizGroupNameLoaded(name)` → `AppBarState.quizGroupName`, null → ресурс `group_all_title`. Порядок групп там не задан — одна группа. |

## 2. Целевое поведение (из брифа)

1. Выбор — только на карточке квиза; чат лишь показывает (Д1).
2. «Все» — пункт сверху, группы — галки; группа ↔ «Все» взаимоисключают; снял последнюю группу → «Все»; пустого выбора нет; пустой набор = «Все» (Д2).
3. Порог на каждую группу, как сейчас; серые некликабельны (Д3).
4. Подпись «Быт +2» на карточке и в сабтайтле: первая по алфавиту + число остальных (Д4).
5. Общий счётчик выбранного не показываем (Д5).
6. Невалидные группы набора молча выпадают, пусто → «Все», очищенный набор сразу пишется (Д6).
7. Новый ключ pref, старый игнорируется (Д7).

## 3. Как делать

### 3.1. Домен (`modules/domain/quiz`)

- `QuizGroupState.selectedGroupIds: Set<Long>` вместо `selectedGroupId: Long?`; пусто = «Все».
- Воронка: `resolveQuizGroupState(groupCounts, dictionaryWordCount, persistedGroupIds: Set<Long>, threshold)` → `selectedGroupIds = persisted ∩ {пригодные}`. Пересечение пусто — «Все». Это и есть Д6 поэлементно.
- **Подпись (Д4) — одна чистая функция в домене**, чтобы таб и чат не разошлись:
  ```kotlin
  /** Подпись выбора: null — «Все»; иначе первая по [order] и сколько ещё. */
  data class QuizGroupLabel(val first: String, val more: Int)
  fun quizGroupLabel(selectedNames: Collection<String>, order: Comparator<String>): QuizGroupLabel?
  ```
  Порядок — `Comparator` вызывающей стороны (Collator, как сейчас у списка пикера; домен pure-JVM, локаль не навязывает). Текст «%1$s +%2$d» — ресурс в каждом UI-модуле (`quiz_group_label_more` в core-resources, общий).

### 3.2. Персист (pref + store)

- Новый ключ `quizGroupsPrefKey(type, dict)` = `quiz_groups_<тип>_dict_<id>` (Д7); старый `quizGroupPrefKey` удалить из кода, ключ в prefs остаётся мусором — принято.
- Формат — id через запятую: `"12,15,40"`; пусто/нет ключа = «Все». Кодек: `split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()`; мусорные токены отбрасываются.
- Store:
  - `getValidatedSelection(type, dict): Set<Long>` — воронка; лог: `raw=… resolved=<ids|all> dropped=<ids|none> fallback=<none|garbage|all_dropped>`.
  - `setSelection(type, dict, groupIds: Set<Long>)` — пусто стирает ключ.
  - `flowSelection(type, dict): Flow<Set<Long>>`.

### 3.3. Выборка (SQL)

- `QuizApi` и три запроса `WordDao`: `groupId: Long?` → `groupIds: List<Long>` (пусто = весь словарь). Room не умеет `:list IS NULL`, поэтому отдельный флаг:
  ```sql
  AND (:allGroups OR EXISTS (
      SELECT 1 FROM lexemes l
      JOIN word_groups wg ON wg.word_id = l.word_id
      JOIN dictionary_groups dg ON dg.id = wg.group_id AND dg.removed_at IS NULL
      WHERE l.id = write_quiz.lexeme_id AND wg.group_id IN (:groupIds)))
  ```
  `allGroups = groupIds.isEmpty()` вычисляет `CoreDbApiImpl`; при `allGroups = true` в `IN` уходит заглушка `listOf(-1L)` (пустой `IN ()` Room отдаёт как ноль строк — безопасно, но явная заглушка понятнее). DAO-сигнатура: `(…, allGroups: Boolean, groupIds: List<Long>, coreTypeIds: List<Long>)`.
- Слово в нескольких выбранных группах — одна строка (`EXISTS`), ключ порции — лексема (§7.1 спеки), повторов нет.
- `QuizChatUseCaseImpl.getRandomWriteQuizList`: `groupIds = store.getValidatedSelection(...)`; лог `groupFilter=<ids|all>`.

### 3.4. Таб «Квизы» (логика)

- State: `selectedGroupIds: Set<Long>` вместо `selectedGroupId`; **явное поле** `selectionLabel: QuizGroupLabel?` (правило «UI-флаги — поля state»), считается атомом при каждом изменении выбора/опций через `quizGroupLabel` с порядком списка опций (они уже отсортированы Collator в сабе).
- Msg: `PickGroup(quizType, groupId)` → два сообщения:
  - `ToggleGroup(quizType, groupId: Long, checked: Boolean)`: `next = checked ? selected + id : selected - id`; пусто → «Все» (Д2: снял последнюю).
  - `PickAll(quizType)`: `next = emptySet()`.
  Guard'ы как сейчас (чужой тип, нет словаря, тот же набор → no-op); иначе оптимистичный state + `PersistGroupSelection(groupIds: Set<Long>)`.
- `GroupOptionsLoaded(…, selectedGroupIds: Set<Long>, selectionInvalidated)`: `selectionInvalidated = persisted != resolved` (выпала хоть одна); редьюсер пишет **очищенный набор** (не всегда «Все», Д6).
- Атомы: `resolveSelection(ids)`, `pickGroups(ids)`, `applySelectionLabel()`; тесты атомов — как сейчас (Noop-логгер).

### 3.5. Пикер (UI)

- `QuizGroupPickerWidget(selectedLabel, selectedGroupIds, items, enabled, onPickAll, onToggle(groupId, checked))`.
- Все пункты, включая «Все», — с галкой (`Checkbox` в `leadingIcon`, цвета явной парой из прецедента `MenuItem.kt` iconDropDowned: `primary`/`onSurface`); меню однородное, взаимоисключение видно глазами: галка «Все» стоит при пустом наборе. Непригодные — серые, галка disabled. Жирное выделение выбранного больше не нужно — его заменяет галка.
- **Меню не закрывается на клик по группе** (мультивыбор: отметить несколько за одно открытие); клик по «Все» — закрывает (выбор сделан).
- Подпись контрола: `selectionLabel?.let { "%1$s +%2$d" / имя при more == 0 } ?: «Все»`.

### 3.6. Чат (сабтайтл)

- `QuizChatUseCase.getSelectedQuizGroupName(dict): String?` → `getSelectedQuizGroupLabel(dict): QuizGroupLabel?` (имена валидированного набора, порядок — Collator, как у пикера).
- `Msg.QuizGroupNameLoaded(name)` → `QuizGroupLabelLoaded(label)`; `AppBarState.quizGroupLabel: QuizGroupLabel?`; `AppBarWidget` форматирует тем же ресурсом, null → `group_all_title`. Лог `subtitle: group=<first+N|all>`.

## 4. Тесты

- **domain `QuizGroupSelectionTest`** (10): переписать на набор — пересечение с пригодными, выпадение мёртвой/усохшей поэлементно, все выпали → пусто; новые — `quizGroupLabel` (пусто → null, одна → more 0, три → первая по компаратору + 2).
- **app `QuizGroupSelectionStoreTest`** (9): новый ключ, кодек CSV (round-trip, мусорные токены, пусто), лог-фолбэки, `setSelection(empty)` стирает.
- **app `QuizChatUseCaseImplTest`**: групповые тесты — `groupIds` в три запроса, пусто → весь словарь; `getSelectedQuizGroupLabel`.
- **quiztab `QuizTabReducerTest`** (16): `PickGroup` → `ToggleGroup`/`PickAll` (добавить, снять, снять последнюю → «Все», «Все» снимает группы, тот же набор no-op), `GroupOptionsLoaded` с частично выпавшим набором → `PersistGroupSelection(очищенный)`, `selectionLabel`.
- **chat** `DatasourceEffectHandlerTest`/`ChatReducerTest`: `LoadQuizGroupName` → label.
- **core-db-impl `QuizGroupFilterDaoTest`** (device): групповые случаи на `groupIds` + новые: две группы — объединение; слово в обеих — одна строка; пусто — весь словарь; пины earliest/mistakes из объединения.
- **Ручник:** выбор нескольких групп (меню не закрывается), «Все» ↔ группы, снял последнюю → «Все», подпись «Быт +2» на карточке и в чате, раунд только из выбранных групп, удаление/усыхание одной из выбранных → выпала, остальные остались, перезапуск — набор сохранён, обновление с 0.1.11 — «Все».

## 5. Спеки

- `quiz-group-filter`: §1 (несколько групп), §5 (набор, `quizGroupLabel`), §6 (новый ключ, CSV, поэлементное закрепление), §7 (SQL `IN` + флаг), §9 (state/Msg/пикер с галками), §10 (подпись «Быт +2»), §11 (логи), §12 (мультивыбор убрать из «вне скоупа»).
- `quiz-chat` §4/§1 — сабтайтл: подпись набора групп.

## 6. Риски

1. **Сброс выбора у юзеров после обновления** (Д7) — осознанно; в ручник — проверка «Все» после обновления.
2. **Room и `IN (:list)` с флагом** — заглушка при `allGroups`; DAO-тест на оба пути.
3. **Меню, не закрывающееся на клик** — `DropdownMenu` M3 не закрывается само, закрытие сейчас явное в `onClick` — просто не вызывать для групп. Проверить на девайсе, что клик по галке и по строке ведут себя одинаково.
4. **Порядок в подписи** — чат и таб должны сортировать одним Collator; функция одна, компаратор передаётся — в тесте закрепить.
5. **Гонка быстрых кликов по галкам** — два `PersistGroupSelection` параллельно, порядок записей не гарантирован; принято как в IS500 и IS511 (эхо подписки выравнивает).
