# IS508 / чат-фикс 10 | План рефакторинга анимаций: до → после → зачем

Бриф: [ui_motion_refactor_brief.md](ui_motion_refactor_brief.md),
анализ: [ui_motion_refactor_analysis.md](ui_motion_refactor_analysis.md).

Два коммита. Коммит 1 (П1–П4): `ChatTiming`, `Entrance`, трекер по
`order` и тестируемый, разбор рендера на функции, юниты. Поведение на
экране не меняется (кроме Д4). Прогон M10–M14. Коммит 2 (П5–П6):
device-тесты + доки/Backlog.

Правила: gradle через `./scripts/cc-build.sh`; sed/find/cat/head/tail/
awk/Python запрещены; на девайс только `installDebug`; per-item
состояние — под ключом (`remember(key)`); KDoc без «чат-фикс N» и дат.

---

## П1. `ChatTiming` (logic)

**Файлы:** новый `logic/ChatTiming.kt`; `logic/DatasourceEffectHandler.kt`;
`widget/ChatMotion.kt`; `widget/state/ChatWidget.kt`, `widget/FlightLayer.kt`,
`widget/ChatMessageWidget.kt`, `widget/message/SystemMessageWidget.kt`
(читатели `DURATION_MS`); новый `logic/ChatTimingTest.kt`;
`logic/DatasourceEffectHandlerTest.kt`.

**До:** `private const val BOT_PAUSE_MIN_MS = 400L / MAX = 650L` в
хендлере; `ChatMotion.DURATION_MS = 350`; KDoc с хроникой («временно
поднимали до 2–7 с (2026-09-29)», «чат-фикс 6»).

**После:** `ChatTiming` из анализа §2.5; `botPauseMs()` →
`Random.nextLong(ChatTiming.BOT_PAUSE_MIN_MS, ChatTiming.BOT_PAUSE_MAX_MS)`;
`ChatMotion.DURATION_MS` удалить, читатели → `ChatTiming.MOTION_DURATION_MS`;
KDoc `botPauseMs`, `ChatMotion`, `DeliverSystemMessages` — без хроники и
шифров. `ChatTimingTest`: `BOT_PAUSE_MIN_MS >= MOTION_DURATION_MS +
MOTION_LATENCY_MS`, `MIN <= MAX`. В `DatasourceEffectHandlerTest`
существующий тест `DeliverSystemMessages waits bot pause` — `currentTime
>= ChatTiming.BOT_PAUSE_MIN_MS`; добавить такие же для `NextQuestion`
(quizGame.hasNextQuestion → true), `CheckAnswer`, `GetAnswer` (моки
`quizGame` relaxed уже есть).

**Зачем.** Инвариант по конструкции и под тестом; один источник темпа.

---

## П2. `Entrance` и трекер (widget/motion)

**Файлы:** новые `widget/motion/Entrance.kt`, `widget/motion/InsertShiftTracker.kt`
(перенос из `ChatMessageWidget`), `widget/motion/DockedPlacementSpec.kt`
(перенос + `dockedFraction`); тесты `EntranceTest`, `InsertShiftTrackerTest`,
`DockedFractionTest`.

### П2.1. `Entrance`, `ChatItem`, `entranceFor` — анализ §2.1

```kotlin
internal fun entranceFor(item: ChatItem, isNew: Boolean, flightOrder: Int?): Entrance = when (item) {
    is ChatItem.Start -> Entrance.None
    is ChatItem.Actions -> if (isNew) Entrance.SlideFromBelow(avatarDescends = false) else Entrance.None
    is ChatItem.Message -> when {
        !isNew -> Entrance.None
        item.message.isSystemMessage -> Entrance.SlideFromBelow(avatarDescends = item.isInChain)
        item.message.origin == UserMessageOrigin.INPUT && flightOrder == item.message.order -> Entrance.Flight
        item.message.origin == UserMessageOrigin.INPUT -> Entrance.SlideFromBelow(avatarDescends = false)
        else -> Entrance.MorphFromButton(item.message.origin)
    }
}
```

