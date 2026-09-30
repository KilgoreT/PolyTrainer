@file:OptIn(ExperimentalLayoutApi::class)

package me.apomazkin.quiz.chat.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.ChatMessageState
import me.apomazkin.quiz.chat.logic.ChatTiming
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.isPreviousHasSameType
import me.apomazkin.quiz.chat.widget.button.StartActionWidget
import me.apomazkin.quiz.chat.widget.button.UserActionsWidget
import me.apomazkin.quiz.chat.widget.message.FlyingUserMessage
import me.apomazkin.quiz.chat.widget.message.MorphState
import me.apomazkin.quiz.chat.widget.message.MorphingUserMessage
import me.apomazkin.quiz.chat.widget.message.SystemMessageWidget
import me.apomazkin.quiz.chat.widget.message.UserMessageWidget
import me.apomazkin.quiz.chat.widget.motion.ChatItem
import me.apomazkin.quiz.chat.widget.motion.DockedPlacementSpec
import me.apomazkin.quiz.chat.widget.motion.Entrance
import me.apomazkin.quiz.chat.widget.motion.InsertShiftTracker
import me.apomazkin.quiz.chat.widget.motion.LaidOutItem
import me.apomazkin.quiz.chat.widget.motion.entranceFor
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Лента чата от низа. Движение: соседей двигает placement LazyList
 * ([DockedPlacementSpec]), новые элементы въезжают по дистанции трекера
 * ([InsertShiftTracker], [slideInWithColumn]); как элемент входит,
 * решает [entranceFor] один раз на ключ, лента только раздаёт роли:
 * въезд, превращение из кнопки ([MorphingUserMessage]), полёт ответа
 * ([FlyingUserMessage]).
 */
