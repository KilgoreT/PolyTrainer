@file:OptIn(ExperimentalLayoutApi::class)

package me.apomazkin.quiz.chat.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.ChatMessageState
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.UserMessageOrigin
import me.apomazkin.quiz.chat.logic.isPreviousHasSameType
import me.apomazkin.quiz.chat.widget.button.ACTION_CHIP_SPACING
import me.apomazkin.quiz.chat.widget.button.StartActionWidget
import me.apomazkin.quiz.chat.widget.button.UserActionsWidget
import me.apomazkin.quiz.chat.widget.button.base.ChatButtonWidget
import me.apomazkin.quiz.chat.widget.message.SystemMessageWidget
import me.apomazkin.quiz.chat.widget.message.UserMessageWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
    showUserActions: Boolean,
    showStartAction: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val durationMs = ChatMotion.DURATION_MS
    // Одна кривая на сдвиг соседей и въезд новых элементов — колонка едет одним куском.
    val slideSpec = remember { tween<Float>(durationMs, easing = ChatMotion.EASING) }
    val placementSpecOn = remember { tween<IntOffset>(durationMs, easing = ChatMotion.EASING) }

    val insertShift = remember { InsertShiftTracker() }
    // «Новый» — ключ, которого не было в предыдущей раскладке (см. tracker):
    // въезжает и морфится только он; старое сообщение, заново скомпонованное
    // после выпадения из окна, — нет. При первой композиции новых нет.
    val isLastMessageNew = state.list.lastOrNull()
        ?.let { insertShift.isNewKey(it.order.toString()) } ?: false
    val morphState = remember { MorphState() }
    val placementSpec = if (morphState.active) null else placementSpecOn
    // Ширина чипа «Пропустить» (px) — на неё + зазор пузырь «Показать ответ»
    // уезжает вправо со своего места. Обычное поле: читается при композиции
    // нового пузыря, чип к этому моменту давно измерен.
    val skipChipWidthPx = remember { IntArray(1) }
    val chipSpacingPx = with(LocalDensity.current) { ACTION_CHIP_SPACING.toPx() }

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
                insertShift.armed = true
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
            .trackInsertShift(lazyListState, insertShift),
        state = lazyListState,
        contentPadding = PaddingValues(all = 16.dp),
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
                UserActionsWidget(
                    modifier = Modifier
                        .animateItem(fadeInSpec = null, placementSpec = placementSpec, fadeOutSpec = null)
                        .slideInWithColumn(
                            key = USER_ACTIONS_KEY,
                            enabled = isLastMessageNew,
                            tracker = insertShift,
                            spec = slideSpec,
                        ),
                    onSkipChipMeasured = { skipChipWidthPx[0] = it },
                    sendMessage = sendMessage,
                )
            }
        }
        // «Начать» — системная кнопка под приветствием, на месте будущего
        // пузыря юзера «Начать»; есть с первой композиции, въезда нет.
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

            // Один корень на элемент: в реверсе несколько placeable одного
            // элемента размещаются в обратном порядке — Spacer лёг бы под
            // пузырь. Отступ при смене стороны — модификатором корня.
            val topSpace = if (state.isPreviousHasSameType(index)) 0.dp else 8.dp
            // Въезд снизу — для сообщений Lexeme и набранных ответов, на
            // дистанцию сдвига соседей (tracker; 0 — если подвозили скроллом
            // из истории). Пузыри из кнопок («Начать», чипы) не въезжают —
            // они превращаются из кнопки на её месте.
            val itemKey = item.order.toString()
            val isNew = insertShift.isNewKey(itemKey)
            val slideIn = isNew &&
                (item.isSystemMessage || item.origin == UserMessageOrigin.INPUT)
            // animateItem — первым в цепочке (анимируется узел элемента), только
            // placement: соседи едут той же кривой, что въезжает новый.
            val bubbleModifier = Modifier
                .animateItem(fadeInSpec = null, placementSpec = placementSpec, fadeOutSpec = null)
                .padding(top = topSpace)
                .slideInWithColumn(key = itemKey, enabled = slideIn, tracker = insertShift, spec = slideSpec)

            if (item.isSystemMessage) {
                val notShowAvatar = index < state.list.lastIndex
                        && state.list[index + 1].isSystemMessage
                val isInChain = index > 0
                        && state.list[index - 1].isSystemMessage
                val isLastMessage = index == state.list.lastIndex
                SystemMessageWidget(
                    modifier = bubbleModifier,
                    showAvatar = notShowAvatar.not(),
                    isInChain = isInChain,
                    // Новое сообщение продолжает цепочку — аватар не перескакивает,
                    // а съезжает с предыдущего пузыря на этот.
                    avatarDescends = isNew && isInChain,
                    motionDurationMs = durationMs,
                    showButtons = isLastMessage,
                    message = item,
                    sendMessage = sendMessage
                )
            } else {
                // t — линейное время превращения; фазы: сосед гаснет → пузырь
                // перекрашивается; геометрия — весь интервал.
                val t = actionMorph(
                    key = itemKey,
                    enabled = item.origin != UserMessageOrigin.INPUT && isNew,
                    morphState = morphState,
                    durationMs = durationMs,
                )
                val geometryMorph = ChatMotion.window(t, 0f, 1f)
                val colorMorph = ChatMotion.window(t, ChatMotion.RECOLOR_START, 1f)
                val ghostFade = ChatMotion.window(t, 0f, ChatMotion.GHOST_FADE_END)
                val fromShowAnswer = item.origin == UserMessageOrigin.SHOW_ANSWER_CHIP
                val fromSkip = item.origin == UserMessageOrigin.SKIP_CHIP
                val skipShiftPx = skipChipWidthPx[0] + chipSpacingPx
                // Box — один корень элемента (реверс переворачивает несколько
                // placeable). Внутри — пузырь и, пока идёт превращение, призрак
                // второго чипа: из «Показать ответ» — «Пропустить» едет вправо
                // с той же скоростью, тает и уходит за край; из «Пропустить» —
                // «Показать ответ» тает на месте слева. Призрак одной высоты с
                // пузырём (чип = пузырь по геометрии), размер элемента не меняет.
                Box(modifier = bubbleModifier) {
                    UserMessageWidget(
                        message = item.message,
                        colorMorph = colorMorph,
                        geometryMorph = geometryMorph,
                        fromStart = item.origin == UserMessageOrigin.START_BUTTON,
                        shiftFromLeftPx = if (fromShowAnswer) skipShiftPx else 0f,
                    )
                    if ((fromShowAnswer || fromSkip) && t < 1f) {
                        ChatButtonWidget(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .graphicsLayer {
                                    // Уезжающий «Пропустить» едет с пузырём и тает всю
                                    // дорогу до края; стоящий «Показать ответ» гаснет в
                                    // первой фазе — до того, как пузырь начнёт синеть.
                                    translationX = if (fromShowAnswer) skipShiftPx * geometryMorph else -skipShiftPx
                                    alpha = if (fromShowAnswer) 1f - geometryMorph else 1f - ghostFade
                                },
                            title = if (fromShowAnswer) {
                                R.string.chat_quiz_msg_user_skip
                            } else {
                                R.string.chat_quiz_msg_user_show_answer
                            },
                            enabled = false,
                        ) {}
                    }
                }
            }
        }
    }
}

