package me.apomazkin.quiz.chat.widget.message

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.quiz.QuizQuestion
import me.apomazkin.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Пузырь вопроса на реальном рендере: верхняя строка — имя ядра, ниже
 * метка слева и значение справа; без метки значение идёт от края;
 * длинное значение переносится в своей колонке, метка остаётся у первой
 * строки; крупный системный шрифт раскладку не ломает.
 */
@RunWith(AndroidJUnit4::class)
class QuestionBubbleTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setQuestion(question: QuizQuestion, fontScale: Float = 1f) {
        rule.setContent {
            AppTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.density, fontScale = fontScale),
                ) {
                    SystemMessageWidget(
                        modifier = Modifier.width(BUBBLE_ROW_WIDTH),
                        message = ChatMessage(
                            order = 1,
                            isSystemMessage = true,
                            message = ChatMessage.MessageValue.Question(question),
                        ),
                        showAvatar = true,
                        isInChain = false,
                        showButtons = false,
                    ) {}
                }
            }
        }
        rule.waitForIdle()
    }

    private fun bounds(text: String): DpRect = rule.onNodeWithText(text).getUnclippedBoundsInRoot()

    private fun question(badge: String?, value: String = VALUE) = QuizQuestion(
        header = HEADER,
        badge = badge,
        value = value,
        debugHeader = null,
    )

    @Test
    fun questionWithBadge_headerOnTop_badgeLeftOfValue() {
        setQuestion(question(badge = BADGE))

        rule.onNodeWithText("$HEADER:").assertIsDisplayed()
        rule.onNodeWithText(BADGE).assertIsDisplayed()
        rule.onNodeWithText(VALUE).assertIsDisplayed()

        val header = bounds("$HEADER:")
        val badge = bounds(BADGE)
        val value = bounds(VALUE)
        assertTrue("имя ядра — отдельной строкой над значением", header.bottom.value <= value.top.value + TOLERANCE_DP)
        assertTrue("метка левее значения", badge.right.value <= value.left.value + TOLERANCE_DP)
        assertTrue("метка в строке значения", badge.top.value >= header.bottom.value - TOLERANCE_DP)
    }

    @Test
    fun questionWithoutBadge_valueStartsAtHeaderEdge() {
        setQuestion(question(badge = null))

        val header = bounds("$HEADER:")
        val value = bounds(VALUE)
        assertTrue(
            "без метки значение от края: ${header.left} и ${value.left}",
            abs((header.left - value.left).value) <= TOLERANCE_DP,
        )
    }

    @Test
    fun longValue_wrapsInOwnColumn_badgeStaysAtFirstLine() {
        setQuestion(question(badge = BADGE, value = LONG_VALUE))

        val badge = bounds(BADGE)
        val value = bounds(LONG_VALUE)
        assertTrue("значение перенеслось", value.height.value > badge.height.value * 2f)
        assertTrue("метка левее колонки значения", badge.right.value <= value.left.value + TOLERANCE_DP)
        assertTrue(
            "метка у первой строки, не по центру абзаца",
            badge.bottom.value < value.top.value + value.height.value / 2f,
        )
    }

    @Test
    fun largeFontScale_badgeNotTallerThanValueLine() {
        setQuestion(question(badge = BADGE), fontScale = 2f)

        rule.onNodeWithText(BADGE).assertIsDisplayed()
        rule.onNodeWithText(VALUE).assertIsDisplayed()
        val badge = bounds(BADGE)
        val value = bounds(VALUE)
        assertTrue(
            "метка не выше строки значения: ${badge.height} и ${value.height}",
            badge.height.value <= value.height.value + TOLERANCE_DP,
        )
        assertTrue("метка левее значения", badge.right.value <= value.left.value + TOLERANCE_DP)
    }

    private companion object {
        const val HEADER = "Перевод"
        const val BADGE = "сущ."
        const val VALUE = "яблоко"
        const val LONG_VALUE = "очень длинное значение ядра, которое точно не помещается в одну строку пузыря"
        const val TOLERANCE_DP = 1f
        val BUBBLE_ROW_WIDTH = 320.dp
    }
}