@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
    showUserActions: Boolean,
    showStartAction: Boolean,
    flight: FlightState,
    sendMessage: (Msg) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val durationMs = ChatTiming.MOTION_DURATION_MS
    val listPaddingPx = with(LocalDensity.current) { ChatMotion.LIST_PADDING.toPx() }
    // Въезд «со стыковкой»: новый элемент едет из-под поля одной кривой на
    // добавку + сдвиг соседей; соседи стоят, пока он проходит добавку, и
    // дальше едут с ним как одно целое (DockedPlacementSpec). Прогресс
    // въезда линейный — кривая применяется в слое вместе с дистанцией.
    val slideSpec = remember { tween<Float>(durationMs, easing = LinearEasing) }
    val placementSpecOn = remember { DockedPlacementSpec(durationMs, listPaddingPx) }

    val tracker = remember { InsertShiftTracker() }
    // «Новое» сообщение — порядок выше всех размещённых (см. tracker):
    // въезжает и морфится только оно; история — нет.
    val isLastMessageNew = state.list.lastOrNull()
        ?.let { tracker.isNewOrder(it.order) } ?: false
    val morphState = remember { MorphState() }
    val placementSpec = if (morphState.active) null else placementSpecOn

    // Лента от низа (reverseLayout): индекс 0 — нижний элемент. При
    // изменении набора элементов, если лента у конца, — закрепить (0, 0)
    // на ближайшей перекомпоновке: requestScrollToItem вместо удержания
    // позиции по ключу (иначе новый элемент встал бы под вьюпорт и его
    // подвозил бы animateScrollToItem своей пружиной). Так и на коротком,
    // и на длинном чате соседи уезжают placement-анимацией, а новый
    // въезжает снизу — одна кривая везде. Юзер в истории — подвозим
    // скроллом, без въезда (tracker не взведён).
    val itemCount = state.list.size +
        (if (showUserActions) 1 else 0) +
        (if (showStartAction) 1 else 0)
    val prevItemCount = remember { IntArray(1) { itemCount } }
    if (prevItemCount[0] != itemCount) {
        prevItemCount[0] = itemCount
        SideEffect {
            if (!lazyListState.canScrollBackward) {
                lazyListState.requestScrollToItem(0)
                tracker.armed = true
            }
        }
    }
    LaunchedEffect(state, showUserActions) {
        if (lazyListState.canScrollBackward) {
            lazyListState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        // Жест клавиатуры (Android 11+): остаток скролла «за конец» ленты
        // вытягивает IME за пальцем, движение вниз при видимой IME сначала
        // прячет её, потом крутит ленту. Ниже API 30 — no-op. Только на
        // ленте, чтобы внутренний скролл поля ввода клавиатурой не управлял.
        modifier = modifier
            .imeNestedScroll()
            .trackInsertShift(lazyListState, tracker, isLastMessageNew),
        state = lazyListState,
        contentPadding = PaddingValues(all = ChatMotion.LIST_PADDING),
        // Реверс не меняет семантику arrangement: прижатие короткого
        // контента к низу — только явным Alignment.Bottom.
        verticalArrangement = Arrangement.spacedBy(ChatMotion.ITEM_SPACING, Alignment.Bottom),
        reverseLayout = true
    ) {
        // В реверсе первый элемент — нижний: чипы действий под последним
        // вопросом, над полем ввода. Отдельным элементом, а не внутри
        // вопроса: элемент ленты не должен менять высоту — placement в
        // реверсе якорит его за нижний край, и при смене высоты верх
        // скачком уезжает (так «прыгала» цепочка над вопросом).
        if (showUserActions) {
            item(key = USER_ACTIONS_KEY) {
                // Вход защёлкивается на первой композиции элемента: на
                // следующих последнее сообщение уже не «новое».
                val entrance = remember(USER_ACTIONS_KEY) {
                    entranceFor(ChatItem.Actions, isNew = isLastMessageNew, flightOrder = null)
                }
                UserActionsWidget(
                    modifier = Modifier
                        .animateItem(fadeInSpec = null, placementSpec = placementSpec, fadeOutSpec = null)
                        .slideInIf(entrance, USER_ACTIONS_KEY, tracker, slideSpec),
                    sendMessage = sendMessage,
                )
            }
        }
        // «Начать» — системная кнопка под приветствием, на месте будущего
        // пузыря юзера «Начать»; есть с первой композиции, входа нет.
        if (showStartAction) {
            item(key = START_ACTION_KEY) {
                StartActionWidget(
                    modifier = Modifier
                        .animateItem(fadeInSpec = null, placementSpec = placementSpec, fadeOutSpec = null),
                    sendMessage = sendMessage,
                )
            }
        }
        itemsIndexed(
            items = state.list.asReversed(),
            key = { _, item -> item.order.toString() }
        ) { reversedIndex: Int, item: ChatMessage ->
            // Логика цепочек / аватара / кнопок финала — по индексу в
            // оригинальном (хронологическом) списке.
            val index = state.list.lastIndex - reversedIndex
            val isInChain = index > 0 && state.list[index - 1].isSystemMessage
            val itemKey = item.order.toString()
            // Вход решается один раз на ключ: на следующих композициях
            // сообщение уже не «новое», а анимация должна дойти до конца.
            val entrance = remember(itemKey) {
                entranceFor(
                    item = ChatItem.Message(message = item, isInChain = isInChain),
                    isNew = tracker.isNewOrder(item.order),
                    flightOrder = flight.order,
                )
            }

            // Один корень на элемент: в реверсе несколько placeable одного
            // элемента размещаются в обратном порядке — Spacer лёг бы под
            // пузырь. Отступ при смене стороны — модификатором корня.
            // animateItem — первым в цепочке (анимируется узел элемента),
            // только placement: соседи едут той же кривой, что въезжает новый.
            val topSpace = if (state.isPreviousHasSameType(index)) 0.dp else 8.dp
            val bubbleModifier = Modifier
                .animateItem(fadeInSpec = null, placementSpec = placementSpec, fadeOutSpec = null)
                .padding(top = topSpace)
                .slideInIf(entrance, itemKey, tracker, slideSpec)

            when {
                item.isSystemMessage -> {
                    val notShowAvatar = index < state.list.lastIndex
                            && state.list[index + 1].isSystemMessage
                    val isLastMessage = index == state.list.lastIndex
                    SystemMessageWidget(
                        modifier = bubbleModifier,
                        showAvatar = notShowAvatar.not(),
                        isInChain = isInChain,
                        // Новое сообщение продолжает цепочку — аватар не перескакивает,
                        // а съезжает с предыдущего пузыря на этот.
                        avatarDescends = (entrance as? Entrance.SlideFromBelow)?.avatarDescends == true,
                        avatarDescentExtraPx = { tracker.slideExtra(itemKey, listPaddingPx) },
                        showButtons = isLastMessage,
                        message = item,
                        sendMessage = sendMessage
                    )
                }
                entrance is Entrance.MorphFromButton -> MorphingUserMessage(
                    modifier = bubbleModifier,
                    itemKey = itemKey,
                    message = item.message,
                    origin = entrance.origin,
                    morphState = morphState,
                )
                entrance is Entrance.Flight -> FlyingUserMessage(
                    modifier = bubbleModifier,
                    message = item.message,
                    order = item.order,
                    flight = flight,
                )
                else -> UserMessageWidget(
                    modifier = bubbleModifier,
                    message = item.message,
                )
            }
        }
    }
}