/** Ключи элементов кнопок; ключи сообщений — числа в строке, не пересекаются. */
private const val USER_ACTIONS_KEY = "user_actions"
private const val START_ACTION_KEY = "start_action"

/**
 * Доля превращения «системная кнопка → пузырь юзера» для нового сообщения,
 * порождённого кнопкой: 0 → 1 той же кривой, что движение ленты. Пузырь
 * встаёт на место кнопки (форма и размер совпадают), меняется только цвет.
 */
@Composable
private fun actionMorph(key: Any, enabled: Boolean, morphState: MorphState, durationMs: Int): Float {
    // Состояние под ключом элемента: LazyList переиспользует композицию.
    val latched = remember(key) { enabled }
    if (!latched) return 1f
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        morphState.active = true
        try {
            // Линейное время: кривые — у каждой фазы своя (ChatMotion.window).
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMs, easing = LinearEasing),
            )
        } finally {
            morphState.active = false
        }
    }
    return progress.value
}

/**
 * Идёт превращение кнопки в пузырь: высота элемента-пузыря меняется
 * покадрово, и placement соседей на это время выключен — иначе они
 * догоняли бы раскладку с отставанием. Вставок в это окно нет (пауза
 * бота длиннее анимации).
 */
private class MorphState {
    var active by mutableStateOf(false)
}

/**
 * Дистанция въезда новых элементов (px) = на сколько placement сдвинул
 * соседей: смещение прежнего нижнего СООБЩЕНИЯ (чипы не в счёт — они
 * исчезают) между двумя раскладками, в координатах ленты от низа.
 * Положительная — соседи уехали вверх (вставили сообщение), отрицательная —
 * осели (исчезли чипы). Новые элементы стартуют на эту дистанцию ниже
 * места и едут с колонкой одним куском. Взводится (`armed`) только когда
 * лента закреплена у конца через requestScrollToItem; при подвозе
 * скроллом из истории — 0.
 */
