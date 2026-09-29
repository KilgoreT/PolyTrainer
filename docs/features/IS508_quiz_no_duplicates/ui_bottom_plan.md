# IS508 / чат-фикс 3 | План правок: код до → код после → зачем

Формат: триада «до / после / зачем» плюс короткое объяснение
конструкций. Решения — [ui_bottom_brief.md](ui_bottom_brief.md) Д1–Д2,
разбор — [ui_bottom_analysis.md](ui_bottom_analysis.md).

Порядок: П1 → сборка + unit модуля + lint → П2 (ручник M7 на девайсе)
→ П3 (Backlog). Один коммит на фикс. `ChatWidget`, `ChatScreen`,
редьюсер, состояние, тесты — без изменений.

Ревью плана (три агента, 2026-09-28) — два блокера, оба исправлены в
П1: **Б1** `spacedBy(4.dp)` в реверсе прижимает к ВЕРХУ (реверс не
меняет семантику arrangement, KDoc `LazyDsl.kt:372`) → явный
`Alignment.Bottom`; **Б2** реверс переворачивает порядок нескольких
placeable внутри элемента (`LazyListMeasuredItem.kt:121-124`) → `Spacer`
перед пузырём заменён на `padding(top)` у виджета (один корень).
Плюс уточнения M6/M7, превью с фиксированной высотой, Backlog.

---

## П1. `ChatMessageWidget` — лента от низа

**Файл:** `modules/screen/quiz/chat/.../widget/ChatMessageWidget.kt`

**До** (после чат-фикса 2, строки 26-87):

```kotlin
@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
    showUserActions: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    // Последний элемент ленты — блок действий, если он показан, иначе
    // последнее сообщение. Лента пуста только в превью (редьюсер кладёт
    // приветствие до stopLoading) — отрицательный индекс не скроллим:
    // animateScrollToItem требует index >= 0.
    val lastIndex = if (showUserActions) state.list.size else state.list.lastIndex
    LaunchedEffect(state, showUserActions) {
        if (lastIndex >= 0) {
            lazyListState.animateScrollToItem(lastIndex)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        reverseLayout = false
    ) {
        itemsIndexed(
            items = state.list,
            key = { _, item -> item.order.toString() }
        ) { index: Int, item: ChatMessage ->
            
            val needSpaceBeforePrevious = !state.isPreviousHasSameType(index)
            …  // цепочки / аватар / кнопки финала по index
        }
        if (showUserActions) {
            item(key = USER_ACTIONS_KEY) {
                UserActionsWidget(sendMessage = sendMessage)
            }
        }
    }
}
```

**После:**

```kotlin
@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
    showUserActions: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    // Лента от низа (reverseLayout): индекс 0 — нижний элемент. Новое
    // сообщение встаёт под вьюпорт (позиция держится по ключу первого
    // видимого) — докручиваем к нему. На пустой ленте (только превью)
    // animateScrollToItem(0) безопасен: элемент 0 считается видимым,
    // дистанция 0 — снап без исключений.
    LaunchedEffect(state, showUserActions) {
        lazyListState.animateScrollToItem(0)
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(all = 16.dp),
        // Реверс не меняет семантику arrangement: прижатие короткого
        // контента к низу — только явным Alignment.Bottom.
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
        reverseLayout = true
    ) {
        // В реверсе первый элемент — нижний: чипы действий под последним
        // вопросом, над полем ввода.
        if (showUserActions) {
            item(key = USER_ACTIONS_KEY) {
                UserActionsWidget(sendMessage = sendMessage)
            }
        }
        itemsIndexed(
            items = state.list.asReversed(),
            key = { _, item -> item.order.toString() }
        ) { reversedIndex: Int, item: ChatMessage ->
            // Логика цепочек / аватара / кнопок финала — по индексу в
            // оригинальном (хронологическом) списке.
            val index = state.list.lastIndex - reversedIndex

            // Один корень на элемент: в реверсе несколько placeable одного
            // элемента размещаются в обратном порядке — Spacer лёг бы под
            // пузырь. Отступ при смене стороны — модификатором виджета.
            val topSpace = if (state.isPreviousHasSameType(index)) 0.dp else 8.dp

            if (item.isSystemMessage) {
                val notShowAvatar = index < state.list.lastIndex
                        && state.list[index + 1].isSystemMessage
                val isInChain = index > 0
                        && state.list[index - 1].isSystemMessage
                val isLastMessage = index == state.list.lastIndex
                SystemMessageWidget(
                    modifier = Modifier.padding(top = topSpace),
                    showAvatar = notShowAvatar.not(),
                    isInChain = isInChain,
                    showButtons = isLastMessage,
                    message = item,
                    sendMessage = sendMessage
                )
            } else {
                UserMessageWidget(
                    modifier = Modifier.padding(top = topSpace),
                    message = item.message,
                )
            }
        }
    }
}
```

