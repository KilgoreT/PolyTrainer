# IS513 | План: несколько групп для тренировки — до → после → зачем

Бриф: [brief.md](brief.md), анализ: [analysis.md](analysis.md), спека:
[quiz-group-filter](../../handbook/specs/quiz-group-filter/spec.md).

Порядок: П1 → П2 → П3 → П4 → П5 → П6 → П7 → П8 → сборка, unit по модулям,
компиляция androidTest (`core-db-impl`, `quiz/chat`), lint, `installDebug`,
DAO-тест на девайсе, ручник → один коммит (доки — в нём же). Правила:
gradle через `./scripts/cc-build.sh`; sed/find/cat/head/tail/awk/Python
запрещены; на девайс только `installDebug`; KDoc без «IS513»/дат;
quiztab — атомарные экстеншны (`QuizTabStateAtoms`, `logStep`).

---

## П1. Домен (`modules/domain/quiz`)

**Файлы:** `QuizGroupSelection.kt`, `QuizGroupLabel.kt` (новый); тест
`QuizGroupSelectionTest.kt`, `QuizGroupLabelTest.kt` (новый).

**До:** `QuizGroupState(options, selectedGroupId: Long?)`;
`resolveQuizGroupState(groupCounts, dictionaryWordCount, persistedGroupId: Long?, threshold)`
→ `persistedGroupId?.takeIf { пригодна }`.

**После:**

```kotlin
data class QuizGroupState(val options: QuizGroupOptions, val selectedGroupIds: Set<Long>)  // пусто = «Все»

fun resolveQuizGroupState(
    groupCounts: List<QuizGroupCount>,
    dictionaryWordCount: Int,
    persistedGroupIds: Set<Long>,
    threshold: Int = MIN_QUIZ_WORDS,
): QuizGroupState  // selectedGroupIds = persisted ∩ { id пригодных групп }

/** Подпись выбора: первая по [order] и сколько ещё; пусто — null («Все»). */
data class QuizGroupLabel(val first: String, val more: Int)
fun quizGroupLabel(selectedNames: Collection<String>, order: Comparator<String>): QuizGroupLabel?
```

**Зачем.** Д2/Д3/Д6 — одна воронка на набор; Д4 — одна функция подписи
для таба и чата.

**Тесты:** воронка — пустой persisted → пусто; часть мёртвых/усохших →
выпали, остальные остались; все выпали → пусто; чужой id → выпал.
Подпись — пусто → null; одна → `more = 0`; три → первая по компаратору,
`more = 2`; порядок входа не влияет.

---

## П2. Ресурсы

**Файлы:** `core/core-resources/src/main/res/values{,-ru-rRU}/strings.xml`.

**После:** `quiz_group_label_more` = `%1$s +%2$d` (ru/en одинаково).

**Зачем.** Д4: «Быт +2» на карточке и в сабтайтле одним ресурсом.

---

## П3. Персист (pref + store)

**Файлы:** `modules/datasource/prefs/.../QuizGroupPrefKey.kt`,
`app/.../quizgroup/QuizGroupSelectionStore.kt`; тест `QuizGroupSelectionStoreTest.kt`.

**До:** `quizGroupPrefKey` = `quiz_group_<тип>_dict_<id>`, значение —
один id; store `Long?`.

**После:**

```kotlin
/** Набор групп тренировки: id через запятую; нет ключа / пусто = «Все». */
fun quizGroupsPrefKey(quizType: String, dictionaryId: Long): String = "quiz_groups_${quizType}_dict_$dictionaryId"
// старый quizGroupPrefKey — удалить (ключ в prefs остаётся мусором, Д7)

class QuizGroupSelectionStore {
    suspend fun getValidatedSelection(quizType: String, dictionaryId: Long): Set<Long>
    suspend fun setSelection(quizType: String, dictionaryId: Long, groupIds: Set<Long>)  // пусто стирает ключ
    fun flowSelection(quizType: String, dictionaryId: Long): Flow<Set<Long>>
}
// кодек: encode = sorted().joinToString(","); decode = split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
```