private class InsertShiftTracker {
    var anchorKey: Any? = null
    var anchorOffset: Int = 0
    var armed: Boolean = false

    /**
     * Дистанция въезда по ключу нового элемента. Заполняется расчётом после
     * измерения ленты — до размещения; сам элемент дистанцию не «захватывает»
     * и ничего не регистрирует: LazyList переиспользует композицию ушедшего
     * элемента для нового, и всё, что элемент помнит про себя, может
     * пережить смену ключа. Пока ключа нет в карте — элемент не рисуется.
     */
    val distances = HashMap<Any, Float>()

    /**
     * Ключи всех элементов, побывавших в раскладке. «Новый» элемент = ключа
     * ещё не было: старое сообщение, заново скомпонованное LazyList после
     * выпадения из окна, новым не считается (иначе оно «въезжало» бы и
     * морфилось заново — мелькание). До первой раскладки новых нет.
     */
    val knownKeys = HashSet<Any>()
    var hasLaidOut: Boolean = false

    fun isNewKey(key: Any): Boolean = hasLaidOut && key !in knownKeys
}

/**
 * Считает дистанцию после измерения ленты (layoutInfo уже свежий) и раздаёт
 * её новым ключам этого прохода — до их размещения. Взведённый расчёт даёт
 * дистанцию сдвига соседей, не взведённый (подвоз скроллом из истории) —
 * 0, без въезда. Обычные поля, не snapshot-state: читаются в graphicsLayer,
 * который и так перерисовывается по прогрессу анимации.
 */
private fun Modifier.trackInsertShift(
    lazyListState: LazyListState,
    tracker: InsertShiftTracker,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val items = lazyListState.layoutInfo.visibleItemsInfo
    val shift = if (tracker.armed) {
        tracker.armed = false
        items.firstOrNull { it.key == tracker.anchorKey }
            ?.let { (it.offset - tracker.anchorOffset).toFloat() }
            ?: 0f
    } else {
        0f
    }
    // Элементы кнопок в «известные» не попадают: они появляются заново с
    // каждым вопросом и въезжают каждый раз. Дистанция выдаётся один раз
    // (пока ключа нет в карте), чтобы промежуточные проходы раскладки не
    // обнулили её посреди въезда. Дистанции сообщений не чистятся (элемент
    // может на проход выпасть из видимых — и остался бы без неё навсегда);
    // у кнопок — чистятся при исчезновении, чтобы следующее появление
    // получило свою.
    val visibleKeys = items.map { it.key }.toSet()
    items.forEach { item ->
        val isActionKey = item.key == USER_ACTIONS_KEY || item.key == START_ACTION_KEY
        if (item.key !in tracker.knownKeys && item.key !in tracker.distances) {
            tracker.distances[item.key] = shift
        }
        if (!isActionKey) tracker.knownKeys += item.key
    }
    tracker.hasLaidOut = true
    if (USER_ACTIONS_KEY !in visibleKeys) tracker.distances.remove(USER_ACTIONS_KEY)
    if (START_ACTION_KEY !in visibleKeys) tracker.distances.remove(START_ACTION_KEY)
    items.firstOrNull { it.key != USER_ACTIONS_KEY && it.key != START_ACTION_KEY }?.let {
        tracker.anchorKey = it.key
        tracker.anchorOffset = it.offset
    }
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

/**
 * Въезд нового элемента снизу: стартует ниже своего места на дистанцию
 * сдвига соседей (её после измерения ленты раздаёт [trackInsertShift] по
 * ключу) и приходит на место той же кривой, что placement соседей.
 * Прогресс читается в graphicsLayer — анимация перерисовывает, не
 * перекомпонует. Всё состояние — под ключом элемента: LazyList
 * переиспользует композицию ушедшего элемента для нового.
 */
@Composable
private fun Modifier.slideInWithColumn(
    key: Any,
    enabled: Boolean,
    tracker: InsertShiftTracker,
    spec: FiniteAnimationSpec<Float>,
): Modifier {
    // Решение защёлкивается на первой композиции ключа: на следующих ключ
    // уже «известен», и без защёлки анимация оборвалась бы на полпути.
    val latched = remember(key) { enabled }
    if (!latched) return this
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(targetValue = 1f, animationSpec = spec)
    }
    return graphicsLayer {
        val d = tracker.distances[key]
        val p = progress.value
        when {
            d != null -> {
                alpha = 1f
                translationY = d * (1f - p)
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
        ) {}
    }
}