### П2.2. Трекер — `onLaidOut` и `order`

**До:** `InsertShiftTracker` — публичные поля `anchorKey/anchorOffset/armed/
distances/knownKeys/hasLaidOut`, `isNewKey(key) = hasLaidOut && key !in
knownKeys`, `slideExtra`; логика в `Modifier.trackInsertShift`.

**После:**

```kotlin
internal class LaidOutItem(val key: Any, val offset: Int, val isAction: Boolean, val order: Int?)

internal class InsertShiftTracker {
    var armed: Boolean = false
    private val distances = HashMap<Any, Float>()
    private var maxLaidOutOrder = Int.MIN_VALUE
    private var hasLaidOut = false
    private var anchorKey: Any? = null
    private var anchorOffset = 0

    /** Новое сообщение — с порядком выше всех уже размещённых; история новой не бывает. */
    fun isNewOrder(order: Int): Boolean = hasLaidOut && order > maxLaidOutOrder
    fun distanceOf(key: Any): Float? = distances[key]
    fun slideExtra(key: Any, extraPx: Float): Float = if ((distances[key] ?: 0f) > 0f) extraPx else 0f

    /** Вызывается после измерения ленты, до размещения: считает сдвиг и раздаёт дистанции новым ключам. */
    fun onLaidOut(items: List<LaidOutItem>, lastMessageIsNew: Boolean) { ... как trackInsertShift, но:
        новые ключи сообщений — order > maxLaidOutOrder; ключи кнопок — lastMessageIsNew && key !in distances;
        после раздачи maxLaidOutOrder = max(orders); чистка distances кнопок при исчезновении; якорь — первое сообщение }
}
```

Модификатор ленты: `layout { … tracker.onLaidOut(layoutInfo.visibleItemsInfo.map { LaidOutItem(it.key, it.offset, isAction, orderOf(it.key)) }, isLastMessageNew) … }`,
где `orderOf` — `key.toString().toIntOrNull()` (ключи сообщений — числа
в строке; кнопок — слова). `isLastMessageNew` в ленте:
`state.list.lastOrNull()?.let { tracker.isNewOrder(it.order) } ?: false`
(как сейчас, через новый предикат).

### П2.3. `dockedFraction`

```kotlin
/** Доля пути соседа при доле времени [eased] (см. DockedPlacementSpec): 0..1. */
internal fun dockedFraction(extraPx: Float, distance: Float, eased: Float): Float {
    if (distance == 0f) return 1f
    return (((extraPx + distance) * eased - extraPx) / distance).coerceIn(0f, 1f)
}
```
`DockedPlacementSpec.docked` использует её.

Тесты — анализ §3.

**Зачем.** Правила «новый/дистанция/стыковка» — чистые и под юнитами;
баг истории после поворота закрыт.

---

## П3. Разбор рендера на функции

**Файлы:** `widget/ChatMessageWidget.kt`; новые `widget/message/MorphingUserMessage.kt`,
`widget/message/FlyingUserMessage.kt`, `widget/button/ActionChipsRow.kt`;
`widget/button/UserActionsWidget.kt`, `widget/button/base/ChatButtonWidget.kt`,
`widget/button/SkipButtonWidget.kt`, `widget/button/ShowAnswerButtonWidget.kt`.

### П3.1. `ActionChipsRow`

**До:** `UserActionsWidget` — `Row(fillMaxWidth, padding(top=8), spacedBy(8, End), Bottom)`
с `ShowAnswerButtonWidget`/`SkipButtonWidget`; призрак в полёте — ручная
`Row(spacedBy(8))` из двух `ChatButtonWidget(enabled=false)`.

**После:**

