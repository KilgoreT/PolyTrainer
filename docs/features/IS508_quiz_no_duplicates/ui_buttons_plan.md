# IS508 / чат-фикс 2 | План правок: код до → код после → зачем

Формат: каждая правка — триада «до / после / зачем» плюс короткое
объяснение конструкций. Решение — вариант А анализа
([ui_buttons_analysis.md](ui_buttons_analysis.md) §5): кнопки
«Показать ответ» / «Пропустить» — отдельный элемент `LazyColumn`,
показывается по существующему флагу `ChatState.showUserActions`.
Модель сообщений, `Msg`, редьюсер не трогаются.

Ревью плана (три агента, 2026-09-28) — принято с правками:
**Р1** чипы под клавиатурой — принимается как ограничение до фикса
клавиатуры (M6 + Backlog, П5); **Р2** отступ чипов `top = 8.dp`;
**Р3** пин-тест редьюсера на инвариант флага (П6).

Порядок: П1 → П2 → П3 → П6 → сборка + unit + lint → П4 (ручник на
девайсе) → П5 (Backlog). Один коммит на фикс.

---

## П1. Виджет «действия по вопросу» (новый файл)

**Файл:** `modules/screen/quiz/chat/.../widget/button/UserActionsWidget.kt`

**До.** Кнопки собираются в `Row` прямо в `ChatWidget.kt:50-60`:

```kotlin
            if (state.showUserActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement
                        .spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ShowAnswerButtonWidget { sendMessage.invoke(it) }
                    SkipButtonWidget { sendMessage.invoke(it) }
                }
            }
```

**После.** Тот же `Row` — отдельным виджетом, без внешних отступов
(их даёт `contentPadding` ленты):

```kotlin
/**
 * Действия по текущему вопросу — элемент ленты чата, прокручивается
 * вместе с сообщениями. Показ/скрытие — по `ChatState.showUserActions`.
 */
@Composable
fun UserActionsWidget(
    modifier: Modifier = Modifier,
    sendMessage: (Msg) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.Bottom,
    ) {
        ShowAnswerButtonWidget { sendMessage(it) }
        SkipButtonWidget { sendMessage(it) }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxWidth().background(color = Color.Gray)) {
            UserActionsWidget {}
        }
    }
}
```

**Зачем.** Элемент ленты должен быть самостоятельной composable-
единицей с превью, как остальные виджеты модуля (`SystemMessageWidget`,
`UserMessageWidget`); `ChatWidget` перестаёт знать о составе кнопок.

**Конструкции.** `padding(end = 16.dp)` убран — лента уже имеет
`contentPadding = PaddingValues(all = 16.dp)`, иначе чипы уедут на 32dp
от края. `padding(bottom = 8.dp)` убран — низ ленты отбивает тот же
`contentPadding`. `padding(top = 8.dp)` + `spacedBy(4.dp)` ленты =
12dp от пузыря вопроса — столько же, сколько между сообщениями разных
сторон (`spacedBy 4` + `Spacer 8` внутри элемента,
`ChatMessageWidget.kt:51-54`): чипы читаются как «ответная» сторона
(Р2). Геометрия у конца ленты: было «вопрос → 16 → чипы → 8 → поле»,
станет «вопрос → 12 → чипы → 16 → поле» — зазор до поля ввода растёт
на 8dp, это осознанно (не заводить как баг на M6).

---

## П2. Элемент действий в ленте и цель автопрокрутки

**Файл:** `modules/screen/quiz/chat/.../widget/ChatMessageWidget.kt`

**До** (строки 25-73):

```kotlin
@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
    sendMessage: (Msg) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    
    LaunchedEffect(state) {
        launch {
            lazyListState.animateScrollToItem(state.list.lastIndex)
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
            …
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
            …  // без изменений
        }
        if (showUserActions) {
            item(key = USER_ACTIONS_KEY) {
                UserActionsWidget(sendMessage = sendMessage)
            }
        }
    }
}

/** Ключ элемента действий; ключи сообщений — числа в строке, не пересекаются. */
private const val USER_ACTIONS_KEY = "user_actions"
```

Превью внизу файла получает `showUserActions = true` — видно чипы под
сообщениями. Импорт `kotlinx.coroutines.launch` удалить — после снятия
вложенного `launch { }` он мёртв (lint его не ловит, ktlint в конвейере
нет).

**Зачем.** Кнопки становятся частью прокручиваемого содержимого — ядро
требования. Автопрокрутка должна доводить ленту до кнопок, иначе после
нового вопроса они окажутся за нижним краем.

**Конструкции.**
- `item(key = …)` после `itemsIndexed` — одиночный элемент в конце
  `LazyListScope`; `key` стабилен, поэтому при появлении/исчезновении
  элемента Compose не пересоздаёт соседние.
