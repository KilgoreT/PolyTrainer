package me.apomazkin.quiz.chat.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.ChatMessageState
import me.apomazkin.quiz.chat.logic.ChatTiming
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.UserMessageOrigin
import me.apomazkin.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Движение ленты по кадрам: часы теста стоят, кадры продвигаются вручную,
 * позиции элементов — по тегам ([ChatTestTags]) без обрезки лентой.
 * Первый кадр соседей (placement LazyList) здесь не проверяется — только
 * глазами: ожидание idle перед чтением позиций само рассылает
 * snapshot-уведомления.
 */
@RunWith(AndroidJUnit4::class)
class ChatMessageMotionTest {

    @get:Rule
    val rule = createComposeRule()

    private class Screen(list: List<ChatMessage>, showUserActions: Boolean) {
        val list = mutableStateOf(list)
        val showUserActions = mutableStateOf(showUserActions)
    }

    private fun sys(order: Int, text: String) = ChatMessage.addSystemMessage(order = order, message = text)

    private fun user(order: Int, text: String, origin: UserMessageOrigin) = ChatMessage.addUserMessage(
        order = order,
        message = MessageContent.create(text = text),
        origin = origin,
    )

    private fun setContent(list: List<ChatMessage>, showUserActions: Boolean = false): Screen {
        val screen = Screen(list, showUserActions)
        rule.setContent {
            AppTheme {
                ChatMessageWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LIST_HEIGHT),
                    state = ChatMessageState(list = screen.list.value),
                    showUserActions = screen.showUserActions.value,
                    showStartAction = false,
                    flight = remember { FlightState() },
                    imeGesture = false,
                ) {}
            }
        }
        rule.waitForIdle()
        return screen
    }

    private fun bounds(tag: String): DpRect = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun listBottom(): Dp = rule.onRoot().getUnclippedBoundsInRoot().bottom

    /** Апдейт состояния и кадр, в котором лента его раскладывает и рисует впервые. */
    private fun firstFrame(update: () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread(update)
        rule.mainClock.advanceTimeByFrame()
    }

    /** К трём четвертям окна: стыковка уже произошла, конец ещё не наступил. */
    private fun advanceToLateMiddle() {
        rule.mainClock.advanceTimeBy(ChatTiming.MOTION_DURATION_MS * 3L / 4L)
    }

    /** За конец окна с запасом на кадры старта. */
    private fun advanceToEnd() {
        rule.mainClock.advanceTimeBy(ChatTiming.MOTION_DURATION_MS * 2L)
    }

    private fun assertSameDp(message: String, expected: Dp, actual: Dp) {
        assertTrue("$message: ожидалось $expected, получено $actual", abs((expected - actual).value) <= TOLERANCE_DP)
    }

    private fun assertBetween(message: String, low: Dp, value: Dp, high: Dp) {
        assertTrue(
            "$message: $value должно быть строго между $low и $high",
            value.value > low.value + TOLERANCE_DP && value.value < high.value - TOLERANCE_DP,
        )
    }

    @Test
    fun newMessageStartsUnderFieldAndDocks() {
        val screen = setContent(listOf(sys(1, "first"), user(2, "answer", UserMessageOrigin.INPUT)))
        val neighborBefore = bounds(ChatTestTags.message(2))
        val bottom = listBottom()

        firstFrame { screen.list.value = screen.list.value + sys(3, "second") }
        val newFirst = bounds(ChatTestTags.message(3))
        assertTrue(
            "новый стартует под низом ленты: top=${newFirst.top}, низ=$bottom",
            newFirst.top.value >= bottom.value - TOLERANCE_DP,
        )
        assertSameDp("сосед на месте в первом кадре", neighborBefore.top, bounds(ChatTestTags.message(2)).top)

        advanceToLateMiddle()
        val newMid = bounds(ChatTestTags.message(3))
        val neighborMid = bounds(ChatTestTags.message(2))

        advanceToEnd()
        val newEnd = bounds(ChatTestTags.message(3))
        val neighborEnd = bounds(ChatTestTags.message(2))

        assertTrue("сосед уехал вверх", neighborEnd.top.value < neighborBefore.top.value - TOLERANCE_DP)
        assertBetween("сосед на подходе к концу", neighborEnd.top, neighborMid.top, neighborBefore.top)
        assertBetween("новый на подходе к концу", newEnd.top, newMid.top, newFirst.top)
        assertSameDp("новый встал у низа ленты", bottom - ChatMotion.LIST_PADDING, newEnd.bottom)
        // Тег стоит после отступа смены стороны: между пузырями зазор ленты плюс этот отступ.
        assertSameDp(
            "зазор между соседом и новым",
            ChatMotion.ITEM_SPACING + ChatMotion.SIDE_SWITCH_SPACING,
            newEnd.top - neighborEnd.bottom,
        )
    }

    @Test
    fun chainAvatarStaysInPlace() {
        val screen = setContent(listOf(sys(1, "first"), sys(2, "second")))
        val avatarBefore = bounds(ChatTestTags.avatar(2))

        firstFrame { screen.list.value = screen.list.value + sys(3, "third") }
        val avatarFirst = bounds(ChatTestTags.avatar(3))
        assertSameDp("аватар нового — на месте аватара предыдущего", avatarBefore.top, avatarFirst.top)

        advanceToLateMiddle()
        assertSameDp("аватар стоит в середине", avatarBefore.top, bounds(ChatTestTags.avatar(3)).top)

        advanceToEnd()
        assertSameDp("аватар стоит в конце", avatarBefore.top, bounds(ChatTestTags.avatar(3)).top)
    }

    @Test
    fun morphFromChipKeepsNeighborsStill() {
        val screen = setContent(listOf(sys(1, "question")), showUserActions = true)
        val questionBefore = bounds(ChatTestTags.message(1))

        firstFrame {
            screen.showUserActions.value = false
            screen.list.value = screen.list.value + user(2, "Пропустить", UserMessageOrigin.SKIP_CHIP)
        }
        assertSameDp("вопрос на месте в первом кадре", questionBefore.top, bounds(ChatTestTags.message(1)).top)

        advanceToLateMiddle()
        assertSameDp("вопрос на месте в середине", questionBefore.top, bounds(ChatTestTags.message(1)).top)

        advanceToEnd()
        assertSameDp("вопрос на месте в конце", questionBefore.top, bounds(ChatTestTags.message(1)).top)
        assertSameDp(
            "пузырь занял место чипов",
            questionBefore.bottom + ChatMotion.ITEM_SPACING + ChatMotion.SIDE_SWITCH_SPACING,
            bounds(ChatTestTags.message(2)).top,
        )
    }

    @Test
    fun tallerAnswerReplacingChipsMovesNeighborSmoothly() {
        val screen = setContent(listOf(sys(1, "question")), showUserActions = true)
        val questionBefore = bounds(ChatTestTags.message(1))
        val longAnswer = "очень длинный ответ, который не поместится в одну строку пузыря и растянет его на несколько"

        firstFrame {
            screen.showUserActions.value = false
            screen.list.value = screen.list.value + user(2, longAnswer, UserMessageOrigin.INPUT)
        }

        advanceToLateMiddle()
        val questionMid = bounds(ChatTestTags.message(1))

        advanceToEnd()
        val questionEnd = bounds(ChatTestTags.message(1))

        assertTrue("сосед уехал вверх на разницу высот", questionEnd.top.value < questionBefore.top.value - TOLERANCE_DP)
        assertBetween("сосед едет, а не прыгает", questionEnd.top, questionMid.top, questionBefore.top)
    }

    private companion object {
        val LIST_HEIGHT = 600.dp
        const val TOLERANCE_DP = 1f
    }
}
