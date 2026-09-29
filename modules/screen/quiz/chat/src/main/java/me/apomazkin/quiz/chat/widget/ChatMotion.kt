package me.apomazkin.quiz.chat.widget

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp

/**
 * Одна кривая на все движения чата: сдвиг соседей, въезд нового пузыря,
 * превращение кнопки в пузырь, спуск аватара, разворот поля ввода. Tween
 * с плавным разгоном и торможением: пружина стартовала резко и читалась
 * как рывок.
 *
 * Длительность намеренно замедленная (решение юзера 2026-09-29): анимации
 * проверяются глазами на магазинной сборке; рабочее значение — 350 мс.
 */
internal object ChatMotion {
    const val DURATION_MS = 1500
    val EASING: Easing = EaseInOutCubic

    /** Зазор между элементами ленты; участвует в дистанции спуска аватара по цепочке. */
    val ITEM_SPACING = 4.dp

    /** Широкая кнопка «Начать»: высота и доля ширины ленты; пузырь стартует из неё. */
    val START_BUTTON_HEIGHT = 48.dp
    const val START_BUTTON_WIDTH_FRACTION = 0.6f
    val START_BUTTON_CORNER = 24.dp

    /**
     * Фазы превращения кнопки в пузырь на линейном времени 0..1: сначала
     * гаснет соседний чип, потом пузырь перекрашивается; геометрия
     * (сдвиг, сжатие) — весь интервал. Внутри окна — [EASING].
     */
    const val GHOST_FADE_END = 0.45f
    const val RECOLOR_START = 0.3f

    /** Доля окна [from, to] на линейном времени [t], сглаженная [EASING]. */
    fun window(t: Float, from: Float, to: Float): Float =
        EASING.transform(((t - from) / (to - from)).coerceIn(0f, 1f))
}
