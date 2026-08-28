# IS493 / Э6 — Design tree

Импакт удаления группы + опция «удалить вместе со словами».

## Решения пользователя (2026-08-28)

- **В1.** Никакой математики «единственная группа»: при ≥1 слова в группе
  конфирм показывает ЧИСЛО СЛОВ ГРУППЫ и что с ними будет; в каких ещё
  группах они состоят — не считаем.
- **В2.** Галка «Удалить вместе со словами» в первом конфирме
  (по умолчанию снята). Галка ОТМЕЧЕНА → подпись конфирма МЕНЯЕТСЯ:
  «с группой будут удалены N слов».
- **В3.** Подтверждение при отмеченной галке → ВТОРОЙ отдельный
  диалог-переспрос; его подтверждение — деструктивное удаление слов.
- Пустая группа (count=0) — конфирм прежний, без текста и без галки.

## Флоу (закреплён юзером)

1. Kebab → «Удалить» → конфирм.
2. count=0 → прежний «Удалить группу?».
3. count≥1, галка снята → текст «В группе слов: N — при удалении группы
   они останутся в общем списке» → «Удалить» = сегодняшнее поведение
   (soft-delete группы + hard-delete её membership).
4. Галка отмечена → текст «С группой будут удалены слова: N» →
   «Удалить» → второй диалог: «Из словаря будет удалено слов: N — из
   общего списка и из всех групп» → «Удалить всё».
5. Подтверждение переспроса → транзакция deleteGroupWithWords.
6. Любая отмена (dismiss любого диалога) — ничего не удалено; отмена
   переспроса закрывает ВСЁ (не возврат к первому конфирму).

## D30. Данные

### D30.1 N — живой счётчик, запросов при RequestDelete нет

N = `GroupUiItem.count` (уже в state из slice, живой). Конфирм рисует
его из state — вставка/снятие слова под открытым конфирмом обновит
цифру сама (подписка). Отдельного `deleteImpact`-запроса НЕТ.

### D30.2 Транзакция deleteGroupWithWords (§2.4)

```kotlin
suspend fun deleteGroupWithWords(groupId: Long): DeleteGroupWithWordsOutcome
// Success(deletedWords: Int) | NotFound
```

Внутри одной `immediateTransaction`:
1. liveness-check группы (`getLivingGroupById`) — null → NotFound
   (лог `delete-with-words outcome: NotFound` обязателен — ревью D32);
2. `wordIdsOfGroup(groupId)` — id слов по membership;
3. чистка легаси-`samples` (ревью Data-1: таблица БЕЗ FK — каскад её не
   тронет, а одиночный deleteWordSuspend чистит руками — семантика
   обязана совпадать): `DELETE FROM samples WHERE lexemeId IN
   (SELECT id FROM lexemes WHERE word_id IN (:chunk))`;
4. hard-delete слов (`DELETE FROM words WHERE id IN (:chunk)`; FK
   CASCADE подтверждён ревью по схеме: words→lexemes→component_values,
   lexemes→write_quiz, words→word_groups ВСЕХ групп);
5. soft-delete группы + hard-delete остатков её membership
   (идемпотентно — каскад уже вычистил);
6. Success(deletedWords = ids.size).

Чанкование: `chunkSize` — параметр реализации (`@VisibleForTesting`,
дефолт 999): androidTest с chunkSize=3 на 7–8 словах пересекает границу
дважды (ревью Data-2 — иначе чанк-арифметика мёртвый путь в тестах).

Число удаляемых — ФАКТ на момент транзакции (конфирм мог устареть на
гонке — удаляем актуальное, N в UI живой).

### D30.3 DAO

`wordIdsOfGroup(groupId): List<Long>` (прямое чтение word_groups);
`deleteWordsByIds(ids): Int` (list-форма). Каскад words→lexemes/
word_groups уже в схеме (FK), новых индексов нет.

## D31. Groupstab: state/msg/атомы

### D31.1 State — конфирм становится объектом

`confirmDeleteGroupId: Long?` →

```kotlin
val confirmDelete: ConfirmDeleteState? = null

data class ConfirmDeleteState(
    val groupId: Long,
    /** Галка «удалить вместе со словами». */
    val deleteWords: Boolean = false,
    /** Второй диалог-переспрос открыт (деструктив). */
    val isRecheckOpen: Boolean = false,
)
```

### D31.2 Атомы (StateAtoms)

- `askDeleteConfirm(groupId)` — ConfirmDeleteState(groupId) (свежий:
  галка всегда снята при повторном открытии);