Лог чтения: `quizGroupStore: read type=… dict=… raw=… resolved=<ids|all> dropped=<ids|none>`.

**Зачем.** Д6/Д7.

**Тесты:** ключ; round-trip; мусорные токены отброшены; пустая строка /
нет ключа → пусто; `setSelection(empty)` пишет null; воронка в чтении
(мёртвая выпала, живая осталась).

---

## П4. Выборка (SQL)

**Файлы:** `core/core-db-api/.../CoreDbApi.kt` (`QuizApi`),
`core/core-db-impl/.../room/WordDao.kt`, `core/core-db-impl/.../CoreDbApiImpl.kt`;
androidTest `QuizGroupFilterDaoTest.kt`.

**До:** `groupId: Long? = null` в трёх методах; SQL
`(:groupId IS NULL OR EXISTS (… AND wg.group_id = :groupId))`.

**После:** API — `groupIds: List<Long> = emptyList()` (пусто = весь
словарь); DAO — `allGroups: Boolean, groupIds: List<Long>`:

```sql
AND (:allGroups OR EXISTS (
    SELECT 1 FROM lexemes l
    JOIN word_groups wg ON wg.word_id = l.word_id
    JOIN dictionary_groups dg ON dg.id = wg.group_id AND dg.removed_at IS NULL
    WHERE l.id = write_quiz.lexeme_id AND wg.group_id IN (:groupIds)))
```

`CoreDbApiImpl`: `allGroups = groupIds.isEmpty()`, при пустом в DAO
уходит `listOf(NO_GROUP_ID)` (`-1L`) — `IN` синтаксически непустой.

**Зачем.** Порция из объединения выбранных групп; фильтр до окна (§7).

**Тесты (device):** существующие групповые — на `groupIds`; новые: две
группы — объединение; слово в обеих — одна строка; пусто — весь словарь;
пины earliest/mistakes из объединения (глобальный топ вне набора не
попадает).

---

## П5. Use case квиз-чата

**Файлы:** `modules/screen/quiz/chat/.../deps/QuizChatUseCase.kt`,
`app/.../quizchat/QuizChatUseCaseImpl.kt`; тест `QuizChatUseCaseImplTest.kt`.

**До:** `getRandomWriteQuizList` берёт `groupId: Long?` из store;
`getSelectedQuizGroupName(dict): String?`.

**После:**
- `getRandomWriteQuizList`: `groupIds = store.getValidatedSelection(...)`, передаётся в три запроса; лог `groupFilter=<ids|all>`.
- `getSelectedQuizGroupName` → `getSelectedQuizGroupLabel(dictionaryId): QuizGroupLabel?` — имена валидированного набора из `flowQuizGroupCounts(dict).first()`, `quizGroupLabel(names, collatorOrder)`; Collator — `java.text.Collator.getInstance()`, как в саб-хендлере таба.

**Зачем.** Д4 в чате; выборка по набору.

**Тесты:** групповой — `groupIds` во все три запроса; пусто — пустой
список; `getSelectedQuizGroupLabel` — пусто → null, две группы → первая
по алфавиту + 1.

---

## П6. Чат: сабтайтл

**Файлы:** `logic/Message.kt`, `logic/State.kt`, `logic/ChatReducer.kt`,
`logic/DatasourceEffectHandler.kt`, `widget/appbar/AppBarWidget.kt`;
тесты `ChatReducerTest`, `DatasourceEffectHandlerTest` (`FakeUseCase`).

**До:** `Msg.QuizGroupNameLoaded(name: String?)`, `AppBarState.quizGroupName: String?`.

**После:** `Msg.QuizGroupLabelLoaded(label: QuizGroupLabel?)`,
`AppBarState.quizGroupLabel: QuizGroupLabel?`; `AppBarWidget`: `label == null` →
`group_all_title`; `more == 0` → `first`; иначе
`stringResource(quiz_group_label_more, first, more)`. Лог
`subtitle: group=<first+N|all>`.