- `lastIndex = list.size`, когда блок показан: индексы сообщений
  `0..size-1`, блок действий — `size`.
- `LaunchedEffect(state, showUserActions)` — перезапуск и при смене
  флага (новый вопрос: сообщение и флаг приходят одним состоянием,
  ключи меняются вместе; скрытие: лента укорачивается, скролл к
  последнему сообщению).
- Внутренний `launch { }` из старого кода убран — `LaunchedEffect` уже
  корутина, вложенный `launch` ничего не добавлял.
- Проверка `lastIndex >= 0` — по исходнику foundation 1.8.2
  (`LazyLayoutScrollScope.animateScrollToItem`:
  `requirePrecondition(index >= 0f)`); в проде не срабатывает, защищает
  превью `ChatScreen` с пустой лентой.

---

## П3. `ChatWidget` — убрать полосу, передать флаг

**Файл:** `modules/screen/quiz/chat/.../widget/state/ChatWidget.kt`

**До** (строки 40-72):

```kotlin
        ChatMessageWidget(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            state = state.messagesState,
            sendMessage = sendMessage,
        )
        
        if (state.readyToStart) {
            if (state.showUserActions) {
                Row(…) {
                    ShowAnswerButtonWidget { sendMessage.invoke(it) }
                    SkipButtonWidget { sendMessage.invoke(it) }
                }
            }
            PrimaryTextFieldWidget(
```

**После:**

```kotlin
        ChatMessageWidget(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            state = state.messagesState,
            showUserActions = state.showUserActions,
            sendMessage = sendMessage,
        )
        
        if (state.readyToStart) {
            PrimaryTextFieldWidget(
```

Импорты `Row`, `Arrangement`, `Alignment`, `ShowAnswerButtonWidget`,
`SkipButtonWidget` — удалить (lint: unused imports).

**Зачем.** Единственное место, где кнопки жили вне ленты. Флаг
`showUserActions` остаётся в состоянии (явный флаг по правилу проекта),
меняется только потребитель.

**Конструкции.** Условие `state.readyToStart` не трогается: до старта
сессии флаг `showUserActions` всегда `false` (редьюсер поднимает его
только на `NextQuestion`), поэтому блок действий до «Начать» не
появится и без дополнительной проверки. Финал сессии без чипов держится
на инварианте редьюсера: `SessionOver` и `QuizReLoaded` флаг не
снимают, но все пути к `DatasourceEffect.NextQuestion` идут через
`UserAttempt` / `GetAnswer` / `Skip`, где флаг уже снят (проверено по
всем веткам `reduce`; закрепляется П6 и шагом 9 M6).

---

## П4. Ручник M6 (документ `manual_test.md`)

Правки документа: в шапку — фраза «документ накапливает ручники всех
фиксов квиз-чата ветки IS508»; в оглавление — строка
`[M6. Кнопки действий в ленте](#m6)`; новый раздел
«## Чат-фикс 2: кнопки «Показать ответ» / «Пропустить» в ленте» с
кейсом M6 в формате M1–M5:

**Что доказывает.** Кнопки действий — элемент ленты: прокручиваются
с сообщениями, появляются с вопросом, исчезают после попытки /
«Показать ответ» / «Пропустить», на финале и в сводке их нет.
Известное ограничение (Р1): при открытой клавиатуре чипы уходят под
неё вместе с последними сообщениями — лечит фикс клавиатуры (Backlog).

**Вход.** Debug-сборка с фиксом; «Show debug» ON (заголовки вопросов
длинные — лента быстро становится длиннее экрана); группа ≥ 5 слов.

**Шаги.**

1. Таб «Квизы» → чат → «Начать». Под первым вопросом справа — два
   чипа «Показать ответ» / «Пропустить»; над полем ввода полосы нет.
2. Прокрути ленту к приветствию — чипы уехали вверх вместе с
   сообщениями; прокрути обратно — на месте под вопросом.
3. «Пропустить» — чипы исчезли, появился пузырь «Пропустить», затем
   следующий вопрос и чипы под ним; лента докручена до чипов.
4. Введи ответ → отправь — чипы исчезли вместе с появлением пузыря
   ответа; оценка; следующий вопрос + чипы.
5. «Показать ответ» — то же, что шаг 3.
6. Тап в поле ввода → клавиатура. Чипы под клавиатурой (ограничение
   Р1); докрути — видны над полем ввода. Закрой клавиатуру.
7. Поверни экран при видимых чипах — один набор чипов, лента в конце.
8. Два быстрых тапа по «Пропустить» — один пузырь «Пропустить», один
   следующий вопрос. Тап по чипу в момент докрутки — одно действие
   либо ничего (докрутка остановилась), не два.
