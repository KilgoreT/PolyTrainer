package me.apomazkin.quiz.chat.widget.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.quiz.QuizQuestion
import me.apomazkin.theme.LexemeStyle

/**
 * Тело пузыря с вопросом: верхняя строка — имя ядра, ниже — слот
 * [leading] слева и значение справа. Длинное значение переносится в
 * своей колонке (висячий отступ), слот остаётся у первой строки.
 *
 * Что в слоте, тело не знает: только держит его не выше строки значения
 * (потолок от кегля стиля, растёт с системным шрифтом), не шире
 * [LEADING_MAX_WIDTH] и на одной базовой линии с первой строкой
 * значения. Стили и цвета — как у остального текста пузыря.
 */
@Composable
internal fun QuestionBody(
    question: QuizQuestion,
    leading: (@Composable () -> Unit)?,
) {
    val secondary = MaterialTheme.colorScheme.secondary
    val lineHeight = lineHeightOf(LexemeStyle.BodyMBold)
    Column {
        question.debugHeader?.let { Text(text = it) }
        Text(
            text = "${question.header}:",
            style = LexemeStyle.BodyM.copy(color = secondary),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LEADING_GAP)) {
            if (leading != null) {
                Box(
                    modifier = Modifier
                        .alignByBaseline()
                        .heightIn(max = lineHeight)
                        .widthIn(max = LEADING_MAX_WIDTH),
                ) {
                    leading()
                }
            }
            Text(
                text = question.value,
                style = LexemeStyle.BodyMBold.copy(color = secondary),
                modifier = Modifier
                    .alignByBaseline()
                    .weight(1f, fill = false),
            )
        }
    }
}

/**
 * Высота строки стиля в dp: явный `lineHeight` в sp, а если тема его не
 * задаёт (так у стилей проекта) или задаёт в em — кегль с обычным
 * межстрочным коэффициентом. В dp переводится только sp: величина
 * растёт вместе с системным шрифтом.
 */
@Composable
private fun lineHeightOf(style: TextStyle): Dp = with(LocalDensity.current) {
    when {
        style.lineHeight.isSp -> style.lineHeight.toDp()
        style.fontSize.isSp -> (style.fontSize * LINE_HEIGHT_FACTOR).toDp()
        else -> LEADING_FALLBACK_HEIGHT
    }
}

private const val LINE_HEIGHT_FACTOR = 1.4f
private val LEADING_FALLBACK_HEIGHT = 24.dp
private val LEADING_GAP = 6.dp

/** Примерно треть пузыря: длинная пользовательская опция режется самим чипом. */
private val LEADING_MAX_WIDTH = 120.dp