**Зачем.** Д4.

---

## П7. Таб «Квизы»: логика

**Файлы:** `quiztab/logic/State.kt`, `Message.kt`, `QuizTabReducer.kt`,
`StateAtoms.kt`, `QuizTabSubHandler.kt`, `deps/QuizTabUseCase.kt`,
`app/.../quiztab/QuizTabUseCaseImpl.kt`; тест `QuizTabReducerTest.kt`.

**До:** state `selectedGroupId: Long?`; `Msg.PickGroup(quizType, groupId)`;
`GroupOptionsLoaded(…, selectedGroupId, selectionInvalidated)` при
инвалидации пишет `null`; `PersistGroupSelection(groupId: Long?)`.

**После:**

```kotlin
data class QuizTabState(…, val selectedGroupIds: Set<Long> = emptySet(), val selectionLabel: QuizGroupLabel? = null, …)

data class ToggleGroup(val quizType: String, val groupId: Long, val checked: Boolean) : Msg
data class PickAll(val quizType: String) : Msg
data class GroupOptionsLoaded(…, val selectedGroupIds: Set<Long>, val selectionInvalidated: Boolean) : Msg
data class PersistGroupSelection(val quizType: String, val dictionaryId: Long, val groupIds: Set<Long>) : QuizTabDatasourceEffect, RecoverableEffect<Msg>
```

- `ToggleGroup`: guard'ы (чужой тип, нет словаря); `next = checked ? selected + id : selected - id` (снял последнюю → пусто = «Все»); тот же набор → no-op; иначе `pickGroups(next).applySelectionLabel()` + `PersistGroupSelection(next)`.
- `PickAll`: пусто уже → no-op; иначе `pickGroups(emptySet()).applySelectionLabel()` + persist пустого.
- `GroupOptionsLoaded`: `applyGroupOptions → resolveSelection(ids) → applySelectionLabel() → setCardEnabled`; при `selectionInvalidated` — `PersistGroupSelection(message.selectedGroupIds)` (очищенный набор, Д6).
- Саб: `selectionInvalidated = persisted != resolved.selectedGroupIds`; `useCase.flowGroupSelection(): Flow<Set<Long>>`; `setGroupSelection(…, groupIds: Set<Long>)`.
- Атом `applySelectionLabel()`: имена `selectedGroupIds` в порядке `groupOptions` (уже по Collator) → `quizGroupLabel(names, порядок списка)`.

**Зачем.** Д2/Д4/Д6; подпись — явное поле state.

**Тесты:** Toggle добавить / снять / снять последнюю → «Все»; PickAll
снимает группы; тот же набор no-op; чужой тип / нет словаря no-op;
`GroupOptionsLoaded` частично выпавший → persist очищенного, полностью
выпавший → persist пустого; без инвалидации — без эффекта; `selectionLabel`
после каждого; atoms-тесты на `applySelectionLabel`.

---

## П8. Таб «Квизы»: пикер (UI)

**Файлы:** `quiztab/widget/QuizGroupPickerWidget.kt`, `QuizTabScreen.kt`,
`widget/QuizItemWidget.kt` (превью).

**До:** одиночный выбор, клик закрывает меню, выбранный — жирным;
`selectedTitle` считается в `QuizTabScreen`.

**После:**

```kotlin
fun QuizGroupPickerWidget(
    selectedTitle: String,
    isAllSelected: Boolean,
    selectedGroupIds: Set<Long>,
    items: List<QuizGroupPickerItem>,  // groupId == null — «Все»
    enabled: Boolean,
    onPickAll: () -> Unit,
    onToggle: (groupId: Long, checked: Boolean) -> Unit,
)
```