9. Дойди до финала — чипов нет, три кнопки в пузыре. «Сводка» — чипов
   нет. «Продолжить» — чипы только с первым вопросом новой сессии, не
   с пузырём «Продолжаем».

**Ожидание глазами.**

Чипы всегда под последним вопросом, справа, 12dp от пузыря; зазор до
поля ввода 16dp (было 8 — осознанно, П1). У длинного вопроса чипы
видны, верх вопроса — докрутить вверх (раньше обрезался низ).

**Маркеры в логах.**

```
(UI — маркеров нет; anti: adb logcat -d | grep -E "FATAL|non-negative" пуст)
```

**Анти-проверки.**

* Нет двух наборов чипов одновременно; нет чипов до «Начать», на
  финале и в сводке.
* Нет пустой полосы над полем ввода после скрытия чипов.
* Нет `FATAL EXCEPTION`, нет `Index should be non-negative`.

**Прогон:**

---

## П5. Backlog (`docs/Backlog.md`, «Срочное» → «Квиз-чат»)

**До.** Пункт «UI: кнопки «Показать ответ» и «Пропустить» должны
скроллиться вместе с чатом» (`Backlog.md:46-47`); пункт «клавиатура
закрывает последние сообщения» (`:43-44`); пункт «Анимация сообщений
чата как в Telegram» (`:49-50`).

**После.** Пункт про кнопки удалён (закрыт этим фиксом). В пункт про
клавиатуру дописано: «После чат-фикса 2 под клавиатуру уходят и чипы
«Показать ответ» / «Пропустить» (элемент ленты). Фикс обязан вернуть
в видимую зону конец ленты вместе с ними; способ — `reverseLayout =
true` или автопрокрутка по появлению IME — решить в брифе (см.
`IS508_quiz_no_duplicates/ui_buttons_analysis.md` §6).» В пункт про
анимацию дописано: «Чипы действий — keyed-элемент ленты; плавное
появление/скрытие — `Modifier.animateItem()` на нём, делать в рамках
этого пункта.»

**Зачем.** Правило «один коммит на фикс, доки вместе с кодом»: без
этого пункт останется в «Срочном» после мержа, а известное ограничение
Р1 — нигде, кроме ручника.

---

## П6. Пин-тест редьюсера: флаг действий и позиция вопроса

**Файл:** `modules/screen/quiz/chat/src/test/.../logic/ChatReducerTest.kt`

**До.** Тесты покрывают только `PrepareToStart` и ветки пикера IS481;
`NextQuestion` / `UserAttempt` / `GetAnswer` / `Skip` не проверяются.

**После** (в конец класса, `ResourceManager` уже застаблен на любые
`stringByResId`):

```kotlin
    // ===== IS508 чат-фикс 2: кнопки действий — элемент ленты =====

    /** Сессия идёт: вопрос — последнее сообщение, флаг действий поднят. */
    private fun questionState(): ChatScreenState = reducer
        .testReduce(initialState, Msg.NextQuestion(MessageContent.create(text = "question")))
        .state()

    @Test
    fun `NextQuestion raises showUserActions, question is last system message without buttons`() {
        val state = questionState()
        val last = state.chat.messagesState.list.last()

        assertTrue(state.chat.showUserActions)
        assertTrue(last.isSystemMessage)
        assertTrue(last.buttons.isEmpty())
    }

    @Test
    fun `UserAttempt, GetAnswer, Skip drop showUserActions`() {
        val state = questionState()

        listOf(Msg.UserAttempt("x"), Msg.GetAnswer, Msg.Skip).forEach { msg ->
            val result = reducer.testReduce(state, msg)
            assertEquals("$msg", false, result.state().chat.showUserActions)
        }
    }
```

**Зачем.** После переноса UI опирается не только на флаг, но и на
позицию: блок действий рисуется под последним сообщением, и оно должно
быть вопросом (системное, без кнопок финала). Тест закрепляет обе
половины инварианта; редьюсер не меняется (Р3).

**Конструкции.** `testReduce` / `state()` — тест-хелперы mate, как в
остальных тестах класса. `ChatScreenState()` без `startQuiz()`
достаточно: `NextQuestion` не проверяет `readyToStart`. Три сообщения
в одном тесте через `forEach` с меткой `"$msg"` в ассерте — при падении
видно, какая ветка.

---

## Тесты

П6 — единственный новый тест. Раскладка Compose UI-тестами не
покрывается (androidTest модуля пуст). Вызовы `ChatMessageWidget` —
только `ChatWidget` и превью (проверено grep), сигнатура меняется в
двух местах.