```kotlin
/** Ряд чипов действий: живой (в ленте) и призрак (внутри пузыря на время полёта). */
@Composable
internal fun ActionChipsRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onShowAnswer: () -> Unit = {},
    onSkip: () -> Unit = {},
) = Row(modifier, horizontalArrangement = Arrangement.spacedBy(ACTION_CHIP_SPACING, Alignment.End), verticalAlignment = Alignment.Bottom) {
    ChatButtonWidget(title = R.string.chat_quiz_msg_user_show_answer, enabled = enabled, onClick = onShowAnswer)
    ChatButtonWidget(title = R.string.chat_quiz_msg_user_skip, enabled = enabled, onClick = onSkip)
}
```
`UserActionsWidget` = `ActionChipsRow(modifier.fillMaxWidth().padding(top = 8.dp), onShowAnswer = { sendMessage(Msg.GetAnswer) }, onSkip = { sendMessage(Msg.Skip) })`
(проверить, какие Msg шлют `ShowAnswerButtonWidget`/`SkipButtonWidget`, и
удалить их, если больше нигде не используются). Параметр
`onSkipChipMeasured` удалить.

### П3.2. `ChatButtonWidget` — геометрия из констант

**До:** `RoundedCornerShape(20/20/20/8)`, `padding(16, 8)` руками.
**После:** `ChatMotion.BUBBLE_CORNER/BUBBLE_TAIL_CORNER/BUBBLE_PADDING_H/V`.

### П3.3. `MorphingUserMessage`

**До:** в ленте: `actionMorph(...)`, `geometryMorph/colorMorph/ghostFade`,
`skipShiftPx = skipChipWidthPx[0] + chipSpacingPx`, `Box { UserMessageWidget(...);
if ((fromShowAnswer || fromSkip) && t < 1f) ChatButtonWidget(призрак) }`.

**После:**

```kotlin
/**
 * Пузырь юзера, рождённый системной кнопкой: превращается из неё на месте
 * (перекраска, для «Начать» — и геометрия), рядом тает призрак соседнего
 * чипа. Прогресс — линейное время под ключом элемента; фазы — ChatMotion.
 */
@Composable
internal fun MorphingUserMessage(
    modifier: Modifier,
    itemKey: Any,
    message: ChatMessage.MessageValue,
    origin: UserMessageOrigin,
    morphState: MorphState,
) {
    val t = actionMorph(itemKey, morphState)           // перенос как есть
    val skipWidthPx = remember(itemKey) { IntArray(1) } // с самого призрака «Пропустить»
    ...
    Box(modifier) {
        UserMessageWidget(..., shiftFromLeftPx = if (fromShowAnswer) skipWidthPx[0] + chipSpacingPx else 0f)
        if ((fromShowAnswer || fromSkip) && t < 1f) ChatButtonWidget(modifier = Modifier.align(CenterEnd).onSizeChanged { if (fromShowAnswer) skipWidthPx[0] = it.width }.graphicsLayer { ... }, ...)
    }
}
```
Примечание: призрак «Пропустить» при `fromShowAnswer` измеряется в той же
раскладке кадра, к рисованию ширина есть; при `fromSkip` ширина не нужна
(сдвиг 0). `actionMorph`, `MorphState` переезжают в этот файл (`internal`).
Морф перекомпонует по кадрам (t читается в композиции) — записать в KDoc
как осознанно: геометрия «Начать» требует layout.

### П3.4. `FlyingUserMessage`

**После:**

```kotlin
/** Пузырь набранного ответа на время полёта копии из поля: скрыт, отдаёт цель слою, призрак ряда чипов гаснет. */
@Composable
internal fun FlyingUserMessage(modifier: Modifier, message: ChatMessage.MessageValue, order: Int, flight: FlightState) {
    Box(modifier) {
        UserMessageWidget(message = message, surfaceModifier = Modifier
            .onGloballyPositioned { flight.target = it.positionInRoot() }
            .graphicsLayer { alpha = if (flight.order == order) 0f else 1f })
        ActionChipsRow(modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer {
            alpha = 1f - ChatMotion.window(flight.progress, ChatMotion.SWAP_START, ChatMotion.SWAP_END)
        }, enabled = false)
    }
}
```

### П3.5. `ChatMessageWidget` — диспетчер

