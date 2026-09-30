package me.apomazkin.quiz.chat.widget

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs

/**
 * Placement соседей при въезде нового элемента «со стыковкой». Новый
 * элемент едет из-под поля ввода одной кривой [ChatMotion.EASING] на
 * дистанцию «добавка [extraPx] + сдвиг D»; соседи стоят, пока он не
 * пройдёт добавку (не встанет к ним в зазор), и дальше едут с ним как
 * одно целое: остаток пути соседа = min(D, остаток пути нового). Сдвиг D
 * известен только самой анимации (initial → target), поэтому кривая
 * считается здесь, а не задаётся tween-ом с задержкой (у того две фазы
 * со своим разгоном — на рабочей скорости читалось как пинок).
 */
internal class DockedPlacementSpec(
    private val durationMs: Int,
    private val extraPx: Float,
) : FiniteAnimationSpec<IntOffset> {

    // IntOffset всегда векторизуется в AnimationVector2D.
    @Suppress("UNCHECKED_CAST")
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<IntOffset, V>,
    ): VectorizedFiniteAnimationSpec<V> =
        Vectorized(durationMs, extraPx) as VectorizedFiniteAnimationSpec<V>

    private class Vectorized(
        durationMs: Int,
        private val extraPx: Float,
    ) : VectorizedFiniteAnimationSpec<AnimationVector2D> {

        private val durationNanos = durationMs * 1_000_000L

        override fun getDurationNanos(
            initialValue: AnimationVector2D,
            targetValue: AnimationVector2D,
            initialVelocity: AnimationVector2D,
        ): Long = durationNanos

        override fun getValueFromNanos(
            playTimeNanos: Long,
            initialValue: AnimationVector2D,
            targetValue: AnimationVector2D,
            initialVelocity: AnimationVector2D,
        ): AnimationVector2D {
            val t = (playTimeNanos.toDouble() / durationNanos).toFloat().coerceIn(0f, 1f)
            val e = ChatMotion.EASING.transform(t)
            return AnimationVector2D(
                docked(initialValue.v1, targetValue.v1, e),
                docked(initialValue.v2, targetValue.v2, e),
            )
        }

        // Скорость нужна только для сцепки прерванных анимаций; вставок в
        // окно анимации нет (пауза бота длиннее), нулевой вектор достаточен.
        override fun getVelocityFromNanos(
            playTimeNanos: Long,
            initialValue: AnimationVector2D,
            targetValue: AnimationVector2D,
            initialVelocity: AnimationVector2D,
        ): AnimationVector2D = AnimationVector2D(0f, 0f)

        /** Сосед стоит, пока новый элемент проходит добавку, затем едет с ним. */
        private fun docked(from: Float, to: Float, e: Float): Float {
            val d = abs(to - from)
            if (d == 0f) return to
            val travelled = (extraPx + d) * e - extraPx
            val fraction = (travelled / d).coerceIn(0f, 1f)
            return from + (to - from) * fraction
        }
    }
}