/** Ключи элементов кнопок; ключи сообщений — числа в строке, не пересекаются. */
private const val USER_ACTIONS_KEY = "user_actions"
private const val START_ACTION_KEY = "start_action"

/**
 * Проекция раскладки в трекер после измерения ленты (layoutInfo уже
 * свежий), до размещения элементов: трекер считает сдвиг и раздаёт
 * дистанции новым ключам этого прохода.
 */
private fun Modifier.trackInsertShift(
    lazyListState: LazyListState,
    tracker: InsertShiftTracker,
    lastMessageIsNew: Boolean,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    tracker.onLaidOut(
        items = lazyListState.layoutInfo.visibleItemsInfo.map { info ->
            val isAction = info.key == USER_ACTIONS_KEY || info.key == START_ACTION_KEY
            LaidOutItem(
                key = info.key,
                offset = info.offset,
                isAction = isAction,
                order = if (isAction) null else (info.key as? String)?.toIntOrNull(),
            )
        },
        lastMessageIsNew = lastMessageIsNew,
    )
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

/** Въезд снизу — только для входа [Entrance.SlideFromBelow]; вход защёлкнут на ключ, условие стабильно. */
@Composable
private fun Modifier.slideInIf(
    entrance: Entrance,
    key: Any,
    tracker: InsertShiftTracker,
    spec: FiniteAnimationSpec<Float>,
): Modifier = if (entrance is Entrance.SlideFromBelow) slideInWithColumn(key, tracker, spec) else this

/**
 * Въезд нового элемента снизу одной кривой [ChatMotion.EASING]. Старт —
 * ниже места на дистанцию сдвига соседей (её после измерения ленты
 * раздаёт трекер по ключу) плюс нижний contentPadding: лента режет
 * содержимое по своей границе, а между низом последнего элемента и
 * границей лежит отступ — без добавки верхняя полоса нового элемента (и
 * аватар у низа ряда) видна в первом кадре над полем ввода. Соседи
 * стыкуются с ним ([DockedPlacementSpec]): стоят, пока он проходит
 * добавку, потом едут вместе. Прогресс читается в graphicsLayer —
 * анимация перерисовывает, не перекомпонует. Всё состояние — под ключом
 * элемента: LazyList переиспользует композицию ушедшего элемента для
 * нового.
 */
@Composable
private fun Modifier.slideInWithColumn(
    key: Any,
    tracker: InsertShiftTracker,
    spec: FiniteAnimationSpec<Float>,
): Modifier {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(targetValue = 1f, animationSpec = spec)
    }
    val extraPx = with(LocalDensity.current) { ChatMotion.LIST_PADDING.toPx() }
    return graphicsLayer {
        val d = tracker.distanceOf(key)
        val p = progress.value
        when {
            d != null -> {
                alpha = 1f
                translationY = (tracker.slideExtra(key, extraPx) + d) * (1f - ChatMotion.EASING.transform(p))
            }
            // Слой может быть вычислен раньше раздачи дистанции: первые кадры
            // без неё не рисуем, иначе элемент мелькнёт на своём месте и только
            // потом уедет вниз на старт.
            p < SLIDE_IN_HIDE_UNTIL -> alpha = 0f
            // Страховка: дистанция так и не пришла — рисуем на месте, а не
            // прячем сообщение навсегда.
            else -> {
                alpha = 1f
                translationY = 0f
            }
        }
    }
}

/** Доля прогресса въезда (1–2 кадра), в течение которой элемент без дистанции скрыт. */
private const val SLIDE_IN_HIDE_UNTIL = 0.001f

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ChatMessageWidget(
            modifier = Modifier.height(600.dp),
            state = ChatMessageState(
                list = listOf(
                    ChatMessage.addSystemMessage(
                        order = 1,
                        message = "Hello, World!"
                    ),
                    ChatMessage.addSystemMessage(
                        order = 2,
                        message = "Fuck, World!"
                    ),
                    ChatMessage.addUserMessage(
                        order = 3,
                        message = MessageContent.create(text = "Kill, World!")
                    ),
                )
            ),
            showUserActions = true,
            showStartAction = false,
            flight = remember { FlightState() },
        ) {}
    }
}