```kotlin
val entrance = remember(itemKey) { entranceFor(ChatItem.Message(item, isInChain), isNew, flight.order) }
val bubbleModifier = Modifier.animateItem(...).padding(top = topSpace)
    .then(if (entrance is Entrance.SlideFromBelow) Modifier.slideInWithColumn(itemKey, tracker, slideSpec) else Modifier)
when {
    item.isSystemMessage -> SystemMessageWidget(bubbleModifier, ..., avatarDescends = (entrance as? SlideFromBelow)?.avatarDescends == true, ...)
    entrance is Entrance.MorphFromButton -> MorphingUserMessage(bubbleModifier, itemKey, item.message, entrance.origin, morphState)
    entrance is Entrance.Flight -> FlyingUserMessage(bubbleModifier, item.message, item.order, flight)
    else -> UserMessageWidget(bubbleModifier, item.message)
}
```
Чипы: `entranceFor(ChatItem.Actions, isLastMessageNew, null)`; «Начать»
— `None`. `slideInWithColumn` теряет параметр `enabled` (решение —
снаружи, по `entrance`), защёлка `remember(key)` остаётся внутри на
случай смены `entrance`... нет: `entrance` защёлкнут `remember(itemKey)`
снаружи, модификатор просто применяется или нет. Внимание (риск 3
анализа): порядок `remember` внутри элемента не меняется между кадрами
— `entrance` фиксирован на ключ.

**Зачем.** Лента — диспетчер; каждая анимация — своя функция с KDoc;
согласование геометрии — константы.

**Контроль коммита 1:** `:modules:screen:quiz:chat:testDebugUnitTest`,
`:app:lintDebug`, `installDebug`, M10–M14 глазами (+ шаг «поворот →
скролл в историю: старые пузыри не морфятся»).

---

## П4. Юнит-тесты коммита 1

`EntranceTest`, `InsertShiftTrackerTest`, `DockedFractionTest`,
`ChatTimingTest`, правки `DatasourceEffectHandlerTest` — по анализу §3.

---

## П5. Device-тесты (коммит 2)

**Файлы:** `modules/screen/quiz/chat/build.gradle.kts` (`defaultConfig {
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }`,
`androidTestImplementation(composeLibs.uiTestJunit4)`,
`debugImplementation(composeLibs.uiTestManifest)`); новый
`src/androidTest/java/me/apomazkin/quiz/chat/widget/ChatMessageMotionTest.kt`;
`testTag` в `ChatMessageWidget` (корень элемента: `"msg-${order}"`,
`"actions"`) и `AvatarWidget` (`"avatar-${order}"` — тег передаётся из
`SystemMessageWidget`).

Тесты 1–4 по анализу §3; техника: `createComposeRule()`,
`mainClock.autoAdvance = false`, `rule.runOnIdle { state.value = ... }`,
`advanceTimeByFrame()` ×2 для кадра старта, `advanceTimeBy(150)` для
середины, конец — `advanceTimeUntil(3_000) { bounds не менялись два
кадра }` (вспомогательная функция), `getUnclippedBoundsInRoot()`.
Запуск: `./scripts/cc-build.sh :modules:screen:quiz:chat:connectedDebugAndroidTest`.

---

## П6. Доки и Backlog (коммит 2)

- `manual_test.md`: у M10–M14 — строка прогона «после рефакторинга»;
  раздел «Device-тесты движения: как гонять».
- `ui_motion_refactor_analysis.md` — §5 «Итог».
- `docs/Backlog.md` → Tech Debt: Robolectric для Compose-тестов в CI;
  перекраска чипов в draw-фазе; пузырь «Продолжить» из кнопки финала —
  морф вместо въезда.

---

## Тесты (сводно)

JVM: `EntranceTest`, `InsertShiftTrackerTest`, `DockedFractionTest`,
`ChatTimingTest`, `DatasourceEffectHandlerTest`, `ChatReducerTest`.
Device: `ChatMessageMotionTest` (4). Регресс: unit модуля, lint,
`installDebug`, M10–M14, `connectedDebugAndroidTest` модуля.