- У каждого пункта (включая «Все») `Checkbox` в `leadingIcon`, цвета парой `primary`/`onSurface` (прецедент iconDropDowned), непригодные — серый текст, галка и строка disabled; жирное выделение убрать.
- Клик по группе — `onToggle(id, !checked)`, меню **не** закрывается; клик по «Все» — `onPickAll()` и закрытие.
- `selectedTitle` в `QuizTabScreen`: `state.selectionLabel?.let { if (more == 0) first else stringResource(quiz_group_label_more, first, more) } ?: allTitle`.

**Зачем.** Д2/Д4.

**Ручник** (`manual_test.md`, формат `~/.claude/skills/manual-tests`):
M1 после обновления с 0.1.11 — «Все» (Д7); M2 отметить две группы за одно
открытие меню, подпись «Быт +1»; M3 «Все» снимает группы, группа снимает
«Все», снял последнюю → «Все»; M4 раунд только из слов выбранных групп,
сабтайтл чата «Быт +1»; M5 одна из выбранных удалена / усохла → выпала,
остальные остались, pref очищен; M6 перезапуск — набор сохранён; M7 серые
группы не отмечаются.

---

## Доки

- Спека `quiz-group-filter`: §1, §5, §6, §7, §9, §10, §11, §12 (убрать
  мультивыбор из «вне скоупа»); `quiz-chat` — сабтайтл набора групп.
- `docs/Backlog.md` — по итогам ревью.

## Тесты (сводно)

JVM: `QuizGroupSelectionTest`, `QuizGroupLabelTest`, `QuizGroupSelectionStoreTest`,
`QuizChatUseCaseImplTest`, `QuizTabReducerTest`, `ChatReducerTest`,
`DatasourceEffectHandlerTest`. Device: `QuizGroupFilterDaoTest`;
регресс `ChatMessageMotionTest` + `QuestionBubbleTest` (сабтайтл в аппбаре
не задевает ленту, но гоняем). Ручник M1–M7.

## Триаж ревью (2026-10-01) — решения, вшитые поверх пунктов выше

Три ревьюера (данные/UI, архитектура/тесты, регрессы/UX), блокеров нет.
Где пункт плана выше расходится с этой таблицей — действует таблица.