- `toggleDeleteWords()` — инверсия галки; лог СО ЗНАЧЕНИЕМ
  (`deleteWords=true|false`, ревью UX-5); **no-op при
  `isRecheckOpen`** (ревью Mate-2: тап по чекбоксу и «Удалить» почти
  одновременно — оба Msg в очереди; состояние
  `isRecheckOpen && !deleteWords` становится недостижимым);
- `openDeleteRecheck()` — isRecheckOpen=true (no-op без конфирма);
- `closeDeleteConfirm()` — null (закрывает ОБА диалога);
- `closeConfirmForDeadGroup()` — конфирм на мёртвой (нет в живых groups)
  группе закрывается; шаг цепочки SliceLoaded ПОСЛЕ applyGroups (ревью
  Mate-1: иначе конфирм висит на трупе, а lookup N проваливается).

### D31.3 Msg / ветки

- `ToggleDeleteWords` — галка.
- `ConfirmDelete` (существующий) — ветвится в reducer, **ПОРЯДОК ВЕТОК
  ЗАКРЕПЛЁН (ревью Mate-2)**:
  1. конфирм null → no-op;
  2. `isRecheckOpen` → closeDeleteConfirm + эффект
     `DeleteGroupWithWords(groupId)` — ПЕРВАЯ содержательная проверка;
  3. `deleteWords && count > 0` → `openDeleteRecheck()` (БЕЗ эффекта);
     guard по живому счётчику (ревью Mate-3): count упал до 0 под
     отмеченной галкой → галка игнорируется, обычный путь;
  4. иначе → closeDeleteConfirm + эффект `DeleteGroup` (как сегодня).
- `DismissDelete` (существующий) — closeDeleteConfirm (оба диалога).
- Итог `DeleteGroupWithWords` — плоский no-op-Msg (список перерисуют
  живые подписки; прецедент DeleteOutcomeMsg), исключение →
  GroupMutationFailed.

### D31.4 UI

НОВЫЙ диалог не нужен (ревью UX-2): у AlarmDialogWidget контент —
свободный ColumnScope-слот, кнопки с явными error/onError.

Первый диалог: рендер при `confirmDelete != null && !isRecheckOpen`
(взаимоисключение — ревью Mate-4, иначе двойной scrim); заголовок
прежний; при count≥1 — динамический текст ПО ГАЛКЕ (снята: «В группе
слов: N. При удалении группы они останутся В СЛОВАРЕ» — НЕ «в общем
списке», ревью UX-3: та формулировка ложно намекала на вылет из других
групп; отмечена: «С группой будут удалены слова: N») + Checkbox
«Удалить вместе со словами»; count=0 — прежний вид без текста/галки.

Второй диалог: рендер при `isRecheckOpen`; текст «Из словаря будет
удалено слов: N — целиком, из всех групп. Отменить будет нельзя»;
кнопка «Удалить всё» **появляется disabled на ~500 мс** (ревью UX-1:
кнопка рендерится в той же точке экрана, где была «Удалить» первого
диалога — дребезг/двойной тап подтверждал бы деструктив не читая;
LaunchedEffect-таймер в диалоге, AlarmButtonWidget.enabled).

`isFabVisible` handle → `confirmDelete == null`. Тексты подписи галки и
динамики — явный `color = MaterialTheme.colorScheme.secondary`
([[explicit-colors-new-widgets]]). Формат «слов: N» — обход plurals
(U-7). Все 6 новых строк — ОБЕ локали (values/ EN — дефолтная).

## D32. Лог-контракт

- `###GROUPS###` шаги атомов: `toggleDeleteWords | deleteWords=b`
  (значение, не факт инверсии — ревью UX-5), `openDeleteRecheck`,
  `closeDeleteConfirm`, `closeConfirmForDeadGroup`; эффект
  `DeleteGroupWithWords(groupId=N)`; handler:
  `delete-with-words outcome: Success(deletedWords=N)` И
  `outcome: NotFound` (гонка — тихое «ничего» обязано отличаться в
  логе от пропавшего эффекта).
- Blacklist прежний + `delete-with-words failed`.
- Анти-инвариант ручников: `DeleteGroupWithWords` в логе БЕЗ
  предшествующего `openDeleteRecheck` = провал (деструктив мимо
  переспроса невозможен).

## Риски

- Деструктив: удаление слов НЕОБРАТИМО (undo нет — как у удаления слова
  из карточки, прецедент поведения).
- Гонка конфирма: N устарел → удаляется факт на момент транзакции;
  расхождение N-в-диалоге и фактического — допустимо (живая цифра
  минимизирует окно).
- Bind-лимит 999 при большой группе — чанковать DELETE.