Импорты: добавить `androidx.compose.ui.Alignment`,
`androidx.compose.foundation.layout.padding`; удалить
`androidx.compose.foundation.layout.Spacer` (`height` остаётся — нужен
превью). Превью внизу файла: `ChatMessageWidget(modifier =
Modifier.height(600.dp), …)` — без фиксированной высоты `@PreviewWidget`
меряет ленту по контенту, и прижатие к низу не видно; с высотой превью
показывает и Б1 (контент внизу, пусто сверху), и Б2 (зазоры: между
двумя системными 4dp, между сторонами 12dp, вопрос→чипы 12dp).

**Зачем.** Якорь ленты — низ: короткий чат у поля ввода (проблема 1),
при клавиатуре нижний край остаётся на месте (проблема 2). Д1.

**Конструкции.**
- `reverseLayout = true` — индекс 0 внизу, скролл-координаты от низа;
  `contentPadding` остаётся визуальным (`LazyList.kt:231-238`).
- `spacedBy(4.dp, Alignment.Bottom)` (Б1). Реверс НЕ меняет семантику
  arrangement — KDoc `LazyDsl.kt:372`: «with Arrangement.Top (top)
  123### (bottom) becomes (top) 321###»; умолчание `Arrangement.Bottom`
  при реверсе (`:393-394`) действует только без явного параметра, а
  `spacedBy(space)` = `SpacedAligned` с выравниванием к началу
  (`Arrangement.kt:296-299`). Смещения флипаются дважды
  (`LazyListMeasure.kt:645-651` и `LazyListMeasuredItem.kt:227-231`) —
  визуальная семантика сохраняется. Зазор 4dp между элементами
  остаётся: измерение читает только `spacing`.
- Элемент чипов **до** `itemsIndexed` — в реверсе это нижняя позиция.
  Его `padding(top = 8.dp)` — зазор до вопроса, который теперь идёт
  следующим элементом выше; модификатор внутри одного placeable реверс
  не трогает. Ключ прежний.
- `state.list.asReversed()` — view без копии; ключи `order.toString()`
  те же — Compose сохраняет композицию элементов при смене порядка.
- `index = lastIndex - reversedIndex` — оригинальный индекс; вся
  существующая логика (`list[index ± 1]`, `isLastMessage`) — по нему,
  без переписывания.
- `padding(top = topSpace)` вместо `Spacer` (Б2). Реверс переворачивает
  порядок нескольких placeable ОДНОГО элемента (KDoc
  `LazyListMeasuredItem.kt:121-124`, `place()` `:227-231`) — `Spacer` +
  пузырь как два корня легли бы «пузырь, потом Spacer», и все зазоры
  съехали бы на одну границу. С одним корнем отступы прежние: между
  сторонами 4 + 8 = 12dp, внутри цепочки 4dp, вопрос→чипы 4 + 8 = 12dp.
- `animateScrollToItem(0)` — «к концу ленты» в реверсе; guard
  `lastIndex >= 0` и сам `lastIndex` не нужны. На пустой ленте (только
  превью экрана) безопасно: элемент 0 считается видимым
  (`LazyListScrollScope.kt:42-43`), дистанция 0, снап в (0, 0) без
  исключений — до цикла прокрутки дело не доходит.
- Вложенных `launch` нет (снят в чат-фиксе 2).

---

## П2. Ручник M7 (документ `manual_test.md`)

Правки: в оглавление — `[M7. Лента от низа: чипы у поля ввода,
клавиатура](#m7)`; в M6 — одна строка в «Что доказывает» после
ограничения Р1 и в шаге 6: «Снято чат-фиксом 3 — см. M7 шаг 3»
(прогон M6 не переписывать); новый раздел «## Чат-фикс 3: лента от
низа» с кейсом в формате M1–M6:

**Что доказывает.** Лента привязана к низу: короткий чат у поля
ввода, при клавиатуре конец ленты виден без докрутки; цепочки,
аватары, отступы, кнопки финала и чипы (M6) — без изменений.