| # | Находка | Решение |
|---|---|---|
| 1 | `quiz/chat` не зависит от `domain/quiz` (A-1, B-1, C-3) | П6: `modules/screen/quiz/chat/build.gradle.kts` + `implementation(project(":modules:domain:quiz"))` — осознанная связь screen → domain |
| 2 | Подпись: компаратор неудобен, имена не уникальны, Collator в двух местах, двойной снапшот в чате (A-6, B-5) | П1: `quizGroupLabel(groups: List<QuizGroup>, selectedIds: Set<Long>): QuizGroupLabel?` — по порядку списка, без компаратора. Сортировку Collator делает store (новый `getValidatedState(type, dict): QuizGroupState`, один снапшот, отсортированные группы) и саб таба; чат берёт `QuizGroupState` из store и зовёт ту же функцию |
| 3 | «+N» обрезается вместе с длинным именем (C-1) | П6/П8: имя — свой `Text` с ellipsis, « +N» — отдельный `Text` вне обрезки (как шеврон «▾»); то же в `AppBarWidget`. Ресурс `quiz_group_label_more` = `+%1$d` |
| 4 | Гонка быстрых тапов в открытом меню (C-2, B-7) | П3: `Mutex` в `QuizGroupSelectionStore.setSelection` — записи строго по порядку эффектов; ручник: три галки быстрыми тапами |
| 5 | Мусор в pref не закрепляется (A-5, B-4) | По Д6 строго: `flowSelection` отдаёт `SelectionRaw(ids: Set<Long>, hasGarbage: Boolean)` (`raw != encode(decoded)`); саб: `selectionInvalidated = hasGarbage \|\| persisted != resolved` |
| 6 | Заглушка `-1L` лишняя (A-2) | П4: без заглушки — `allGroups = groupIds.isEmpty()`, `groupIds` как есть (пустой `IN ()` даёт ноль строк, KDoc DAO); DAO-тест с `emptyList()` + `allGroups = true` |
| 7 | Галка с собственным `onCheckedChange` (A-7, C-4) | П8: `Checkbox(checked, onCheckedChange = null, enabled = isEligible)` — индикатор; клик только `DropdownMenuItem.onClick`; риск 3 снят |
| 8 | Пропущен `quiztab/logic/DatasourceEffectHandler.kt` (B-2) | П7: в файлы; лог `persistGroupSelection: type=… dict=… groups=<ids\|all>` |
| 9 | Инвентарь `QuizChatUseCaseImplTest` (A-3, B-3) | П5: `stubPrefs` → `Set<Long>`; позиционные `null` → `emptyList()` во всех тестах порции/ядер; use case передаёт `groupIds.sorted()` (A-4), стабы — отсортированный список |
| 10 | Guard'ы `PickAll`, устаревший тап по непригодной (B-6) | П7: `PickAll` — «чужой тип», «нет словаря», «уже все»; `ToggleGroup(checked = true)` по id вне пригодных `groupOptions` → `noOp("ineligible group")`; тесты на все |
| 11 | Лог store без причины (C-6, B-8a) | П3: `quizGroupStore: read … resolved=<ids\|all> dropped=<id:dead\|id:below,…\|none> garbage=<bool>` |
| 12 | Чат: эффект/хелпер/атом с «Name» (B-8b, d) | П6: `LoadQuizGroupName` → `LoadQuizGroupLabel`, `loadQuizGroupName()` → `loadQuizGroupLabel()`, `updateQuizGroupName` → `updateQuizGroupLabel`, `ChatAssembly`; в `ChatReducerTest` — новый тест на `QuizGroupLabelLoaded` |
| 13 | Тесты атомов quiztab (B-8c) | П7: новый `QuizTabStateAtomsTest` (Noop-логгер) — `applySelectionLabel`, `pickGroups` |
| 14 | Порядок Msg и `isAllSelected` (B-8e, f) | П7: в sealed `GroupOptionsLoaded` раньше `ToggleGroup`/`PickAll`; П8: пикер без `isAllSelected` — галка «Все» = `selectedGroupIds.isEmpty()` из state (поле уже явное) |
| 15 | Ручник теряет регресс IS500 (C-5) | П8: добавить M8 изоляция набора по словарям; M9 выпавшая не возвращается после «исцеления» (поэлементно); M10 переименование выбранной → подпись; M11 длинное имя + «+N»; M12 много групп — прокрутка меню; M13 быстрые тапы; регресс IS500 M7 (карточка disabled), M8 (сабтайтл до «Начать»), M10 (без мигания). M1: предусловие — `installDebug` с master `cd24a2a8`, выбрать группу, поставить ветку поверх; анти-проверка — в логе нет чтения старого ключа |
| 16 | Доки и строки (C-7) | Спека §2 («Все» = пустой набор) и §9 (галки, `selectionLabel` в state); KDoc `QuizTabState`; `quiz_group_picker_cd` ru → «Выбор групп» |
| 17 | Родительская группа не включает слова подгрупп | Решение юзера: вложенных групп в продукте пока нет и в ближайшее время не будет — не реализуем. Совместимость сохраняется сама: фильтр по набору id, «родитель тянет подгруппы» в будущем = раскрыть набор id до запроса, без смены SQL и pref |
| — | Вне скоупа | Backlog: подпись форматируется в двух UI-модулях; id групп после восстановления бэкапа (IS488) могут указать на чужие группы |

## Риски исполнения

1. Сброс выбора у юзеров после обновления (Д7) — осознанно, M1.
2. `IN (:groupIds)` с флагом и заглушкой — DAO-тест на оба пути.
3. Меню не закрывается на клик по группе — проверить, что галка и строка
   ведут себя одинаково (двойной `onToggle` не прилетает).
4. Порядок в подписи — таб берёт порядок списка опций (Collator в сабе),
   чат — Collator в use case; оба через `quizGroupLabel`.
5. Гонка быстрых кликов по галкам — принята (как IS500/IS511).
