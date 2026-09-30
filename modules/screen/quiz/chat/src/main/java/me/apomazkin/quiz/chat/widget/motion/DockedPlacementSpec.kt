package me.apomazkin.quiz.chat.widget.motion

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.ui.unit.IntOffset
import me.apomazkin.quiz.chat.widget.ChatMotion
import kotlin.math.abs

/**
 * Доля пути соседа при доле времени [eased] (после кривой) — стыковка с
 * новым элементом: новый едет на «добавка [extraPx] + сдвиг [distance]»,
 * сосед стоит, пока новый проходит добавку, и дальше остаток пути у них
 * общий. Сдвиг 0 — сосед на месте (доля 1).
 */
internal fun dockedFraction(extraPx: Float, distance: Float, eased: Float): Float {
    val d = abs(distance)
    if (d == 0f) return 1f
    return (((extraPx + d) * eased - extraPx) / d).coerceIn(0f, 1f)
}

/**
 * Placement соседей при въезде нового элемента «со стыковкой» — по
 * [dockedFraction]. Сдвиг известен только самой анимации
 * (initial → target), поэтому кривая считается здесь, а не задаётся
 * tween-ом с задержкой (у того две фазы со своим разгоном — читалось как
 * пинок).
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

        private fun docked(from: Float, to: Float, e: Float): Float =
            from + (to - from) * dockedFraction(extraPx, to - from, e)
    }
}