**Вход.** Debug-сборка с фиксом; «Show debug» ON (заголовки вопросов
длинные — двух вопросов хватает, чтобы лента стала длиннее экрана);
группа ≥ 5 слов.

**Шаги.**

1. Открой чат (до «Начать»): приветствие — внизу, над кнопкой
   «Начать», не под аппбаром. Взгляни на фон-иллюстрацию: пузырь
   закрывает её низ — оцени, не перекрыт ли важный фрагмент (если да —
   отдельное решение, не этот фикс).
2. «Начать» → пузырь «Начали», первый вопрос и чипы — прямо над полем
   ввода; сверху пусто.
3. Тап в поле ввода → клавиатура: вопрос и чипы над клавиатурой, без
   докрутки. Закрой клавиатуру — на месте.
4. Введи любой ответ, отправь: пузырь оценки и следующий вопрос —
   цепочка из двух системных подряд. Аватар только у нижнего
   (вопроса), у верхнего (оценки) — скругление «в цепочке»; зазор
   между ними мал (4dp), зазор между твоим ответом и оценкой заметно
   больше (12dp). Признак длинной ленты — приветствие ушло за верх.
5. «Пропустить», затем «Показать ответ»: конец всегда у поля ввода,
   новый вопрос и чипы внизу; у вопроса выше экрана видны низ и чипы,
   верх — докруткой вверх (как M6).
6. Прокрути вверх к приветствию, открой клавиатуру: нижний видимый
   пузырь остался у нижнего края (теперь над клавиатурой), верхние
   ушли под аппбар; лента НЕ уехала к чипам. Закрой клавиатуру — тот
   же пузырь снизу. Прокрути вниз — упирается в чипы.
7. Дойди до финала: три кнопки в последнем пузыре внизу, чипов нет.
   «Сводка» → пузырь с кнопками «Продолжить»/«Завершить» внизу над
   полем ввода, начало сводки — прокруткой вверх, чипов нет.
   «Продолжить» → старая лента остаётся выше без разрыва, «Начали» +
   первый вопрос + чипы внизу.
8. Поверни экран — лента в конце, один набор чипов, в ландшафте те
   же ожидания. Верни.

**Ожидание глазами.** Всё содержимое «сидит» на поле ввода; пустое
место — сверху, никогда снизу. Отступы как до фикса.

**Маркеры в логах.**

```
(UI — маркеров нет; anti: adb logcat -d | grep -E "FATAL|non-negative" пуст)
```

**Анти-проверки.**

* Нет пустого места между последним элементом и полем ввода.
* Зазоры не съехали: внутри цепочки системных 4dp, между сторонами
  12dp, вопрос→чипы 12dp (Б2).
* Аватар не у верхнего пузыря цепочки; кнопки финала не у не того
  сообщения.
* Нет `FATAL EXCEPTION`.

**Прогон:** пусто до прогона.

---

## П3. Backlog (`docs/Backlog.md`, «Срочное» → «Квиз-чат»)

**До.** Пункт «UI-баг: клавиатура закрывает последние сообщения чата»
(с дописанной после чат-фикса 2 строкой про чипы); пункт «Поле ввода
и клавиатура прячутся при прокрутке к ранним сообщениям».

**После.** Пункт про клавиатуру удалён (закрыт этим фиксом). В пункт
про поле ввода дописано: «После чат-фикса 3 лента в `reverseLayout`:
«лента в конце» = `!lazyListState.canScrollBackward`; «юзер ушёл от
конца» — порог по смещению в px, не `firstVisibleItemIndex > 0`:
элемент 0 при показанных чипах — сами чипы, и `index > 0` сработает
от микроскролла.»

**Зачем.** Один коммит на фикс, доки вместе с кодом; зацепка для
следующего пункта — чтобы не выяснять влияние реверса заново.

---

## Тесты

Новых unit-тестов нет: редьюсер и состояние не меняются, вся правка —
раскладка одного composable; индексная арифметика и зазоры (Б2)
проверяются превью `ChatMessageWidget` (высота 600dp) и шагом 4 M7.
Прогон перед ручником, всё через `./scripts/cc-build.sh`:
`:modules:screen:quiz:chat:testDebugUnitTest` (регресс — все unit
модуля, ~41, из них ChatReducerTest 12), `assembleDebug`,
`:app:lintDebug`.
